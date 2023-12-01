package com.signomix.core.application.port.in;

import com.signomix.core.domain.DatabaseUC;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CommandPort {
    
    @Inject
    DatabaseUC databaseLogic;

    public void runBackup(){
        databaseLogic.doBackup();
    }
    

}
