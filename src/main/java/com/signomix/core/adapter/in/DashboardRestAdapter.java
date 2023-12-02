package com.signomix.core.adapter.in;

import java.util.List;

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
            @QueryParam("offset") Integer offset,
            @QueryParam("search") String searchString) {
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
            List<Dashboard> dashboards = dashboardPort.getUserDashboards(user, includeShared, isAdmin(user), limit,
                    offset, searchString);
            return Response.ok().entity(dashboards).build();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
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
        try {
            Dashboard dashboard = dashboardPort.getUserDashboard(user, id);
            return Response.ok().entity(dashboard).build();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
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
        try {
            dashboardPort.updateDashboard(user, dashboard);
        } catch (Exception e) {
            logger.error("Unable to update dashboard: " + e.getMessage());
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
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
        // return user.type == User.OWNER || (user.role!=null &&
        // user.role.contains("admin"));
        return user.type == User.OWNER;

    }

}
