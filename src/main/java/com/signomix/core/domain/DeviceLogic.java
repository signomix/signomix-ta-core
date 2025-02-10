package com.signomix.core.domain;

import java.util.List;
import java.util.concurrent.Executors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import com.signomix.common.Tag;
import com.signomix.common.Tenant;
import com.signomix.common.User;
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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * 
 * @author Grzegorz
 */
@ApplicationScoped
public class DeviceLogic {
    private static final Logger LOG = Logger.getLogger(DeviceLogic.class);

    /*
     * @ConfigProperty(name = "signomix.app.key", defaultValue = "not_configured")
     * String appKey;
     */
    @ConfigProperty(name = "signomix.auth.host", defaultValue = "not_configured")
    String authHost;
    @ConfigProperty(name = "signomix.device.eui.prefix", defaultValue = "S-")
    String deviceEuiPrefix;
    @ConfigProperty(name = "signomix.exception.api.unauthorized", defaultValue = "")
    String exceptionApiUnauthorized;

/*     @Inject
    @DataSource("iot")
    AgroalDataSource deviceDataSource; */

    @Inject
    @DataSource("oltp")
    AgroalDataSource tsDs;

    IotDatabaseIface iotDao;

    @Inject
    MessageService messageService;

    @Inject
    UserPort userPort;

    @Inject
    UserLogic userLogic;

    @Inject
    OrganizationLogic organizationLogic;

    @Inject
    DashboardPort dashboardPort;

    @Inject
    Logger logger;

    @Inject
    EuiGenerator euiGenerator;

    @Inject
    @Channel("device-removed")
    Emitter<String> deviceRemovalEmitter;
    @Inject
    @Channel("device-created")
    Emitter<String> deviceCreationEmitter;
    @Inject
    @Channel("device-updated")
    Emitter<String> deviceModificationEmitter;

    long defaultOrganizationId;

    @ConfigProperty(name = "signomix.database.type")
    String databaseType;

    void onStart(@Observes StartupEvent ev) {
        /* if ("h2".equalsIgnoreCase(databaseType)) {
            iotDao = new IotDatabaseDao();
            iotDao.setDatasource(deviceDataSource);
            defaultOrganizationId = 0;
        } else  */
         if ("postgresql".equalsIgnoreCase(databaseType)) {
            iotDao = new com.signomix.common.tsdb.IotDatabaseDao();
            iotDao.setDatasource(tsDs);
            defaultOrganizationId = 1;
        } else {
            logger.error("Unknown database type: " + databaseType);
        }

        /*
         * try {
         * defaultOrganizationId =
         * iotDao.getParameterValue("system.default.organization", User.ANY);
         * } catch (IotDatabaseException e) {
         * logger.error("Unable to get default organization id: " + e.getMessage());
         * }
         */
    }

    private IotDatabaseIface getIotDao() {
        if (iotDao != null) {
            return iotDao;
        } else {
            iotDao = new com.signomix.common.tsdb.IotDatabaseDao();
            iotDao.setDatasource(tsDs);
            // iotDao = new IotDatabaseDao();
            // iotDao.setDatasource(deviceDataSource);
            return iotDao;
        }
    }

    // TODO: add organizationId to all methods
    public Device getDevice(User user, String eui, boolean withStatus) throws ServiceException {
        return getDevice(user, eui, withStatus, true);
    }

    public Device getDevice(User user, String eui, boolean withStatus, boolean withTags) throws ServiceException {
        try {
            Device device = iotDao.getDevice(eui, withStatus);
            if (userLogic.hasObjectAccess(user, false, defaultOrganizationId, device)) {
                if (withTags) {
                    List<Tag> tags = iotDao.getDeviceTags(device.getEUI());
                    String tagString = "";
                    for (Tag tag : tags) {
                        tagString += tag.name + ":" + tag.value + ";";
                    }
                    if (tagString.length() > 0 && tagString.charAt(tagString.length() - 1) == ';') {
                        tagString = tagString.substring(0, tagString.length() - 1);
                    }
                    device.setTags(tagString);
                }
                return device;
            } else {
                throw new ServiceException(exceptionApiUnauthorized);
            }
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public List<Device> getDevices(User user, Boolean withStatus, Long organizationId, Integer context, String path,
            String search, Integer limit, Integer offset) {
        try {
            Integer searchOffset = null;
            Integer searchLimit = null;
            searchLimit = limit == null ? 10000 : limit;
            searchOffset = offset == null ? 0 : offset;
            String searchPath = path;
            if (searchPath != null) {
                searchPath = searchPath.replace(".ALL", ".*");
            }
            boolean searchStatus = withStatus == null ? false : withStatus;
            if (organizationId == null || organizationId == defaultOrganizationId) {
                return iotDao.getUserDevices(user, searchStatus, searchLimit, searchOffset, search);
            } else {
                if (user.type == User.MANAGING_ADMIN && context != null && context > 0) {
                    return iotDao.getDevicesByPath(user.uid, user.organization, context, searchPath, search,
                            searchLimit,
                            searchOffset);
                } else if (user.tenant > 0) {
                    return iotDao.getDevicesByPath(user.uid, user.organization, user.tenant, user.path, search,
                            searchLimit,
                            searchOffset);
                } else {
                    return iotDao.getOrganizationDevices(organizationId, searchStatus, searchLimit, searchOffset, path);
                }
            }
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /*
     * public List<Device> getUserDevices(User user, boolean withStatus, Integer
     * limit, Integer offset,
     * String searchString)
     * throws ServiceException {
     * try {
     * Integer searchOffset = null;
     * Integer searchLimit = null;
     * searchLimit = limit == null ? 10000 : limit;
     * searchOffset = offset == null ? 0 : offset;
     * String[] searchParams;
     * if (searchString != null) {
     * searchParams = searchString.split(":");
     * } else {
     * searchParams = new String[0];
     * }
     * if (user.organization == defaultOrganizationId ||
     * user.uid.equalsIgnoreCase("public")) {
     * // if (searchParams.length == 3) {
     * // return iotDao.getUserDevicesByTag(user,searchParams[1], searchParams[2],
     * // searchLimit, searchOffset);
     * // } else {
     * return iotDao.getUserDevices(user, withStatus, searchLimit, searchOffset,
     * searchString);
     * // }
     * } else {
     * return iotDao.getOrganizationDevices(user.organization, withStatus,
     * searchLimit, searchOffset,
     * searchString);
     * }
     * } catch (IotDatabaseException e) {
     * throw new ServiceException(e.getMessage(), e);
     * }
     * }
     * 
     * public List<Device> getDevicesByPath(User user, Long organizationId, boolean
     * withStatus, Integer limit,
     * Integer offset,
     * String path) throws ServiceException {
     * try {
     * Integer searchOffset = null;
     * Integer searchLimit = null;
     * String searchPath = path;
     * searchLimit = limit == null ? 10000 : limit;
     * searchOffset = offset == null ? 0 : offset;
     * if (path == null) {
     * searchPath = user.path;
     * }
     * if (searchPath != null) {
     * searchPath = searchPath.replace("ALL", "*");
     * }
     * if (user.tenant > 0) {
     * return iotDao.getDevicesByPath(user.uid, user.tenant, searchPath,
     * searchLimit, searchOffset);
     * } else {
     * return iotDao.getOrganizationDevices(organizationId, withStatus, searchLimit,
     * searchOffset, path);
     * }
     * } catch (IotDatabaseException e) {
     * throw new ServiceException(e.getMessage(), e);
     * }
     * }
     */

    public void deleteDevice(User user, String eui) throws ServiceException {
        Device device = getDevice(user, eui, false);
        if (null == device) {
            throw new ServiceException("Device not found");
        }
        try {
            if (userLogic.hasObjectAccess(user, true, defaultOrganizationId, device)) {
                List<Tag> tags = iotDao.getDeviceTags(eui);
                boolean isProtected = false;
                for (Tag tag : tags) {
                    if (tag.name.equals("protected") && tag.value.equalsIgnoreCase("true")) {
                        isProtected = true;
                        break;
                    }
                }
                if (!isProtected) {
                    iotDao.deleteDevice(user, eui);
                    iotDao.removeAllDeviceTags(user, eui);
                    iotDao.clearDeviceData(eui);
                } else {
                    device.setActive(false);
                    iotDao.updateDevice(user, device);
                }
                /*
                 * String[] tags = device.getTags().split(",");
                 * for (String tag : tags) {
                 * String[] tagParts = tag.split(":");
                 * if (tagParts.length > 0) {
                 * iotDao.removeDeviceTag(user, device.getEUI(), tagParts[0]);
                 * }
                 * }
                 */
                deviceRemovalEmitter.send(eui);
                sendNotification(device, "DELETED");
            } else {
                throw new ServiceException(exceptionApiUnauthorized);
            }
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public void updateDevice(User user, String eui, Device device) throws ServiceException {
        Device updated = getDevice(user, eui, false);
        if (null == updated) {
            throw new ServiceException("Device not found");
        }
        try {
            if (userLogic.hasObjectAccess(user, true, defaultOrganizationId, device)) {
                if (eui != null && !eui.toUpperCase().equals(device.getEUI().toUpperCase())) {
                    iotDao.changeDeviceEui(eui, device.getEUI());
                }
                if (device.getOrganizationId() == defaultOrganizationId) {
                    device.setPath("");
                }
                iotDao.updateDevice(user, device);
                if (!updated.getChannelsAsString().equals(device.getChannelsAsString())) {
                    iotDao.clearDeviceData(device.getEUI());
                    iotDao.updateDeviceChannels(device.getEUI(), device.getChannelsAsString());
                }
                String[] tags;
                /*
                 * tags = updated.getTags().split(";");
                 * for (String tag : tags) {
                 * String[] tagParts = tag.split(":");
                 * if (tagParts.length > 0) {
                 * iotDao.removeDeviceTag(user, device.getEUI(), tagParts[0]);
                 * }
                 * }
                 */
                iotDao.removeAllDeviceTags(user, device.getEUI());
                tags = device.getTags().split(";");
                for (String tag : tags) {
                    if (tag.length() > 0) {
                        String[] tagParts = tag.split(":");
                        if (tagParts.length > 1) {
                            iotDao.addDeviceTag(user, device.getEUI(), tagParts[0], tagParts[1]);
                        }
                    }
                }
                if (eui != null && !eui.toUpperCase().equals(device.getEUI().toUpperCase())) {
                    deviceModificationEmitter.send(eui);
                    sendNotification(updated, "DELETED");
                }
                deviceModificationEmitter.send(device.getEUI());
                sendNotification(device, "UPDATED");
            } else {
                throw new ServiceException(exceptionApiUnauthorized);
            }
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public void createDevice(User user, Device device) throws ServiceException {
        try {
            List<Device> userDevices = getIotDao().getUserDevices(user, false, null, null, null);
            int deviceCount = userDevices.size();
            long maxDevices = iotDao.getParameterValue("devicesLimit", user.type);
            if (maxDevices != -1 && deviceCount >= maxDevices && user.type != User.SUPERUSER) {
                throw new ServiceException("User has reached maximum number of devices: " + maxDevices);
            }

            // remove all non-alphanumeric characters from EUI
            device.setEUI(removeNonAlphanumeric(device.getEUI()));

            if (device.getEUI() == null || device.getEUI().isEmpty() || device.getEUI().toLowerCase().equals("new")) {
                device.setEUI(euiGenerator.createEui(deviceEuiPrefix));
            }
            if (device.getKey() == null || device.getKey().isEmpty()) {
                device.setKey(euiGenerator.createEui(""));
            }
            logger.info("Creating device: " + device.getEUI());
            device.setOrganizationId(user.organization);
            device.setPath(verifyDevicePath(user, device));
            iotDao.createDevice(user, device);
            iotDao.updateDeviceChannels(device.getEUI(), device.getChannelsAsString());
            iotDao.updateDeviceStatus(device.getEUI(), device.getTransmissionInterval(), 0.0, Device.ALERT_UNKNOWN);
            String[] tags = device.getTags().split(";");
            for (String tag : tags) {
                if (tag.length() > 0) {
                    String[] tagParts = tag.split(":");
                    if (tagParts.length > 1) {
                        iotDao.addDeviceTag(user, device.getEUI(), tagParts[0], tagParts[1]);
                    }
                }
            }
            deviceCreationEmitter.send(device.getEUI());
            if (device.isDashboard()) {
                dashboardPort.addDefaultDashboard(device);
            }
            sendNotification(device, "CREATED");
            if ((deviceCount + 1) == maxDevices) {
                sendNotification(device, "DEVICES_LIMIT");
            }
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    private String verifyDevicePath(User user, Device device) {
        // default organization path is empty
        if (device.getOrganizationId() == defaultOrganizationId) {
            return "";
        }
        // not default organization path in actual implementation is empty
        if (user.tenant == 0) {
            // organization structure (paths) is not implemented yet
            if (user.type == User.SUPERUSER) {
                // superuser can set path related to tenant or organization
                List<Tenant> tenants = organizationLogic.getTenants(user, device.getOrganizationId(), 1000, 0);
                if (tenants != null && tenants.size() > 0) {
                    for (Tenant tenant : tenants) {
                        if (device.getPath().startsWith(tenant.root + ".") || device.getPath().equals(tenant.root)) {
                            return device.getPath();
                        }
                    }
                } else {
                    // no tenants, set path to organization root
                }
                // if path is not related to any tenant, set path to organization root
                // return "org_" + device.getOrganizationId();
                return "";
            } else {
                // return "org_" + device.getOrganizationId();
                return "";
            }
        }
        // tenant path
        if (user.tenant > 0) {
            Tenant tenant = organizationLogic.getTenant(user, user.tenant);
            String root = tenant.root;
            String path = device.getPath();
            if (path.startsWith(root + ".")) {
                return path;
            } else {
                return root;
            }
        }
        return "";
    }

    /**
     * Removes all non-alphanumeric characters from a given string.
     *
     * @param str the string to remove non-alphanumeric characters from
     * @return the string with non-alphanumeric characters removed
     */
    private String removeNonAlphanumeric(String str) {
        return str.trim().replaceAll("[^a-zA-Z0-9]", "");
    }

    /**
     * Check if devices are sending data. If not, send notification.
     * 
     * @throws ServiceException
     */
    public void checkDevices(boolean paid) throws ServiceException {
        logger.info("Checking devices...");
        Executors.newSingleThreadExecutor().execute(new DeviceChecker(iotDao, messageService, paid));
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

    public void deleteDeviceData(User user, String eui) throws ServiceException {
        Device device = getDevice(user, eui, false);
        if (null == device) {
            throw new ServiceException("Device not found");
        }
        try {
            if (userLogic.hasObjectAccess(user, true, defaultOrganizationId, device)) {
                List<Tag> tags = iotDao.getDeviceTags(eui);
                boolean isProtected = false;
                for (Tag tag : tags) {
                    if (tag.name.equals("protected") && tag.value.equalsIgnoreCase("true")) {
                        isProtected = true;
                        break;
                    }
                }
                if (!isProtected) {
                    iotDao.clearDeviceData(eui);
                }
                //deviceRemovalEmitter.send(eui);
                //sendNotification(device, "DELETED");
            } else {
                throw new ServiceException(exceptionApiUnauthorized);
            }
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }
}
