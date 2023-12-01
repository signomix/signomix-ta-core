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
import com.signomix.core.application.port.in.FavouritesPort;
import com.signomix.core.application.port.in.UserPort;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@InboundAdapter
@Path("/api/core/favourite")
public class FavouritesRestAdapter {
    
    @Inject
    Logger logger;

    @Inject
    AuthPort authPort;
    @Inject
    UserPort userPort;
    @Inject
    FavouritesPort favouritesPort;

    @ConfigProperty(name = "signomix.exception.api.unauthorized")
    String unauthorizedException;
    @ConfigProperty(name = "signomix.exception.user.database")
    String userDatabaseException;

    @GET
    @Path("/dashboards")
    public Response getFavouriteDashboards(
        @HeaderParam("Authentication") String token) {
        User authorizedUser;
        try {
            authorizedUser = userPort.getAuthorizing(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            throw new ServiceException(unauthorizedException);
        }
        List<Dashboard> dashboards;
        try {
            dashboards = favouritesPort.getFavouriteDashboards(authorizedUser);
        } catch (ServiceException e) {
            e.printStackTrace();
            throw new ServiceException(userDatabaseException);
        }
        return Response.ok().entity(dashboards).build();
    }

    @POST
    @Path("/dashboards/{dashboardId}")
    public Response addFavouriteDashboard(
        @HeaderParam("Authentication") String token, 
        @PathParam("dashboardId") String dashboardId) {
        User authorizedUser;
        try {
            authorizedUser = userPort.getAuthorizing(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            throw new ServiceException(unauthorizedException);
        }
        try {
            favouritesPort.addFavouriteDashboard(authorizedUser, dashboardId);
        } catch (ServiceException e) {
            e.printStackTrace();
            throw new ServiceException(userDatabaseException);
        }
        return Response.ok().build();
    }

    @DELETE
    @Path("/dashboards/{dashboardId}")
    public Response removeFavouriteDashboard(
        @HeaderParam("Authentication") String token, 
        @PathParam("dashboardId") String dashboardId) {
        User authorizedUser;
        try {
            authorizedUser = userPort.getAuthorizing(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            throw new ServiceException(unauthorizedException);
        }
        try {
            favouritesPort.removeFavouriteDashboard(authorizedUser, dashboardId);
        } catch (ServiceException e) {
            e.printStackTrace();
            throw new ServiceException(userDatabaseException);
        }
        return Response.ok().build();
    }


}
