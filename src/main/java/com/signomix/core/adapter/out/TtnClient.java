package com.signomix.core.adapter.out;

import java.util.HashMap;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
@RegisterRestClient(configKey="ttn-api")
@Path("/api/v3/as/applications/{appId}/webhooks/{webhookId}/devices/{devId}/down/push")
public interface TtnClient {

    /**
     * Send downlink message to ChirpStack
     * @param key
     * @param eui
     * @param data
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public TtnResponse sendDownlink(
        @HeaderParam("Authorization") String key,
        @HeaderParam("User-Agent") String userAgent,
        @PathParam("appId") String appId,
        @PathParam("webhookId") String webhookId,
        @PathParam("devId") String eui,
        HashMap<String,Object> data);
    
}
