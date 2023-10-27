package com.signomix.core.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.signomix.common.User;
import com.signomix.common.db.DashboardDao;
import com.signomix.common.db.DashboardIface;
import com.signomix.common.db.IotDatabaseDao;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.gui.Dashboard;
import com.signomix.common.gui.DashboardItem;
import com.signomix.common.gui.DashboardTemplate;
import com.signomix.common.gui.Widget;
import com.signomix.common.iot.Channel;
import com.signomix.common.iot.Device;
import com.signomix.common.iot.DeviceGroup;
import com.signomix.core.application.exception.ServiceException;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class GroupLogic {

    @Inject
    Logger logger;

    @Inject
    @DataSource("iot")
    AgroalDataSource dataSource;
    @Inject
    @DataSource("oltp")
    AgroalDataSource tsDs;

    IotDatabaseIface iotDao;

    @Inject
    UserLogic userLogic;

    @Inject
    EuiGenerator euiGenerator;

    long defaultOrganizationId = 0;

    @ConfigProperty(name = "signomix.exception.api.unauthorized", defaultValue = "")
    String exceptionApiUnauthorized;
    @ConfigProperty(name = "signomix.database.type")
    String databaseType;

    void onStart(@Observes StartupEvent ev) {
        if ("h2".equalsIgnoreCase(databaseType)) {
            iotDao = new IotDatabaseDao();
            iotDao.setDatasource(dataSource);
            defaultOrganizationId = 0;
        } else if ("postgresql".equalsIgnoreCase(databaseType)) {
            iotDao = new com.signomix.common.tsdb.IotDatabaseDao();
            iotDao.setDatasource(tsDs);
            defaultOrganizationId = 1;
        } else {
            logger.error("Unknown database type: " + databaseType);
        }
    }

    public List<DeviceGroup> getUserGroups(User user, Integer limit,
            Integer offset, String searchString, boolean includeShared) throws ServiceException {
        try {
            if (user.organization != defaultOrganizationId) {
                return iotDao.getOrganizationGroups(user.organization, limit, offset, searchString);
            } else {
                return iotDao.getUserGroups(user.uid, limit, offset, searchString, includeShared);
            }
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public DeviceGroup getGroup(User user, String groupEui) throws ServiceException {
        try {
            DeviceGroup group = iotDao.getGroup(groupEui);
            if (null != group) {
                if (!userLogic.hasObjectAccess(user, false, defaultOrganizationId, group)) {
                    throw new ServiceException(exceptionApiUnauthorized);
                }
            } else {
                throw new ServiceException(exceptionApiUnauthorized);
            }
            return group;
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public DeviceGroup updateGroup(User user, DeviceGroup updatedGroup) throws ServiceException {
        try {
            DeviceGroup group = iotDao.getGroup(updatedGroup.getEUI());
            if (null != group) {
                if (!userLogic.hasObjectAccess(user, true, defaultOrganizationId, group)) {
                    throw new ServiceException(exceptionApiUnauthorized);
                }
            } else {
                throw new ServiceException(exceptionApiUnauthorized);
            }
            iotDao.updateGroup(updatedGroup);
            return updatedGroup;
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public DeviceGroup saveGroup(User user, DeviceGroup newGroup) throws ServiceException {
        try {
            DeviceGroup group = iotDao.getGroup(newGroup.getEUI());
            if (null != group) {
                throw new ServiceException("Dashboard already exists");
            }
            newGroup.setEUI(euiGenerator.createEui("S-"));
            newGroup.setUserID(user.uid);
            newGroup.setOrganization(user.organization);
            iotDao.createGroup(newGroup);
            return newGroup;
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public void removeGroup(User user, String groupEUI) throws ServiceException {
        try {
            DeviceGroup group = iotDao.getGroup(groupEUI);
            if (null != group) {
                if (!userLogic.hasObjectAccess(user, true, defaultOrganizationId, group)) {
                    throw new ServiceException(exceptionApiUnauthorized);
                }
            } else {
                throw new ServiceException(exceptionApiUnauthorized);
            }
            iotDao.deleteGroup(groupEUI);
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

}
