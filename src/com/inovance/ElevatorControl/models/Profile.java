package com.inovance.elevatorcontrol.models;

import net.tsz.afinal.annotation.sqlite.Id;
import net.tsz.afinal.annotation.sqlite.Table;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-17.
 * Time: 16:43.
 */
@Table(name = "PROFILE")
public class Profile {

    @Id
    private int Id;

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

    public int getId() {
        return Id;
    }

    public void setId(int Id) {
        this.Id = Id;
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
