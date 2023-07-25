package com.signomix.core.domain;

import java.util.List;
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

    @ConfigProperty(name = "signomix.app.key", defaultValue = "not_configured")
    String appKey;
    @ConfigProperty(name = "signomix.auth.host", defaultValue = "not_configured")
    String authHost;
    @ConfigProperty(name = "signomix.device.eui.prefix", defaultValue = "S-")
    String deviceEuiPrefix;

    @Inject
    @DataSource("iot")
    AgroalDataSource deviceDataSource;

    IotDatabaseIface iotDao;

    @Inject
    MessageService messageService;

    @Inject
    UserPort userPort;

    @Inject
    DashboardPort dashboardPort;

    void onStart(@Observes StartupEvent ev) {
        iotDao = new IotDatabaseDao();
        iotDao.setDatasource(deviceDataSource);
    }

    // TODO: add organizationId to all methods
    public Device getDevice(User user, String eui, boolean withStatus) throws ServiceException {
        try {
            return iotDao.getDevice(user, eui, true, withStatus);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    /*
     * public List<Device> getUserDevices(User user, boolean withStatus) throws
     * ServiceException {
     * try {
     * return iotDao.getUserDevices(user, withStatus, null, null);
     * } catch (IotDatabaseException e) {
     * throw new ServiceException(e.getMessage(), e);
     * }
     * }
     */

    public List<Device> getUserDevices(User user, boolean withStatus, Integer limit, Integer offset)
            throws ServiceException {
        try {
            return iotDao.getUserDevices(user, withStatus, limit, offset);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public void deleteDevice(User user, String eui) throws ServiceException {
        Device device = getDevice(user, eui, false);
        try {
            iotDao.deleteDevice(user, eui);
            sendNotification(device, "DELETED");
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
            iotDao.updateDevice(user, device);
            if (!updated.getChannelsAsString().equals(device.getChannelsAsString())) {
                iotDao.clearDeviceData(device.getEUI());
                iotDao.updateDeviceChannels(device.getEUI(), device.getChannelsAsString());
            }
            sendNotification(device, "UPDATED");
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
        str = str.replaceAll("[^a-zA-Z0-9]", "");
        return str;
    }

    /**
     * Check if devices are sending data. If not, send notification.
     * 
     * @throws ServiceException
     */
    public void checkDevices() throws ServiceException {
        List<Device> devices;
        try {
            devices = iotDao.getInactiveDevices();
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(), e);
        }
        for (Device device : devices) {
            try {
                if (device.getTransmissionInterval() > 0 && (System.currentTimeMillis()
                        - device.getLastSeen() > device.getTransmissionInterval() * 1.5)) {
                    if (device.getAlertStatus() < Device.ALERT_FAILURE) {
                        // send notification
                        iotDao.updateDeviceStatus(device.getEUI(), device.getTransmissionInterval(), device.getState(),
                                Device.ALERT_FAILURE);
                        sendNotification(device, "ALERT_FAILURE");
                    }
                }
            } catch (IotDatabaseException e) {
                LOG.error(e.getMessage());
                e.printStackTrace();
            }
        }
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

}
