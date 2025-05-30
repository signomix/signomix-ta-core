package com.signomix.core.adapter.in;

import com.signomix.common.User;
import com.signomix.common.annotation.InboundAdapter;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.iot.Application;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.application.port.in.ApplicationPort;
import com.signomix.core.application.port.in.AuthPort;
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

    @GET
    @Path("/application")
    public Response getApplications(
            @HeaderParam("Authentication") String token,
            @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset,
            @QueryParam("organization") Integer organization,
            @QueryParam("name") String name) {
        try {
            int appLimit = limit == null ? 1000 : limit;
            int appOffset = offset == null ? 0 : offset;
            User user;
            try {
                user = userPort.getAuthorizing(authPort.getUserId(token));
            } catch (IotDatabaseException e) {
                throw new ServiceException(unauthorizedException);
            }
            if (null == user) {
                throw new ServiceException(unauthorizedException);
            }
            if(organization!=null && name!=null && !name.isEmpty()){
                Application application = applicationPort.getApplicationByName(user, organization, name);
                return Response.ok().entity(application).build();
            }
            List<Application> applications = null;
            applications = applicationPort.getApplications(user, appLimit, appOffset);
            return Response.ok().entity(applications).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/application/{id}")
    public Response getApplication(@HeaderParam("Authentication") String token, @PathParam("id") int id) {
        User user;
        try {
            user = userPort.getAuthorizing(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        Application application = applicationPort.getApplication(user, id);
        return Response.ok().entity(application).build();
    }

/*     @GET
    @Path("/application/")
    public Response getApplicationByName(@HeaderParam("Authentication") String token,
            @QueryParam("organization") long organization,
            @QueryParam("name") String name) {
        User user;
        try {
            user = userPort.getAuthorizing(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        Application application = applicationPort.getApplicationByName(user, organization, name);
        return Response.ok().entity(application).build();
    } */

    @DELETE
    @Path("/application/{id}")
    public Response delete(@HeaderParam("Authentication") String token, @PathParam("id") int id) {
        User user;
        try {
            user = userPort.getAuthorizing(authPort.getUserId(token));
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
    public Response updateApplication(@HeaderParam("Authentication") String token, @PathParam("id") int id,
            Application application) {
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
        } catch (Exception e) {
            e.printStackTrace();
            LOG.warn(e.getMessage());
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/application")
    public Response addApplication(@HeaderParam("Authentication") String token, Application application) {
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
            applicationPort.addApplication(user, application);
            return Response.ok().entity("OK").build();
        } catch (Exception e) {
            e.printStackTrace();
            LOG.warn(e.getMessage());
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

}
