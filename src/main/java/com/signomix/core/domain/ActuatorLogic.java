package com.signomix.core.domain;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.signomix.common.User;
import com.signomix.common.db.ApplicationDaoIface;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.iot.Device;
import com.signomix.core.adapter.out.ChirpStackClient;
import com.signomix.core.adapter.out.ChirpStackResponse;
import com.signomix.core.adapter.out.TtnClient;
import com.signomix.core.adapter.out.TtnDecodedPayload;
import com.signomix.core.adapter.out.TtnDownlink;
import com.signomix.core.adapter.out.TtnResponse;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class ActuatorLogic {

    @Inject
    Logger logger = Logger.getLogger(ActuatorLogic.class);

    @Inject
    @RestClient
    ChirpStackClient chirpStackClient;

    @Inject
    @RestClient
    TtnClient ttnClient;

    @Inject
    @DataSource("oltp")
    AgroalDataSource oltpDs;

    @Inject
    @DataSource("olap")
    AgroalDataSource olapDs;

    IotDatabaseIface iotDao;
    ApplicationDaoIface appDao;

    public void onStart(@Observes StartupEvent ev) {
        iotDao = new com.signomix.common.tsdb.IotDatabaseDao();
        iotDao.setDatasource(oltpDs);
        iotDao.setAnalyticDatasource(olapDs);
        appDao = new com.signomix.common.tsdb.ApplicationDao();
        appDao.setDatasource(oltpDs);
    }

    public void processPlainCommand(User user, Device device, String command) throws IotDatabaseException {
        try {
            logger.info("Saving command");
            iotDao.putDeviceCommand(device.getEUI(), "ACTUATOR_PLAINCMD", command, System.currentTimeMillis());
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            throw new IotDatabaseException(IotDatabaseException.UNKNOWN, "Could not save command");
        }
    }

    public void processJsonCommand(User user, Device device, String command) throws IotDatabaseException {
        // Check if command is a valid JSON string
        String json = command;
        if (command.indexOf("@@@") > 0) {
            json = command.substring(0, command.indexOf("@@@"));
        }
        if (json.startsWith("&") || json.startsWith("#")) {
            json = json.substring(1);
        }
        if (!json.matches("^\\{.*\\}$")) {
            logger.debug("Invalid JSON string");
            throw new IotDatabaseException(IotDatabaseException.UNKNOWN, "Invalid JSON string");
        }
        try {
            logger.info("Saving command");
            if(device.getType().equalsIgnoreCase(Device.VIRTUAL)){
                sendToVirtual(device.getEUI(), json);
                iotDao.putCommandLog(0, device.getEUI(), "ACTUATOR_CMD",command, System.currentTimeMillis());
            }else{
                iotDao.putDeviceCommand(device.getEUI(), "ACTUATOR_CMD", command, System.currentTimeMillis());
            }
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            throw new IotDatabaseException(IotDatabaseException.UNKNOWN, "Could not save command");
        }
    }

    public void processHexCommand(User user, Device device, String command) throws IotDatabaseException {
        // Check if command is a valid hex string
        String hexStr = command;
        if (command.indexOf("@@@") > 0) {
            hexStr = command.substring(0, command.indexOf("@@@"));
        }
        if (hexStr.startsWith("&") || hexStr.startsWith("#")) {
            hexStr = hexStr.substring(1);
        }
        if (!hexStr.matches("^[0-9A-Fa-f]+$")) {
            logger.debug("Invalid hex string");
            throw new IotDatabaseException(IotDatabaseException.UNKNOWN, "Invalid hex string");
        }
        try {
            logger.info("Saving command");
            iotDao.putDeviceCommand(device.getEUI(), "ACTUATOR_HEXCMD", command, System.currentTimeMillis());
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            throw new IotDatabaseException(IotDatabaseException.UNKNOWN, "Could not save command");
        }

    }

    public void sendWaitingCommands(String eui) {
        try {
            boolean processAll = true; // Process all commands for each device, not only the first one as in the
                                       // previous version
            boolean paidDevicesOnly = false;
            if (eui != null) {
                paidDevicesOnly = true;
            }
            // Get all commands for the device and send them one by one
            iotDao.getCommands(eui, processAll, paidDevicesOnly).forEach((command) -> {
                // Process command
                logger.info("Processing command for device: " + command.getOrigin());
                logger.info("Command: " + command.getPayload());
                try {
                    Device device = iotDao.getDevice(command.getOrigin(), false);
                    boolean success = false;
                    logger.info("Device type: " + device.getType());
                    // logger.info(device.)
                    if (device != null) {
                        HashMap<String, Object> config = device.getConfigurationMap();
                        if (config == null || config.isEmpty()) {
                            logger.warn("Device configuration is empty");
                            /*
                             * Application app = appDao.getApplication(device.getApplicationID());
                             * if (app != null) {
                             * config = (HashMap<String,Object>)app.config.getAsMap();
                             * }
                             */
                            config = device.getApplicationConfig();
                        }
                        String apiKey = (String) config.get("apiKey");
                        String appId = device.getApplicationID();
                        String deviceId = device.getDeviceID();
                        String webhookId = (String) device.getConfigurationMap().get("webhookId");
                        if (device.getType().equalsIgnoreCase(Device.TTN)) {
                            // Send command to TTN
                            success = sendToTtn(appId, webhookId, deviceId, apiKey, command.getPayload(),
                                    command.getType());
                        } else if (device.getType().equalsIgnoreCase(Device.CHIRPSTACK) || device.getType().equals("LORA")) {
                            // Send command to ChirpStack
                            success = sendToChirpstack(device.getEUI(), apiKey, command.getPayload(),
                                    command.getType());
                        } else if (device.getType().equalsIgnoreCase(Device.VIRTUAL)) {
                            sendToVirtual(device.getEUI(), command.getPayload());
                            success = true;
                        } else {
                            logger.info("Commands are not supported for device type: " + device.getType());
                        }
                        if (success) {
                            iotDao.removeCommand(command.getId());
                            iotDao.putCommandLog(device.getEUI(), command);
                        }
                    }

                } catch (Exception e) {
                    logger.warn("Error removing command", e);
                }
            });
        } catch (IotDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private boolean sendToChirpstack(String eui, String key, Object payload, String commandType) {
        logger.info("Sending command to ChirpStack");
        String command = payload.toString();
        if (command.indexOf("@@@") < 0) {
            logger.warn("Invalid command format");
            return false;
        }
        String sPort = command.substring(command.indexOf("@@@") + 3);
        Integer port;
        try {
            port = Integer.parseInt(sPort);
        } catch (NumberFormatException e) {
            logger.warn("Invalid port number: " + sPort);
            return false;
        }
        try {
            command = command.substring(0, command.indexOf("@@@"));
            ChirpStackQueueItem item = new ChirpStackQueueItem();
            item.fPort = port;
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);

            // command is a JSON string, eg. {"led":1}
            item.object = mapper.readValue(command, Map.class);

            HashMap<String, Object> body = new HashMap<>();
            body.put("queueItem", item);

            // serialize item back to String
            String json = mapper.writeValueAsString(body);
            logger.info("Sending body: " + json);

            // Send command to ChirpStack
            logger.info("eui: " + eui);
            logger.info("key: " + "Bearer " + key);
            ChirpStackResponse response = chirpStackClient.sendDownlink("Bearer " + key, eui, body);
            if (response.id == null) {
                logger.warn("Error sending command to ChirpStack. Code: " + response.code);
                logger.warn("Response message: " + response.message);
                // TODO: save error message to command log?
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.warn("Error sending command to ChirpStack. Message" + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean sendToTtn(String appId, String webhookId, String deviceId, String key, Object payload,
            String commandType) {
        // https://www.thethingsindustries.com/docs/integrations/webhooks/scheduling-downlinks/
        logger.info("Sending command to TTN (sandbox)");
        String command = payload.toString();
        if (command.indexOf("@@@") < 0) {
            logger.warn("Invalid command format");
            return false;
        }
        String sPort = command.substring(command.indexOf("@@@") + 3);
        Integer port;
        try {
            port = Integer.parseInt(sPort);
        } catch (NumberFormatException e) {
            logger.warn("Invalid port number: " + sPort);
            return false;
        }
        try {
            command = command.substring(0, command.indexOf("@@@"));

            TtnDownlink item = new TtnDownlink();
            item.f_port = port;
            item.priority = "NORMAL";
            if (commandType.equals("ACTUATOR_HEXCMD")) {
                TtnDecodedPayload decodedPayload = new TtnDecodedPayload();
                decodedPayload.bytes = new byte[command.length() / 2];
                // convert hex string to byte array
                for (int i = 0; i < command.length(); i += 2) {
                    decodedPayload.bytes[i / 2] = (byte) ((Character.digit(command.charAt(i), 16) << 4)
                            + Character.digit(command.charAt(i + 1), 16));
                }
                item.decoded_payload = decodedPayload;
                // }else if(commandType.equals("ACTUATOR_CMD")){
                // item.decoded_payload = command;
            } else if (commandType.equals("ACTUATOR_PLAINCMD")) {
                item.frm_payload = command;
            }
            TtnDownlink[] downlinks = new TtnDownlink[1];
            HashMap<String, Object> body = new HashMap<>();
            body.put("downlinks", downlinks);
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
            String json = mapper.writeValueAsString(body);
            logger.info("Sending body: " + json);

            // Send command to ChirpStack
            logger.info("devId: " + deviceId);
            logger.info("key: " + "Bearer " + key);
            TtnResponse response = ttnClient.sendDownlink("Bearer " + key, "Signomix", appId, webhookId, deviceId,
                    body);
            if (response.id == null) {
                logger.warn("Error sending command to ChirpStack. Code: " + response.code);
                logger.warn("Response message: " + response.message);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.warn("Error sending command to ChirpStack. Message" + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean sendToVirtual(String eui, Object payload) {
        return sendToVirtual(eui, payload.toString());
    }

    private boolean sendToVirtual(String eui, String command) {
        logger.debug("Sending command to Virtual device");
        Device device = null;
        try {
            device = iotDao.getDevice(eui, true, true);
        } catch (IotDatabaseException e) {
            logger.warn("Error getting device: " + e.getMessage());
            return false;
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);

        // command is a JSON string, eg. {"led":1}
        HashMap<String, Object> map;
        try {
            map = (HashMap) mapper.readValue(command, Map.class);
        } catch (JsonProcessingException e) {
            logger.warn("Error parsing command: " + e.getMessage());
            return false;
        }

        boolean neddsUpdate = false;
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            switch(key) {
                case "status":
                    device.setState((Double)map.get(key));
                    neddsUpdate = true;
                    break;
                case "active":
                    device.setActive((Boolean)map.get(key));
                    neddsUpdate = true;
                    break;
                case "interval":
                    device.setTransmissionInterval((Integer)map.get(key));
                    neddsUpdate = true;
                    break;
                default:
                    logger.warn("Unknown device command name: " + key);
                    break;

            }
        }
        if (neddsUpdate) {
            try {
                iotDao.updateDevice(device);
            } catch (IotDatabaseException e) {
                logger.warn("Error updating device: " + e.getMessage());
                return false;
            }
        }

        return true;
    }

}
