package com.inovance.elevatorcontrol.daos;

import android.content.Context;

import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.config.ParameterUpdateTool;
import com.inovance.elevatorcontrol.models.ParameterSettings;

import net.tsz.afinal.FinalDb;

import java.util.ArrayList;
import java.util.List;

public class ParameterSettingsDao {

    private static final boolean DEBUG = false;

    public static List<ParameterSettings> findAllByCodes(Context context, String[] codes) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        int size = codes.length;
        if (size > 0) {
            String condition = "";
            if (size == 1) {
                condition = " code = '" + codes[0] + "'";
            } else {
                for (int i = 0; i < size; i++) {
                    if (i == 0) {
                        condition += " code = '" + codes[i];
                    } else if (i == size - 1) {
                        condition += "' or code = '" + codes[i] + "'";
                    } else {
                        condition += "' or code = '" + codes[i];
                    }
                }
            }
            condition = "(" + condition + ")" + " and "
                    + " deviceID = '" + ParameterUpdateTool.getInstance().getDeviceSQLID() + "'";
            return db.findAllByWhere(ParameterSettings.class, condition);
        }
        return new ArrayList<ParameterSettings>();
    }

    /**
     * Find By Type
     *
     * @param context Context
     * @param type    Type
     * @return List<ParameterSettings>
     */
    public static List<ParameterSettings> findByType(Context context, int type) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        String condition = " type = '" + type + "'" + " and "
                + " deviceID = '" + ParameterUpdateTool.getInstance().getDeviceSQLID() + "'";
        return db.findAllByWhere(ParameterSettings.class, condition);
    }

    public static void deleteAllByDeviceID(Context context, int deviceID) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.deleteByWhere(ParameterSettings.class, " deviceID = '" + deviceID + "'");
    }
}
