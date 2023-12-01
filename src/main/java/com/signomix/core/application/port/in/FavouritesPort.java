package com.signomix.core.application.port.in;

import java.util.List;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.gui.Dashboard;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.domain.FavouritesLogic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FavouritesPort {

    @Inject
    Logger logger;

    @Inject
    FavouritesLogic favouritesLogic;

    public List<Dashboard> getFavouriteDashboards(User user) throws ServiceException {
        return favouritesLogic.getFavouriteDashboards(user);
    }

    public void addFavouriteDashboard(User user, String dashboardId) throws ServiceException {
        favouritesLogic.addFavouriteDashboard(user, dashboardId);
    }

    public void removeFavouriteDashboard(User user, String dashboardId) throws ServiceException {
        favouritesLogic.removeFavouriteDashboard(user, dashboardId);
    }
    
}
