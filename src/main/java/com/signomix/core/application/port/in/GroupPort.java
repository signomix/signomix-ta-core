package com.signomix.core.application.port.in;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.signomix.common.User;
import com.signomix.common.iot.DeviceGroup;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.domain.GroupLogic;

@ApplicationScoped
public class GroupPort {
    @Inject
    GroupLogic groupLogic;


    public DeviceGroup getGroup(User user, String eui) throws ServiceException{
        return groupLogic.getGroup(user, eui);
    }

    public List<DeviceGroup> getUserGroups(User user, Integer limit, Integer offset, String searchString)throws ServiceException{
        return groupLogic.getUserGroups(user, limit, offset, searchString);
    }

    public void deleteGroup(User user, String eui) throws ServiceException{
        groupLogic.removeGroup(user, eui);
    }

    public void updateGroup(User user, DeviceGroup group) throws ServiceException{
        groupLogic.updateGroup(user, group);
    }

    public void createGroup(User user, DeviceGroup group) throws ServiceException{
        groupLogic.saveGroup(user, group);
    }

}
