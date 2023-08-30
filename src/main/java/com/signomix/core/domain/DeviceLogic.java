package com.signomix.core.domain;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.IotDatabaseDao;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.event.IotEvent;
import com.signomix.common.iot.Device;
import com.signomix.core.adapter.out.MessageService;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.application.port.in.DashboardPort;
import com.signomix.core.application.port.in.UserPort;
import com.signomix.core.application.port.out.DeviceChecker;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;

/**
 * 
 * @author Grzegorz
 */
@ApplicationScoped
public class DeviceLogic {
    private static final Logger LOG = Logger.getLogger(DeviceLogic.class);
    private static AtomicLong euiSeed = new AtomicLong(System.currentTimeMillis());

    /* @ConfigProperty(name = "signomix.app.key", defaultValue = "not_configured")
    String appKey; */
    @ConfigProperty(name = "signomix.auth.host", defaultValue = "not_configured")
    String authHost;
    @ConfigProperty(name = "signomix.device.eui.prefix", defaultValue = "S-")
    String deviceEuiPrefix;
    @ConfigProperty(name = "signomix.exception.api.unauthorized", defaultValue = "")
    String exceptionApiUnauthorized;

    @Inject
    @DataSource("iot")
    AgroalDataSource deviceDataSource;

    IotDatabaseIface iotDao;

    @Inject
    MessageService messageService;

    @Inject
    UserPort userPort;

    @Inject
    UserLogic userLogic;

    @Inject
    DashboardPort dashboardPort;

    @Inject
    Logger logger;

    long defaultOrganizationId;

    void onStart(@Observes StartupEvent ev) {
        iotDao = new IotDatabaseDao();
        iotDao.setDatasource(deviceDataSource);
        try {
            defaultOrganizationId = iotDao.getParameterValue("system.default.organization", User.ANY);
        } catch (IotDatabaseException e) {
            logger.error("Unable to get default organization id: " + e.getMessage());
        }
    }

    // TODO: add organizationId to all methods
    public Device getDevice(User user, String eui, boolean withStatus) throws ServiceException {
        try {
            Device device = iotDao.getDevice(eui, withStatus);
            if (userLogic.hasObjectAccess(user, false, defaultOrganizationId, device)) {
                return device;
            } else {
                throw new ServiceException(exceptionApiUnauthorized);
            }
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public List<Device> getUserDevices(User user, boolean withStatus, Integer limit, Integer offset)
            throws ServiceException {
        try {
            if (user.organization == defaultOrganizationId) {
                return iotDao.getUserDevices(user, withStatus, limit, offset);
            } else {
                return iotDao.getOrganizationDevices(user.organization, withStatus, limit, offset);
            }
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public void deleteDevice(User user, String eui) throws ServiceException {
        Device device = getDevice(user, eui, false);
        if (null == device) {
            throw new ServiceException("Device not found");
        }
        try {
            if (userLogic.hasObjectAccess(user, true, defaultOrganizationId, device)) {
                iotDao.deleteDevice(user, eui);
                sendNotification(device, "DELETED");
            } else {
                throw new ServiceException(exceptionApiUnauthorized);
            }
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public void updateDevice(User user, Device device) throws ServiceException {
        Device updated = getDevice(user, device.getEUI(), false);
        if (null == updated) {
            throw new ServiceException("Device not found");
        }
        try {
            if (userLogic.hasObjectAccess(user, true, defaultOrganizationId, device)) {
                iotDao.updateDevice(user, device);
                if (!updated.getChannelsAsString().equals(device.getChannelsAsString())) {
                    iotDao.clearDeviceData(device.getEUI());
                    iotDao.updateDeviceChannels(device.getEUI(), device.getChannelsAsString());
                }
                sendNotification(device, "UPDATED");
            } else {
                throw new ServiceException(exceptionApiUnauthorized);
            }
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public void createDevice(User user, Device device) throws ServiceException {
        try {
            List<Device> userDevices = iotDao.getUserDevices(user, false, null, null);
            int deviceCount = userDevices.size();
            long maxDevices = iotDao.getParameterValue("devicesLimit", user.type);
            if (deviceCount == maxDevices) {
                throw new ServiceException("User has reached maximum number of devices: " + maxDevices);
            }
            device.setEUI(removeNonAlphanumeric(device.getEUI()));
            if (device.getEUI().isEmpty()) {
                device.setEUI(createEui(deviceEuiPrefix));
            }
            logger.info("Creating device: " + device.getEUI());
            device.setOrganizationId(user.organization);
            iotDao.createDevice(user, device);
            iotDao.updateDeviceChannels(device.getEUI(), device.getChannelsAsString());
            iotDao.updateDeviceStatus(device.getEUI(), device.getTransmissionInterval(), 0.0, Device.ALERT_UNKNOWN);
            dashboardPort.addDefaultDashboard(device);
            sendNotification(device, "CREATED");
            if ((deviceCount + 1) == maxDevices) {
                sendNotification(device, "DEVICES_LIMIT");
            }
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    private String removeNonAlphanumeric(String str) {
        return str.trim().replaceAll("[^a-zA-Z0-9]", "");
    }

    /**
     * Check if devices are sending data. If not, send notification.
     * 
     * @throws ServiceException
     */
    public void checkDevices() throws ServiceException {
        logger.info("Checking devices...");
        Executors.newSingleThreadExecutor().execute(new DeviceChecker(iotDao, messageService));
    }

    private void sendNotification(Device device, String type) {
        String messageText = "";
        switch (type) {
            case "ALERT_FAILURE":
                messageText = "Device " + device.getEUI() + " is not sending data. Check if it is working properly.";
                break;
            case "DELETED":
                messageText = "Device " + device.getEUI() + " has been deleted.";
                break;
            case "UPDATED":
                messageText = "Device " + device.getEUI() + " has been updated.";
                break;
            case "CREATED":
                messageText = "Device " + device.getEUI() + " has been created.";
                break;
            case "DEVICES_LIMIT":
                messageText = "Maximum number of devices has been reached.";
                break;
            default:
                LOG.error("Unknown notification type: " + type);
                return;
        }
        // device owner
        IotEvent event = new IotEvent();
        event.setGeneralMessage(messageText);
        event.setOrigin(device.getUserID() + "\t" + device.getEUI());
        try {
            iotDao.addAlert(event);
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
        }
        messageService.sendNotification(event);

        String[] deviceUsers;
        // device team
        deviceUsers = device.getTeam().split(",");
        for (String deviceUser : deviceUsers) {
            if (deviceUser.length() > 0) {
                event = new IotEvent();
                event.setGeneralMessage(messageText);
                event.setOrigin(deviceUser + "\t" + device.getEUI());
                try {
                    iotDao.addAlert(event);
                } catch (IotDatabaseException e) {
                    LOG.error(e.getMessage());
                }
                messageService.sendNotification(event);
            }
        }
        // device administrators
        deviceUsers = device.getAdministrators().split(",");
        for (String deviceUser : deviceUsers) {
            if (deviceUser.length() > 0) {
                event = new IotEvent();
                event.setGeneralMessage(messageText);
                event.setOrigin(deviceUser + "\t" + device.getEUI());
                try {
                    iotDao.addAlert(event);
                } catch (IotDatabaseException e) {
                    LOG.error(e.getMessage());
                }
                messageService.sendNotification(event);
            }
        }
    }

    public String createEui(String prefix) {
        String eui = Long.toHexString(euiSeed.getAndIncrement());
        StringBuilder tmp = new StringBuilder(prefix).append(eui.substring(0, 2));
        for (int i = 2; i < eui.length() - 1; i = i + 2) {
            tmp.append("-").append(eui.substring(i, i + 2));
        }
        return tmp.toString();
    }

    /**
     * Check if user has access to device.
     * 
     * @param user        User
     * @param device      Device
     * @param writeAccess true if write access is required
     * @return true if user has access to device
     */
    @Deprecated
    private boolean hasAccessToDevice(User user, Device device, boolean writeAccess) {
        // TODO: Organization access
        if (user.type == User.OWNER) { // platform administator
            return true;
        }
        if (device.getUserID().equals(user.uid)) {
            return true;
        }
        if (device.getAdministrators().contains("," + user.uid + ",")) {
            return true;
        }
        if (!writeAccess) {
            if (device.getTeam().contains("," + user.uid + ",")) {
                return true;
            }
            if (device.getOrganizationId() != defaultOrganizationId
                    && user.organization == device.getOrganizationId()
                    && user.type == User.ADMIN) {
                return true;
            }
        } else {
            if (device.getOrganizationId() != defaultOrganizationId
                    && user.organization == device.getOrganizationId()) {
                return true;
            }
        }
        return false;
    }
}
