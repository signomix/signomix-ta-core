package com.signomix.core.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.signomix.common.User;
import com.signomix.common.db.DashboardDao;
import com.signomix.common.db.DashboardIface;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.gui.Dashboard;
import com.signomix.common.gui.DashboardItem;
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

    @Inject
    DeviceLogic deviceLogic;

    void onStart(@Observes StartupEvent ev) {
        dashboardDao = new DashboardDao();
        dashboardDao.setDatasource(dataSource);
    }

    public void addDefaultDashboard(Device device) throws ServiceException {
        Dashboard dashboard = null;
        try {
            dashboard = dashboardDao.getDashboard(device.getEUI());
            if (null != dashboard) {
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
                dashboard.addItem(new DashboardItem(0, i, i % 10, (i / 10)));
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

    public List<Dashboard> getUserDashboards(User user, Boolean withShared, Boolean isAdmin, Integer limit,
            Integer offset) throws ServiceException {
        try {
            return dashboardDao.getUserDashboards(user.uid, withShared, isAdmin, limit, offset);
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public Dashboard getDashboard(User user, String dashboardId) throws ServiceException {
        try {
            Dashboard dashboard = dashboardDao.getDashboard(dashboardId);
            if (null != dashboard) {
                if (!hasAccessToDashboard(user, dashboard, false)) {
                    throw new ServiceException("Dashboard not found");
                }
            } else {
                throw new ServiceException("Dashboard not found");
            }
            if (null == dashboard.getItems() || dashboard.getItems().isEmpty()) {
                ArrayList<DashboardItem> items = new ArrayList<>();
                ArrayList widgets = dashboard.getWidgets();
                for (int i = 0; i < widgets.size(); i++) {
                    items.add(new DashboardItem(0, i, i % 10, (i / 10)));
                }
                dashboard.setItems(items);
                dashboard.setVersion(1);
            } else {
                dashboard.setVersion(2);
            }
            return dashboard;
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    private boolean hasAccessToDashboard(User user, Dashboard dashboard, boolean writeAccess) {
        //TODO: Organization access
        if(user.type==User.OWNER){ // platform administator
            return true;
        }
        if (dashboard.getUserID().equals(user.uid)) {
            return true;
        }
        if (dashboard.getAdministrators().contains("," + user.uid + ",")) {
            return true;
        }
        if (!writeAccess) {
            if (dashboard.getTeam().contains("," + user.uid + ",")) {
                return true;
            }
        }
        return false;
    }

    public Dashboard updateDashboard(User user, Dashboard updatedDashboard) throws ServiceException {
        try {
            Dashboard dashboard = dashboardDao.getDashboard(updatedDashboard.getId());
            if (null != dashboard) {
                if (!hasAccessToDashboard(user, dashboard, true)) {
                    throw new ServiceException("Dashboard not found");
                }
            } else {
                throw new ServiceException("Dashboard not found");
            }
            dashboardDao.updateDashboard(sanitizeWidgets(updatedDashboard));
            return dashboard;
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public Dashboard saveDashboard(User user, Dashboard newDashboard) throws ServiceException {
        try {
            Dashboard dashboard = dashboardDao.getDashboard(newDashboard.getId());
            if (null != dashboard) {
                throw new ServiceException("Dashboard already exists");
            }
            newDashboard.setId(deviceLogic.createEui("S-"));
            newDashboard.setUserID(user.uid);
            dashboardDao.addDashboard(sanitizeWidgets(newDashboard));
            return newDashboard;
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public void removeDashboard(User user, String dashboardId) throws ServiceException {
        try {
            Dashboard dashboard = dashboardDao.getDashboard(dashboardId);
            if (null != dashboard) {
                if (!hasAccessToDashboard(user, dashboard, true)) {
                    throw new ServiceException("Dashboard not found");
                }
            } else {
                throw new ServiceException("Dashboard not found");
            }
            dashboardDao.removeDashboard(dashboardId);
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

    private Dashboard sanitizeWidgets(Dashboard dashboard) {
        // return dashboard;
        ArrayList widgets = dashboard.getWidgets();
        System.out.println("widgets " + widgets.getClass().getName());
        System.out.println("widgets.size() = " + widgets.size());
        // Widget widget;
        LinkedHashMap map;
        String description;
        for (int i = 0; i < widgets.size(); i++) {
            System.out.println("widgets.get(i) = " + widgets.get(i).getClass().getName());
            map = (LinkedHashMap) widgets.get(i);
            description = (String) map.get("description");
            if (null != description && !description.isEmpty()) {
                String safe = Jsoup.clean(description, Safelist.basic());
                map.put("description", safe);
            }
            widgets.set(i, map);
        }
        return dashboard;
    }
}
