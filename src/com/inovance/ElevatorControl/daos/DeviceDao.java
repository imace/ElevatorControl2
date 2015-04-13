package com.inovance.elevatorcontrol.daos;

import android.content.Context;

import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.models.Device;

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

    public static void update(Context context, Device device) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.update(device);
    }

    public static List<Device> findAll(Context context) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        return db.findAll(Device.class);
    }

    /**
     * 更加设备名称和类型查找
     *
     * @param context Context
     * @param name    Name
     * @param type    Type
     * @return Device
     */
    public static Device findByName(Context context, String name, int type) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        String condition = "(" + " deviceName = '" + name.toUpperCase() + "' or deviceName='" + name.toLowerCase() + "'" + ")";
        condition = condition + " and deviceType ='" + type + "'";
        List<Device> deviceList = db.findAllByWhere(Device.class, condition);
        if (deviceList != null && deviceList.size() > 0) {
            return deviceList.get(0);
        }
        return null;
    }

}
