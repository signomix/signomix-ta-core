package com.signomix.core.adapter.in;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.annotation.InboundAdapter;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.iot.DeviceGroup;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.application.port.in.AuthPort;
import com.signomix.core.application.port.in.GroupPort;
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

@InboundAdapter
@Path("/api/core")
public class GroupRestAdapter {

    @Inject
    Logger logger;
    @Inject
    AuthPort authPort;
    @Inject
    UserPort userPort;
    @Inject
    GroupPort groupPort;

    @ConfigProperty(name = "signomix.exception.api.unauthorized")
    String unauthorizedException;

    @GET
    @Path("/group")
    public Response getGroups(
            @HeaderParam("Authentication") String token,
            @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset,
            @QueryParam("search") String search) {
        try {
            logger.info("getGroups");
            User user;
            try {
                user = userPort.getAuthorizing(authPort.getUserId(token));
            } catch (IotDatabaseException e) {
                throw new ServiceException(unauthorizedException);
            }
            if (null == user) {
                throw new ServiceException(unauthorizedException);
            }
            List<DeviceGroup> groups = groupPort.getUserGroups(user, limit, offset, search);
            return Response.ok().entity(groups).build();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Unable to get groups: " + e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }

    @GET
    @Path("/group/{id}")
    public Response getGroup(
            @HeaderParam("Authentication") String token,
            @PathParam("id") String id) {
        User user;
        try {
            user = userPort.getAuthorizing(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        try {
            DeviceGroup group = groupPort.getGroup(user, id);
            return Response.ok().entity(group).build();
        } catch (Exception e) {
            logger.error("Unable to get group: " + e.getMessage());
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    @PUT
    @Path("/group/{id}")
    public Response updateGroup(
            @HeaderParam("Authentication") String token,
            @PathParam("id") String id, DeviceGroup group) {
        User user;
        try {
            user = userPort.getAuthorizing(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }

        try {
            groupPort.updateGroup(user, group);
        } catch (Exception e) {
            logger.error("Unable to update group: " + e.getMessage());
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
        return Response.ok().entity("ok").build();
    }

    @POST
    @Path("/group")
    public Response addGroup(@HeaderParam("Authentication") String token, DeviceGroup group) {
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
            groupPort.createGroup(user, group);
            return Response.ok().entity("ok").build();
        } catch (Exception e) {
            logger.error("Unable to create group: " + e.getMessage());
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    @DELETE
    @Path("/group/{id}")
    public Response removeGroup(
            @HeaderParam("Authentication") String token,
            @PathParam("id") String id) {
        User user;
        try {
            user = userPort.getAuthorizing(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        groupPort.deleteGroup(user, id);
        return Response.ok().entity("ok").build();
    }

}
