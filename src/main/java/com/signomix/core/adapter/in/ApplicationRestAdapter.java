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
import com.signomix.common.iot.Application;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.application.port.in.ApplicationPort;
import com.signomix.core.application.port.in.AuthPort;
import com.signomix.core.application.port.in.UserPort;

@InboundAdapter
@Path("/api/core")
public class ApplicationRestAdapter {

    @Inject
    Logger LOG;

    @Inject
    AuthPort authPort;
    @Inject
    UserPort userPort;
    @Inject
    ApplicationPort applicationPort;

    @ConfigProperty(name = "signomix.exception.api.unauthorized")
    String unauthorizedException;

    @Path("/application")
    public Response getApplications(
            @HeaderParam("Authentication") String token,
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
        List<Application> applications = null;
        applications = applicationPort.getApplications(user, limit, offset);
        return Response.ok().entity(applications).build();
    }

    @GET
    @Path("/application/{id}")
    public Response getApplication(@HeaderParam("Authentication") String token, @PathParam("id") long id) {
        User user;
        try {
            user = userPort.getUser(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        Application application = applicationPort.getApplication(user, id);
        return Response.ok().entity(application).build();
    }

    @GET
    @Path("/application/")
    public Response getApplicationByName(@HeaderParam("Authentication") String token, @QueryParam("name") String name) {
        User user;
        try {
            user = userPort.getUser(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        Application application = applicationPort.getApplicationByName(user, name);
        return Response.ok().entity(application).build();
    }

    @DELETE
    @Path("/application/{id}")
    public Response delete(@HeaderParam("Authentication") String token, @PathParam("id") long id) {
        User user;
        try {
            user = userPort.getUser(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        applicationPort.deleteApplication(user, id);
        return Response.ok().entity("OK").build();
    }

    @PUT
    @Path("/application/{id}")
    public Response updateApplication(@HeaderParam("Authentication") String token, @PathParam("id") long id,
            Application application) {
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
        if (null == application) {
            LOG.warn("Organization is null");
            throw new ServiceException("Organization not found");
        }

        try {
            applicationPort.updateApplication(user, application);
        } catch (Exception e) {
            LOG.warn(e.getMessage());
            throw new ServiceException(e.getMessage());
        }
        return Response.ok().entity("OK").build();
    }

    @POST
    @Path("/application")
    public Response addApplication(@HeaderParam("Authentication") String token, Application application) {
        User user;
        try {
            user = userPort.getUser(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        applicationPort.addApplication(user, application);
        return Response.ok().entity("OK").build();
    }

}
