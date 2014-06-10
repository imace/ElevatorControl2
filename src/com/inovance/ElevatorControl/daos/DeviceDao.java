package com.inovance.ElevatorControl.daos;

import android.content.Context;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.models.Device;
import net.tsz.afinal.FinalDb;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-6-9.
 * Time: 11:33.
 */
public class DeviceDao {

    private static final boolean DEBUG = false;

    public static void save(Context context, Device device) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.saveBindId(device);
    }

    public static Device findByName(Context context, String name) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        List<Device> deviceList = db.findAllByWhere(Device.class, " deviceType = '"
                + name.toUpperCase()
                + "' or deviceType='"
                + name.toLowerCase()
                + "'");
        if (deviceList != null && deviceList.size() > 0) {
            return deviceList.get(0);
        }
        return null;
    }

}
