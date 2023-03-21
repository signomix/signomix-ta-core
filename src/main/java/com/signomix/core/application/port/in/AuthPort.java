package com.signomix.core.application.port.in;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.signomix.common.User;
import com.signomix.core.application.usecase.AuthLogic;

@ApplicationScoped
public class AuthPort {
    @Inject
    AuthLogic authLogic;

    public String getUserId(String token){
        return authLogic.getUserId(token);
    }

    public User getUser(String token){
        return authLogic.getUser(getUserId(token));
    }
}
