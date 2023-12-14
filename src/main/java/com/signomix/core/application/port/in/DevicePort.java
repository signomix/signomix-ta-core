package com.signomix.core.application.port.in;

import java.util.List;

import com.signomix.common.User;
import com.signomix.common.iot.Device;
import com.signomix.core.application.exception.ServiceException;
import com.signomix.core.domain.DeviceLogic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DevicePort {
    @Inject
    DeviceLogic deviceLogic;


    public Device getDevice(User user, String eui, Boolean withStatus) throws ServiceException{
        return deviceLogic.getDevice(user, eui, (null!=withStatus?withStatus:false));
    }

    public List<Device> getUserDevices(User user, Boolean withStatus, Integer limit, Integer offset, String searchString)throws ServiceException{
        return deviceLogic.getUserDevices(user, (null!=withStatus?withStatus:false), limit, offset, searchString);
    }

    public void deleteDevice(User user, String eui) throws ServiceException{
        deviceLogic.deleteDevice(user, eui);
    }

    public void updateDevice(User user, String eui, Device device) throws ServiceException{
        deviceLogic.updateDevice(user, eui, device);
    }

    public void createDevice(User user, Device device) throws ServiceException{
        deviceLogic.createDevice(user, device);
    }

    public void checkDevices() throws ServiceException{
        deviceLogic.checkDevices();
    }
}
