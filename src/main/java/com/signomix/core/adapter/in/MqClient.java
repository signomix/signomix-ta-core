package com.signomix.core.adapter.in;

import java.nio.charset.StandardCharsets;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.signomix.common.EventEnvelope;

@ApplicationScoped
public class MqClient {

    private static final Logger LOG = Logger.getLogger(MqClient.class);

    //@Inject
    //MailingUC processMessageUseCase;

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
        LOG.info(wrapper.type + " " + wrapper.uuid + " " + wrapper.payload);
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
    }

}
