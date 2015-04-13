package com.inovance.elevatorcontrol.factory;

import android.content.Context;

import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.config.ParameterUpdateTool;
import com.inovance.elevatorcontrol.models.ParameterSettings;
import com.inovance.elevatorcontrol.models.ParameterStatusItem;
import com.inovance.elevatorcontrol.models.TroubleGroup;

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
        public List<ParameterStatusItem> getInputTerminalStateList(boolean[] bitValues,
                                                                   List<ParameterSettings> settingsList);

        public List<ParameterStatusItem> getOutputTerminalStateList(boolean[] bitValues,
                                                                    List<ParameterSettings> settingsList);

        public int[] getIndexStatus(ParameterSettings settings);

        public int getWriteInputTerminalValue(int value1, int value2, boolean alwaysOn);

        public String getDescriptionText(ParameterSettings settings);

        // 1000 F6-11 ~ F6-36 | 1000+ F6-11 ~ F6-60 只针对 1000/1000+
        public int getSelectedIndex(ParameterSettings settings);

        // 1000 F6-11 ~ F6-36 | 1000+ F6-11 ~ F6-60 只针对 1000/1000+
        public int getWriteValue(ParameterSettings settings, int index);

        // 输入端子状态列表 常开/常闭
        // public String[] getInputTerminalStatusList(ParameterSettings settings);

        // 输入端子常闭值
        public int getAlwaysCloseValue(int value);

        public List<TroubleGroup> getTroubleGroupList(Context context, List<ParameterSettings> settingsList);
    }
}
