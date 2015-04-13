package com.inovance.elevatorcontrol.models;

import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.config.ParameterUpdateTool;

import net.tsz.afinal.annotation.sqlite.Id;
import net.tsz.afinal.annotation.sqlite.Table;

import org.json.JSONObject;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-10.
 * Time: 9:35.
 */
@Table(name = "USER")
public class User {

    @Id
    private int Id;

    /**
     * 用户名
     */
    private String name;

    /**
     * 公司
     */
    private String company;

    /**
     * 手机号码
     */
    private String cellPhone;

    /**
     * 电话号码
     */
    private String telephone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 蓝牙地址
     */
    private String bluetoothAddress;

    /**
     * 用户权限
     */
    private int permission;

    public User() {

    }

    public User(JSONObject object) {
        this.name = object.optString("UserName".toUpperCase());
        this.bluetoothAddress = object.optString("BluetoothAddress".toUpperCase());
        this.company = object.optString("CompanyName".toUpperCase());
        this.cellPhone = object.optString("MobilePhone".toUpperCase());
        this.telephone = object.optString("ContactTel".toUpperCase());
        this.email = object.optString("Email".toUpperCase());
        switch (object.optInt("Permission".toUpperCase())) {
            case 0:
                this.permission = ParameterUpdateTool.Normal;
                break;
            case 1:
                this.permission = ParameterUpdateTool.Special;
                break;
        }
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getCellPhone() {
        return cellPhone;
    }

    public void setCellPhone(String cellPhone) {
        this.cellPhone = cellPhone;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBluetoothAddress() {
        return bluetoothAddress;
    }

    public void setBluetoothAddress(String bluetoothAddress) {
        this.bluetoothAddress = bluetoothAddress;
    }

    public int getPermission() {
        return ApplicationConfig.IsInternalVersion ? ParameterUpdateTool.Special : permission;
    }

    public void setPermission(int permission) {
        this.permission = permission;
    }
}
