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
public class UserLogic {
    @Inject
    Logger logger;

    @Inject
    @DataSource("user")
    AgroalDataSource userDataSource;
    @Inject
    @DataSource("iot")
    AgroalDataSource deviceDataSource;

    UserDaoIface userDao;
    IotDatabaseIface iotDao;

    @ConfigProperty(name = "signomix.exception.api.unauthorized")
    String userNotAuthorizedException;

    private long defaultOrganizationId = 0;

    void onStart(@Observes StartupEvent ev) {
        userDao = new UserDao();
        userDao.setDatasource(userDataSource);
        iotDao = new IotDatabaseDao();
        iotDao.setDatasource(deviceDataSource);
        try {
            defaultOrganizationId = iotDao.getParameterValue("system.default.organization", User.ANY);
        } catch (IotDatabaseException e) {
            logger.error("Unable to get default organization id: " + e.getMessage());
        }
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
        user.password = "***";
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
            return userDao.getUsers(limit, offset);
        } else if (isOrganizationAdmin(authorizingUser, authorizingUser.organization)) {
            return userDao.getOrganizationUsers(authorizingUser.organization, limit, offset);
        } else {
            throw new ServiceException(userNotAuthorizedException);
        }
    }

    public List<Organization> getOrganizations(Integer limit, Integer offset) throws ServiceException {
        try {
            return userDao.getOrganizations(limit, offset);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public Organization getOrganization(long organizationId) throws ServiceException {
        try {
            return userDao.getOrganization(organizationId);
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    public void addOrganization(Organization organization) throws ServiceException {
        try {
            userDao.addOrganization(organization);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public void updateOrganization(Organization organization) throws ServiceException {
        try {
            userDao.updateOrganization(organization);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public void deleteOrganization(long organizationId) throws ServiceException {
        try {
            userDao.deleteOrganization(organizationId);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage());
        }
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
        logger.info("user " + user);
        logger.info("user.organization: " + user.organization);
        logger.info("organizationId: " + organizationId);
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
        if (user.type == User.OWNER) {
            return true;
        }
        String owner = null;
        String team = null;
        String admins = null;
        long organizationId = 0;
        if (accessedObject instanceof Dashboard) {
            Dashboard dashboard = (Dashboard) accessedObject;
            team = dashboard.getTeam();
            admins = dashboard.getAdministrators();
            owner = dashboard.getUserID();
            organizationId = dashboard.getOrganizationId();
        } else if (accessedObject instanceof Device) {
            Device device = (Device) accessedObject;
            team = device.getTeam();
            admins = device.getAdministrators();
            owner = device.getUserID();
            organizationId = device.getOrganizationId();
        } else {
            return false;
        }

        // object owner has read/write access
        if (owner.equals(user.uid))
            return true;
        // access depands on organization
        if (user.organization == defaultOrganizationId) {
            if (admins.contains("," + user.uid + ","))
                return true;
            if (!writeAccess) {
                if (team.contains("," + user.uid + ","))
                    return true;
            }
        } else {
            if (!writeAccess) {
                if (user.organization == organizationId)
                    return true;
            } else {
                if (user.organization == organizationId && user.type == User.ADMIN) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }
}
