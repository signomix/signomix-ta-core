package com.signomix.core.domain;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.AuthDao;
import com.signomix.common.db.AuthDaoIface;
import com.signomix.common.db.CmsDao;
import com.signomix.common.db.CmsDaoIface;
import com.signomix.common.db.DashboardDao;
import com.signomix.common.db.DashboardIface;
import com.signomix.common.db.IotDatabaseDao;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.db.ShortenerDao;
import com.signomix.common.db.ShortenerDaoIface;
import com.signomix.common.db.UserDao;
import com.signomix.common.db.UserDaoIface;
import com.signomix.common.iot.Device;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class DatabaseUC {
    private static final Logger LOG = Logger.getLogger(DatabaseUC.class);

    @Inject
    @DataSource("auth")
    AgroalDataSource authDataSource;

    @Inject
    @DataSource("iot")
    AgroalDataSource iotDataSource;

    @Inject
    @DataSource("user")
    AgroalDataSource userDataSource;

    @Inject
    @DataSource("shortener")
    AgroalDataSource shortenerDataSource;

    @Inject
    @DataSource("cms")
    AgroalDataSource cmsDataSource;

    // IotDatabaseIface dataDao;
    AuthDaoIface authDao;
    UserDaoIface userDao;
    IotDatabaseIface iotDao;
    CmsDaoIface cmsDao;
    ShortenerDaoIface shortenerDao;
    DashboardIface dashboardDao;

    @ConfigProperty(name = "signomix.data.retention.demo", defaultValue = "1")
    int demoDataRetention;
    @ConfigProperty(name = "signomix.data.retention.free", defaultValue = "5")
    int freeDataRetention;
    @ConfigProperty(name = "signomix.data.retention.extended", defaultValue = "30")
    int extendedDataRetention;
    @ConfigProperty(name = "signomix.data.retention.standard", defaultValue = "30")
    int standardDataRetention;
    @ConfigProperty(name = "signomix.data.retention.primary", defaultValue = "30")
    int primaryDataRetention;
    @ConfigProperty(name = "signomix.data.retention.super", defaultValue = "365")
    int superDataRetention;
    
    @ConfigProperty(name = "quarkus.datasource.auth.jdbc.url", defaultValue = "not configured")
    String authDbUrl;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("Starting ...");
        LOG.info("AUTH DB URL: "+authDbUrl);
        authDao = new AuthDao();
        authDao.setDatasource(authDataSource);
        // dataDao = new IotDatabaseDao();
        // dataDao.setDatasource(iotDataSource);
        userDao = new UserDao();
        userDao.setDatasource(userDataSource);
        iotDao = new IotDatabaseDao();
        iotDao.setDatasource(iotDataSource);
        dashboardDao = new DashboardDao();
        dashboardDao.setDatasource(iotDataSource);
        
        shortenerDao = new ShortenerDao();
        shortenerDao.setDatasource(shortenerDataSource);
        cmsDao=new CmsDao();
        cmsDao.setDatasource(cmsDataSource);
        //TODO: create DB structure
        try {
            iotDao.createStructure();
            dashboardDao.createStructure();
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
        try {
            userDao.createStructure();
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
        /* try {
            LOG.info("test backup");
            //shortenerDao.createStructure();
            iotDao.backupDb();
            authDao.backupDb();
            cmsDao.backupDb();
            userDao.backupDb();
            shortenerDao.backupDb();
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        } */
        setSignomixParameters();
        setSignomixFeatures();

        // TODO: create predefined users
        // TODO
    }

    public void doBackup() {
        LOG.info("doBackup");
        try {
            iotDao.backupDb();
            dashboardDao.backupDb();
            shortenerDao.backupDb();
            authDao.backupDb();
            cmsDao.backupDb();
            userDao.backupDb();
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
        }
    }

    public void clearData() {
        authDao.clearExpiredTokens();
        long TWO_DAYS = 2;
        userDao.removeNotConfirmed(TWO_DAYS);
        clearOldData();
    }

    private void clearOldData() {
        // data retention
        // how long data is kept in signomix depends on user userType
        long ONE_DAY = 24 * 3600 * 1000;
        try {
            long freeRetention = ONE_DAY * freeDataRetention;
            long extendedRetention = ONE_DAY * extendedDataRetention;
            long standardRetention = ONE_DAY * standardDataRetention;
            long primaryRetention = ONE_DAY * primaryDataRetention;
            long superuserRetention = ONE_DAY * superDataRetention;
            List<User> users = userDao.getAll();
            List<Device> devices;
            long now = System.currentTimeMillis();
            long tooOldPoint = now - 2 * ONE_DAY;
            long tooOldPointDemo = now - ONE_DAY;
            long tooOldPointFree = now - freeRetention;
            long tooOldPointExtended = now - extendedRetention;
            long tooOldPointStandard = now - standardRetention;
            long tooOldPointPrimary = now - primaryRetention;
            long tooOldPointSuperuser = now - superuserRetention;
            boolean demoMode = false;
            User user;
            for (int i = 0; i < users.size(); i++) {
                user = users.get(i);
                if (!demoMode) {
                    switch (user.type) {
                        case User.DEMO:
                            tooOldPoint = tooOldPointDemo;
                            break;
                        case User.OWNER:
                        case User.PRIMARY:
                            tooOldPoint = tooOldPointPrimary;
                            break;
                        case User.USER:
                            tooOldPoint = tooOldPointStandard;
                            break;
                        case User.EXTENDED:
                            tooOldPoint = tooOldPointExtended;
                            break;
                        case User.SUPERUSER:
                            tooOldPoint = tooOldPointSuperuser;
                            break;
                        default:
                            tooOldPoint = tooOldPointFree;
                    }
                }
                iotDao.removeAlerts(user.uid, tooOldPoint);
                /*
                 * devices = dataDao.getUserDevices(user.uid, -1, false);
                 * for (int j = 0; j < devices.size(); j++) {
                 * dataDao.clearAllChannels(devices.get(j).getEUI(), tooOldPoint);
                 * try {
                 * dataDao.removeCommands(devices.get(j).getEUI(), tooOldPoint);
                 * } catch (Exception e) {
                 * e.printStackTrace();
                 * }
                 * }
                 */
            }
        } catch (IotDatabaseException ex) {
        }

    }

    private void setSignomixParameters(){
        try {
            iotDao.setParameter("collectionLimit", User.DEMO, 144, ""); // 24h X 6 transmission/hour
            iotDao.setParameter("collectionLimitMonthly", User.DEMO, 4320, "");
            iotDao.setParameter("dataRetention", User.DEMO, 1, "");
            iotDao.setParameter("devicesLimit", User.DEMO, 1, "");
            iotDao.setParameter("notifications", User.DEMO, 0, "SMTP"); //SMTP,SLACK,PUSHOVER,TELEGRAM,DISCORD,WEBHOOK


            iotDao.setParameter("collectionLimit", User.FREE, 144, ""); // 24h X 6 transmission/hour
            iotDao.setParameter("collectionLimitMonthly", User.FREE, 4464, "");
            iotDao.setParameter("dataRetention", User.FREE, 7, "");
            iotDao.setParameter("devicesLimit", User.FREE, 5, "");
            iotDao.setParameter("notifications", User.FREE, 0, "SMTP,WEBHOOK"); //SMTP,SLACK,PUSHOVER,TELEGRAM,DISCORD,WEBHOOK
            
            iotDao.setParameter("collectionLimit", User.EXTENDED, 144, ""); // 24h X 6 transmission/hour
            iotDao.setParameter("collectionLimitMonthly", User.EXTENDED, 4464, "");
            iotDao.setParameter("dataRetention", User.EXTENDED, 61, "");
            iotDao.setParameter("devicesLimit", User.EXTENDED, 20, "");
            iotDao.setParameter("notifications", User.EXTENDED, 0, "SMTP,WEBHOOK"); //SMTP,SLACK,PUSHOVER,TELEGRAM,DISCORD,WEBHOOK

            //Standard account
            iotDao.setParameter("collectionLimit", User.USER, 2160, ""); // 24h X 6 transmission/hour
            iotDao.setParameter("collectionLimitMonthly", User.USER, 64800, "");
            iotDao.setParameter("dataRetention", User.USER, 30, "");
            iotDao.setParameter("devicesLimit", User.USER, 20, "");
            iotDao.setParameter("notifications", User.USER, 0, "SMTP,WEBHOOK"); //SMTP,SLACK,PUSHOVER,TELEGRAM,DISCORD,WEBHOOK

            //Professional account
            iotDao.setParameter("collectionLimit", User.PRIMARY, 7200, ""); // 24h X 6 transmission/hour
            iotDao.setParameter("collectionLimitMonthly", User.PRIMARY, 216000, "");
            iotDao.setParameter("dataRetention", User.PRIMARY, 365, "");
            iotDao.setParameter("devicesLimit", User.PRIMARY, 50, "");
            iotDao.setParameter("notifications", User.PRIMARY, 0, "SMTP,WEBHOOK,SMS"); //SMTP,SLACK,PUSHOVER,TELEGRAM,DISCORD,WEBHOOK

            //Superuser account
            iotDao.setParameter("collectionLimit", User.SUPERUSER, 7200, ""); // 24h X 6 transmission/hour
            iotDao.setParameter("collectionLimitMonthly", User.SUPERUSER, 216000, "");
            iotDao.setParameter("dataRetention", User.SUPERUSER, 365, "");
            iotDao.setParameter("devicesLimit", User.SUPERUSER, 50, "");
            iotDao.setParameter("notifications", User.SUPERUSER, 0, "SMTP,WEBHOOK,SMS"); //SMTP,SLACK,PUSHOVER,TELEGRAM,DISCORD,WEBHOOK
            
            iotDao.setParameter("collectionLimit", User.ADMIN, 7200, ""); // 24h X 6 transmission/hour
            iotDao.setParameter("collectionLimitMonthly", User.ADMIN, 216000, "");
            iotDao.setParameter("dataRetention", User.ADMIN, 365, "");
            iotDao.setParameter("devicesLimit", User.ADMIN, 50, "");
            iotDao.setParameter("notifications", User.ADMIN, 0, "SMTP,WEBHOOK,SMS"); //SMTP,SLACK,PUSHOVER,TELEGRAM,DISCORD,WEBHOOK

            iotDao.setParameter("system.default.organization", User.ANY, 0, "");
            
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            LOG.error(e.getMessage());
        }

    }

    private void setSignomixFeatures(){
        try {
            iotDao.setFeature("SMS", User.DEMO, false);
            iotDao.setFeature("SMS", User.FREE, false);
            iotDao.setFeature("SMS", User.EXTENDED, false);
            iotDao.setFeature("SMS", User.USER, false);
            iotDao.setFeature("SMS", User.PRIMARY, true);
            iotDao.setFeature("SMS", User.SUPERUSER, true);
            iotDao.setFeature("SMS", User.ADMIN, true);
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
        }

    }


}
