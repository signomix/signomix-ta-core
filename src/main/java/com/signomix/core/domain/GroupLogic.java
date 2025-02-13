package com.signomix.core.domain;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.iot.DeviceGroup;
import com.signomix.core.application.exception.ServiceException;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class GroupLogic {

    @Inject
    Logger logger;

/*     @Inject
    @DataSource("iot")
    AgroalDataSource dataSource; */
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
        /* if ("h2".equalsIgnoreCase(databaseType)) {
            iotDao = new IotDatabaseDao();
            iotDao.setDatasource(dataSource);
            defaultOrganizationId = 0;
        } else */ 
         if ("postgresql".equalsIgnoreCase(databaseType)) {
            iotDao = new com.signomix.common.tsdb.IotDatabaseDao();
            iotDao.setDatasource(tsDs);
            defaultOrganizationId = 1;
        } else {
            logger.error("Unknown database type: " + databaseType);
        }
    }

    public List<DeviceGroup> getUserGroups(User user, Integer limit,
            Integer offset, String searchString) throws ServiceException {
                int requestedLimit = limit!=null?limit:100;
                int requestedOffset = offset!=null?offset:0;
        try {
            if (user.organization != defaultOrganizationId) {
                return iotDao.getOrganizationGroups(user.organization, requestedLimit, requestedOffset, searchString);
            } else {
                return iotDao.getUserGroups(user.uid, requestedLimit, requestedOffset, searchString);
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
                if (user!=null && !userLogic.hasObjectAccess(user, false, defaultOrganizationId, group)) {
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
                if (user!=null && !userLogic.hasObjectAccess(user, true, defaultOrganizationId, group)) {
                    throw new ServiceException(exceptionApiUnauthorized);
                }
            } else {
                throw new ServiceException(exceptionApiUnauthorized);
            }
            logger.info("update group: " + updatedGroup.getEUI() + " " + updatedGroup.getChannelsAsString());
            iotDao.updateGroup(updatedGroup);
            //TODO: send notification to dashboard owner
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
                throw new ServiceException("Group already exists");
            }
            if(newGroup.getEUI()==null || newGroup.getEUI().isEmpty()||newGroup.getEUI().equalsIgnoreCase("new")){
                newGroup.setEUI(euiGenerator.createEui("S-"));
            }
            //newGroup.setEUI(euiGenerator.createEui("S-"));
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
