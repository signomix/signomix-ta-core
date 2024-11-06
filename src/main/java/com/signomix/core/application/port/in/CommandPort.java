package com.signomix.core.application.port.in;

import com.signomix.core.domain.DatabaseUC;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CommandPort {
    private static final Logger LOG = Logger.getLogger(CommandPort.class);
    
    @Inject
    DatabaseUC databaseLogic;

    public void runBackup(){
        databaseLogic.doBackup();
    }

    public void runArchive(){
        LOG.info("Archiving data...");
        databaseLogic.doArchive();
    }

    public void runClean(){
        LOG.info("Cleaning data...");
        databaseLogic.clearData();
    }
    

}
