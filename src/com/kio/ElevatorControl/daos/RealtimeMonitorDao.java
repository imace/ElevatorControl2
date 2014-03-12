package com.kio.ElevatorControl.daos;

import android.content.Context;
import com.kio.ElevatorControl.models.RealTimeMonitor;
import net.tsz.afinal.FinalDb;

import java.util.List;

public class RealTimeMonitorDao {

    private static final boolean DEBUG = true;

    public static List<RealTimeMonitor> findAll(Context context) {
        FinalDb db = FinalDb.create(context,
                context.getString(context.getApplicationInfo().labelRes) + ".db",
                DEBUG);
        return db.findAll(RealTimeMonitor.class);
    }

    public static List<RealTimeMonitor> findByType(Context context, String type) {
        FinalDb db = FinalDb.create(context,
                context.getString(context.getApplicationInfo().labelRes) + ".db",
                DEBUG);
        return db.findAllByWhere(RealTimeMonitor.class, " type = '" + type + "'");
    }

}
