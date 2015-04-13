package com.inovance.elevatorcontrol.models;

import net.tsz.afinal.annotation.sqlite.Table;

import org.json.JSONObject;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-23.
 * Time: 10:33.
 */
@Table(name = "VENDOR")
public class Vendor {

    private int Id;

    /**
     * 厂商编号
     */
    private String serialNumber;

    /**
     * 名称
     */
    private String name;

    /**
     * 简称
     */
    private String shortName;

    /**
     * 联系人
     */
    private String contact;

    /**
     * 电话
     */
    private String telPhone;

    /**
     * 地址
     */
    private String Address;

    /**
     * 传真
     */
    private String fax;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 备注
     */
    private String remark;

    public Vendor() {

    }

    public Vendor(JSONObject object) {
        this.Id = object.optInt("Id".toUpperCase());
        this.name = object.optString("VendorName".toUpperCase());
        this.serialNumber = object.optString("VendorNum".toUpperCase());
    }

    public int getId() {
        return Id;
    }

    public void setId(int Id) {
        this.Id = Id;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getTelPhone() {
        return telPhone;
    }

    public void setTelPhone(String telPhone) {
        this.telPhone = telPhone;
    }

    public String getAddress() {
        return Address;
    }

    public void setAddress(String address) {
        Address = address;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
