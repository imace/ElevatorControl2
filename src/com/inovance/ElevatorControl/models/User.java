package com.inovance.ElevatorControl.models;

import net.tsz.afinal.annotation.sqlite.Id;
import org.json.JSONObject;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-10.
 * Time: 9:35.
 */
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

    public User() {

    }

    public User(JSONObject object) {
        this.name = object.optString("UserName");
        this.bluetoothAddress = object.optString("BluetoothAddress");
        this.company = object.optString("CompanyName");
        this.cellPhone = object.optString("MobilePhone");
        this.telephone = object.optString("ContactTel");
        this.email = object.optString("Email");
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
}
