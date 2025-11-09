package com.signomix.core.application.port.in;

import java.util.List;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.gui.Dashboard;
import com.signomix.common.gui.DashboardTemplate;
import com.signomix.common.iot.Device;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.domain.DashboardLogic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DashboardPort {
    @Inject
    Logger logger;

    @Inject
    DashboardLogic dashboardLogic;

    public void addDefaultDashboard(Device device) throws ServiceException {
        dashboardLogic.addDefaultDashboard(device);
    }

    public List<Dashboard> getUserDashboards(User user, Boolean withShared, Boolean isAdmin, Integer limit, Integer offset, String searchString)throws ServiceException{
        return dashboardLogic.getUserDashboards(user, withShared, isAdmin, limit, offset, searchString);
    }

    public Dashboard getPublicDashboard(String dashboardId) throws ServiceException {
        return dashboardLogic.getPublicDashboard(dashboardId);
    }

    public Dashboard getUserDashboard(User user, String dashboardId) throws ServiceException {
        return dashboardLogic.getDashboard(user, dashboardId, false);
    }

    public void updateDashboard(User user, Dashboard dashboard) throws ServiceException {
        dashboardLogic.updateDashboard(user, dashboard);
    }

    public void addDashboard(User user, Dashboard dashboard) throws ServiceException {
        dashboardLogic.saveDashboard(user, dashboard);
    }

    public void removeDashboard(User user, String dashboardId) throws ServiceException {
        dashboardLogic.removeDashboard(user, dashboardId);
    }

    public DashboardTemplate getDashboardTemplate(User user, String templateId) throws ServiceException {
        return dashboardLogic.getDashboardTemplate(user, templateId);
    }
    public List<DashboardTemplate> getDashboardTemplates(User user, Integer limit, Integer offset, String searchString) throws ServiceException {
        return dashboardLogic.getDashboardTemplates(user, limit, offset, searchString);
    }
    public void addDashboardTemplate(User user, DashboardTemplate dashboardTemplate) throws ServiceException {
        dashboardLogic.addDashboardTemplate(user, dashboardTemplate);
    }
    public void updateDashboardTemplate(User user, String templateId, DashboardTemplate dashboardTemplate) throws ServiceException {
        dashboardLogic.updateDashboardTemplate(user, templateId, dashboardTemplate);
    }
    public void removeDashboardTemplate(User user, String templateId) throws ServiceException {
        dashboardLogic.removeDashboardTemplate(user, templateId);
    }

}
