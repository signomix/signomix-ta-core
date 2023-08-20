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

import com.signomix.common.Organization;
import com.signomix.common.User;
import com.signomix.common.annotation.InboundAdapter;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.application.port.in.AuthPort;
import com.signomix.core.application.port.in.OrganizationPort;
import com.signomix.core.application.port.in.UserPort;

@InboundAdapter
@Path("/api/core")
public class OrganizationRestAdapter {

    @Inject
    Logger LOG;
    
    @Inject
    AuthPort authPort;
    @Inject
    UserPort userPort;
    @Inject
    OrganizationPort organizationPort;

    @ConfigProperty(name = "signomix.exception.api.unauthorized")
    String unauthorizedException;

    @Path("/organization")
    public Response getOrganizations(
            @HeaderParam("Authentication") String token,
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
        List<Organization> organizations = null;
        organizations= organizationPort.getOrganizations(user, limit, offset);
        return Response.ok().entity(organizations).build();
    }

    @GET
    @Path("/organization/{id}")
    public Response getOrganization(@HeaderParam("Authentication") String token, @PathParam("id") long id,
            @QueryParam("full") Boolean full) {
        User user;
        try {
            user = userPort.getAuthorizing(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        Organization organization = null;
        organization = organizationPort.getOrganization(user, id);
        return Response.ok().entity(organization).build();
    }

    @DELETE
    @Path("/organization/{id}")
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
        organizationPort.deleteOrganization(user, id);
        return Response.ok().entity("OK").build();
    }

    @PUT
    @Path("/organization/{id}")
    public Response updateOrganization(@HeaderParam("Authentication") String token, @PathParam("id") long id,
            Organization organization) {
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
        if (null == organization) {
            LOG.warn("Organization is null");
            throw new ServiceException("Organization not found");
        }

        try {
            organizationPort.updateOrganization(user, organization);
        } catch (Exception e) {
            LOG.warn(e.getMessage());
            throw new ServiceException(e.getMessage());
        }
        return Response.ok().entity("OK").build();
    }

    @POST
    @Path("/organization")
    public Response addOrganization(@HeaderParam("Authentication") String token, Organization organization) {
        User user;
        try {
            user = userPort.getAuthorizing(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            throw new ServiceException(unauthorizedException);
        }
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        organizationPort.addOrganization(user, organization);
        return Response.ok().entity("OK").build();
    }

}
