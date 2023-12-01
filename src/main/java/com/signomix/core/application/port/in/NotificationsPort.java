package com.signomix.core.application.port.in;

import java.util.List;

import com.signomix.common.User;
import com.signomix.common.iot.Alert;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.domain.NotificationsLogic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class NotificationsPort {

    @Inject
    NotificationsLogic notificationsLogic;

    public List<Alert> getNotifications(User user, Integer limit, Integer offset) throws ServiceException {
        return notificationsLogic.getNotifications(user, limit, offset);
    }

    public void deleteNotification(User user, long notificationId) throws ServiceException {
        notificationsLogic.deleteNotification(user, notificationId);
    }

    public void deleteNotifications(User user) throws ServiceException {
        notificationsLogic.deleteAllNotifications(user);
    }

    public Long getNotificationsStatus(User user){
        return notificationsLogic.getNotificationsStatus(user);
    }


    
}
