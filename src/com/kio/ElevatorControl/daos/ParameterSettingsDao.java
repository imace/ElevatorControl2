package com.kio.ElevatorControl.daos;

import android.content.Context;
import com.kio.ElevatorControl.models.ParameterSettings;
import net.tsz.afinal.FinalDb;

public class ParameterSettingsDao {
    private static final boolean DBDEBUG = true;

    public static ParameterSettings findById(Context ctx, int id) {
        // (android:label).db
        FinalDb db = FinalDb.create(ctx,
                ctx.getString(ctx.getApplicationInfo().labelRes) + ".db",
                DBDEBUG);
        return db.findById(id, ParameterSettings.class);
    }
}
