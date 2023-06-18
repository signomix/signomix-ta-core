package com.signomix.core.adapter.in;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

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
        List<Dashboard> dashboards = dashboardPort.getUserDashboards(user, full, limit, offset);
        return Response.ok().entity(dashboards).build();
    }

    @GET
    @Path("/v2/dashboards/{id}")
    public Response getDashboard(
        @HeaderParam("Authentication") String token, 
        @PathParam("id") String id) {
        User user;
        try {
            user = userPort.getUser(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        Dashboard dashboards = dashboardPort.getUserDashboard(user, id);
        return Response.ok().entity(dashboards).build();
    }
    
}
