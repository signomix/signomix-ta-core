package com.signomix.core.application.port.in;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.core.domain.AuthLogic;

@ApplicationScoped
public class AuthPort {

    @Inject
    Logger logger;

    @Inject
    AuthLogic authLogic;

    public String getUserId(String token){
        logger.info("getUserId: "+token);
        return authLogic.getUserId(token);
    }

    public User getUser(String token){
        return authLogic.getUser(getUserId(token));
    }
}
