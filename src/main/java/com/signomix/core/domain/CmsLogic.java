package com.signomix.core.domain;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.db.CmsDao;
import com.signomix.common.db.CmsDaoIface;
import com.signomix.common.db.IotDatabaseException;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;

/**
 * CMS logic
 *
 */
@ApplicationScoped
public class CmsLogic {
    private static final Logger LOG = Logger.getLogger(CmsLogic.class);

    @Inject
    @DataSource("cms")
    AgroalDataSource cmsDataSource;

    CmsDaoIface cmsDao;
    
    @ConfigProperty(name = "quarkus.datasource.auth.jdbc.url", defaultValue = "not configured")
    String authDbUrl;

    void onStart(@Observes StartupEvent ev) {
        cmsDao=new CmsDao();
        cmsDao.setDatasource(cmsDataSource);
    }

    /**
     * Cleanup database
     * 
     * @throws IotDatabaseException
     */
    public void doCleanup() {
        try {
            cmsDao.doCleanup();
        } catch (IotDatabaseException e) {
            LOG.error("Error while cleaning up database", e);
        }
    }

}
