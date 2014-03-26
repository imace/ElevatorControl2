package com.kio.ElevatorControl.daos;

import android.content.Context;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.models.RealTimeMonitor;
import net.tsz.afinal.FinalDb;

import java.util.ArrayList;
import java.util.List;

public class RealTimeMonitorDao {

    private static final boolean DEBUG = true;

    private static final String TAG = RealTimeMonitorDao.class.getSimpleName();

    public static List<RealTimeMonitor> findAll(Context context) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        return db.findAll(RealTimeMonitor.class);
    }

    /**
     * Find by type
     *
     * @param context context
     * @param type    type
     * @return List
     */
    public static List<RealTimeMonitor> findByType(Context context, String type) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        return db.findAllByWhere(RealTimeMonitor.class, " type = '" + type + "'");
    }

    /**
     * Find by names array
     *
     * @param context context
     * @param names   names
     * @return List
     */
    public static List<RealTimeMonitor> findByNames(Context context, String[] names) {
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
            return db.findAllByWhere(RealTimeMonitor.class, condition);
        }
        return new ArrayList<RealTimeMonitor>();
    }

}
