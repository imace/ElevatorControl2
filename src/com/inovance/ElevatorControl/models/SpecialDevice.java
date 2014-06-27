package com.inovance.ElevatorControl.models;

import org.json.JSONObject;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-10.
 * Time: 15:27.
 */
public class SpecialDevice {

    private int ID;

    /**
     * 设备名称
     */
    private String name;

    /**
     * 设备别名
     */
    private String displayName;

    /**
     * 设备型号
     */
    private String number;

    /**
     * 厂商编号
     */
    private int vendorID;

    /**
     * 设备通信码
     */
    private String code;

    /**
     * 设备描述
     */
    private String description;

    public SpecialDevice(JSONObject object) {
        this.ID = object.optInt("ID");
        this.name = object.optString("DeviceName");
        this.displayName = object.optString("DeviceDisplayName");
        this.number = object.optString("DeviceNum");
        this.vendorID = object.optInt("FK_Vendor");
        this.description = object.optString("DeviceDescription");
        this.code = object.optString("Code");
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public int getVendorID() {
        return vendorID;
    }

    public void setVendorID(int vendorID) {
        this.vendorID = vendorID;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
