package com.kio.ElevatorControl.daos;

import android.content.Context;
import com.kio.ElevatorControl.models.ParameterGroupSettings;
import net.tsz.afinal.FinalDb;

import java.util.List;

public class ParameterGroupSettingsDao {
    private static final boolean DEBUG = true;

    public static List<ParameterGroupSettings> findAll(Context ctx) {
        // (android:label).db
        FinalDb db = FinalDb.create(ctx,
                ctx.getString(ctx.getApplicationInfo().labelRes) + ".db",
                DEBUG);
        List<ParameterGroupSettings> list = db
                .findAll(ParameterGroupSettings.class);
        return list;
    }

    public static ParameterGroupSettings findById(Context ctx, int id) {
        // (android:label).db
        FinalDb db = FinalDb.create(ctx,
                ctx.getString(ctx.getApplicationInfo().labelRes) + ".db",
                DEBUG);
        return db.findById(id, ParameterGroupSettings.class);
    }

}
