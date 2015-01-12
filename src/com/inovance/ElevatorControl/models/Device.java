package com.inovance.elevatorcontrol.models;

import net.tsz.afinal.annotation.sqlite.Id;
import net.tsz.afinal.annotation.sqlite.Table;
import net.tsz.afinal.annotation.sqlite.Transient;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-6-9.
 * Time: 11:14.
 */
@Table(name = "DEVICE")
public class Device {

    @Id
    private int Id;

    /**
     * 标准设备
     */
    @Transient
    public static final int NormalDevice = 1;

    /**
     * 专有设备
     */
    @Transient
    public static final int SpecialDevice = 2;

    /**
     * 设备类型
     */
    private int deviceType;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * API 设备对应 ID
     */
    private int remoteID;

    /**
     * 设备厂家编号
     */
    private String deviceSupplierCode;

    /**
     * 状态码更新时间
     */
    private String stateCodeUpdateTime;

    /**
     * 功能码更新时间
     */
    private String functionCodeUpdateTime;

    /**
     * 故障帮助更新时间
     */
    private String errorHelpUpdateTime;

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public int getRemoteID() {
        return remoteID;
    }

    public void setRemoteID(int remoteID) {
        this.remoteID = remoteID;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceSupplierCode() {
        return deviceSupplierCode;
    }

    public void setDeviceSupplierCode(String deviceSupplierCode) {
        this.deviceSupplierCode = deviceSupplierCode;
    }

    public String getStateCodeUpdateTime() {
        return stateCodeUpdateTime;
    }

    public void setStateCodeUpdateTime(String stateCodeUpdateTime) {
        this.stateCodeUpdateTime = stateCodeUpdateTime;
    }

    public String getFunctionCodeUpdateTime() {
        return functionCodeUpdateTime;
    }

    public void setFunctionCodeUpdateTime(String functionCodeUpdateTime) {
        this.functionCodeUpdateTime = functionCodeUpdateTime;
    }

    public String getErrorHelpUpdateTime() {
        return errorHelpUpdateTime;
    }

    public void setErrorHelpUpdateTime(String errorHelpUpdateTime) {
        this.errorHelpUpdateTime = errorHelpUpdateTime;
    }

}
