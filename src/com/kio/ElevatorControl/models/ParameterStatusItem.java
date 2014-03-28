package com.kio.ElevatorControl.models;

import com.kio.ElevatorControl.R;
import com.mobsandgeeks.adapters.InstantText;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-28.
 * Time: 13:24.
 */
public class ParameterStatusItem {

    public String id;

    public String name;

    public boolean status;

    public String statusString;

    @InstantText(viewId = R.id.status_name)
    public String getName() {
        return name;
    }

    public boolean getStatus() {
        return status;
    }

    @InstantText(viewId = R.id.status_value)
    public String getStatusString() {
        if (statusString == null) {
            return status ? "开" : "关";
        }
        return statusString;
    }
}
