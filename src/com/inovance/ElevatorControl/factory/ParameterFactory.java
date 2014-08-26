package com.inovance.ElevatorControl.factory;

import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.config.ParameterUpdateTool;
import com.inovance.ElevatorControl.models.ParameterSettings;
import com.inovance.ElevatorControl.models.ParameterStatusItem;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-8-14.
 * Time: 9:42.
 */
public class ParameterFactory {

    public static Parameter getParameter() {
        // 当前连接的设备名称
        String deviceName = ParameterUpdateTool.getInstance().getDeviceName();
        if (deviceName.equals(ApplicationConfig.NormalDeviceType[0])) {
            return new NICE1000Parameter();
        } else if (deviceName.equals(ApplicationConfig.NormalDeviceType[1])) {
            return new NICE1000PlusParameter();
        } else if (deviceName.equals(ApplicationConfig.NormalDeviceType[2])) {
            return new NICE3000Parameter();
        } else if (deviceName.equals(ApplicationConfig.NormalDeviceType[3])) {
            return new NICE3000PlusParameter();
        }
        // Default device
        return new NICE3000PlusParameter();
    }

    public interface Parameter {
        public List<ParameterStatusItem> getInputTerminalStateList(boolean[] bitValues, List<ParameterSettings> settingsList);
    }
}
