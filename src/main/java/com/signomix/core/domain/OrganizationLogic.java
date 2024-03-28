package com.signomix.core.domain;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.Organization;
import com.signomix.common.Tenant;
import com.signomix.common.User;
import com.signomix.common.db.IotDatabaseDao;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.db.OrganizationDaoIface;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.application.port.in.UserPort;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrganizationLogic {

    @Inject
    Logger logger;

    @Inject
    @DataSource("iot")
    AgroalDataSource deviceDataSource;

    @Inject
    @DataSource("oltp")
    AgroalDataSource tsDs;

    IotDatabaseIface iotDao;
    OrganizationDaoIface organizationDao;

    @Inject
    UserLogic userLogic;

    @Inject
    UserPort userPort;

    @ConfigProperty(name = "signomix.exception.api.unauthorized")
    String userNotAuthorizedException;

    @ConfigProperty(name = "signomix.database.type")
    String databaseType;

    private long defaultOrganizationId = 0;

    void onStart(@Observes StartupEvent ev) {
        if ("h2".equalsIgnoreCase(databaseType)) {
            iotDao = new IotDatabaseDao();
            iotDao.setDatasource(deviceDataSource);
            defaultOrganizationId = 0;
            organizationDao = new com.signomix.common.db.OrganizationDao();
            organizationDao.setDatasource(deviceDataSource);
        } else if ("postgresql".equalsIgnoreCase(databaseType)) {
            iotDao = new com.signomix.common.tsdb.IotDatabaseDao();
            iotDao.setDatasource(tsDs);
            defaultOrganizationId = 1;
            organizationDao = new com.signomix.common.tsdb.OrganizationDao();
            organizationDao.setDatasource(tsDs);
        } else {
            logger.error("Unknown database type: " + databaseType);
        }
        /*
         * try {
         * defaultOrganizationId =
         * iotDao.getParameterValue("system.default.organization", User.ANY);
         * } catch (IotDatabaseException e) {
         * logger.error("Unable to get default organization id: " + e.getMessage());
         * }
         */
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
        if (organizationId == defaultOrganizationId) {
            throw new ServiceException("Unable to delete default organization");
        }
        userLogic.deleteOrganization(organizationId);
    }

    public Tenant getTenant(User user, Integer tenantId) throws ServiceException {
        if (!(userLogic.isSystemAdmin(user) || userLogic.isOrganizationAdmin(user, user.organization)
                || user.tenant == tenantId)) {
            return null;
        }
        return getTenant(user, user.tenant);
    }

    public List<Tenant> getTenants(User user, Long organizationId, Integer limit, Integer offset)
            throws ServiceException {
        if (!(userLogic.isSystemAdmin(user) || userLogic.isOrganizationAdmin(user, organizationId))) {
            return null;
        }
        List<Tenant> tenants;
        try {
            return organizationDao.getTenants(organizationId, limit, offset);
        } catch (IotDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return new ArrayList<Tenant>();
        }
        
    }
}
