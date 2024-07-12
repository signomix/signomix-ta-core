package com.signomix.core.adapter.in;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import com.signomix.core.application.port.in.CommandPort;
import com.signomix.core.domain.UserLogic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MqttClient {

    @Inject
    Logger logger = Logger.getLogger(MqttClient.class);

    @Inject
    CommandPort commandLogic;

    @Incoming("commands")
    public void processCommand(byte[] bytes) {
        logger.info("Command event received: "+new String(bytes));
        String msg = new String(bytes);
        switch(msg.toLowerCase()){
            case "backup":
                commandLogic.runBackup();
                break;
            case "archive":
                commandLogic.runArchive();
                break;
            default:
                logger.warn("Unknown command: "+msg);
        }
    }
    
}
