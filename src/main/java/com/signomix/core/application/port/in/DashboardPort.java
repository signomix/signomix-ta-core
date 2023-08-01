package com.signomix.core.application.port.in;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.signomix.common.User;
import com.signomix.common.gui.Dashboard;
import com.signomix.common.iot.Device;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.domain.DashboardLogic;

@ApplicationScoped
public class DashboardPort {
    @Inject
    DashboardLogic dashboardLogic;

    public void addDefaultDashboard(Device device) throws ServiceException {
        dashboardLogic.addDefaultDashboard(device);
    }

    public List<Dashboard> getUserDashboards(User user, Boolean withStatus, Boolean withShared, Boolean isAdmin, Integer limit, Integer offset)throws ServiceException{
        return dashboardLogic.getUserDashboards(user, withShared, isAdmin, limit, offset);
    }

    public Dashboard getUserDashboard(User user, String dashboardId) throws ServiceException {
        return dashboardLogic.getDashboard(user, dashboardId);
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
}
