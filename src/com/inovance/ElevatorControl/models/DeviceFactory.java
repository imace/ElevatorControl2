package com.inovance.ElevatorControl.models;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-5-29.
 * Time: 11:51.
 */
public class DeviceFactory {

    private static DeviceFactory ourInstance = new DeviceFactory();

    /**
     * 设备型号
     */
    private String deviceType;

    /**
     * 厂家编号
     */
    private String supplierCode;

    /**
     * 故障复位指令
     */
    private String restoreErrorCode = "6000";

    public static DeviceFactory getInstance() {
        return ourInstance;
    }

    private DeviceFactory() {

    }

    public String getRestoreErrorCode() {
        return restoreErrorCode;
    }

    public void setRestoreErrorCode(String restoreErrorCode) {
        this.restoreErrorCode = restoreErrorCode;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

}
