package com.inovance.elevatorcontrol.models;

import net.tsz.afinal.annotation.sqlite.Id;
import net.tsz.afinal.annotation.sqlite.Table;

import org.json.JSONObject;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-12.
 * Time: 17:00.
 */
@Table(name = "FIRMWARE")
public class Firmware {

    @Id
    private int Id;

    private String bluetoothAddress;

    private String deviceID;

    /**
     * 是否专用设备
     */
    private boolean isSpecialDevice;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否已批准
     */
    private boolean isApproved;

    private String createDate;

    private String approveDate;

    private String approveRemark;

    private String fileURL;

    private String fileName;

    private String vendorName;

    /**
     * 提取日期
     */
    private String downloadDate;

    /**
     * 烧录次数
     */
    private int burnTimes;

    /**
     * 总的烧录次数
     */
    private int totalBurnTimes;

    /**
     * 过期时间
     */
    private String expireDate;

    public Firmware() {

    }

    public Firmware(JSONObject object) {
        this.Id = object.optInt("ID".toUpperCase());
        this.bluetoothAddress = object.optString("BluetoothAddress".toUpperCase());
        this.deviceID = object.optString("FK_DeviceID".toUpperCase());
        this.isSpecialDevice = object.optInt("IsSpecialDevice".toUpperCase()) == 1;
        this.remark = object.optString("Remark".toUpperCase());
        this.isApproved = object.optString("ApproveState".toUpperCase()).equalsIgnoreCase("已审批");
        this.createDate = object.optString("CreateDate".toUpperCase());
        this.approveDate = object.optString("ApproveDate".toUpperCase());
        this.approveRemark = object.optString("ApproveRemark".toUpperCase());
        this.fileURL = object.optString("FileUrl".toUpperCase());
        this.downloadDate = object.optString("GetFileDate".toUpperCase());
        this.totalBurnTimes = object.optInt("UseTimes".toUpperCase());
        this.vendorName = object.optString("VendorName".toUpperCase());
        this.expireDate = object.optString("DateLimit".toUpperCase());
    }

    public int getId() {
        return Id;
    }

    public void setId(int Id) {
        this.Id = Id;
    }

    public String getBluetoothAddress() {
        return bluetoothAddress;
    }

    public void setBluetoothAddress(String bluetoothAddress) {
        this.bluetoothAddress = bluetoothAddress;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public boolean isSpecialDevice() {
        return isSpecialDevice;
    }

    public void setSpecialDevice(boolean isSpecialDevice) {
        this.isSpecialDevice = isSpecialDevice;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean isApproved) {
        this.isApproved = isApproved;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getApproveDate() {
        return approveDate;
    }

    public void setApproveDate(String approveDate) {
        this.approveDate = approveDate;
    }

    public String getApproveRemark() {
        return approveRemark;
    }

    public void setApproveRemark(String approveRemark) {
        this.approveRemark = approveRemark;
    }

    public String getFileURL() {
        return fileURL;
    }

    public void setFileURL(String fileURL) {
        this.fileURL = fileURL;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDownloadDate() {
        return downloadDate;
    }

    public void setDownloadDate(String downloadDate) {
        this.downloadDate = downloadDate;
    }

    public int getBurnTimes() {
        return burnTimes;
    }

    public void setBurnTimes(int burnTimes) {
        this.burnTimes = burnTimes;
    }

    public int getTotalBurnTimes() {
        return totalBurnTimes;
    }

    public void setTotalBurnTimes(int totalBurnTimes) {
        this.totalBurnTimes = totalBurnTimes;
    }

    public String getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(String expireDate) {
        this.expireDate = expireDate;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    /**
     * 是否超过使用期限
     *
     * @return boolean
     */
    public boolean isExpired() {
        return !(getExpireDate() == null || getExpireDate().length() == 0)
                && System.currentTimeMillis() > Long.parseLong(getExpireDate()) * 1000;
    }

}
