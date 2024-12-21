package com.signomix.core.adapter.in;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import com.signomix.core.application.port.in.CommandPort;
import com.signomix.core.application.port.in.DevicePort;

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

    @Incoming("commands")
    public void processCommand(byte[] bytes) {
        logger.info("Command event received: " + new String(bytes));
        String msg = new String(bytes);
        switch (msg.toLowerCase()) {
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
                devicePort.checkDevices();
                break;
            case "devicecommands":
                //TODO: implement device commands
                break;
            case "system-monitor":
                //TODO: implement system monitor
                break;
            default:
                logger.warn("Unknown command: " + msg);
        }
    }

    @Incoming("sms-sent")
    public void processSmsSent(byte[] bytes) {
        logger.info("SMS sent: " + new String(bytes));
        String msg = new String(bytes);
        String[] parts = msg.split(";");
    }

}
