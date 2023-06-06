package com.signomix.core.domain;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.signomix.common.Organization;
import com.signomix.common.User;
import com.signomix.common.db.IotDatabaseDao;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.db.UserDao;
import com.signomix.common.db.UserDaoIface;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.application.port.in.UserPort;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class OrganizationLogic {
    
    @Inject
    @DataSource("iot")
    AgroalDataSource deviceDataSource;

    @Inject
    @DataSource("user")
    AgroalDataSource userDataSource;

    IotDatabaseIface iotDao;
    UserDaoIface userDao;

    @Inject
    UserPort userPort;

    void onStart(@Observes StartupEvent ev) {
        iotDao = new IotDatabaseDao();
        iotDao.setDatasource(deviceDataSource);
        userDao = new UserDao();
        userDao.setDatasource(userDataSource);
    }

    public List<Organization> getOrganizations(User user, Integer limit, Integer offset) throws ServiceException {
        try {
            return userDao.getOrganizations(limit, offset);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public Organization getOrganization(User user, long organizationId) throws ServiceException {
        try {
            return userDao.getOrganization(organizationId);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public void addOrganization(User user, Organization organization) throws ServiceException {
        try {
            userDao.addOrganization(organization);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public void updateOrganization(User user, Organization organization) throws ServiceException {
        try {
            userDao.updateOrganization(organization);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public void deleteOrganization(User user, long organizationId) throws ServiceException {
        try {
            userDao.deleteOrganization(organizationId);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage());
        }
    }
}
