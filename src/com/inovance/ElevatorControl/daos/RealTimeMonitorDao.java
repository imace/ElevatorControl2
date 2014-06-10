package com.inovance.ElevatorControl.daos;

import android.content.Context;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.models.RealTimeMonitor;
import com.inovance.ElevatorControl.models.UserFactory;
import net.tsz.afinal.FinalDb;

import java.util.ArrayList;
import java.util.List;

public class RealTimeMonitorDao {

    private static final boolean DEBUG = false;

    private static final String TAG = RealTimeMonitorDao.class.getSimpleName();

    public static List<RealTimeMonitor> findAll(Context context) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        String condition = " deviceID = '" + UserFactory.getInstance().getDeviceType() + "'";
        return db.findAllByWhere(RealTimeMonitor.class, condition);
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
        String condition = " type = '" + type + "'" + " and "
                + " deviceID = '" + UserFactory.getInstance().getDeviceType() + "'";
        return db.findAllByWhere(RealTimeMonitor.class, condition);
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
            condition = "(" + condition + ")" + " and "
                    + " deviceID = '" + UserFactory.getInstance().getDeviceType() + "'";
            return db.findAllByWhere(RealTimeMonitor.class, condition);
        }
        return new ArrayList<RealTimeMonitor>();
    }

    public static void deleteAllByDeviceID(Context context, int deviceID) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.deleteByWhere(RealTimeMonitor.class, " deviceID = '" + deviceID + "'");
    }

}
