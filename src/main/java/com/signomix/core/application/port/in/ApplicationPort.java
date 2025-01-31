package com.signomix.core.application.port.in;

import java.util.List;

import com.signomix.common.User;
import com.signomix.common.iot.Application;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.domain.ApplicationLogic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ApplicationPort {

    @Inject
    ApplicationLogic applicationLogic;

    public List<Application> getApplications(User user, Integer limit, Integer offset) throws ServiceException {
        return applicationLogic.getApplications(user, limit, offset);
    }

    public Application getApplication(User user, int applicationId) throws ServiceException {
        return applicationLogic.getApplication(user, applicationId);
    }

    public Application getApplicationByName(User user, int organizationId, String name) throws ServiceException {
        return applicationLogic.getApplicationByName(user, organizationId, name);
    }

    public void addApplication(User user, Application application) throws ServiceException {
        applicationLogic.addApplication(user, application);
    }

    public void deleteApplication(User user, int applicationId) throws ServiceException {
        applicationLogic.deleteApplication(user, applicationId);
    }

    public void updateApplication(User user, Application application) throws ServiceException {
        applicationLogic.updateApplication(user, application);
    }

    
}
