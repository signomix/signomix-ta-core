package com.signomix.core.adapter.in;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.LogFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.annotation.InboundAdapter;
import com.signomix.common.db.IotDatabaseException;
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
            LOG.info("Handling getUser request for uid token: " + uid+" "+token);

            User user;
            User authorizingUser;
            try {
                authorizingUser = userPort.getAuthorizing(authPort.getUserId(token));
            } catch (IotDatabaseException e) {
                LOG.error("getUser: "+e.getMessage());
                e.printStackTrace();
                throw new ServiceException(unauthorizedException);
            } catch (Exception e) {
                LOG.error("getUser: "+e.getMessage());
                e.printStackTrace();
                throw new ServiceException(unauthorizedException);
            }
            if (authorizingUser == null) {
                throw new ServiceException(unauthorizedException);
            }else{
                LOG.info("getUser uid from token: " + authorizingUser.uid);
            }
            try {
                user = userPort.getUser(authorizingUser, uid);
                user.sessionToken = token;
            } catch (IotDatabaseException e) {
                e.printStackTrace();
                throw new ServiceException(userDatabaseException);
            }
            if (user == null) {
                LOG.info("getUser uid not found: " + uid);
                throw new ServiceException(unauthorizedException);
            }
            return Response.ok().entity(user).build();
    }

}
