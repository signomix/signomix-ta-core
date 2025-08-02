package com.signomix.core.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.HashMaker;
import com.signomix.common.Organization;
import com.signomix.common.User;
import com.signomix.common.db.ApplicationDaoIface;
import com.signomix.common.db.AuthDaoIface;
import com.signomix.common.db.BillingDaoIface;
import com.signomix.common.db.DashboardIface;
import com.signomix.common.db.EventLogDaoIface;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.db.NewsDaoIface;
import com.signomix.common.db.OrganizationDaoIface;
import com.signomix.common.db.ReportDaoIface;
import com.signomix.common.db.SchedulerDaoIface;
import com.signomix.common.db.SentinelDaoIface;
import com.signomix.common.db.ShortenerDaoIface;
import com.signomix.common.db.SignalDaoIface;
import com.signomix.common.db.UserDaoIface;
import com.signomix.common.gui.Dashboard;
import com.signomix.common.gui.DashboardTemplate;
import com.signomix.common.iot.Application;
import com.signomix.common.iot.ApplicationConfig;
import com.signomix.common.iot.Channel;
import com.signomix.common.iot.Device;
import com.signomix.common.iot.DeviceTemplate;
import com.signomix.common.tsdb.NewsDao;
import com.signomix.core.application.port.in.DevicePort;
import com.signomix.core.application.port.in.UserPort;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class DatabaseUC {
    private static final Logger LOG = Logger.getLogger(DatabaseUC.class);

    private static Long DEFAULT_ORGANIZATION_ID = 1L;

    @Inject
    @DataSource("auth")
    AgroalDataSource authDataSource;

    /*
     * @Inject
     * 
     * @DataSource("iot")
     * AgroalDataSource iotDataSource;
     */

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
    @DataSource("qdb")
    AgroalDataSource qdbDataSource;

    @Inject
    DevicePort devicePort;
    @Inject
    UserPort userPort;

    // @Inject
    // @DataSource("cms")
    // AgroalDataSource cmsDataSource;

    // IotDatabaseIface dataDao;
    AuthDaoIface authDao;
    ApplicationDaoIface applicationDao;
    UserDaoIface userDao;
    IotDatabaseIface iotDao;
    IotDatabaseIface tsDao;
    // CmsDaoIface cmsDao;
    ShortenerDaoIface shortenerDao;
    DashboardIface dashboardDao;
    DashboardIface tsDashboardDao;
    SentinelDaoIface sentinelDao;
    SignalDaoIface signalDao;
    OrganizationDaoIface organizationDao;
    ReportDaoIface reportDao;
    BillingDaoIface billingDao;
    EventLogDaoIface qdbDao;
    EventLogDaoIface eventLogDao;
    NewsDaoIface newsDao;
    SchedulerDaoIface schedulerDao;

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

    @ConfigProperty(name = "questdb.client.config")
    String questDbConfig;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("Starting ...");
        LOG.info("AUTH DB URL: " + authDbUrl);

        /*
         * if ("h2".equalsIgnoreCase(databaseType)) {
         * tsDao = null;
         * authDao = new AuthDao();
         * authDao.setDatasource(authDataSource, questDbConfig);
         * applicationDao = new ApplicationDao();
         * applicationDao.setDatasource(iotDataSource);
         * userDao = new UserDao();
         * userDao.setDatasource(userDataSource);
         * iotDao = new com.signomix.common.db.IotDatabaseDao();
         * iotDao.setDatasource(iotDataSource);
         * dashboardDao = new DashboardDao();
         * dashboardDao.setDatasource(iotDataSource);
         * shortenerDao = new ShortenerDao();
         * shortenerDao.setDatasource(shortenerDataSource);
         * } else if ("both".equalsIgnoreCase(databaseType)) {
         * tsDao = new com.signomix.common.tsdb.IotDatabaseDao();
         * tsDao.setDatasource(tsDs);
         * tsDao.setAnalyticDatasource(olapDs);
         * tsDashboardDao = new com.signomix.common.tsdb.DashboardDao();
         * tsDashboardDao.setDatasource(tsDs);
         * authDao = new AuthDao();
         * authDao.setDatasource(authDataSource, questDbConfig);
         * applicationDao = new ApplicationDao();
         * applicationDao.setDatasource(iotDataSource);
         * userDao = new UserDao();
         * userDao.setDatasource(userDataSource);
         * iotDao = new com.signomix.common.db.IotDatabaseDao();
         * iotDao.setDatasource(iotDataSource);
         * dashboardDao = new DashboardDao();
         * dashboardDao.setDatasource(iotDataSource);
         * shortenerDao = new ShortenerDao();
         * shortenerDao.setDatasource(shortenerDataSource);
         * } else
         */
        if ("postgresql".equalsIgnoreCase(databaseType)) {
            iotDao = new com.signomix.common.tsdb.IotDatabaseDao();
            iotDao.setDatasource(tsDs);
            iotDao.setAnalyticDatasource(olapDs);
            dashboardDao = new com.signomix.common.tsdb.DashboardDao();
            dashboardDao.setDatasource(tsDs);
            userDao = new com.signomix.common.tsdb.UserDao();
            userDao.setDatasource(tsDs);
            authDao = new com.signomix.common.tsdb.AuthDao();
            authDao.setDatasource(tsDs, questDbConfig);
            applicationDao = new com.signomix.common.tsdb.ApplicationDao();
            applicationDao.setDatasource(tsDs);
            shortenerDao = new com.signomix.common.tsdb.ShortenerDao();
            shortenerDao.setDatasource(tsDs);
            // cmsDao = new CmsDao();
            // cmsDao.setDatasource(cmsDataSource);
            sentinelDao = new com.signomix.common.tsdb.SentinelDao();
            sentinelDao.setDatasource(tsDs);
            signalDao = new com.signomix.common.tsdb.SignalDao();
            signalDao.setDatasource(tsDs);
            organizationDao = new com.signomix.common.tsdb.OrganizationDao();
            organizationDao.setDatasource(tsDs);
            reportDao = new com.signomix.common.tsdb.ReportDao();
            reportDao.setDatasource(tsDs);
            billingDao = new com.signomix.common.tsdb.BillingDao();
            billingDao.setDatasource(tsDs);
            qdbDao = new com.signomix.common.tsdb.QuestDbDao();
            qdbDao.setDatasource(qdbDataSource);
            newsDao = new NewsDao();
            newsDao.setDatasource(tsDs);
            eventLogDao = new com.signomix.common.tsdb.EventLogDao();
            eventLogDao.setDatasource(tsDs);
            schedulerDao = new com.signomix.common.tsdb.SchedulerDao();
            schedulerDao.setDatasource(tsDs);
        } else {
            LOG.error("Database type not configured or not supported: " + databaseType);
        }

        // TODO: create DB structure
        try {
            organizationDao.createStructure();
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
        try {
            LOG.info("Creating application structure");
            applicationDao.createStructure();
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
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
        try {
            sentinelDao.createStructure();
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
        try {
            signalDao.createStructure();
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }

        try {
            reportDao.createStructure();
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
        try {
            savePredefinedReports();
        } catch (Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
        try {
            billingDao.createStructure();
        } catch (Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }

        try {
            qdbDao.createStructure();
        } catch (Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }

        try {
            eventLogDao.createStructure();
        } catch (Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }

        try {
            newsDao.createStructure();
        } catch (Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
        try {
            schedulerDao.createStructure();
        } catch (Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }

        setSignomixParameters();
        setSignomixFeatures();

        createObjects();

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

        // TODO: migrate cms data
        /*
         * ArrayList<Document> documents = new ArrayList<>();
         * try {
         * documents = (ArrayList) cmsDao.getAllDocuments();
         * } catch (IotDatabaseException e) {
         * LOG.error(e.getMessage());
         * e.printStackTrace();
         * }
         * documents.forEach(document -> {
         * try {
         * tsDao.addDocument(document);
         * } catch (IotDatabaseException e) {
         * LOG.error(e.getMessage());
         * e.printStackTrace();
         * }
         * });
         * documents = null;
         */
    }

    public void doBackup() {
        LOG.info("doBackup");
        boolean backupError = false;
        StringBuilder backupErrorMessage = new StringBuilder();
        try {
            authDao.backupDb(); // tokens,ptokens
        } catch (IotDatabaseException e) {
            backupError = true;
            backupErrorMessage.append("applicationDao: ").append(e.getMessage()).append("\n");

        }
        try {
            applicationDao.backupDb(); // applications
        } catch (IotDatabaseException e) {
            backupError = true;
            backupErrorMessage.append("applicationDao: ").append(e.getMessage()).append("\n");
        }
        try {
            iotDao.backupDb();
        } catch (IotDatabaseException e) {
            backupError = true;
            backupErrorMessage.append("iotDao: ").append(e.getMessage()).append("\n");
        }
        try {
            dashboardDao.backupDb();
        } catch (IotDatabaseException e) {
            backupError = true;
            backupErrorMessage.append("dashboardDao: ").append(e.getMessage()).append("\n");
        }
        try {
            sentinelDao.backupDb();
        } catch (IotDatabaseException e) {
            backupError = true;
            backupErrorMessage.append("sentinelDao: ").append(e.getMessage()).append("\n");
        }
        try {
            shortenerDao.backupDb();
        } catch (IotDatabaseException e) {
            backupError = true;
            backupErrorMessage.append("shortenerDao: ").append(e.getMessage()).append("\n");
        }
        try {
            signalDao.backupDb();
        } catch (IotDatabaseException e) {
            backupError = true;
            backupErrorMessage.append("signalDao: ").append(e.getMessage()).append("\n");
        }
        try {
            userDao.backupDb();
        } catch (IotDatabaseException e) {
            backupError = true;
            backupErrorMessage.append("userDao: ").append(e.getMessage()).append("\n");
        }
        try {
            organizationDao.backupDb();
        } catch (IotDatabaseException e) {
            backupError = true;
            backupErrorMessage.append("organizationDao: ").append(e.getMessage()).append("\n");
        }
        try {
            reportDao.backupDb();
        } catch (IotDatabaseException e) {
            backupError = true;
            backupErrorMessage.append("reportDao: ").append(e.getMessage()).append("\n");
        }
        try {
            billingDao.backupDb();
        } catch (IotDatabaseException e) {
            backupError = true;
            backupErrorMessage.append("billingDao: ").append(e.getMessage()).append("\n");
        }
/*         try {
            qdbDao.backupDb();
        } catch (IotDatabaseException e) {
            backupError = true;
            backupErrorMessage.append("qdbDao: ").append(e.getMessage()).append("\n");
        } */
        try {
            eventLogDao.backupDb();
        } catch (IotDatabaseException e) {
            backupError = true;
            backupErrorMessage.append("eventLogDao: ").append(e.getMessage()).append("\n");
        }
        try {
            newsDao.backupDb();
        } catch (IotDatabaseException e) {
            backupError = true;
            backupErrorMessage.append("newsDao: ").append(e.getMessage()).append("\n");
        }
        try {
            schedulerDao.backupDb();
        } catch (IotDatabaseException e) {
            backupError = true;
            backupErrorMessage.append("schedulerDao: ").append(e.getMessage()).append("\n");
        }

        if (backupError) {
            LOG.error("Backup finished with errors");
            //TODO: send backupErrorMessage to admin
        } else {
            LOG.info("Backup finished successfully.");
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
            long tooOldPoint30 = now - 30 * ONE_DAY;
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

                List<Device> protectedDevices = iotDao.getDevicesByTag(user.uid, DEFAULT_ORGANIZATION_ID, "protected",
                        "true");
                /*
                 * devices = iotDao.getUserDevices(user.uid, -1, false);
                 * for (int j = 0; j < devices.size(); j++) {
                 * if (protectedDevices.contains(devices.get(j))) {
                 * continue;
                 * }
                 * iotDao.clearAllChannels(devices.get(j).getEUI(), tooOldPoint);
                 * try {
                 * iotDao.removeCommands(devices.get(j).getEUI(), tooOldPoint);
                 * } catch (Exception e) {
                 * e.printStackTrace();
                 * }
                 * }
                 */
            }
        } catch (IotDatabaseException ex) {
        }

    }

    public void doArchive() {
        long ONE_DAY = 24 * 3600 * 1000;
        long tooOldPoint30 = System.currentTimeMillis() - 30 * ONE_DAY;
        // alerts
        try {
            iotDao.archiveAlerts(tooOldPoint30);
            iotDao.removeAlerts(tooOldPoint30);
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
        }
        // signals
        try {
            signalDao.archiveSignals(tooOldPoint30);
            signalDao.clearOldSignals(tooOldPoint30);
            signalDao.archiveUserSignals(tooOldPoint30);
            signalDao.clearOldUserSignals(tooOldPoint30);
        } catch (IotDatabaseException e) {
            LOG.error(e.getMessage());
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

            // Superuser account: high limit - for specific contracts - should be managed
            // manually
            iotDao.setParameter("collectionLimit", User.SUPERUSER, 720000, ""); // TODO: 24h X 6 transmission/hour
            iotDao.setParameter("collectionLimitMonthly", User.SUPERUSER, 21600000, "");
            iotDao.setParameter("dataRetention", User.SUPERUSER, 365, "");
            iotDao.setParameter("devicesLimit", User.SUPERUSER, 10000, "");
            iotDao.setParameter("notifications", User.SUPERUSER, 0, "SMTP,WEBHOOK,SMS"); // SMTP,SLACK,PUSHOVER,TELEGRAM,DISCORD,WEBHOOK

            iotDao.setParameter("collectionLimit", User.ADMIN, 720000, ""); // 24h X 6 transmission/hour
            iotDao.setParameter("collectionLimitMonthly", User.ADMIN, 21600000, "");
            iotDao.setParameter("dataRetention", User.ADMIN, 365, "");
            iotDao.setParameter("devicesLimit", User.ADMIN, 10000, "");
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

    private void createObjects() {
        Organization organization = new Organization(0, "demo","0123456789", "Demo Organization",
                "Organization for demonstration purposes", "{}");
        try {
            organizationDao.addOrganization(organization);
        } catch (IotDatabaseException e) {
            LOG.warn("Error inserting demo organization: " + e.getMessage());
        }
        Organization demoOrganization = null;
        try {
            demoOrganization = organizationDao.getOrganization("demo");
        } catch (IotDatabaseException e) {
            LOG.error("Unable to read demo organization: " + e.getMessage());
        }

        // Application
        // System application 
        ApplicationConfig config = new ApplicationConfig();
        config.put("refreshInterval", "60");
        Application systemApplication;
        Application demoApplication;
        systemApplication = new Application(null, 1, 1, "system", "");
        systemApplication.setConfig(config);
        try {
            systemApplication=applicationDao.addApplication(systemApplication);
        } catch (IotDatabaseException e) {
            LOG.warn("Error inserting system application: " + e.getMessage());
            try {
                systemApplication=applicationDao.getApplication(1, "system");
            } catch (IotDatabaseException e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
            } catch (Exception e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
            }
        }
        // Demo application
        config = new ApplicationConfig();
        config.put("refreshInterval", "60");
        demoApplication = new Application(null, demoOrganization.id, 1, "demo", "");
        demoApplication.setConfig(config);
        try {
            demoApplication=applicationDao.addApplication(demoApplication);
        } catch (IotDatabaseException e) {
            LOG.warn("Error inserting demo application: " + e.getMessage());
            try {
                demoApplication=applicationDao.getApplication(demoOrganization.id, "demo");
            } catch (IotDatabaseException e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
            } catch (Exception e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
            }
        }
        if(null == demoOrganization.id) {
            LOG.error("Demo organization not created - exiting");
            System.exit(1);
        }
        if(null == demoApplication) {
            LOG.error("Demo application not created - exiting");
            System.exit(1);
        }
        if(null == systemApplication) {
            LOG.error("System application not created - exiting");
            System.exit(1);
        }
        if(systemApplication.id==null) {
            LOG.error("System application id is null - exiting");
            System.exit(1);
        }
        

        // Users
        User user = new User();
        user.uid = "admin";
        user.type = User.OWNER;
        user.email = "";
        user.name = "admin";
        user.surname = "admin";
        user.role = "";
        user.confirmString = "";
        user.password = HashMaker.md5Java("test123");
        user.generalNotificationChannel = "";
        user.infoNotificationChannel = "";
        user.warningNotificationChannel = "";
        user.alertNotificationChannel = "";
        user.confirmed = true;
        user.unregisterRequested = false;
        user.authStatus = User.IS_ACTIVE;
        user.createdAt = System.currentTimeMillis();
        user.number = 0L;
        user.services = 0;
        user.phonePrefix = "";
        user.credits = 0L;
        user.autologin = false;
        user.preferredLanguage = "en";
        user.organization = DEFAULT_ORGANIZATION_ID;
        user.path = "";
        try {
            userDao.addUser(user);
        } catch (IotDatabaseException e) {
            LOG.warn("Error inserting default admin user: " + e.getMessage());
        }
        User tester1 = new User();
        tester1.uid = "tester1";
        tester1.email = "";
        tester1.organization = DEFAULT_ORGANIZATION_ID;
        tester1.password = HashMaker.md5Java("test123");
        tester1.type = User.USER;
        tester1.confirmed = true;
        tester1.name = "Tester";
        tester1.surname = "One";
        tester1.phone = 0;
        tester1.preferredLanguage = "en";
        tester1.role = "";
        tester1.alertNotificationChannel = "";
        tester1.infoNotificationChannel = "";
        tester1.warningNotificationChannel = "";
        tester1.generalNotificationChannel = "";
        tester1.authStatus = User.IS_ACTIVE;
        tester1.credits = 0L;
        tester1.phonePrefix = "";
        tester1.unregisterRequested = false;
        tester1.path = "";
        tester1.tenant = 0;
        tester1.autologin = false;
        tester1.services = 0;
        try {
            userDao.addUser(tester1);
        } catch (IotDatabaseException e) {
            LOG.warn("Error inserting tester1 user: " + e.getMessage());
        }
        user = new User();
        user.uid = "public";
        user.type = User.READONLY;
        user.email = "";
        user.name = "Public";
        user.surname = "User";
        user.role = "";
        user.confirmString = "";
        user.password = HashMaker.md5Java("public");
        user.generalNotificationChannel = "";
        user.infoNotificationChannel = "";
        user.warningNotificationChannel = "";
        user.alertNotificationChannel = "";
        user.confirmed = true;
        user.unregisterRequested = false;
        user.authStatus = User.IS_ACTIVE;
        user.createdAt = System.currentTimeMillis();
        user.number = null;
        user.services = 0;
        user.phonePrefix = "";
        user.credits = 0L;
        user.autologin = false;
        user.preferredLanguage = "en";
        user.organization = DEFAULT_ORGANIZATION_ID;
        user.path = "";
        try {
            userDao.addUser(user);
        } catch (IotDatabaseException e) {
            LOG.warn("Error inserting public user: " + e.getMessage());
        }
        User organizationAdmin = new User();
        organizationAdmin.uid = "admin_demo";
        organizationAdmin.email = "";
        organizationAdmin.organization = (long) demoOrganization.id;
        organizationAdmin.password = HashMaker.md5Java("test123");
        organizationAdmin.type = User.MANAGING_ADMIN;
        organizationAdmin.confirmed = true;
        organizationAdmin.name = "Admin";
        organizationAdmin.surname = "Demo";
        organizationAdmin.phone = 0;
        organizationAdmin.preferredLanguage = "en";
        organizationAdmin.role = "";
        organizationAdmin.alertNotificationChannel = "";
        organizationAdmin.infoNotificationChannel = "";
        organizationAdmin.warningNotificationChannel = "";
        organizationAdmin.generalNotificationChannel = "";
        organizationAdmin.authStatus = User.IS_ACTIVE;
        organizationAdmin.confirmed = true;
        organizationAdmin.credits = 0L;
        organizationAdmin.phonePrefix = "";
        organizationAdmin.unregisterRequested = false;
        organizationAdmin.path = "";
        organizationAdmin.tenant = 0;
        organizationAdmin.autologin = false;
        organizationAdmin.services = 0;
        organizationAdmin.organizationCode = "0123456789";
        try {
            userDao.addUser(organizationAdmin);
        } catch (IotDatabaseException e) {
            LOG.warn("Error inserting admin_demo user: " + e.getMessage());
        }

        Device device = new Device();
        device.setType(Device.GENERIC);
        device.setName("IoT emulator");
        device.setEUI("IOT-EMULATOR");
        device.setTeam("admin");
        device.setUserID("tester1");
        device.setKey("6022140857");
        device.setOrganizationId(1L);
        device.setOrgApplicationId(Long.valueOf(systemApplication.id));
        device.setPath("");
        device.setActive(true);
        device.setAltitude(null);
        device.setLatitude(null);
        device.setLongitude(null);
        device.setCheckFrames(false);
        device.setDashboard(true);
        device.setType(Device.GENERIC);

        LinkedHashMap<String, Channel> channels;
        channels = new LinkedHashMap<>();
        channels.put("temperature", new Channel("temperature"));
        channels.put("humidity", new Channel("humidity"));
        channels.put("latitude", new Channel("latitude"));
        channels.put("longitude", new Channel("longitude"));
        device.setChannels(channels);

        try {
            devicePort.createDevice(tester1, device);
        } catch (Exception e) {
            LOG.warn("Error creating device: " + e.getMessage());
        }
    }

    private void savePredefinedReports() {
        String className = "com.signomix.reports.pre.Html";
        try {
            reportDao.saveReport(className, 0L, null, null, null);
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            LOG.warn("Error saving report " + className + ": " + e.getMessage());
        }
        className = "com.signomix.reports.pre.DummyReport";
        try {
            reportDao.saveReport(className, 0L, null, null, null);
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            LOG.warn("Error saving report " + className + ": " + e.getMessage());
        }
        className = "com.signomix.reports.pre.LoginReportExample";
        try {
            reportDao.saveReport(className, 0L, null, null, null);
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            LOG.warn("Error saving report " + className + ": " + e.getMessage());
        }
        className = "com.signomix.reports.pre.DeviceInfo";
        try {
            reportDao.saveReport(className, 0L, null, null, null);
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            LOG.warn("Error saving report " + className + ": " + e.getMessage());
        }
        className = "com.signomix.reports.pre.UserLoginReport";
        try {
            reportDao.saveReport(className, 0L, null, null, null);
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            LOG.warn("Error saving report " + className + ": " + e.getMessage());
        }
        className = "com.signomix.reports.pre.DqlReport";
        try {
            reportDao.saveReport(className, 0L, null, null, null);
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            LOG.warn("Error saving report " + className + ": " + e.getMessage());
        }
        className = "com.signomix.reports.pre.DqlTvReport";
        try {
            reportDao.saveReport(className, 0L, null, null, null);
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            LOG.warn("Error saving report " + className + ": " + e.getMessage());
        }
        className = "com.signomix.reports.pre.GroupReport";
        try {
            reportDao.saveReport(className, 0L, null, null, null);
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            LOG.warn("Error saving report " + className + ": " + e.getMessage());
        }
    }

}
