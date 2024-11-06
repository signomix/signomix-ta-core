package com.signomix.core.adapter.in;

import com.signomix.core.application.port.in.CommandPort;
import com.signomix.core.application.port.in.DevicePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MqClient {

    private static final Logger LOG = Logger.getLogger(MqClient.class);

    @Inject
    CommandPort commandPort;
    @Inject
    DevicePort devicePort;

/*     @Incoming("events_db")
    public void processDbEvent(byte[] bytes) {
        // processMessageUseCase.processEvent(bytes);
        String message = new String(bytes, StandardCharsets.UTF_8);
        EventEnvelope wrapper;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            wrapper = objectMapper.readValue(message, EventEnvelope.class);
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
            return;
        }
        LOG.info("Skipping message "+wrapper.type + " " + wrapper.uuid + " " + wrapper.payload);
        LOG.debug(wrapper.type + " " + wrapper.uuid + " " + wrapper.payload);
    } */

/*     @Incoming("events_device")
    public void processDeviceEvent(byte[] bytes) {
        // processMessageUseCase.processEvent(bytes);
        String message = new String(bytes, StandardCharsets.UTF_8);
        EventEnvelope wrapper;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            wrapper = objectMapper.readValue(message, EventEnvelope.class);
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
            return;
        }
        LOG.info("Handling message "+wrapper.type + " " + wrapper.uuid + " " + wrapper.payload);
        switch (wrapper.payload.toLowerCase()) {
            case "check":
                devicePort.checkDevices();
                break;
            default:
                LOG.warn("Unknown command " + wrapper.payload);

        }
    } */

}
