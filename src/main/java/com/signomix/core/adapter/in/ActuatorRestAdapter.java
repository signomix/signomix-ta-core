package com.signomix.core.adapter.in;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.annotation.InboundAdapter;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.iot.Device;
import com.signomix.core.application.port.in.ActuatorPort;
import com.signomix.core.application.port.in.AuthPort;
import com.signomix.core.application.port.in.DevicePort;
import com.signomix.core.domain.MultiCommand;

import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@InboundAdapter
@Path("/api/core")
public class ActuatorRestAdapter {
    private static final Logger logger = Logger.getLogger(CommandRestAdapter.class);

    @Inject
    ActuatorPort actuatorPort;
    @Inject
    AuthPort authPort;
    @Inject
    DevicePort devicePort;

    @GET
    public Response test() {
        return Response.ok().entity("OK").build();
    }

    @POST
    @Path("/actuator/{eui}/{type}")
    public Response processCommand(
            @HeaderParam("Authentication") String token,
            @PathParam("eui") String eui,
            @PathParam("type") String type,
            String command) {
        logger.info("Authentication: " + token);
        logger.info("Processing command: " + command);
        User user = authPort.getUser(token);
        if (null == user) {
            return Response.status(Status.FORBIDDEN).build();
        }
        Device device = devicePort.getDevice(user, eui, false, false);
        if (null == device) {
            return Response.status(Status.FORBIDDEN).build();
        }
        try {
            processCommand(user, device, command, type);
        } catch (IotDatabaseException e) {
            logger.warn("Error processing command", e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        /* switch (type.toLowerCase()) {
            case "plain":
                try {
                    actuatorPort.processPlainCommand(user, device, command);
                } catch (IotDatabaseException e) {
                    logger.warn("Error processing command", e);
                    return Response.status(Status.BAD_REQUEST).build();
                }
                break;
            case "json":
                try {
                    actuatorPort.processJsonCommand(user, device, command);
                } catch (IotDatabaseException e) {
                    logger.warn("Error processing command", e);
                    return Response.status(Status.BAD_REQUEST).build();
                }
                break;
            case "hex":
                try {
                    actuatorPort.processHexCommand(user, device, command);
                } catch (IotDatabaseException e) {
                    logger.warn("Error processing command", e);
                    return Response.status(Status.BAD_REQUEST).build();
                }
                break;
            default:
                logger.warn("Unknown command type: " + command);
                return Response.status(Status.BAD_REQUEST).build();
        } */
        return Response.ok().build();
    }

    @POST
    @Path("/actuators")
    @Blocking
    public Response processCommands(
            @HeaderParam("Authentication") String token, MultiCommand multiCommand) {
        logger.info("Authentication: " + token);
        logger.info("Processing multi-command: " + multiCommand.command);
        User user = authPort.getUser(token);
        if (null == user) {
            return Response.status(Status.FORBIDDEN).build();
        }
        String[] euisArray = multiCommand.euis.split(",");
        for (int i = 0; i < euisArray.length; i++) {
            Device device = devicePort.getDevice(user, euisArray[i], false, false);
            if (null == device) {
                logger.warn("Device not found: " + euisArray[i]);
                continue;
            }
            try {
                processCommand(user, device, multiCommand.command, multiCommand.type);
            } catch (IotDatabaseException e) {
                logger.warn("Error processing command for device: " + euisArray[i], e);
            }
        }
        return Response.ok().build();
    }

    private void processCommand(User user, Device device, String command, String type) throws IotDatabaseException {
        switch (type.toLowerCase()) {
            case "plain":
                actuatorPort.processPlainCommand(user, device, command);
                break;
            case "json":
                actuatorPort.processJsonCommand(user, device, command);
                break;
            case "hex":
                actuatorPort.processHexCommand(user, device, command);
                break;
            default:
                logger.warn("Unknown command type: " + type);
        }
    }

}
