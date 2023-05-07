package com.signomix.core.application.port.in;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.signomix.core.domain.DatabaseUC;

@ApplicationScoped
public class CommandPort {
    
    @Inject
    DatabaseUC databaseLogic;

    public void runBackup(){
        databaseLogic.doBackup();
    }
    

}
