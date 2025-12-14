package com.signomix.core.domain;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.Organization;
import com.signomix.common.User;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.db.UserDaoIface;
import com.signomix.common.gui.Dashboard;
import com.signomix.common.gui.DashboardTemplate;
import com.signomix.common.iot.Device;
import com.signomix.common.iot.DeviceGroup;
import com.signomix.common.iot.sentinel.SentinelConfig;
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
public class UserLogic {
    @Inject
    Logger logger;

    @Inject
    @DataSource("user")
    AgroalDataSource userDataSource;
    /* @Inject
    @DataSource("iot")
    AgroalDataSource deviceDataSource; */

    @Inject
    @DataSource("oltp")
    AgroalDataSource tsDs;

    UserDaoIface userDao;
    IotDatabaseIface iotDao;

    @ConfigProperty(name = "signomix.exception.api.unauthorized")
    String userNotAuthorizedException;

    @ConfigProperty(name = "signomix.database.type")
    String databaseType;

    private long defaultOrganizationId = 0;

    void onStart(@Observes StartupEvent ev) {
        /* if ("h2".equalsIgnoreCase(databaseType)) {
            userDao = new UserDao();
            userDao.setDatasource(userDataSource);
            iotDao = new IotDatabaseDao();
            iotDao.setDatasource(deviceDataSource);
            defaultOrganizationId = 0;
        } else */ 
         if ("postgresql".equalsIgnoreCase(databaseType)) {
            userDao = new com.signomix.common.tsdb.UserDao();
            userDao.setDatasource(tsDs);
            iotDao = new com.signomix.common.tsdb.IotDatabaseDao();
            iotDao.setDatasource(tsDs);
            defaultOrganizationId = 1;
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
     * Gets user by uid
     * 
     * @param uid
     * @return
     * @throws IotDatabaseException
     */
    public User getUser(User authorizingUser, String uid) throws IotDatabaseException {
        if (authorizingUser == null) {
            throw new ServiceException(userNotAuthorizedException);
        }
        User user = userDao.getUser(uid);
        // user.password = "***";
        if (isSystemAdmin(authorizingUser)
                || isOrganizationAdmin(authorizingUser, user.organization)
                || authorizingUser.uid.equals(uid)) {
            return user;
        } else {
            throw new ServiceException(userNotAuthorizedException);
        }
    }

    public void updateUser(User authorizingUser, User user) throws IotDatabaseException {
        User actualUser = userDao.getUser(user.uid);
        if (actualUser == null) {
            throw new ServiceException("User not found");
        }
        user.number = actualUser.number;
        user.password = actualUser.password;
        user.sessionToken = actualUser.sessionToken;
        user.createdAt = actualUser.createdAt;
        // user can update only himself or if he is system admin or organization admin
        if (!(isOrganizationAdmin(authorizingUser, actualUser.organization)
                || isSystemAdmin(authorizingUser) || authorizingUser.uid.equals(user.uid))) {
            throw new ServiceException(userNotAuthorizedException);
        }
        if (!isSystemAdmin(authorizingUser)) {
            user.organization = actualUser.organization;
            user.uid = actualUser.uid;
            user.type = actualUser.type;
            user.credits = actualUser.credits;
            user.services = actualUser.services;
            user.unregisterRequested = actualUser.unregisterRequested;
        } else if (!isOrganizationAdmin(authorizingUser, actualUser.organization)) {
            user.authStatus = actualUser.authStatus;
            user.confirmString = actualUser.confirmString;
            user.confirmed = actualUser.confirmed;
            user.role = actualUser.role;
        }
        userDao.updateUser(user);
    }

    public User getAuthorizingUser(String uid) throws IotDatabaseException {
        return userDao.getUser(uid);
    }

    public List<User> getUsers(User authorizingUser, Integer limit, Integer offset) throws IotDatabaseException {
        if (authorizingUser == null) {
            throw new ServiceException(userNotAuthorizedException);
        }
        if (isSystemAdmin(authorizingUser)) {
            return userDao.getUsers(limit, offset, null, null);
        } else if (isOrganizationAdmin(authorizingUser, authorizingUser.organization)) {
            return userDao.getOrganizationUsers(authorizingUser.organization, limit, offset, null, null);
        } else {
            throw new ServiceException(userNotAuthorizedException);
        }
    }

    public List<Organization> getOrganizations(Integer limit, Integer offset) throws ServiceException {
        return null;
        /*
         * try {
         * return userDao.getOrganizations(limit, offset);
         * } catch (IotDatabaseException e) {
         * throw new ServiceException(e.getMessage());
         * }
         */
    }

    public Organization getOrganization(long organizationId) throws ServiceException {
        return null;
        /*
         * try {
         * return userDao.getOrganization(organizationId);
         * } catch (IotDatabaseException e) {
         * e.printStackTrace();
         * throw new ServiceException(e.getMessage());
         * }
         */
    }

    public void addOrganization(Organization organization) throws ServiceException {
        /*
         * try {
         * userDao.addOrganization(organization);
         * } catch (IotDatabaseException e) {
         * throw new ServiceException(e.getMessage());
         * }
         */
    }

    public void updateOrganization(Organization organization) throws ServiceException {
        /*
         * try {
         * userDao.updateOrganization(organization);
         * } catch (IotDatabaseException e) {
         * throw new ServiceException(e.getMessage());
         * }
         */
    }

    public void deleteOrganization(long organizationId) throws ServiceException {
        /*
         * try {
         * userDao.deleteOrganization(organizationId);
         * } catch (IotDatabaseException e) {
         * throw new ServiceException(e.getMessage());
         * }
         */
    }

    /**
     * Checks if user is system admin
     * 
     * @param user
     * @return
     */
    public boolean isSystemAdmin(User user) {
        return user != null && user.type == User.OWNER;
    }

    /**
     * Checks if user is organization admin
     * 
     * @param user
     * @param organizationId
     * @return
     */
    public boolean isOrganizationAdmin(User user, long organizationId) {
        return user != null && user.organization == organizationId && user.type == User.ORGANIZATION_ADMIN;
    }

    public boolean isTenantAdmin(User user, long organizationId){
        return user != null && user.organization == organizationId && user.type == User.ADMIN;
    }

    /**
     * Checks if user is organization user
     * 
     * @param user
     * @param organizationId
     * @return
     */
    public boolean isOrganizationMember(User user, long organizationId) {
        if(logger.isDebugEnabled()){
            logger.debug("user " + user);
            logger.debug("user.organization: " + (user!=null?user.organization:"null"));
            logger.debug("organizationId: " + organizationId);
        }
        return user != null && user.organization == organizationId;
    }

    /**
     * Checks user access to object (Device, Dashboard)
     * 
     * @param user
     * @param writeAccess
     * @param defaultOrganizationId
     * @param accessedObject
     * @return
     */
    public boolean hasObjectAccess(
            User user,
            boolean writeAccess,
            long defaultOrganizationId,
            Object accessedObject) {
        // Platfor administrator has access to all objects
        if (accessedObject == null) {
            logger.error("Accessed object is null");
            return false;
        }
        // System admin has access to all objects
        if (user.type == User.OWNER) {
            return true;
        }
        String owner = null;
        String team = null;
        String admins = null;
        long organizationId = 0;
        int tenantId = 0;
        boolean isPublic = false;
        String path = "";
        if (accessedObject instanceof Dashboard) {
            Dashboard dashboard = (Dashboard) accessedObject;
            team = dashboard.getTeam();
            admins = dashboard.getAdministrators();
            owner = dashboard.getUserID();
            organizationId = dashboard.getOrganizationId();
            // tenantId = dashboard.getTenantId();
            isPublic = dashboard.isShared();
            // path = dashboard.getPath();
        } else if (accessedObject instanceof DashboardTemplate) {
            DashboardTemplate dashboard = (DashboardTemplate) accessedObject;
            organizationId = dashboard.getOrganizationId();
            owner = "";
            team = "";
            admins = "";
            // path = dashboard.getPath();
            if(user.organization == dashboard.getOrganizationId()) {
                return true;
            }else{
                logger.error("User " + user.uid +"in organization ("+user.organization+")"+ " has no access to dashboard template in organization " + dashboard.getOrganizationId());
                return false;
            }
        } else if (accessedObject instanceof Device) {
            Device device = (Device) accessedObject;
            team = device.getTeam();
            admins = device.getAdministrators();
            owner = device.getUserID();
            organizationId = device.getOrganizationId();
            // tenantId = device.getTenantId();
            isPublic = device.getTeam().contains(",public,");
            path = device.getPath();
        } else if (accessedObject instanceof DeviceGroup) {
            DeviceGroup group = (DeviceGroup) accessedObject;
            team = group.getTeam();
            admins = group.getAdministrators();
            owner = group.getUserID();
            organizationId = group.getOrganization();
            // tenantId = group.getTenantId();
            isPublic = group.getTeam().contains(",public,");
            // path = group.getPath();
        } else if (accessedObject instanceof SentinelConfig) {
            SentinelConfig sentinelConfig = (SentinelConfig) accessedObject;
            team = sentinelConfig.team;
            admins = sentinelConfig.administrators;
            owner = sentinelConfig.userId;
            organizationId = sentinelConfig.organizationId;
            // tenantId = sentinelConfig.tenantId;
            isPublic = sentinelConfig.team.contains(",public,");
            // path = sentinelConfig.path;
        } else {
            logger.error("Unknown object type: " + accessedObject.getClass().getName());
            return false;
        }
        if(logger.isDebugEnabled()){
        logger.debug("hasObjectAccess: " + user.uid + " " + owner + " " + team + " " + admins + " " + organizationId + " "
                + tenantId + " " + isPublic + " " + path + " " + writeAccess + " " + user.path);
        }
        // object owner has read/write access
        if (owner.equals(user.uid)) {
            return true;
        }
        if (user.uid.equalsIgnoreCase("public")) {
            return isPublic;
        }
        // access depands on organization
        if (user.organization == defaultOrganizationId) {
            if (admins.contains("," + user.uid + ","))
                return true;
            if (!writeAccess) {
                if (team.contains("," + user.uid + ","))
                    return true;
            }
        } else {
            if (user.tenant < 1) {
                if (!writeAccess) {
                    // user has read access to all objects in organization
                    if (user.organization == organizationId)
                        return true;
                } else {
                    // organization admin has write access to all objects in organization
                    if (user.organization == organizationId && user.type == User.MANAGING_ADMIN) {
                        return true;
                    }
                }
            } else {
                if (user.organization != organizationId /* || user.tenant != tenantId */) {
                    return false;
                }
                boolean readAccess = false;
                String parentPath;
                boolean withChildren = false;
                if (user.path.endsWith(".ALL") || user.path.endsWith(".*")) {
                    parentPath = user.path.substring(0, user.path.lastIndexOf("."));
                    withChildren = true;
                } else {
                    parentPath = user.path;
                }
                // tenant users have read access to objects depending on path
                if (path.equalsIgnoreCase(parentPath)) {
                    readAccess = true;
                } else if (withChildren) {
                    readAccess = path.startsWith(parentPath + ".");
                }
                if (writeAccess) {
                    return readAccess && user.type == User.ADMIN;
                } else {
                    return readAccess;
                }

            }
        }
        return false;
    }
}
