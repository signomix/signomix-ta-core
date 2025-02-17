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
@RegisterRestClient(configKey="chirpstack-api")
@Path("/api/devices/{eui}/queue") // https://api2.smsplanet.pl
public interface ChirpStackClient {

    /**
     * Send downlink message to ChirpStack
     * @param key
     * @param eui
     * @param data
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public ChirpStackResponse sendDownlink(
        @HeaderParam("Grpc-Metadata-Authorization") String key,
        @PathParam("eui") String eui,
        HashMap<String,Object> data);
    
}
