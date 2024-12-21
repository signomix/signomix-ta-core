package com.signomix.core.domain;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.iot.Alert;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.application.port.in.UserPort;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class NotificationsLogic {

    @Inject
    Logger logger;

/*     @Inject
    @DataSource("iot")
    AgroalDataSource deviceDataSource; */

    @Inject
    @DataSource("oltp")
    AgroalDataSource tsDs;

    IotDatabaseIface iotDatabaseDao;

    @Inject
    UserLogic userLogic;

    @Inject
    UserPort userPort;

    @ConfigProperty(name = "signomix.exception.api.unauthorized")
    String userNotAuthorizedException;

    @ConfigProperty(name = "signomix.database.type")
    String databaseType;

    void onStart(@Observes StartupEvent ev) {
        /* if ("h2".equalsIgnoreCase(databaseType)) {
            iotDatabaseDao = new IotDatabaseDao();
            iotDatabaseDao.setDatasource(deviceDataSource);
        } else */ 
         if ("postgresql".equalsIgnoreCase(databaseType)) {
            iotDatabaseDao = new com.signomix.common.tsdb.IotDatabaseDao();
            iotDatabaseDao.setDatasource(tsDs);
        }else {
            logger.error("Unknown database type: " + databaseType);
        }

    }

    /**
     * Gets notifications for user.
     * 
     * @param user
     * @param limit
     * @param offset
     * @return
     * @throws ServiceException
     */
    public List<Alert> getNotifications(User user, Integer limit, Integer offset) throws ServiceException {
        try {
            return iotDatabaseDao.getAlerts(user.uid, limit, offset, true);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * Deletes notification.
     * Requires system admin privileges.
     * 
     * @param user
     * @param alertId
     * @throws ServiceException
     */
    public void deleteNotification(User user, long alertId) throws ServiceException {
        try {
            iotDatabaseDao.removeAlert(alertId);
        } catch (IotDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    public void deleteAllNotifications(User user) throws ServiceException {
        try {
            iotDatabaseDao.removeAlerts(user.uid);
        } catch (IotDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    public Long getNotificationsStatus(User user) {
        try {
            return iotDatabaseDao.getAlertsCount(user.uid);
        } catch (IotDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 0L;
        }
    }
}
