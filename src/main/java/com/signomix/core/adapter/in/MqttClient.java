package com.signomix.core.adapter.in;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import com.signomix.core.application.port.in.ActuatorPort;
import com.signomix.core.application.port.in.CommandPort;
import com.signomix.core.application.port.in.DevicePort;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MqttClient {

    @Inject
    Logger logger = Logger.getLogger(MqttClient.class);

    @Inject
    CommandPort commandLogic;
    @Inject
    DevicePort devicePort;
    @Inject
    ActuatorPort actuatorPort;

    @Incoming("commands")
    @Blocking
    public void processCommand(byte[] bytes) {
        String msg = new String(bytes).toLowerCase();
        if(msg.startsWith("type=")) {
            msg= msg.substring(5);
        }
        logger.info("Command event received: " + msg);
        
        if(msg.startsWith("backup")) {
            commandLogic.runBackup();
        } else if(msg.startsWith("archive")) {
            commandLogic.runArchive();
        } else if(msg.startsWith("clean") || msg.startsWith("datacleaner")) {
            commandLogic.runClean();
        } else if(msg.startsWith("check") || msg.startsWith("devicechecker")) {
            devicePort.checkDevices(false);
        } else if(msg.startsWith("devicechecker-paid")) {
            devicePort.checkDevices(true);
        } else if(msg.startsWith("devicecommands")) {
            actuatorPort.sendWaitingCommands(null);
        } else if(msg.startsWith("system-monitor")) {  
            // TODO: implement system monitor
        } else {
            logger.debug("Unknown command: " + msg); 
        }
        /* switch (msg.toLowerCase()) {
            case "backup":
                commandLogic.runBackup();
                break;
            case "archive":
                commandLogic.runArchive();
                break;
            case "clean":
            case "datacleaner":
                commandLogic.runClean();
                break;
            case "check":
            case "devicechecker":
                devicePort.checkDevices(false);
                break;
            case "devicechecker-paid":
                devicePort.checkDevices(true);
                break;
            case "devicecommands":
                actuatorPort.sendWaitingCommands(null);
                break;
            case "system-monitor":
                // TODO: implement system monitor
                break;
            default:
                logger.debug("Unknown command: " + msg);
        } */
    }

    @Incoming("command-ready")
    public void processCommandReady(byte[] bytes) {
        logger.info("Command ready for: " + new String(bytes));
        String eui = new String(bytes);
        actuatorPort.sendWaitingCommands(eui);
    }

    @Incoming("sms-sent")
    public void processSmsSent(byte[] bytes) {
        logger.info("SMS sent: " + new String(bytes));
        String msg = new String(bytes);
        String[] parts = msg.split(";");
    }

}
