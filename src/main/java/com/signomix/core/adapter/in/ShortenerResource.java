package com.signomix.core.adapter.in;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.ShortenerDaoIface;
import com.signomix.common.tsdb.ShortenerDao;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/gt")
public class ShortenerResource {

    @Inject
    Logger logger;

    @Inject
    @DataSource("oltp")
    AgroalDataSource shortenerDataSource;
    ShortenerDaoIface shortenerDao;

    @ConfigProperty(name = "signomix.database.type")
    String databaseType;

    void onStart(@Observes StartupEvent ev) {
        if ("postgresql".equalsIgnoreCase(databaseType)) {
            shortenerDao = new ShortenerDao();
            shortenerDao.setDatasource(shortenerDataSource);
        } else {
            logger.error("Unsupported database type: " + databaseType);
        }

    }

    @GET
    @Path("/{path}")
    public Response getTargetUrl(@PathParam("path") String path) {
        String target = null;
        try {
            target = shortenerDao.getTarget(path);
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }

        if (null != target && !target.isEmpty()) {
            return Response.status(Response.Status.MOVED_PERMANENTLY).header("Location", target).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // TODO: POST, DELETE, PUT

    /**
     * Registers in the database a new short URL for the given target URL.
     * 
     * @return
     */
/*     @POST
    public Response registerUrl(@HeaderParam("Authentication") String token, UrlShortcut shortcut) {
        String target = shortcut.getTarget();
        String source = shortcut.getSource();
        try {
            shortenerDao.putUrl(source, target);
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        if (null != source && !source.isEmpty()) {
            return Response.status(Response.Status.OK).entity(source).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    } */

}