package com.signomix.core.domain;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.ApplicationDaoIface;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.iot.Application;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.application.port.in.UserPort;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class ApplicationLogic {

    @Inject
    Logger logger;

/*     @Inject
    @DataSource("iot")
    AgroalDataSource deviceDataSource; */
    @Inject
    @DataSource("oltp")
    AgroalDataSource tsDs;

    ApplicationDaoIface applicationDao;

    @Inject
    UserLogic userLogic;

    @Inject
    UserPort userPort;

    @ConfigProperty(name = "signomix.exception.api.unauthorized")
    String userNotAuthorizedException;
    @ConfigProperty(name = "signomix.database.type")
    String databaseType;

    void onStart(@Observes StartupEvent ev) {

            applicationDao = new com.signomix.common.tsdb.ApplicationDao();
            applicationDao.setDatasource(tsDs);

    }

    /**
     * Gets applications.
     * 
     * @param user
     * @param limit
     * @param offset
     * @return
     * @throws ServiceException
     */
    public List<Application> getApplications(User user, Integer limit, Integer offset) throws ServiceException {
        try {
            if (userLogic.isSystemAdmin(user)) {
                return applicationDao.getApplications(limit, offset);
            } else {
                return applicationDao.getApplications(user.organization.intValue(), limit, offset);
            }
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * Gets organization.
     * Requires system admin or organization member privileges.
     * 
     * @param user
     * @param applicationId
     * @return
     * @throws ServiceException
     */
    public Application getApplication(User user, int applicationId) throws ServiceException {
        Application application;
        try {
            application = applicationDao.getApplication(applicationId);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage());
        }
        if (application==null || userLogic.isOrganizationMember(user, application.organization) || userLogic.isSystemAdmin(user)) {
            return application;
        } else {
            throw new ServiceException(userNotAuthorizedException);
        }
    }

    public Application getApplicationByName(User user, int organizationId, String name) throws ServiceException {
        Application application;
        try {
            application = applicationDao.getApplication(organizationId, name);
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
        if (null != application) {
            if (userLogic.isOrganizationMember(user, application.organization) || userLogic.isSystemAdmin(user)) {
                return application;
            } else {
                throw new ServiceException(userNotAuthorizedException);
            }
        } else {
            return null;
        }
    }

    /**
     * Adds organization.
     * Requires system admin privileges.
     * 
     * @param user
     * @param organization
     * @throws ServiceException
     */
    public void addApplication(User user, Application application) throws ServiceException {
        if (!(userLogic.isSystemAdmin(user) || userLogic.isOrganizationMember(user, application.organization))) {
            logger.warn("User not authorized to add application");
            throw new ServiceException(userNotAuthorizedException);
        }
        try {
            applicationDao.addApplication(application);
        } catch (IotDatabaseException e) {
            logger.warn(e.getMessage());
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * Updates organization.
     * Requires system admin privileges.
     * 
     * @param user
     * @param organization
     * @throws ServiceException
     */
    public void updateApplication(User user, Application application) throws ServiceException {
        if (!(userLogic.isSystemAdmin(user)|| userLogic.isOrganizationMember(user, application.organization))) {
            throw new ServiceException(userNotAuthorizedException);
        }
        try {
            applicationDao.updateApplication(application);
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * Deletes organization.
     * Requires system admin privileges.
     * 
     * @param user
     * @param organizationId
     * @throws ServiceException
     */
    public void deleteApplication(User user, int applicationId) throws ServiceException {
        Application application;
        try {
            application = applicationDao.getApplication(applicationId);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage());
        }
        if (!(userLogic.isSystemAdmin(user)|| userLogic.isOrganizationMember(user, application.organization))) {
            throw new ServiceException(userNotAuthorizedException);
        }
        try {
            applicationDao.removeApplication(applicationId);
        } catch (IotDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }
}
