package com.signomix.core.adapter.in;


import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.annotation.InboundAdapter;
import com.signomix.core.application.port.in.AuthPort;
import com.signomix.core.application.port.in.CommandPort;
import com.signomix.core.domain.Command;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@InboundAdapter
@Path("/api/core/command")
public class CommandRestAdapter {
    private static final Logger LOG = Logger.getLogger(CommandRestAdapter.class);

    @Inject
    CommandPort commandPort;
    @Inject
    AuthPort authPort;

    @GET
    public Response test() {
        return Response.ok().entity("OK").build();
    }
    
    @POST
    public Response processCommand(@HeaderParam("Authorization") String token, Command command) {
        User user=authPort.getUser(token);
        LOG.info("User:"+(null==user?"null":user.name));
        if(null==user || !user.role.contains("admin")){
            return Response.status(Status.FORBIDDEN).build();
        }
        switch(command.command().toLowerCase()){
            case "backup":
                commandPort.runBackup();
                break;
            default:
                LOG.warn("Unknown command: "+command.command());
        }
        return Response.ok().build();
    }

}
