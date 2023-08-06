package com.signomix.core.adapter.in;

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
import com.signomix.common.iot.Device;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.application.port.in.AuthPort;
import com.signomix.core.application.port.in.DevicePort;
import com.signomix.core.application.port.in.UserPort;

@InboundAdapter
@Path("/api/core")
public class UserRestAdapter {
    private static final Logger LOG = Logger.getLogger(DeviceRestAdapter.class);

    @Inject
    AuthPort authPort;
    @Inject
    UserPort userPort;
    @Inject
    DevicePort devicePort;

    @ConfigProperty(name = "signomix.exception.api.unauthorized")
    String unauthorizedException;
    @ConfigProperty(name = "signomix.exception.user.database")
    String userDatabaseException;

    @GET
    @Path("/user/{uid}")
    public Response getUser(
        @HeaderParam("Authentication") String token, 
        @PathParam("uid") String uid) {
            LOG.info("Handling getUser request for uid: " + uid);
        User user;
        try {
            user = userPort.getUser(authPort.getUserId(token));
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            throw new ServiceException(unauthorizedException);
        }
        if (null == user || !user.uid.equals(uid)) {
            LOG.warn("User uid not match: " + uid + " != " + user.uid);
            throw new ServiceException(unauthorizedException);
        }
        try {
            user = userPort.getUser(uid);
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            throw new ServiceException(userDatabaseException);
        }
        return Response.ok().entity(user).build();
    }


}
