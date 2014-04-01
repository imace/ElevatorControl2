package com.kio.ElevatorControl.models;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-31.
 * Time: 14:36.
 */
public class ListHolder {

    /**
     * Temp List RealTimeMonitor Holder
     */
    private List<RealTimeMonitor> realTimeMonitorList;

    /**
     * Temp List ParameterSettings Holder
     */
    private List<ParameterSettings> parameterSettingsList;

    public List<ParameterSettings> getParameterSettingsList() {
        return parameterSettingsList;
    }

    public void setParameterSettingsList(List<ParameterSettings> parameterSettingsList) {
        this.parameterSettingsList = parameterSettingsList;
    }

    public List<RealTimeMonitor> getRealTimeMonitorList() {
        return realTimeMonitorList;
    }

    public void setRealTimeMonitorList(List<RealTimeMonitor> realTimeMonitorList) {
        this.realTimeMonitorList = realTimeMonitorList;
    }
}
