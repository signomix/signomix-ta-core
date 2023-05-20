package com.signomix.core.domain;

import java.util.List;

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

    @ConfigProperty(name = "signomix.app.key", defaultValue = "not_configured")
    String appKey;
    @ConfigProperty(name = "signomix.auth.host", defaultValue = "not_configured")
    String authHost;

    @Inject
    @DataSource("iot")
    AgroalDataSource deviceDataSource;

    IotDatabaseIface iotDao;

    @Inject
    MessageService messageService;

    @Inject
    UserPort userPort;

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

    public List<Device> getUserDevices(User user, boolean withStatus) throws ServiceException {
        try {
            return iotDao.getUserDevices(user, withStatus);
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
        try {
            iotDao.updateDevice(user, device);
            sendNotification(device, "UPDATED");
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public void createDevice(User user, Device device) throws ServiceException {
        try {
            List<Device> userDevices=iotDao.getUserDevices(user, false);
            int deviceCount=userDevices.size();
            long maxDevices=iotDao.getParameterValue("devicesLimit", user.type);
            if (deviceCount==maxDevices){
                throw new ServiceException("User has reached maximum number of devices: "+maxDevices);
            }
            iotDao.createDevice(user, device);
            sendNotification(device, "CREATED");
            if((deviceCount+1)==maxDevices){
                sendNotification(device, "DEVICES_LIMIT");
            }
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public void checkDevices() throws ServiceException {
        List<Device> devices;
        try {
            devices = iotDao.getInactiveDevices();
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(), e);
        }
        // TODO: dla każdego urządzenia zweryfikować, czy jest konieczne powiadomienie o
        // braku aktywności
        // wygenerować zdarzenie DEVICE_LOST
        // String[] deviceUsers;
        for (Device device : devices) {
            try {
                // iotDao.getDevice(device.getEUI(), true);
                if (System.currentTimeMillis() - device.getLastSeen() > device.getTransmissionInterval() * 2) {
                    if (device.getAlertStatus() < Device.ALERT_FAILURE) {
                        // send notification
                        iotDao.updateDeviceStatus(device.getEUI(), device.getTransmissionInterval(), device.getState(),
                                Device.ALERT_FAILURE);
                        sendNotification(device, "ALERT_FAILURE");
                        /*
                         * sendNotification("ALERT_FAILURE", device.getUserID(), device.getEUI());
                         * deviceUsers = device.getTeam().split(",");
                         * for (String deviceUser : deviceUsers) {
                         * if (deviceUser.length() > 0) {
                         * sendNotification("ALERT_FAILURE", deviceUser, device.getEUI());
                         * }
                         * }
                         * deviceUsers = device.getAdministrators().split(",");
                         * for (String deviceUser : deviceUsers) {
                         * if (deviceUser.length() > 0) {
                         * sendNotification("ALERT_FAILURE", deviceUser, device.getEUI());
                         * }
                         * }
                         */
                    }
                }
            } catch (IotDatabaseException e) {
                LOG.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /*
     * private void sendNotification(String type, String userId, String eui) {
     * String messageText = "";
     * switch (type) {
     * case "ALERT_FAILURE":
     * messageText = "Device is not sending data. Check if it is working properly.";
     * break;
     * }
     * IotEvent event = new IotEvent();
     * event.setGeneralMessage(messageText);
     * event.setOrigin(userId + "\t" + eui);
     * try {
     * iotDao.addAlert(event);
     * } catch (IotDatabaseException e) {
     * LOG.error(e.getMessage());
     * }
     * messageService.sendNotification(event);
     * }
     */

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

}
