package com.signomix.core.application.port.out;

import java.util.HashSet;
import java.util.List;

import org.jboss.logging.Logger;

import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.event.IotEvent;
import com.signomix.common.iot.Device;
import com.signomix.core.adapter.out.MessageService;
import com.signomix.core.application.exception.ServiceException;

public class DeviceChecker implements Runnable {

    Logger logger = Logger.getLogger(DeviceChecker.class);

    IotDatabaseIface iotDao;
    MessageService messageService;

    public DeviceChecker(IotDatabaseIface iotDao, MessageService messageService) {
        this.iotDao = iotDao;
        this.messageService = messageService;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        logger.info("DeviceChecker started");
        List<Device> devices;
        try {
            devices = iotDao.getDevicesRequiringAlert();
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(), e);
        }
        // TODO: optimize this
        // divide devices into groups then udpate all in one query for each group
        // and send notifications to each group
        long counter = 0;
        long delta;
        for (Device device : devices) {
            try {
                if (device.getAlertStatus() >= Device.ALERT_FAILURE) {
                    continue;
                }
                delta = System.currentTimeMillis() - device.getLastSeen();
                // if (System.currentTimeMillis() - device.getLastSeen() < device.getTransmissionInterval() * 2) {
                //     logger.info("DeviceOK: " + device.getEUI() + " " + device.getAlertStatus());
                //     continue;
                // }
                logger.debug("DeviceFailure: " + device.getEUI() + " " + device.getAlertStatus() + " " + delta + ">"
                        + device.getTransmissionInterval());
                iotDao.updateDeviceStatus(device.getEUI(), device.getTransmissionInterval(), device.getState(),
                        Device.ALERT_FAILURE);
                counter++;
                sendNotification(device, "ALERT_FAILURE");
            } catch (IotDatabaseException e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            }
        }
        //iotDao.commit();
        long end = System.currentTimeMillis();
        logger.info("DeviceChecker finished: " + counter + "/" + devices.size() + " devices, " + (end - start) + " ms");
    }

    @Deprecated
    private void runPrevVersion() {
        long start = System.currentTimeMillis();
        logger.info("DeviceChecker started");
        List<Device> devices;
        try {
            devices = iotDao.getInactiveDevices();
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(), e);
        }
        // TODO: optimize this
        // divide devices into groups then udpate all in one query for each group
        // and send notifications to each group
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
                logger.error(e.getMessage());
                e.printStackTrace();
            }
        }
        long end = System.currentTimeMillis();
        logger.info("DeviceChecker finished " + (end - start) + " ms");
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
                logger.error("Unknown notification type: " + type);
                return;
        }
        HashSet<String> users = new HashSet<>();
        // device owner
        users.add(device.getUserID());
        /*
         * IotEvent event = new IotEvent();
         * event.setGeneralMessage(messageText);
         * event.setOrigin(device.getUserID() + "\t" + device.getEUI());
         * try {
         * iotDao.addAlert(event);
         * } catch (IotDatabaseException e) {
         * logger.error(e.getMessage());
         * }
         * messageService.sendNotification(event);
         */

        String[] deviceUsers;
        // device team
        deviceUsers = device.getTeam().split(",");
        for (String deviceUser : deviceUsers) {
            if (deviceUser.length() > 0) {
                users.add(deviceUser);
                /*
                 * event = new IotEvent();
                 * event.setGeneralMessage(messageText);
                 * event.setOrigin(deviceUser + "\t" + device.getEUI());
                 * 
                 * try {
                 * iotDao.addAlert(event);
                 * } catch (IotDatabaseException e) {
                 * logger.error(e.getMessage());
                 * }
                 * messageService.sendNotification(event);
                 */
            }
        }
        // device administrators
        deviceUsers = device.getAdministrators().split(",");
        for (String deviceUser : deviceUsers) {
            if (deviceUser.length() > 0) {
                users.add(deviceUser);
                /*
                 * event = new IotEvent();
                 * event.setGeneralMessage(messageText);
                 * event.setOrigin(deviceUser + "\t" + device.getEUI());
                 * try {
                 * iotDao.addAlert(event);
                 * } catch (IotDatabaseException e) {
                 * logger.error(e.getMessage());
                 * }
                 * messageService.sendNotification(event);
                 */
            }
        }

        // send notifications
        IotEvent event = new IotEvent();
        event.setGeneralMessage(messageText);
        String origin = "";
        for (String user : users) {
            origin += user + ";";
        }
        origin += "\t" + device.getEUI();
        event.setOrigin(origin);
        try {
            iotDao.addAlert(event);
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
        }
        messageService.sendNotification(event);
    }

}
