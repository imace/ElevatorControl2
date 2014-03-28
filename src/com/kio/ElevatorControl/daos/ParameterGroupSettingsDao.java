package com.kio.ElevatorControl.daos;

import android.content.Context;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.models.ParameterGroupSettings;
import net.tsz.afinal.FinalDb;

import java.util.List;

public class ParameterGroupSettingsDao {

    private static final boolean DEBUG = true;

    public static List<ParameterGroupSettings> findAll(Context context) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        return db.findAll(ParameterGroupSettings.class);
    }

    public static ParameterGroupSettings findById(Context context, int id) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        return db.findById(id, ParameterGroupSettings.class);
    }

    /**
     * Find By Name
     *
     * @param context context
     * @param name    Name
     * @return ParameterGroupSettings
     */
    public static ParameterGroupSettings findByName(Context context, String name) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        List<ParameterGroupSettings> lists = db.findAllByWhere(ParameterGroupSettings.class,
                " groupText = '" + name + "'");
        return lists.size() > 0 ? lists.get(0) : null;
    }

}
