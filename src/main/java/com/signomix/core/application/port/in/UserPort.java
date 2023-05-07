package com.signomix.core.application.port.in;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.signomix.common.User;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.core.domain.UserLogic;

@ApplicationScoped
public class UserPort {
    @Inject
    UserLogic userLogic;


    public User getUser(String uid) throws IotDatabaseException {
        return userLogic.getUser(uid);
    }
}
