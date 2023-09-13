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
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.application.port.in.AuthPort;
import com.signomix.core.application.port.in.DashboardPort;
import com.signomix.core.application.port.in.UserPort;

@InboundAdapter
@Path("/api/core")
public class DashboardRestAdapter {

    @Inject
    Logger logger;
    @Inject
    AuthPort authPort;
    @Inject
    UserPort userPort;
    @Inject
    DashboardPort dashboardPort;

    @ConfigProperty(name = "signomix.exception.api.unauthorized")
    String unauthorizedException;

    @GET
    @Path("/v2/dashboards")
    public Response getDashboards(
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
        List<Dashboard> dashboards = dashboardPort.getUserDashboards(user, includeShared, isAdmin(user), limit, offset);
        return Response.ok().entity(dashboards).build();
    }

    @GET
    @Path("/v2/dashboards/{id}")
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
        Dashboard dashboard = dashboardPort.getUserDashboard(user, id);
        return Response.ok().entity(dashboard).build();
    }

    @PUT
    @Path("/v2/dashboards/{id}")
    public Response updateDashboard(
            @HeaderParam("Authentication") String token,
            @PathParam("id") String id, Dashboard dashboard) {
        User user;
        try {
            user = userPort.getAuthorizing(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        dashboardPort.updateDashboard(user, dashboard);
        return Response.ok().entity("ok").build();
    }

    @POST
    @Path("/v2/dashboards")
    public Response addDashboard(
            @HeaderParam("Authentication") String token,
            Dashboard dashboard) {
        User user;
        try {
            user = userPort.getAuthorizing(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        dashboardPort.addDashboard(user, dashboard);
        return Response.ok().entity("ok").build();
    }

    @DELETE
    @Path("/v2/dashboards/{id}")
    public Response removeDashboard(
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
        dashboardPort.removeDashboard(user, id);
        return Response.ok().entity("ok").build();
    }

    private boolean isAdmin(User user) {
        return user.type == User.ADMIN || (user.role!=null && user.role.contains("admin"));
    }

}
