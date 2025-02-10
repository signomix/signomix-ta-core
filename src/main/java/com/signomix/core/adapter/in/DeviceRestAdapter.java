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
import jakarta.ws.rs.Consumes;
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
import java.util.Scanner;
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
    @Path("/device")
    public Response addDevice(@HeaderParam("Authentication") String token, Device device) {
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
            device.setUserID(user.uid);
            devicePort.createDevice(user, device);
            return Response.ok().entity("OK").build();
        } catch (Exception e) {
            LOG.warn(e.getMessage());
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

            // validate parameters
            if (definition.newEui == null || definition.newEui.isEmpty()) {
                throw new ServiceException("New EUI is empty");
            } else {
                device.setEUI(definition.newEui);
            }
            // TTN device parameters
            if (device.getType() == Device.TTN) {
                if (definition.deviceID == null || definition.deviceID.isEmpty()) {
                    throw new ServiceException("Device ID is empty");
                } else {
                    device.setDeviceID(definition.deviceID);
                }
                if (definition.applicationID != null && !definition.applicationID.isEmpty()) {
                    device.setApplicationID(definition.applicationID);
                } else {
                    // the sanme as of the copied device
                }
            }

            device.setName(definition.name);
            device.setDashboard(definition.dashboard == null ? false : definition.dashboard);

            // device.setDownlink(definition.downlink);
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

    @POST
    @Path("/device/bulkcopy/{eui}")
    @Consumes("text/plain")
    public Response copyDevice(@HeaderParam("Authentication") String token,
            @PathParam("eui") String eui,
            String text) {
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
            // get copied device
            Device device = devicePort.getDevice(user, eui, false);
            if (null == device) {
                throw new ServiceException("Device not found");
            }
            // read csv line by line from text
            boolean isHeader = true;
            String[] header = null;
            String[] parts;
            Scanner scanner = new Scanner(text);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                LOG.info(line);
                if (isHeader) {
                    header = line.split(",");
                    isHeader = false;
                } else {
                    parts = line.split(",");
                    // validate parameters
                    String newEui = getParam("eui", header, parts);
                    LOG.info("New EUI: " + newEui);
                    if (newEui == null || newEui.isEmpty()) {
                        LOG.warn("New EUI is empty");
                        throw new ServiceException("New EUI is empty");
                    } else {
                        device.setEUI(newEui);
                    }
                    // TTN device parameters
                    if (device.getType() == Device.TTN) {
                        String deviceID = getParam("deviceID", header, parts);
                        String applicationID = getParam("applicationID", header, parts);
                        if (deviceID == null || deviceID.isEmpty()) {
                            LOG.warn("Device ID is empty");
                            throw new ServiceException("Device ID is empty");
                        } else {
                            device.setDeviceID(deviceID);
                        }
                        if (applicationID != null && !applicationID.isEmpty()) {
                            device.setApplicationID(applicationID);
                        } else {
                            // the sanme as of the copied device
                        }
                    }
                    String name = getParam("name", header, parts);
                    Boolean dashboard = Boolean.parseBoolean(getParam("dashboard", header, parts));
                    device.setName(name);
                    device.setDashboard(dashboard);

                    Double latitude;
                    Double longitude;
                    Double altitude;
                    try {
                        latitude = Double.parseDouble(getParam("latitude", header, parts));
                        device.setLatitude(latitude);
                    } catch (Exception e) {
                    }
                    try {
                        longitude = Double.parseDouble(getParam("longitude", header, parts));
                        device.setLongitude(longitude);
                    } catch (Exception e) {
                    }
                    try {
                        altitude = Double.parseDouble(getParam("altitude", header, parts));
                        device.setAltitude(altitude);
                    } catch (Exception e) {
                    }

                    device.setState(0d);
                    device.setLastSeen(0L);
                    device.setLastFrame(0L);
                    device.setAlertStatus(0);

                    device.setUserID(user.uid);
                    LOG.info("Registering device: " + device.getEUI());
                    devicePort.createDevice(user, device);
                    LOG.info("Device copied: " + device.getEUI());

                }
                isHeader = false;
            }
            return Response.ok().entity("OK").build();
        } catch (Exception e) {
            LOG.warn(e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }

    @DELETE
    @Path("/device/{eui}/data")
    public Response deleteData(@HeaderParam("Authentication") String token, @PathParam("eui") String eui) {
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
            devicePort.deleteDeviceData(user, eui);
            return Response.ok().entity("OK").build();
        } catch (Exception e) {
            LOG.warn(e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }


    private String getParam(String name, String[] paramNames, String[] values) {
        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(name)) {
                return values[i];
            }
        }
        return null;
    }

}
