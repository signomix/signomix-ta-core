package com.signomix.core.domain;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import com.signomix.common.db.DashboardDao;
import com.signomix.common.db.DashboardIface;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.gui.Dashboard;
import com.signomix.common.gui.DashboardTemplate;
import com.signomix.common.gui.Widget;
import com.signomix.common.iot.Channel;
import com.signomix.common.iot.Device;
import com.signomix.core.application.exception.ServiceException;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class DashboardLogic {

    @Inject
    Logger logger;

    @Inject
    @DataSource("iot")
    AgroalDataSource dataSource;

    DashboardIface dashboardDao;

    void onStart(@Observes StartupEvent ev) {
        dashboardDao = new DashboardDao();
        dashboardDao.setDatasource(dataSource);
    }

    public void addDefaultDashboard(Device device) throws ServiceException{
        Dashboard dashboard = null;
        try {
            dashboard=dashboardDao.getDashboard(device.getEUI());
            if(null!=dashboard){
                dashboard.setUserID(device.getUserID());
                dashboardDao.updateDashboard(dashboard);
                return;
            }
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
        String dashboardTemplateId = device.getTemplate();
        dashboard = new Dashboard(device.getEUI());
        DashboardTemplate dTemplate = null;
        try {
            dTemplate = dashboardDao.getDashboardTemplate(dashboardTemplateId);
        } catch (IotDatabaseException ex) {
            logger.error(ex.getMessage());
            throw new ServiceException(ex.getMessage(), ex);
        }
        if (null != dTemplate) {
            dashboard.applyTemplate(dTemplate, device.getEUI());
        } else {
            Widget widget;
            Channel chnl;
            HashMap channels = device.getChannels();
            Iterator itr = channels.keySet().iterator();
            int i = 0;
            while (itr.hasNext()) {
                chnl = (Channel) channels.get(itr.next());
                widget = new Widget();
                widget.setName("w" + i);
                widget.setTitle(chnl.getName());
                widget.setType("symbol");
                widget.setDev_id(device.getEUI());
                widget.setChannel(chnl.getName());
                widget.setQuery("last 1");
                widget.setRange("");
                widget.setDescription("");
                widget.setUnitName(guessChannelUnit(chnl.getName()));
                dashboard.addWidget(widget);
                i++;
            }
        }
        dashboard.setName(device.getEUI());
        dashboard.setId(device.getEUI());
        dashboard.setTitle(device.getEUI());
        dashboard.setUserID(device.getUserID());
        dashboard.setShared(false);
        dashboard.setTeam(device.getTeam());
        dashboard.setAdministrators(device.getAdministrators());

        try {
            dashboardDao.addDashboard(dashboard);
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public List<Dashboard> getUserDashboards(String userId, Integer limit, Integer offset)throws ServiceException{
        try {
            return dashboardDao.getUserDashboards(userId, limit, offset);
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    private String guessChannelUnit(String channelName) {
        String unitName = "";
        switch (channelName.toUpperCase()) {
            case "TEMPERATURE":
                unitName = "&deg;C";
                break;
            case "HUMIDITY":
            case "MOISTURE":
                unitName = "%";
                break;
            case "PERCENTAGE":
            case "PERCENT":
            case "BATTERY":
            case "CHARGE":
                unitName = "%";
                break;
            case "VOLTAGE":
            case "BATV":
                unitName = "V";
                break;
            case "CURRENT":
                unitName = "A";
                break;
            case "SPEED":
                unitName = "m/s";
                break;
            case "PRESSURE":
                unitName = "Pa";
                break;
            case "ILLUMINANCE":
            case "LIGHT":
                unitName = "lx";
                break;
            case "MASS":
            case "WEIGHT":
                unitName = "kg";
                break;
            case "DISTANCE":
            case "LENGTH":
            case "WIDTH":
            case "HEIGTH":
                unitName = "m";
                break;
        }
        return unitName;
    }
}
