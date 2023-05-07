package com.signomix.core.domain;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.IotDatabaseDao;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.iot.Device;
import com.signomix.core.application.exception.ServiceException;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;

/**
 * 
 * @author Grzegorz
 */
@ApplicationScoped
public class DeviceLogic {
    private static final Logger LOG = Logger.getLogger(DeviceLogic.class);

    @ConfigProperty(name = "signomix.app.key", defaultValue = "not_configured")
    String appKey;
    @ConfigProperty(name = "signomix.auth.host", defaultValue = "not_configured")
    String authHost;

    @Inject
    @DataSource("iot")
    AgroalDataSource deviceDataSource;

    IotDatabaseIface iotDao;

    void onStart(@Observes StartupEvent ev) {
        iotDao = new IotDatabaseDao();
        iotDao.setDatasource(deviceDataSource);
    }

    //TODO: add organizationId to all methods
    public Device getDevice(User user, String eui, boolean withStatus) throws ServiceException {
        try {
            return iotDao.getDevice(user, eui, true, withStatus);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(),e);
        }  
    }

    public List<Device> getUserDevices(User user, boolean withStatus) throws ServiceException {
        try {
            return iotDao.getUserDevices(user, withStatus);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(),e);
        }  
    }

    public void deleteDevice(User user, String eui) throws ServiceException {
        try {
            iotDao.deleteDevice(user, eui);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(),e);
        }  
    }

    public void updateDevice(User user, Device device) throws ServiceException {
        try {
            iotDao.updateDevice(user, device);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(),e);
        }  
    }

    public void createDevice(User user, Device device) throws ServiceException {
        try {
            iotDao.createDevice(user, device);
        } catch (IotDatabaseException e) {
            throw new ServiceException(e.getMessage(),e);
        }  
    }

}
