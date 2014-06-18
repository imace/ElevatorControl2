package com.inovance.ElevatorControl.models;

import net.tsz.afinal.annotation.sqlite.Id;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-17.
 * Time: 16:43.
 */
public class Profile {

    @Id
    private int ID;

    /**
     * 配置文件保存日期
     */
    private String createTime;

    /**
     * 配置文件保存名称
     */
    private String fileName;

    /**
     * 设备型号
     */
    private String deviceType;

    /**
     * 厂家名称
     */
    private String vendorName;

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

}
