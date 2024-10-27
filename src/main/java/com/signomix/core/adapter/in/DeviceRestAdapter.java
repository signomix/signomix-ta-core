package com.signomix.core.adapter.in;

import com.signomix.common.User;
import com.signomix.common.annotation.InboundAdapter;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.iot.Device;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.application.port.in.AuthPort;
import com.signomix.core.application.port.in.DevicePort;
import com.signomix.core.application.port.in.UserPort;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

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

    @Inject
    Logger logger;

    @ConfigProperty(name = "signomix.exception.api.unauthorized")
    String unauthorizedException;

    @GET
    @Path("/device")
    public Response getDevices(
            @HeaderParam("Authentication") String token,
            @QueryParam("full") Boolean full,
            @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset,
            @QueryParam("search") String search,
            @QueryParam("organization") Long organizationId,
            @QueryParam("path") String path,
            @QueryParam("context") Integer context) {
        try {
            User user;
            try {
                user = userPort.getAuthorizing(authPort.getUserId(token));
            } catch (IotDatabaseException e) {
                throw new ServiceException(unauthorizedException);
            }
            if (null == user) {
                throw new ServiceException(unauthorizedException);
            }
            logger.info("getDevices limit:" + limit + " offset:" + offset + " full:" + full);
            List<Device> devices;
            /*
             * if(null==organizationId){
             * devices = devicePort.getUserDevices(user, full, limit, offset, search);
             * }else{
             * devices = devicePort.getDevicesByPath(user, organizationId, full, limit,
             * offset, path);
             * }
             */
            devices = devicePort.getDevices(user, full, organizationId, context, path, search, limit, offset);
            return Response.ok().entity(devices).build();
        } catch (Exception e) {
            logger.error("getDevices error:" + e.getMessage());
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    @GET
    @Path("/device/{eui}")
    public Response getDevice(@HeaderParam("Authentication") String token, @PathParam("eui") String eui,
            @QueryParam("full") Boolean full) {
        try {
            User user;
            try {
                user = userPort.getAuthorizing(authPort.getUserId(token));
            } catch (IotDatabaseException e) {
                e.printStackTrace();
                throw new ServiceException(unauthorizedException);
            }
            if (null == user) {
                throw new ServiceException(unauthorizedException);
            }
            Device device = devicePort.getDevice(user, eui, full);
            return Response.ok().entity(device).build();
        } catch (Exception e) {
            e.printStackTrace();
            LOG.warn(e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }

    @DELETE
    @Path("/device/{eui}")
    public Response delete(@HeaderParam("Authentication") String token, @PathParam("eui") String eui) {
        try {
            User user;
            try {
                user = userPort.getAuthorizing(authPort.getUserId(token));
            } catch (IotDatabaseException e) {
                throw new ServiceException(unauthorizedException);
            }
            if (null == user) {
                throw new ServiceException(unauthorizedException);
            }
            devicePort.deleteDevice(user, eui);
            return Response.ok().entity("OK").build();
        } catch (Exception e) {
            LOG.warn(e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }

    @PUT
    @Path("/device/{eui}")
    public Response updateDevice(@HeaderParam("Authentication") String token, @PathParam("eui") String eui,
            Device device) {
        try {
            User user;
            try {
                user = userPort.getAuthorizing(authPort.getUserId(token));
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
                devicePort.updateDevice(user, eui, device);
            } catch (Exception e) {
                LOG.warn(e.getMessage());
                e.printStackTrace();
                throw new ServiceException(e.getMessage());
            }
            return Response.ok().entity("OK").build();
        } catch (Exception e) {
            LOG.warn(e.getMessage());
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    @POST
    @Path("/device/copy")
    public Response copyDevice(@HeaderParam("Authentication") String token, 
    DeviceCopyDefinition definition) {
        try {
            User user;
            try {
                user = userPort.getAuthorizing(authPort.getUserId(token));
            } catch (IotDatabaseException e) {
                throw new ServiceException(unauthorizedException);
            }
            if (null == user) {
                throw new ServiceException(unauthorizedException);
            }
            String eui = definition.eui;
            Device device = devicePort.getDevice(user, eui, false);
            if (null == device) {
                throw new ServiceException("Device not found");
            }

            device.setEUI(definition.newEui);
            device.setName(definition.name);
            device.setDashboard(definition.dashboard==null?false:definition.dashboard);
            device.setDeviceID(definition.deviceID);
            device.setApplicationID(definition.applicationID);
            //device.setDownlink(definition.downlink);
            device.setLatitude(definition.latitude);
            device.setLongitude(definition.longitude);
            device.setAltitude(definition.altitude);
            
            device.setState(0d);
            device.setLastSeen(0L);
            device.setLastFrame(0L);
            device.setAlertStatus(0);

            device.setUserID(user.uid);
            devicePort.createDevice(user, device);
            LOG.info("Device copied: " + device.getEUI());
            return Response.ok().entity("OK").build();
        } catch (Exception e) {
            LOG.warn(e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }

    

}
