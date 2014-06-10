package com.inovance.ElevatorControl.models;

import net.tsz.afinal.annotation.sqlite.Id;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-6-9.
 * Time: 11:14.
 */
public class Device {

    @Id
    private int Id;

    /**
     * 设备型号
     */
    private String deviceType;

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

}
