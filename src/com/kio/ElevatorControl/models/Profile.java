package com.kio.ElevatorControl.models;

import net.tsz.afinal.annotation.sqlite.Id;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-17.
 * Time: 16:43.
 */
public class Profile {

    @Id
    private int Id;

    /**
     * 设备型号
     */
    private String equipmentModel;

    /**
     * 厂家编号
     */
    private String manufacturersSerialNumber;

    /**
     * 配置文件版本
     */
    private String version;

    /**
     * 配置文件更新日期
     */
    private String updateDate;

    /**
     * 配置文件保存名称
     */
    private String fileName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getEquipmentModel() {
        return equipmentModel;
    }

    public void setEquipmentModel(String equipmentModel) {
        this.equipmentModel = equipmentModel;
    }

    public String getManufacturersSerialNumber() {
        return manufacturersSerialNumber;
    }

    public void setManufacturersSerialNumber(String manufacturersSerialNumber) {
        this.manufacturersSerialNumber = manufacturersSerialNumber;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }
}
