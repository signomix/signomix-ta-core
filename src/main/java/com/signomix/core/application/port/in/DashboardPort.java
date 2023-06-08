package com.signomix.core.application.port.in;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.signomix.common.User;
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
}
