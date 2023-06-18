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

    public List<Dashboard> getUserDashboards(User user, Boolean withStatus, Integer limit, Integer offset)throws ServiceException{
        return dashboardLogic.getUserDashboards(user, limit, offset);
    }

    public Dashboard getUserDashboard(User user, String dashboardId) throws ServiceException {
        return dashboardLogic.getDashboard(user, dashboardId);
    }
}
