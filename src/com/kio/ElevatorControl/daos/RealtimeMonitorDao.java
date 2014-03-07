package com.kio.ElevatorControl.daos;

import android.content.Context;
import com.kio.ElevatorControl.models.RealTimeMonitor;
import net.tsz.afinal.FinalDb;

import java.util.List;

public class RealTimeMonitorDao {
    private static final boolean DEBUG = true;

    public static List<RealTimeMonitor> findAll(Context ctx) {
        // (android:label).db
        FinalDb db = FinalDb.create(ctx,
                ctx.getString(ctx.getApplicationInfo().labelRes) + ".db",
                DEBUG);
        List<RealTimeMonitor> list = db.findAll(RealTimeMonitor.class);
//		for(RealTimeMonitor r:list){//lazyload
//		}
        return list;
    }
}
