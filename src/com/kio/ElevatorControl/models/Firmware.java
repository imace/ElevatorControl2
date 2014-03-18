package com.kio.ElevatorControl.models;

import android.content.Context;
import com.kio.ElevatorControl.R;
import net.tsz.afinal.annotation.sqlite.Id;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-12.
 * Time: 17:00.
 */
public class Firmware {

    @Id
    private int Id;

    private String name;

    private String version;

    private boolean status;

    private String updateDate;

    private String expireDate;

    private int burnTime;

    private int totalBurnTime;

    private String localLocation;

    private String[] downloadStatusTextArray;

    private Context mContext;

    public Firmware(Context context) {
        mContext = context;
        downloadStatusTextArray = mContext
                .getResources()
                .getStringArray(R.array.firmware_download_status);
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getDownloadStatusText() {
        return this.status ? downloadStatusTextArray[0] : downloadStatusTextArray[1];
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }

    public String getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(String expireDate) {
        this.expireDate = expireDate;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
    }

    public int getTotalBurnTime() {
        return totalBurnTime;
    }

    public void setTotalBurnTime(int totalBurnTime) {
        this.totalBurnTime = totalBurnTime;
    }

    public String getResidueTime() {
        return (totalBurnTime - burnTime)
                + mContext
                .getResources()
                .getString(R.string.time_unit_text);
    }

    public String getLocalLocation() {
        return localLocation;
    }

    public void setLocalLocation(String localLocation) {
        this.localLocation = localLocation;
    }


}
