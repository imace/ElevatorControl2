package com.kio.ElevatorControl.models;

import android.content.res.Resources;
import com.kio.ElevatorControl.R;
import com.mobsandgeeks.adapters.InstantText;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-12.
 * Time: 17:00.
 */
public class Firmware {

    private int Id;

    private String name;

    private String version;

    private boolean status;

    private String localLocation;

    private String[] downloadStatusText;

    public Firmware(){
        downloadStatusText = Resources
                .getSystem()
                .getStringArray(R.array.firmware_download_status);
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    @InstantText(viewId = R.id.firmware_name)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @InstantText(viewId = R.id.firmware_version)
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

    @InstantText(viewId = R.id.firmware_version)
    public String getDownloadStatusText() {
        if (this.status){
            return downloadStatusText[0];
        }
        else {
            return downloadStatusText[1];
        }
    }

    public String getLocalLocation() {
        return localLocation;
    }

    public void setLocalLocation(String localLocation) {
        this.localLocation = localLocation;
    }
}
