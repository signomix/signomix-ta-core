package com.signomix.core.domain;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.ApplicationDao;
import com.signomix.common.db.ApplicationDaoIface;
import com.signomix.common.db.AuthDao;
import com.signomix.common.db.AuthDaoIface;
import com.signomix.common.db.CmsDao;
import com.signomix.common.db.CmsDaoIface;
import com.signomix.common.db.DashboardDao;
import com.signomix.common.db.DashboardIface;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.db.SentinelDaoIface;
import com.signomix.common.db.ShortenerDao;
import com.signomix.common.db.ShortenerDaoIface;
import com.signomix.common.db.SignalDaoIface;
import com.signomix.common.db.UserDao;
import com.signomix.common.db.UserDaoIface;
import com.signomix.common.gui.Dashboard;
import com.signomix.common.gui.DashboardTemplate;
import com.signomix.common.iot.Device;
import com.signomix.common.iot.DeviceTemplate;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

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
    @DataSource("oltp")
    AgroalDataSource tsDs;

    @Inject
    @DataSource("olap")
    AgroalDataSource olapDs;

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
    ApplicationDaoIface applicationDao;
    UserDaoIface userDao;
    IotDatabaseIface iotDao;
    IotDatabaseIface tsDao;
    CmsDaoIface cmsDao;
    ShortenerDaoIface shortenerDao;
    DashboardIface dashboardDao;
    DashboardIface tsDashboardDao;
    SentinelDaoIface sentinelDao;
    SignalDaoIface signalDao;

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
    @ConfigProperty(name = "signomix.database.type")
    String databaseType;
    @ConfigProperty(name = "signomix.database.migration")
    boolean migration;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("Starting ...");
        LOG.info("AUTH DB URL: " + authDbUrl);

        if ("h2".equalsIgnoreCase(databaseType)) {
            tsDao = null;
            authDao = new AuthDao();
            authDao.setDatasource(authDataSource);
            applicationDao = new ApplicationDao();
            applicationDao.setDatasource(iotDataSource);
            userDao = new UserDao();
            userDao.setDatasource(userDataSource);
            iotDao = new com.signomix.common.db.IotDatabaseDao();
            iotDao.setDatasource(iotDataSource);
            dashboardDao = new DashboardDao();
            dashboardDao.setDatasource(iotDataSource);
            shortenerDao = new ShortenerDao();
            shortenerDao.setDatasource(shortenerDataSource);
            cmsDao = new CmsDao();
            cmsDao.setDatasource(cmsDataSource);
        } else if ("both".equalsIgnoreCase(databaseType)) {
            tsDao = new com.signomix.common.tsdb.IotDatabaseDao();
            tsDao.setDatasource(tsDs);
            tsDao.setAnalyticDatasource(olapDs);
            tsDashboardDao = new com.signomix.common.tsdb.DashboardDao();
            tsDashboardDao.setDatasource(tsDs);
            authDao = new AuthDao();
            authDao.setDatasource(authDataSource);
            applicationDao = new ApplicationDao();
            applicationDao.setDatasource(iotDataSource);
            userDao = new UserDao();
            userDao.setDatasource(userDataSource);
            iotDao = new com.signomix.common.db.IotDatabaseDao();
            iotDao.setDatasource(iotDataSource);
            dashboardDao = new DashboardDao();
            dashboardDao.setDatasource(iotDataSource);
            shortenerDao = new ShortenerDao();
            shortenerDao.setDatasource(shortenerDataSource);
            cmsDao = new CmsDao();
            cmsDao.setDatasource(cmsDataSource);
        } else if ("postgresql".equalsIgnoreCase(databaseType)){
            iotDao = new com.signomix.common.tsdb.IotDatabaseDao();
            iotDao.setDatasource(tsDs);
            iotDao.setAnalyticDatasource(olapDs);
            dashboardDao = new com.signomix.common.tsdb.DashboardDao();
            dashboardDao.setDatasource(tsDs);
            userDao= new com.signomix.common.tsdb.UserDao();
            userDao.setDatasource(tsDs);
            authDao=new com.signomix.common.tsdb.AuthDao();
            authDao.setDatasource(tsDs);
            applicationDao = new com.signomix.common.tsdb.ApplicationDao();
            applicationDao.setDatasource(tsDs);
            shortenerDao = new com.signomix.common.tsdb.ShortenerDao();
            shortenerDao.setDatasource(tsDs);
            cmsDao = new CmsDao();
            cmsDao.setDatasource(cmsDataSource);
            sentinelDao = new com.signomix.common.tsdb.SentinelDao();
            sentinelDao.setDatasource(tsDs);
            signalDao = new com.signomix.common.tsdb.SignalDao();
            signalDao.setDatasource(tsDs);
        } else {
            LOG.error("Database type not configured or not supported: " + databaseType);
        }

        // TODO: create DB structure
        try {
            iotDao.createStructure();
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
        try {
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
        try {
            authDao.createStructure();
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
        try {
            applicationDao.createStructure();
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
        try {
            shortenerDao.createStructure();
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
        if (null != tsDao) {
            try {
                tsDao.createStructure();
            } catch (IotDatabaseException e) {
                LOG.error(e.getMessage());
                e.printStackTrace();
            }
        }
        try{
            sentinelDao.createStructure();
        }catch(IotDatabaseException e){
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
        try{
            signalDao.createStructure();
        }catch(IotDatabaseException e){
            LOG.error(e.getMessage());
            e.printStackTrace();
        }

        /*
         * try {
         * LOG.info("test backup");
         * //shortenerDao.createStructure();
         * iotDao.backupDb();
         * authDao.backupDb();
         * cmsDao.backupDb();
         * userDao.backupDb();
         * shortenerDao.backupDb();
         * } catch (IotDatabaseException e) {
         * LOG.error(e.getMessage());
         * e.printStackTrace();
         * }
         */
        setSignomixParameters();
        setSignomixFeatures();

        // TODO: create predefined users
        // TODO

        // data migration
        if ("both".equalsIgnoreCase(databaseType) && migration) {
            migrateData();
        }
    }

    private void migrateData() {
        ArrayList<Device> devices = new ArrayList<>();
        try {
            devices = (ArrayList) iotDao.getAllDevices();
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
        }
        devices.forEach(device -> {
            try {
                tsDao.addDevice(device);
            } catch (IotDatabaseException e) {
                LOG.error(e.getMessage());
            }
        });
        devices = null;
        ArrayList<DeviceTemplate> templates = new ArrayList<>();
        try {
            templates = (ArrayList) iotDao.getAllDeviceTemplates();
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
        }
        templates.forEach(template -> {
            try {
                tsDao.addDeviceTemplate(template);
            } catch (IotDatabaseException e) {
                LOG.error(e.getMessage());
            }
        });
        templates = null;

        ArrayList<Dashboard> dashboards = new ArrayList<>();
        try {
            dashboards = (ArrayList) dashboardDao.getDashboards(10000, 0);
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
        }
        dashboards.forEach(dashboard -> {
            try {
                tsDashboardDao.addDashboard(dashboard);
            } catch (IotDatabaseException e) {
                LOG.error(e.getMessage());
            }
        });
        dashboards = null;

        ArrayList<DashboardTemplate> dashboardTemplates = new ArrayList<>();
        try {
            dashboardTemplates = (ArrayList) dashboardDao.getDashboardTemplates(10000, 0);
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
        }
        dashboardTemplates.forEach(dashboardTemplate -> {
            try {
                tsDashboardDao.addDashboardTemplate(dashboardTemplate);
            } catch (IotDatabaseException e) {
                LOG.error(e.getMessage());
                e.printStackTrace();
            }
        });
        dashboardTemplates = null;
        LOG.info("data migration finished");

        //TODO: migrate cms data
        /* ArrayList<Document> documents = new ArrayList<>();
        try {
            documents = (ArrayList) cmsDao.getAllDocuments();
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
        documents.forEach(document -> {
            try {
                tsDao.addDocument(document);
            } catch (IotDatabaseException e) {
                LOG.error(e.getMessage());
                e.printStackTrace();
            }
        });
        documents = null; */
    }

    public void doBackup() {
        LOG.info("doBackup");
        try {
            authDao.backupDb();
            applicationDao.backupDb();
            iotDao.backupDb();
            dashboardDao.backupDb();
            sentinelDao.backupDb();
            shortenerDao.backupDb();
            signalDao.backupDb();
            //cmsDao.backupDb();
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

    private void setSignomixParameters() {
        try {
            iotDao.setParameter("collectionLimit", User.DEMO, 144, ""); // 24h X 6 transmission/hour
            iotDao.setParameter("collectionLimitMonthly", User.DEMO, 4320, "");
            iotDao.setParameter("dataRetention", User.DEMO, 1, "");
            iotDao.setParameter("devicesLimit", User.DEMO, 1, "");
            iotDao.setParameter("notifications", User.DEMO, 0, "SMTP"); // SMTP,SLACK,PUSHOVER,TELEGRAM,DISCORD,WEBHOOK

            iotDao.setParameter("collectionLimit", User.FREE, 144, ""); // 24h X 6 transmission/hour
            iotDao.setParameter("collectionLimitMonthly", User.FREE, 4464, "");
            iotDao.setParameter("dataRetention", User.FREE, 7, "");
            iotDao.setParameter("devicesLimit", User.FREE, 5, "");
            iotDao.setParameter("notifications", User.FREE, 0, "SMTP,WEBHOOK"); // SMTP,SLACK,PUSHOVER,TELEGRAM,DISCORD,WEBHOOK

            iotDao.setParameter("collectionLimit", User.EXTENDED, 144, ""); // 24h X 6 transmission/hour
            iotDao.setParameter("collectionLimitMonthly", User.EXTENDED, 4464, "");
            iotDao.setParameter("dataRetention", User.EXTENDED, 61, "");
            iotDao.setParameter("devicesLimit", User.EXTENDED, 20, "");
            iotDao.setParameter("notifications", User.EXTENDED, 0, "SMTP,WEBHOOK"); // SMTP,SLACK,PUSHOVER,TELEGRAM,DISCORD,WEBHOOK

            // Standard account
            iotDao.setParameter("collectionLimit", User.USER, 2160, ""); // 24h X 6 transmission/hour
            iotDao.setParameter("collectionLimitMonthly", User.USER, 64800, "");
            iotDao.setParameter("dataRetention", User.USER, 30, "");
            iotDao.setParameter("devicesLimit", User.USER, 20, "");
            iotDao.setParameter("notifications", User.USER, 0, "SMTP,WEBHOOK"); // SMTP,SLACK,PUSHOVER,TELEGRAM,DISCORD,WEBHOOK

            // Professional account
            iotDao.setParameter("collectionLimit", User.PRIMARY, 14400, ""); // 24h X 6 transmission/hour
            iotDao.setParameter("collectionLimitMonthly", User.PRIMARY, 432000, "");
            iotDao.setParameter("dataRetention", User.PRIMARY, 365, "");
            iotDao.setParameter("devicesLimit", User.PRIMARY, 100, "");
            iotDao.setParameter("notifications", User.PRIMARY, 0, "SMTP,WEBHOOK,SMS"); // SMTP,SLACK,PUSHOVER,TELEGRAM,DISCORD,WEBHOOK

            // Superuser account: high limit - for specific contracts - should be managed manually
            iotDao.setParameter("collectionLimit", User.SUPERUSER, 720000, ""); // TODO: 24h X 6 transmission/hour
            iotDao.setParameter("collectionLimitMonthly", User.SUPERUSER, 21600000, "");
            iotDao.setParameter("dataRetention", User.SUPERUSER, 365, "");
            iotDao.setParameter("devicesLimit", User.SUPERUSER, 10000, "");
            iotDao.setParameter("notifications", User.SUPERUSER, 0, "SMTP,WEBHOOK,SMS"); // SMTP,SLACK,PUSHOVER,TELEGRAM,DISCORD,WEBHOOK

            iotDao.setParameter("collectionLimit", User.ADMIN, 7200, ""); // 24h X 6 transmission/hour
            iotDao.setParameter("collectionLimitMonthly", User.ADMIN, 216000, "");
            iotDao.setParameter("dataRetention", User.ADMIN, 365, "");
            iotDao.setParameter("devicesLimit", User.ADMIN, 50, "");
            iotDao.setParameter("notifications", User.ADMIN, 0, "SMTP,WEBHOOK,SMS"); // SMTP,SLACK,PUSHOVER,TELEGRAM,DISCORD,WEBHOOK

            iotDao.setParameter("system.default.organization", User.ANY, 0, "");

        } catch (IotDatabaseException e) {
            e.printStackTrace();
            LOG.error(e.getMessage());
        }

    }

    private void setSignomixFeatures() {
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
