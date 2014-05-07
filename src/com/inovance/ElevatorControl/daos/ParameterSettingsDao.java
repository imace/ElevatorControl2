package com.inovance.ElevatorControl.daos;

import android.content.Context;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.models.ParameterSettings;
import net.tsz.afinal.FinalDb;

import java.util.ArrayList;
import java.util.List;

public class ParameterSettingsDao {

    private static final boolean DEBUG = true;

    public static ParameterSettings findById(Context context, int id) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        return db.findById(id, ParameterSettings.class);
    }

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
        return db.findAllByWhere(ParameterSettings.class, " type = '" + String.valueOf(type) + "'");
    }

}
