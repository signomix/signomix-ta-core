package com.signomix.core.domain;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.iot.Device;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class ActuatorLogic {

    @Inject
    Logger logger = Logger.getLogger(ActuatorLogic.class);

    @Inject
    @DataSource("oltp")
    AgroalDataSource oltpDs;

    @Inject
    @DataSource("olap")
    AgroalDataSource olapDs;

    IotDatabaseIface iotDao;

    public void onStart(@Observes StartupEvent ev) {
        iotDao = new com.signomix.common.tsdb.IotDatabaseDao();
        iotDao.setDatasource(oltpDs);
        iotDao.setAnalyticDatasource(olapDs);
    }

    public void processPlainCommand(User user, Device device, String command) throws IotDatabaseException {
        logger.info("processPlainCommand");
        try {
            logger.info("Saving command");
            iotDao.putDeviceCommand(device.getEUI(), "ACTUATOR_PLAINCMD", command, System.currentTimeMillis());
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            throw new IotDatabaseException(IotDatabaseException.UNKNOWN, "Could not save command");
        }
    }

    public void processJsonCommand(User user, Device device, String command) throws IotDatabaseException {
        logger.info("processJsonCommand");
        // Check if command is a valid JSON string
        if (!command.matches("^\\{.*\\}$")) {
            logger.debug("Invalid JSON string");
            throw new IotDatabaseException(IotDatabaseException.UNKNOWN, "Invalid JSON string");
        }
        try {
            logger.info("Saving command");
            iotDao.putDeviceCommand(device.getEUI(), "ACTUATOR_CMD", command, System.currentTimeMillis());
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            throw new IotDatabaseException(IotDatabaseException.UNKNOWN, "Could not save command");
        }
    }

    public void processHexCommand(User user, Device device, String command) throws IotDatabaseException {
        logger.info("processJsonCommand");
        // Check if command is a valid hex string
        if (!command.matches("^[0-9A-Fa-f]+$")) {
            logger.debug("Invalid hex string");
            throw new IotDatabaseException(IotDatabaseException.UNKNOWN, "Invalid hex string");
        }
        try {
            logger.info("Saving command");
            iotDao.putDeviceCommand(device.getEUI(), "ACTUATOR_HEXCMD", command, System.currentTimeMillis());
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            throw new IotDatabaseException(IotDatabaseException.UNKNOWN, "Could not save command");
        }

    }

}
