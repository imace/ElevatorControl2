package com.kio.ElevatorControl.daos;

import android.content.Context;
import com.kio.ElevatorControl.models.RealtimeMonitor;
import net.tsz.afinal.FinalDb;

import java.util.List;

public class RealtimeMonitorDao {
    private static final boolean DBDEBUG = true;

    public static List<RealtimeMonitor> findAll(Context ctx) {
        // (android:label).db
        FinalDb db = FinalDb.create(ctx,
                ctx.getString(ctx.getApplicationInfo().labelRes) + ".db",
                DBDEBUG);
        List<RealtimeMonitor> list = db.findAll(RealtimeMonitor.class);
//		for(RealtimeMonitor r:list){//lazyload
//		}
        return list;
    }
}
