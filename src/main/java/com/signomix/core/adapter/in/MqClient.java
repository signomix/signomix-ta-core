package com.signomix.core.adapter.in;

import java.nio.charset.StandardCharsets;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.signomix.common.EventEnvelope;
import com.signomix.core.application.port.in.CommandPort;
import com.signomix.core.application.port.in.DevicePort;

@ApplicationScoped
public class MqClient {

    private static final Logger LOG = Logger.getLogger(MqClient.class);

    @Inject
    CommandPort commandPort;
    @Inject
    DevicePort devicePort;

    @Incoming("events_db")
    public void processDbEvent(byte[] bytes) {
        //processMessageUseCase.processEvent(bytes);
        String message = new String(bytes, StandardCharsets.UTF_8);
        EventEnvelope wrapper;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            wrapper = objectMapper.readValue(message, EventEnvelope.class);
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
            return;
        }
        switch(wrapper.payload.toLowerCase()){
            case "backup":
                commandPort.runBackup();
            default:
                LOG.warn("Unknown command "+wrapper.payload);
                
        }
        LOG.debug(wrapper.type + " " + wrapper.uuid + " " + wrapper.payload);
    }

    @Incoming("events_device")
    public void processDeviceEvent(byte[] bytes) {
        //processMessageUseCase.processEvent(bytes);
        String message = new String(bytes, StandardCharsets.UTF_8);
        EventEnvelope wrapper;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            wrapper = objectMapper.readValue(message, EventEnvelope.class);
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
            return;
        }
        LOG.info(wrapper.type + " " + wrapper.uuid + " " + wrapper.payload);
        switch(wrapper.payload.toLowerCase()){
            case "check":
                devicePort.checkDevices();
            default:
                LOG.warn("Unknown command "+wrapper.payload);
                
        }
    }

}
