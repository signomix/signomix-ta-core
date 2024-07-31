package com.signomix.core.adapter.in;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.annotation.InboundAdapter;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.iot.Alert;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.application.port.in.AuthPort;
import com.signomix.core.application.port.in.NotificationsPort;
import com.signomix.core.application.port.in.UserPort;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@InboundAdapter
@Path("/api/core")
public class NotificationRestAdapter {

    @Inject
    Logger logger;

    @Inject
    AuthPort authPort;
    @Inject
    UserPort userPort;
    @Inject
    NotificationsPort notificationsPort;

    @ConfigProperty(name = "signomix.exception.api.unauthorized")
    String unauthorizedException;

    @GET
    @Path("/notifications")
    public Response getNotifications(
            @HeaderParam("Authentication") String token,
            @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset) {
        try {
            String name = null;
            int nLimit = (limit == null || limit == 0 ? 1000 : limit);
            int nOffset = offset == null ? 0 : offset;
            User user;
            try {
                user = userPort.getAuthorizing(authPort.updateUserId(token));
            } catch (IotDatabaseException e) {
                throw new ServiceException(unauthorizedException);
            }
            if (null == user) {
                throw new ServiceException(unauthorizedException);
            }
            logger.debug("getNotifications: " + user.uid + " " + nLimit + " " + nOffset);
            List<Alert> notifications = new ArrayList<>();
            notifications = notificationsPort.getNotifications(user, nLimit, nOffset);
            return Response.ok().entity(notifications).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/notifications/status")
    public Response getNotificationStatus(
            @HeaderParam("Authentication") String token) {
        try {
            User user;
            try {
                user = userPort.getAuthorizing(authPort.updateUserId(token));
            } catch (IotDatabaseException e) {
                throw new ServiceException(unauthorizedException);
            }
            if (null == user) {
                throw new ServiceException(unauthorizedException);
            }
            logger.debug("getNotificationStatus: " + user.uid);
            Long count = notificationsPort.getNotificationsStatus(user);
            return Response.ok().entity(count).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/notifications/{id}")
    public Response delete(@HeaderParam("Authentication") String token, @PathParam("id") long id) {
        User user;
        try {
            user = userPort.getAuthorizing(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        logger.info("delete: " + user.uid + " " + id);
        try {
            if (id == -1) {
                notificationsPort.deleteNotifications(user);
            } else {
                notificationsPort.deleteNotification(user, id);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity(e.getMessage()).build();
        }
        return Response.ok().entity("OK").build();
    }

}
