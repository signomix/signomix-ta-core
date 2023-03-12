package com.signomix.core.application.port;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.signomix.core.application.usecase.DatabaseUC;
import com.signomix.core.domain.Command;

@ApplicationScoped
public class CommandPort {
    
    @Inject
    DatabaseUC databaseLogic;

    public void runBackup(){
        databaseLogic.doBackup();
    }
    

}
