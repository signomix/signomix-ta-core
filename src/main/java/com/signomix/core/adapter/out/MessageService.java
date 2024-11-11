package com.signomix.core.adapter.out;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.signomix.common.EventEnvelope;
import com.signomix.common.MessageEnvelope;
import com.signomix.common.User;
import com.signomix.common.event.IotEvent;
import com.signomix.common.event.MessageServiceIface;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MessageService implements MessageServiceIface {

    private static final Logger LOG = Logger.getLogger(MessageService.class);

    //@Channel("events")
    //Emitter<byte[]> eventEmitter;

    @Channel("notifications")
    Emitter<byte[]> iotEventEmitter;

    //@Channel("events_db")
    //Emitter<byte[]> eventDbEmitter;

    //@Channel("events_device")
    //Emitter<byte[]> eventDeviceEmitter;

    //@Channel("admin_email")
    //Emitter<byte[]> adminEmailEmitter;

    @Override
    public void sendEvent(EventEnvelope wrapper) {
/*         LOG.info("sending event to MQ");
        String encodedMessage;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            encodedMessage = objectMapper.writeValueAsString(wrapper);
            //eventEmitter.send(encodedMessage.getBytes());
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
        } */
    }

    @Override
    public void sendNotification(IotEvent event) {
        LOG.info("sending notification to MQTT, origin:" + event.getOrigin());

        String[] origin = event.getOrigin().split("\t");
        User user = new User();
        user.uid = origin[0];

        MessageEnvelope wrapper = new MessageEnvelope();
        wrapper.type = event.getType();
        wrapper.eui = origin[1];
        wrapper.message = (String) event.getPayload();
        wrapper.user = user;

        String encodedMessage;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            encodedMessage = objectMapper.writeValueAsString(wrapper);
            iotEventEmitter.send(encodedMessage.getBytes());
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
        }
    }

    @Override
    public void sendData(IotEvent event) {
        //LOG.info("sending data to MQ");
    }

    @Override
    public void sendCommand(IotEvent event) {
/*         LOG.info("sending command to MQ");
        String[] origin = event.getOrigin().split("\t");
        User user = new User();
        user.uid = origin[0];
        MessageEnvelope wrapper = new MessageEnvelope();
        wrapper.type = event.getType();
        wrapper.eui = origin[1];
        wrapper.message = (String) event.getPayload();
        wrapper.user = user;
        String encodedMessage;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            encodedMessage = objectMapper.writeValueAsString(event);
            //eventEmitter.send(encodedMessage.getBytes());
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
        } */
    }

    @Override
    public void sendDeviceEvent(EventEnvelope wrapper) {
/*         LOG.info("sending device event to MQ");
        String encodedMessage;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            LOG.info("eventDeviceEmitter with payload " + wrapper.payload);
            encodedMessage = objectMapper.writeValueAsString(wrapper);
            //eventDeviceEmitter.send(encodedMessage.getBytes());
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
        } */
    }

    @Override
    public void sendDbEvent(EventEnvelope wrapper) {
/*         String encodedMessage;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            LOG.info("eventDbEmitter with payload " + wrapper.payload);
            encodedMessage = objectMapper.writeValueAsString(wrapper);
            //eventDbEmitter.send(encodedMessage.getBytes());
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
        } */
    }

    @Override
    public void sendAdminEmail(MessageEnvelope wrapper) {
/*         LOG.info("sending admin e-mail to MQ");
        String encodedMessage;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            encodedMessage = objectMapper.writeValueAsString(wrapper);
            //adminEmailEmitter.send(encodedMessage.getBytes());
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
        } */
    }

    @Override
    public void sendErrorInfo(EventEnvelope wrapper) {
/*         String encodedMessage;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            LOG.info("eventEmitter with payload " + wrapper.payload);
            encodedMessage = objectMapper.writeValueAsString(wrapper);
            //eventEmitter.send(encodedMessage.getBytes());
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
        } */
    }

}
