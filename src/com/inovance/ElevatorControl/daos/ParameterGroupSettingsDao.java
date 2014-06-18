package com.inovance.ElevatorControl.daos;

import android.content.Context;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.models.ConfigFactory;
import com.inovance.ElevatorControl.models.ParameterGroupSettings;
import net.tsz.afinal.FinalDb;

import java.util.List;

public class ParameterGroupSettingsDao {

    private static final boolean DEBUG = false;

    public static List<ParameterGroupSettings> findAll(Context context) {
        String condition = " deviceID = '" + ConfigFactory.getInstance().getDeviceSQLID() + "'";
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        return db.findAllByWhere(ParameterGroupSettings.class, condition);
    }

    public static ParameterGroupSettings findById(Context context, int id) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        return db.findById(id, ParameterGroupSettings.class);
    }

    public static void deleteAllByDeviceID(Context context, int deviceID) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.deleteByWhere(ParameterGroupSettings.class, " deviceID = '" + deviceID + "'");
    }

}
