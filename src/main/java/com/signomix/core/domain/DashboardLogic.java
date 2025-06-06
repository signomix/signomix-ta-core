package com.signomix.core.domain;

import com.signomix.common.Organization;
import com.signomix.common.Token;
import com.signomix.common.TokenType;
import com.signomix.common.User;
import com.signomix.common.db.DashboardIface;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.gui.Dashboard;
import com.signomix.common.gui.DashboardItem;
import com.signomix.common.gui.DashboardTemplate;
import com.signomix.common.gui.Widget;
import com.signomix.common.iot.Channel;
import com.signomix.common.iot.Device;
import com.signomix.common.iot.DeviceGroup;
import com.signomix.core.application.exception.ServiceException;
import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

@ApplicationScoped
public class DashboardLogic {

    @Inject
    Logger logger;

    /*
     * @Inject
     *
     * @DataSource("iot")
     * AgroalDataSource dataSource;
     */
    @Inject
    @DataSource("oltp")
    AgroalDataSource tsDs;

    DashboardIface dashboardDao;

    // IotDatabaseIface iotDao;

    @Inject
    UserLogic userLogic;

    @Inject
    AuthLogic authLogic;

    @Inject
    DeviceLogic deviceLogic;

    @Inject
    GroupLogic groupLogic;

    @Inject
    OrganizationLogic organizationLogic;

    @Inject
    EuiGenerator euiGenerator;

    long defaultOrganizationId = 0;

    @ConfigProperty(
        name = "signomix.exception.api.unauthorized",
        defaultValue = ""
    )
    String exceptionApiUnauthorized;

    @ConfigProperty(name = "signomix.database.type")
    String databaseType;

    void onStart(@Observes StartupEvent ev) {
        /*
         * if ("h2".equalsIgnoreCase(databaseType)) {
         * dashboardDao = new DashboardDao();
         * dashboardDao.setDatasource(dataSource);
         * // iotDao = new IotDatabaseDao();
         * // iotDao.setDatasource(dataSource);
         * defaultOrganizationId = 0;
         * } else
         */
        if ("postgresql".equalsIgnoreCase(databaseType)) {
            dashboardDao = new com.signomix.common.tsdb.DashboardDao();
            dashboardDao.setDatasource(tsDs);
            // iotDao = new com.signomix.common.tsdb.IotDatabaseDao();
            // iotDao.setDatasource(tsDs);
            defaultOrganizationId = 1;
        } else {
            logger.error("Unknown database type: " + databaseType);
        }
        /*
         * try {
         * defaultOrganizationId =
         * iotDao.getParameterValue("system.default.organization", User.ANY);
         * } catch (IotDatabaseException e) {
         * logger.error("Unable to get default organization id: " + e.getMessage());
         * }
         */
    }

    private DashboardIface getDashboardDao() {
        if (null != dashboardDao) {
            return dashboardDao;
        } else {
            /*
             * if ("h2".equalsIgnoreCase(databaseType)) {
             * dashboardDao = new DashboardDao();
             * dashboardDao.setDatasource(dataSource);
             * // iotDao = new IotDatabaseDao();
             * // iotDao.setDatasource(dataSource);
             * defaultOrganizationId = 0;
             * } else
             */if ("postgresql".equalsIgnoreCase(databaseType)) {
                dashboardDao = new com.signomix.common.tsdb.DashboardDao();
                dashboardDao.setDatasource(tsDs);
                // iotDao = new com.signomix.common.tsdb.IotDatabaseDao();
                // iotDao.setDatasource(tsDs);
                defaultOrganizationId = 1;
            } else {
                logger.error("Unknown database type: " + databaseType);
            }
            return dashboardDao;
        }
    }

    public void addDefaultDashboard(Device device) throws ServiceException {
        Dashboard dashboard = null;
        try {
            dashboard = getDashboardDao().getDashboard(device.getEUI());
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
        dashboard.setOrganizationId(device.getOrganizationId());

        try {
            dashboardDao.addDashboard(dashboard);
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public List<Dashboard> getUserDashboards(
        User user,
        Boolean withShared,
        Boolean isAdmin,
        Integer limit,
        Integer offset,
        String searchString
    ) throws ServiceException {
        int limitInt = limit != null ? limit : 100;
        int offsetInt = offset != null ? offset : 0;
        try {
            if (user.uid.equalsIgnoreCase("public")) {
                return dashboardDao.getUserDashboards(
                    user.uid,
                    true,
                    false,
                    limit,
                    offset,
                    searchString
                );
            }
            if (user.organization == defaultOrganizationId) {
                return dashboardDao.getUserDashboards(
                    user.uid,
                    withShared != null ? withShared : true,
                    isAdmin,
                    limitInt,
                    offsetInt,
                    searchString
                );
            } else {
                return dashboardDao.getOrganizationDashboards(
                    user.organization,
                    limitInt,
                    offsetInt,
                    searchString
                );
            }
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public Dashboard getDashboard(User user, String dashboardId)
        throws ServiceException {
        try {
            Dashboard dashboard = getDashboardDao().getDashboard(dashboardId);
            if (null != dashboard) {
                if (
                    !userLogic.hasObjectAccess(
                        user,
                        false,
                        defaultOrganizationId,
                        dashboard
                    )
                ) {
                    logger.warn(
                        "Dashboard found but no access: " + dashboardId
                    );
                    throw new ServiceException(exceptionApiUnauthorized);
                }
            } else {
                logger.warn("Dashboard not found: " + dashboardId);
                throw new ServiceException(exceptionApiUnauthorized);
            }
            if (
                dashboard.getTemplateId() != null &&
                !dashboard.getTemplateId().isEmpty()
            ) {
                DashboardTemplate template = dashboardDao.getDashboardTemplate(
                    dashboard.getTemplateId()
                );
                if (null != template) {
                    dashboard.setWidgets(template.getWidgets());
                    dashboard.setItems(template.getItems());
                    dashboard.replaceVariables(
                        template.getVariables(),
                        dashboard.getVariables()
                    );
                } else {
                    logger.warn(
                        "Dashboard template not found: " +
                        dashboard.getTemplateId()
                    );
                }
            }
            if (
                null == dashboard.getItems() || dashboard.getItems().isEmpty()
            ) {
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
            // if (null != dashboard.getTemplateId() && !dashboard.getTemplateId().isEmpty()) {
            //     DashboardTemplate template = dashboardDao.getDashboardTemplate(dashboard.getTemplateId());
            //     if (null == template) {
            //         logger.warn("Dashboard not found: " + dashboardId);
            //         throw new ServiceException(exceptionApiUnauthorized);
            //     }
            //     dashboard.setWidgets(template.getWidgets());
            //     dashboard.setItems(template.getItems());
            //     dashboard.replaceVariables(template.getVariables(), dashboard.getVariables());
            // }
            return dashboard;
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public Dashboard updateDashboard(User user, Dashboard updatedDashboard)
        throws ServiceException {
        Organization org = organizationLogic.getOrganization(
            user,
            user.organization
        );
        if (org == null) {
            throw new ServiceException("Organization not found");
        }
        if (org.locked) {
            throw new ServiceException("Organization is locked");
        }
        try {
            Dashboard dashboard = dashboardDao.getDashboard(
                updatedDashboard.getId()
            );
            if (null != dashboard) {
                if (
                    !userLogic.hasObjectAccess(
                        user,
                        true,
                        defaultOrganizationId,
                        dashboard
                    )
                ) {
                    throw new ServiceException(exceptionApiUnauthorized);
                }
            } else {
                throw new ServiceException(exceptionApiUnauthorized);
            }
            updatedDashboard = updateToken(updatedDashboard, user);
            if (updatedDashboard.getTemplateId() != null) {
                updatedDashboard.setTemplateId(
                    updatedDashboard.getTemplateId().trim()
                );
            }
            if (updatedDashboard.getVariables() != null) {
                updatedDashboard.setVariables(
                    updatedDashboard.getVariables().trim()
                );
            }
            dashboardDao.updateDashboard(sanitizeWidgets(updatedDashboard));
            boolean sharedAttributeChanged =
                dashboard.isShared() != updatedDashboard.isShared();
            updateDevicesAndGroups(
                updatedDashboard,
                user,
                sharedAttributeChanged
            );
            return updatedDashboard;
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public Dashboard saveDashboard(User user, Dashboard newDashboard)
        throws ServiceException {
        try {
            Dashboard dashboard = dashboardDao.getDashboard(
                newDashboard.getId()
            );
            if (null != dashboard) {
                throw new ServiceException("Dashboard already exists");
            }
            if (
                newDashboard.getId() == null ||
                newDashboard.getId().isEmpty() ||
                newDashboard.getId().equalsIgnoreCase("new")
            ) {
                newDashboard.setId(euiGenerator.createEui("S-"));
            }
            newDashboard.setUserID(user.uid);
            newDashboard.setOrganizationId(user.organization);
            newDashboard = updateToken(newDashboard, user);
            dashboardDao.addDashboard(sanitizeWidgets(newDashboard));
            updateDevicesAndGroups(newDashboard, user, newDashboard.isShared());
            return newDashboard;
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public void removeDashboard(User user, String dashboardId)
        throws ServiceException {
        // logger.warn("REMOVE_WARN: " + dashboardId);
        Organization org = organizationLogic.getOrganization(
            user,
            user.organization
        );
        if (org == null) {
            throw new ServiceException("Organization not found");
        }
        if (org.locked) {
            throw new ServiceException("Organization is locked");
        }
        try {
            Dashboard dashboard = dashboardDao.getDashboard(dashboardId);
            if (null != dashboard) {
                if (
                    !userLogic.hasObjectAccess(
                        user,
                        true,
                        defaultOrganizationId,
                        dashboard
                    )
                ) {
                    throw new ServiceException(exceptionApiUnauthorized);
                }
            } else {
                throw new ServiceException(exceptionApiUnauthorized);
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
        /*
         * System.out.println("widgets " + widgets.getClass().getName());
         * System.out.println("widgets.size() = " + widgets.size());
         */
        // Widget widget;
        LinkedHashMap map;
        String description;
        String type;
        Boolean axisOptions;
        Boolean yAxisAutoScale;
        for (int i = 0; i < widgets.size(); i++) {
            // System.out.println("widgets.get(i) = " +
            // widgets.get(i).getClass().getName());
            map = (LinkedHashMap) widgets.get(i);
            description = (String) map.get("description");
            type = (String) map.get("type");
            // System.out.println("widget type = " + type);
            if (
                null != description &&
                !description.isEmpty() &&
                !"plan".equalsIgnoreCase(type)
            ) {
                String safe = Jsoup.clean(description, Safelist.basic());
                map.put("description", safe);
            }
            axisOptions = (Boolean) map.get("axisOptions");
            if (null == axisOptions) {
                map.put("axisOptions", false);
                axisOptions = false;
            }
            yAxisAutoScale = (Boolean) map.get("yAxisAutoScale");
            if (null == yAxisAutoScale || !axisOptions) {
                map.put("yAxisAutoScale", false);
            }
            if (null == map.get("chartArea")) {
                map.put("chartArea", false);
            }
            if (null == map.get("chartMarkers")) {
                map.put("chartMarkers", false);
            }
            widgets.set(i, map);
        }
        return dashboard;
    }

    private Dashboard updateToken(Dashboard dashboard, User user)
        throws IotDatabaseException {
        Token token;
        logger.info(
            "updateToken: " +
            dashboard.getId() +
            " " +
            dashboard.isShared() +
            " " +
            dashboard.getSharedToken()
        );
        if (dashboard.isShared()) {
            long lifetime = 20 * 365 * 24 * 60; // 20 years in minutes
            if (
                dashboard.getSharedToken() == null ||
                dashboard.getSharedToken().isEmpty()
            ) {
                // TODO: create shared token
                token = new Token("public", lifetime, true);
                token.setIssuer(user.uid);
                token.setType(TokenType.DASHBOARD);
                token.setPayload(dashboard.getId());
                authLogic.saveToken(token);
            } else {
                // TODO: verify or recreate shared token
                token = authLogic.getToken(dashboard.getSharedToken());
                if (token == null) {
                    token = new Token("public", lifetime, true);
                    token.setIssuer(user.uid);
                    token.setType(TokenType.DASHBOARD);
                    token.setPayload(dashboard.getId());
                    authLogic.saveToken(token);
                } else {
                    token.setUid("public");
                    token.setIssuer(user.uid);
                    token.setType(TokenType.DASHBOARD);
                    token.setPayload(dashboard.getId());
                    token.setPermanent(true);
                    token.setLifetime(lifetime);
                    authLogic.modifyToken(token);
                }
            }
            dashboard.setSharedToken(token.getToken());
        } else {
            authLogic.removeDashboardToken(dashboard.getId());
            dashboard.setSharedToken(null);
        }
        return dashboard;
    }

    private void updateDevicesAndGroups(
        Dashboard dashboard,
        User user,
        boolean sharedAttributeChanged
    ) throws IotDatabaseException {
        HashSet<String> deviceIds = dashboard.getDeviceEuis();
        HashSet<String> groupIds = dashboard.getGroupEuis();
        dashboard.isShared();
        deviceIds.forEach(deviceId -> {
            try {
                String team;
                Device device = deviceLogic.getDevice(user, deviceId, false);
                if (device != null) {
                    team = device.getTeam();
                    if (dashboard.isShared()) {
                        if (team == null || team.isEmpty()) {
                            device.setTeam(",public,");
                        } else {
                            if (!team.contains(",public,")) {
                                device.setTeam(team + "public,");
                            }
                        }
                    } else {
                        device.setTeam(team.replace(",public,", ","));
                    }
                    if (sharedAttributeChanged) {
                        deviceLogic.updateDevice(user, deviceId, device);
                    }
                }
            } catch (ServiceException e) {
                logger.error(e.getMessage());
            }
        });
        groupIds.forEach(groupId -> {
            try {
                DeviceGroup group = groupLogic.getGroup(null, groupId);
                if (group != null) {
                    String team = group.getTeam();
                    if (dashboard.isShared()) {
                        if (team == null || team.isEmpty()) {
                            group.setTeam(",public,");
                        } else {
                            if (!team.contains(",public,")) {
                                group.setTeam(team + "public,");
                            }
                        }
                    } else {
                        group.setTeam(team.replace(",public,", ","));
                    }
                    groupLogic.updateGroup(null, group);
                }
            } catch (ServiceException e) {
                logger.error(e.getMessage());
            }
        });
    }

    public DashboardTemplate getDashboardTemplate(User user, String templateId)
        throws ServiceException {
        try {
            DashboardTemplate template = dashboardDao.getDashboardTemplate(
                templateId
            );
            if (null != template) {
                if (
                    !userLogic.hasObjectAccess(
                        user,
                        false,
                        defaultOrganizationId,
                        template
                    )
                ) {
                    logger.warn(
                        "Dashboard template found but no access: " + templateId
                    );
                    throw new ServiceException(exceptionApiUnauthorized);
                }
            } else {
                logger.warn("Dashboard template not found: " + templateId);
                throw new ServiceException(exceptionApiUnauthorized);
            }
            return template;
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public List<DashboardTemplate> getDashboardTemplates(
        User user,
        Integer limit,
        Integer offset,
        String searchString
    ) throws ServiceException {
        int limitInt = limit != null ? limit : 100;
        int offsetInt = offset != null ? offset : 0;
        try {
            if (user.organization == defaultOrganizationId) {
                return dashboardDao.getUserDashboardTemplates(
                    user.uid,
                    limitInt,
                    offsetInt,
                    searchString
                );
            } else {
                return dashboardDao.getOrganizationDashboardTemplates(
                    user.organization,
                    limitInt,
                    offsetInt,
                    searchString
                );
            }
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public void addDashboardTemplate(
        User user,
        DashboardTemplate dashboardTemplate
    ) throws ServiceException {
        Organization org = organizationLogic.getOrganization(
            user,
            user.organization
        );
        if (org == null) {
            throw new ServiceException("Organization not found");
        }
        if (org.locked) {
            throw new ServiceException("Organization is locked");
        }
        try {
            if (
                dashboardTemplate.getId() == null ||
                dashboardTemplate.getId().isEmpty() ||
                dashboardTemplate.getId().equalsIgnoreCase("new")
            ) {
                dashboardTemplate.setId(euiGenerator.createEui("T-"));
            }
            dashboardTemplate.setOrganizationId(user.organization); //
            dashboardTemplate.parseVariables();

            dashboardDao.addDashboardTemplate(dashboardTemplate);
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public void updateDashboardTemplate(
        User user,
        String templateId,
        DashboardTemplate dashboardTemplate
    ) throws ServiceException {
        Organization org = organizationLogic.getOrganization(
            user,
            user.organization
        );
        if (org == null) {
            throw new ServiceException("Organization not found");
        }
        if (org.locked) {
            throw new ServiceException("Organization is locked");
        }
        try {
            DashboardTemplate template = dashboardDao.getDashboardTemplate(
                templateId
            );
            if (null == template) {
                throw new ServiceException("Dashboard template not found");
            }
            if (null != template) {
                if (
                    !userLogic.hasObjectAccess(
                        user,
                        true,
                        defaultOrganizationId,
                        template
                    )
                ) {
                    throw new ServiceException(exceptionApiUnauthorized);
                }
            } else {
                throw new ServiceException(exceptionApiUnauthorized);
            }
            if (!templateId.equals(template.getId())) {
                throw new ServiceException("Dashboard template ID mismatch");
            }
            dashboardTemplate.parseVariables();
            dashboardDao.updateDashboardTemplate(dashboardTemplate);
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public void removeDashboardTemplate(User user, String templateId)
        throws ServiceException {
        Organization org = organizationLogic.getOrganization(
            user,
            user.organization
        );
        if (org == null) {
            throw new ServiceException("Organization not found");
        }
        if (org.locked) {
            throw new ServiceException("Organization is locked");
        }
        try {
            DashboardTemplate template = dashboardDao.getDashboardTemplate(
                templateId
            );
            if (null != template) {
                if (
                    !userLogic.hasObjectAccess(
                        user,
                        true,
                        defaultOrganizationId,
                        template
                    )
                ) {
                    throw new ServiceException(exceptionApiUnauthorized);
                }
            } else {
                throw new ServiceException(exceptionApiUnauthorized);
            }
            dashboardDao.removeDashboardTemplate(templateId);
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public static List<String> findBracedSubstrings(String input) {
        List<String> result = new ArrayList<>();
        int start = -1;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '{') {
                start = i;
            } else if (input.charAt(i) == '}' && start != -1) {
                result.add(input.substring(start, i + 1));
                start = -1;
            }
        }
        return result;
    }
}
