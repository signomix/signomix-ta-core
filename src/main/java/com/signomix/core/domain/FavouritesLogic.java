package com.signomix.core.domain;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.Organization;
import com.signomix.common.User;
import com.signomix.common.db.DashboardDao;
import com.signomix.common.db.DashboardIface;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.UserDao;
import com.signomix.common.db.UserDaoIface;
import com.signomix.common.gui.Dashboard;
import com.signomix.common.iot.Device;
import com.signomix.core.application.exception.ServiceException;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;

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

    @Inject
    @DataSource("iot")
    AgroalDataSource dataSource;

    DashboardIface dashboardDao;
    UserDaoIface userDao;


    @ConfigProperty(name = "signomix.exception.dashboard.database")
    String dashboardDatabaseExcepption;

    void onStart(@Observes StartupEvent ev) {
        userDao = new UserDao();
        userDao.setDatasource(userDataSource);
        dashboardDao = new DashboardDao();
        dashboardDao.setDatasource(dataSource);
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
