package com.signomix.core.adapter.in;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import com.signomix.common.annotation.InboundAdapter;
import com.signomix.core.application.port.CommandPort;
import com.signomix.core.domain.Command;

@InboundAdapter
@Path("/api/core/command")
public class CommandRestAdapter {
    private static final Logger LOG = Logger.getLogger(CommandRestAdapter.class);

    @Inject
    CommandPort commandPort;

    @GET
    public Response test() {
        return Response.ok().entity("OK").build();
    }
    
    @POST
    public Response addCommand(Command command) {
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
