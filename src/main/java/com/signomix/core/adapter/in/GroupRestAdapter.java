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
import com.signomix.common.gui.Dashboard;
import com.signomix.common.iot.DeviceGroup;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.application.port.in.AuthPort;
import com.signomix.core.application.port.in.GroupPort;
import com.signomix.core.application.port.in.UserPort;

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
    @Path("/v2/groups")
    public Response getGrpups(
            @HeaderParam("Authentication") String token,
            @QueryParam("shared") Boolean includeShared,
            @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset) {
        User user;
        try {
            user = userPort.getAuthorizing(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        List<DeviceGroup> groups = groupPort.getUserGroups(user, limit, offset);
        return Response.ok().entity(groups).build();
    }

    @GET
    @Path("/v2/groups/{id}")
    public Response getDashboard(
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
        DeviceGroup group = groupPort.getGroup(user, id);
        return Response.ok().entity(group).build();
    }

    @PUT
    @Path("/v2/groups/{id}")
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
        try{
        groupPort.updateGroup(user, group);
        }catch(Exception e){
            logger.error("Unable to update group: "+e.getMessage());
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
        return Response.ok().entity("ok").build();
    }

    @POST
    @Path("/v2/groups")
    public Response addGroup(
            @HeaderParam("Authentication") String token,
            DeviceGroup group) {
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
    }

    @DELETE
    @Path("/v2/groups/{id}")
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

    private boolean isAdmin(User user) {
        return user.type == User.ADMIN || (user.role!=null && user.role.contains("admin"));
    }

}
