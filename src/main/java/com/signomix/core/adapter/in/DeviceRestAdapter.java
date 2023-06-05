package com.signomix.core.adapter.in;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.annotation.InboundAdapter;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.iot.Device;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.application.port.in.AuthPort;
import com.signomix.core.application.port.in.DevicePort;
import com.signomix.core.application.port.in.UserPort;

@InboundAdapter
@Path("/api/core")
public class DeviceRestAdapter {
    private static final Logger LOG = Logger.getLogger(DeviceRestAdapter.class);

    @Inject
    AuthPort authPort;
    @Inject
    UserPort userPort;
    @Inject
    DevicePort devicePort;

    @ConfigProperty(name = "signomix.exception.api.unauthorized")
    String unauthorizedException;

    @GET
    @Path("/device")
    public Response getDevices(
        @HeaderParam("Authentication") String token, 
        @QueryParam("full") Boolean full,
        @QueryParam("limit") Integer limit,
        @QueryParam("offset") Integer offset) {
        User user;
        try {
            user = userPort.getUser(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        List<Device> devices = devicePort.getUserDevices(user, full, limit, offset);
        return Response.ok().entity(devices).build();
    }

    @GET
    @Path("/device/{eui}")
    public Response getDevice(@HeaderParam("Authentication") String token, @PathParam("eui") String eui,
            @QueryParam("full") Boolean full) {
        User user;
        try {
            user = userPort.getUser(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        Device device = devicePort.getDevice(user, eui, full);
        return Response.ok().entity(device).build();
    }

    @DELETE
    @Path("/device/{eui}")
    public Response delete(@HeaderParam("Authentication") String token, @PathParam("eui") String eui) {
        User user;
        try {
            user = userPort.getUser(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        devicePort.deleteDevice(user, eui);
        return Response.ok().entity("OK").build();
    }

    @PUT
    @Path("/device/{eui}")
    public Response updateDevice(@HeaderParam("Authentication") String token, @PathParam("eui") String eui,
            Device device) {
        User user;
        try {
            user = userPort.getUser(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            LOG.warn(e.getMessage());
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            LOG.warn("User not found");
            throw new ServiceException(unauthorizedException);
        }
        if (null == device) {
            LOG.warn("Device is null");
            throw new ServiceException("Device not found");
        }
        if (null == device.getUserID() || device.getUserID().isEmpty()) {
            device.setUserID(user.uid);
        }
        try {
            devicePort.updateDevice(user, device);
        } catch (Exception e) {
            LOG.warn(e.getMessage());
            throw new ServiceException(e.getMessage());
        }
        return Response.ok().entity("OK").build();
    }

    @POST
    @Path("/device")
    public Response addDevice(@HeaderParam("Authentication") String token, Device device) {
        User user;
        try {
            user = userPort.getUser(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        device.setUserID(user.uid);
        devicePort.createDevice(user, device);
        return Response.ok().entity("OK").build();
    }

}
