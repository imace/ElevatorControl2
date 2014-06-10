package com.inovance.ElevatorControl.daos;

import android.content.Context;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.models.ParameterSettings;
import com.inovance.ElevatorControl.models.UserFactory;
import net.tsz.afinal.FinalDb;

import java.util.ArrayList;
import java.util.List;

public class ParameterSettingsDao {

    private static final boolean DEBUG = false;

    /**
     * Find By Names Array
     *
     * @param context context
     * @param names   Names Array
     * @return List<ParameterSettings>
     */
    public static List<ParameterSettings> findByNames(Context context, String[] names) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        int size = names.length;
        if (size > 0) {
            String condition = "";
            if (size == 1) {
                condition = " name = '" + names[0] + "'";
            } else {
                for (int i = 0; i < size; i++) {
                    if (i == 0) {
                        condition += " name = '" + names[i];
                    } else if (i == size - 1) {
                        condition += "' or name = '" + names[i] + "'";
                    } else {
                        condition += "' or name = '" + names[i];
                    }
                }
            }
            condition = "(" + condition + ")" + " and "
                    + " deviceID = '" + UserFactory.getInstance().getDeviceType() + "'";
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
                + " deviceID = '" + UserFactory.getInstance().getDeviceType() + "'";
        return db.findAllByWhere(ParameterSettings.class, condition);
    }

    public static void deleteAllByDeviceID(Context context, int deviceID) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.deleteByWhere(ParameterSettings.class, " deviceID = '" + deviceID + "'");
    }
}
