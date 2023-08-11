package com.signomix.core.domain;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.Organization;
import com.signomix.common.User;
import com.signomix.common.db.IotDatabaseDao;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.application.port.in.UserPort;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class OrganizationLogic {

    @Inject
    Logger logger;

    @Inject
    @DataSource("iot")
    AgroalDataSource deviceDataSource;

    IotDatabaseIface iotDao;

    @Inject
    UserLogic userLogic;

    @Inject
    UserPort userPort;

    @ConfigProperty(name = "signomix.exception.api.unauthorized")
    String userNotAuthorizedException;

    private long defaultOrganizationId=0;

    void onStart(@Observes StartupEvent ev) {
        iotDao = new IotDatabaseDao();
        iotDao.setDatasource(deviceDataSource);
        try {
            defaultOrganizationId = iotDao.getParameterValue("system.default.organization", User.ANY);
        } catch (IotDatabaseException e) {
            logger.error("Unable to get default organization id: " + e.getMessage());
        }
    }

    /**
     * Gets organizations
     * 
     * @param user
     * @param limit
     * @param offset
     * @return
     * @throws ServiceException
     */
    public List<Organization> getOrganizations(User user, Integer limit, Integer offset) throws ServiceException {
        if (!userLogic.isSystemAdmin(user)) {
            throw new ServiceException(userNotAuthorizedException);
        }
        return userLogic.getOrganizations(limit, offset);
    }

    /**
     * Gets organization.
     * Requires system admin or organization member privileges.
     * 
     * @param user
     * @param organizationId
     * @return
     * @throws ServiceException
     */
    public Organization getOrganization(User user, long organizationId) throws ServiceException {
        if (!(userLogic.isSystemAdmin(user) || userLogic.isOrganizationMember(user, organizationId))) {
            throw new ServiceException(userNotAuthorizedException);
        }
        return userLogic.getOrganization(organizationId);
    }

    /**
     * Adds organization.
     * Requires system admin privileges.
     * 
     * @param user
     * @param organization
     * @throws ServiceException
     */
    public void addOrganization(User user, Organization organization) throws ServiceException {
        if (!userLogic.isSystemAdmin(user)) {
            throw new ServiceException(userNotAuthorizedException);
        }
        userLogic.addOrganization(organization);
    }

    /**
     * Updates organization.
     * Requires system admin privileges.
     * 
     * @param user
     * @param organization
     * @throws ServiceException
     */
    public void updateOrganization(User user, Organization organization) throws ServiceException {
        if (!userLogic.isSystemAdmin(user)) {
            throw new ServiceException(userNotAuthorizedException);
        }
        userLogic.updateOrganization(organization);
    }

    /**
     * Deletes organization.
     * Requires system admin privileges.
     * 
     * @param user
     * @param organizationId
     * @throws ServiceException
     */
    public void deleteOrganization(User user, long organizationId) throws ServiceException {
        if (!userLogic.isSystemAdmin(user)) {
            throw new ServiceException(userNotAuthorizedException);
        }
        if(organizationId==defaultOrganizationId){
            throw new ServiceException("Unable to delete default organization");
        }
        userLogic.deleteOrganization(organizationId);
    }
}
