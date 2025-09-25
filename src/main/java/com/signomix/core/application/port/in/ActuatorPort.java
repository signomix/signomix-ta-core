package com.signomix.core.application.port.in;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.iot.Device;
import com.signomix.core.domain.ActuatorLogic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ActuatorPort {

    @Inject
    ActuatorLogic actuatorLogic;

    private static final Logger logger = Logger.getLogger(ActuatorPort.class);

    public void processPlainCommand(User user, Device device, String command) throws IotDatabaseException {
        logger.debug("processPlainCommand");
        actuatorLogic.processPlainCommand(user, device, command);
    }

    public void processJsonCommand(User user, Device device, String command) throws IotDatabaseException {
        logger.debug("processJsonCommand");
        actuatorLogic.processJsonCommand(user, device, command);
    }

    public void processHexCommand(User user, Device device, String command) throws IotDatabaseException {
        logger.debug("processJsonCommand");
        actuatorLogic.processHexCommand(user, device, command);
    }

    public void sendWaitingCommands() {
        logger.debug("sendWaitingCommands");
        actuatorLogic.sendWaitingCommands(null);
    }

    public void sendWaitingCommands(String eui) {
        logger.info("sendWaitingCommands for " + eui);
        actuatorLogic.sendWaitingCommands(eui);
    }
}
