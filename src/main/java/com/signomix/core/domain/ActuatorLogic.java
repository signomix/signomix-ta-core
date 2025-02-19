package com.signomix.core.domain;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signomix.common.User;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.iot.Device;
import com.signomix.core.adapter.out.ChirpStackClient;
import com.signomix.core.adapter.out.ChirpStackResponse;

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
    @DataSource("oltp")
    AgroalDataSource oltpDs;

    @Inject
    @DataSource("olap")
    AgroalDataSource olapDs;

    IotDatabaseIface iotDao;

    public void onStart(@Observes StartupEvent ev) {
        iotDao = new com.signomix.common.tsdb.IotDatabaseDao();
        iotDao.setDatasource(oltpDs);
        iotDao.setAnalyticDatasource(olapDs);
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
        if(json.startsWith("&") || json.startsWith("#")){
            json = json.substring(1);
        }
        if (!json.matches("^\\{.*\\}$")) {
            logger.debug("Invalid JSON string");
            throw new IotDatabaseException(IotDatabaseException.UNKNOWN, "Invalid JSON string");
        }
        try {
            logger.info("Saving command");
            iotDao.putDeviceCommand(device.getEUI(), "ACTUATOR_CMD", command, System.currentTimeMillis());
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
        if(hexStr.startsWith("&") || hexStr.startsWith("#")){
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

    public void sendWaitingCommands() {
        try {
            iotDao.getCommands().forEach((command) -> {
                // Process command
                logger.info("Processing command for device: " + command.getOrigin());
                logger.info("Command: " + command.getPayload());
                try {
                    Device device = iotDao.getDevice(command.getOrigin(), false);
                    boolean success = false;
                    logger.info("Device type: " + device.getType());
                    if (device != null) {
                        if (device.getType() == Device.TTN) {
                            // Send command to TTN
                            success = true;
                        } else if (device.getType() == Device.CHIRPSTACK || device.getType().equals("LORA")) {
                            // Send command to ChirpStack
                            String apiKey = (String)device.getConfigurationMap().get("apiKey");
                            success = sendToChirpstack(device.getEUI(), apiKey, command.getPayload());
                        }
                        if (success) {
                            iotDao.removeCommand(command.getId());
                            iotDao.putCommandLog(null, command);
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
    private boolean sendToChirpstack(String eui, String key, Object payload) {
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
            logger.info("eui: "+eui);
            logger.info("key: "+"Bearer "+key);
            ChirpStackResponse response = chirpStackClient.sendDownlink("Bearer "+key, eui, body);
            if(response.id==null){
                logger.warn("Error sending command to ChirpStack. Code: "+response.code);
                logger.warn("Response message: "+response.message);
                //TODO: save error message to command log?
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.warn("Error sending command to ChirpStack. Message"+e.getMessage());
            return false;
        }
        return true;
    }
}
