package com.signomix.core.adapter.out;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/api/auth")
@RegisterRestClient
public interface SignomixAuthClient {

    @GET
    @Path("/user")
    Response getUser(@HeaderParam("Authentication") String token);

    @GET
    @Path("/token/{dashboardSlug}")
    Response getToken(@PathParam("Authentication") String dashboardSlug);

}
