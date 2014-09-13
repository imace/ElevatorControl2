package com.inovance.elevatorcontrol.models;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-31.
 * Time: 14:36.
 */
public class ObjectListHolder {

    /**
     * Temp List RealTimeMonitor Holder
     */
    private List<RealTimeMonitor> realTimeMonitorList;

    /**
     * Temp List HistoryError Holder
     */
    private List<HistoryError> historyErrorList;

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

    public List<HistoryError> getHistoryErrorList() {
        return historyErrorList;
    }

    public void setHistoryErrorList(List<HistoryError> historyErrorList) {
        this.historyErrorList = historyErrorList;
    }


    public List<RealTimeMonitor> getRealTimeMonitorList() {
        return realTimeMonitorList;
    }

    public void setRealTimeMonitorList(List<RealTimeMonitor> realTimeMonitorList) {
        this.realTimeMonitorList = realTimeMonitorList;
    }
}
