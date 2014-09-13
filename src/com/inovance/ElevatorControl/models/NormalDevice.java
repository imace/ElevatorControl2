package com.inovance.elevatorcontrol.models;

import org.json.JSONObject;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-25.
 * Time: 16:05.
 */
public class NormalDevice {

    private int ID;

    /**
     * 设备名称
     */
    private String name;

    /**
     * 设备编号
     */
    private String number;

    /**
     * 备注
     */
    private String description;

    public NormalDevice(JSONObject object) {
        this.ID = object.optInt("ID".toUpperCase());
        this.name = object.optString("DeviceName".toUpperCase());
        this.number = object.optString("DeviceNum".toUpperCase());
        this.description = object.optString("DeviceDescription".toUpperCase());
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
