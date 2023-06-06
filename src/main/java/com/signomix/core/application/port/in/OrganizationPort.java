package com.signomix.core.application.port.in;

import java.util.List;

import javax.inject.Inject;

import com.signomix.common.User;
import com.signomix.common.Organization;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.domain.OrganizationLogic;


public class OrganizationPort {

    @Inject
    OrganizationLogic organizationLogic;

    public List<Organization> getOrganizations(User user, Integer limit, Integer offset) throws ServiceException {
        return organizationLogic.getOrganizations(user, limit, offset);
    }

    public Organization getOrganization(User user, long organizationId) throws ServiceException {
        return organizationLogic.getOrganization(user, organizationId);
    }

    public void addOrganization(User user, Organization organization) throws ServiceException {
        organizationLogic.addOrganization(user, organization);
    }

    public void deleteOrganization(User user, long organizationId) throws ServiceException {
        organizationLogic.deleteOrganization(user, organizationId);
    }

    public void updateOrganization(User user, Organization organization) throws ServiceException {
        organizationLogic.updateOrganization(user, organization);
    }

    
}
