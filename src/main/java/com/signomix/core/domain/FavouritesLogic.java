package com.signomix.core.domain;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.DashboardIface;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.UserDaoIface;
import com.signomix.common.gui.Dashboard;
import com.signomix.core.application.exception.ServiceException;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Klasa zawierająca logikę biznesową dotyczącą autoryzacji.
 * 
 * @author Grzegorz
 */
@ApplicationScoped
public class FavouritesLogic {
    @Inject
    Logger logger;

    @Inject
    @DataSource("user")
    AgroalDataSource userDataSource;

/*     @Inject
    @DataSource("iot")
    AgroalDataSource dataSource; */

    @Inject
    @DataSource("oltp")
    AgroalDataSource tsDs;

    DashboardIface dashboardDao;
    UserDaoIface userDao;


    @ConfigProperty(name = "signomix.exception.dashboard.database")
    String dashboardDatabaseExcepption;
    @ConfigProperty(name = "signomix.database.type")
    String databaseType;

    void onStart(@Observes StartupEvent ev) {
        /* if ("h2".equalsIgnoreCase(databaseType)) {
            dashboardDao = new DashboardDao();
            dashboardDao.setDatasource(dataSource);
            userDao = new UserDao();
            userDao.setDatasource(userDataSource);
        } else  */if ("postgresql".equalsIgnoreCase(databaseType)) {
            dashboardDao = new com.signomix.common.tsdb.DashboardDao();
            dashboardDao.setDatasource(tsDs);
            userDao = new com.signomix.common.tsdb.UserDao();
            userDao.setDatasource(userDataSource);
        } else {
            logger.error("Unknown database type: " + databaseType);
        }
    }

    public List<Dashboard> getFavouriteDashboards(User user) throws ServiceException {
        try {
            return dashboardDao.getFavouriteDashboards(user.uid);
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            throw new ServiceException(dashboardDatabaseExcepption);
        }
    }

    public void addFavouriteDashboard(User user, String dashboardId) throws ServiceException {
        try {
            dashboardDao.addFavouriteDashboard(user.uid, dashboardId);
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            throw new ServiceException(dashboardDatabaseExcepption);
        }
    }

    public void removeFavouriteDashboard(User user, String dashboardId) throws ServiceException {
        try {
            dashboardDao.removeFavouriteDashboard(user.uid, dashboardId);
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            throw new ServiceException(dashboardDatabaseExcepption);
        }
    }
}
