package com.inovance.elevatorcontrol.daos;

import android.content.Context;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.config.ParameterUpdateTool;
import com.inovance.elevatorcontrol.models.RealTimeMonitor;
import net.tsz.afinal.FinalDb;

import java.util.ArrayList;
import java.util.List;

public class RealTimeMonitorDao {

    private static final boolean DEBUG = false;

    private static final String TAG = RealTimeMonitorDao.class.getSimpleName();

    public static List<RealTimeMonitor> findAll(Context context) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        String condition = " deviceID = '" + ParameterUpdateTool.getInstance().getDeviceSQLID() + "'";
        return db.findAllByWhere(RealTimeMonitor.class, condition);
    }

    public static RealTimeMonitor findByStateID(Context context, int stateID) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        String condition = " stateID = '" + stateID + "'";
        condition = "(" + condition + ")" + " and "
                + " deviceID = '" + ParameterUpdateTool.getInstance().getDeviceSQLID() + "'";
        List<RealTimeMonitor> monitorList = db.findAllByWhere(RealTimeMonitor.class, condition);
        if (monitorList != null && monitorList.size() == 1) {
            return monitorList.get(0);
        }
        return null;
    }

    public static List<RealTimeMonitor> findAllByStateID(Context context, int stateID) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        String condition = " stateID = '" + stateID + "'";
        condition = "(" + condition + ")" + " and "
                + " deviceID = '" + ParameterUpdateTool.getInstance().getDeviceSQLID() + "'";
        List<RealTimeMonitor> monitorList = db.findAllByWhere(RealTimeMonitor.class, condition);
        return monitorList != null ? monitorList : new ArrayList<RealTimeMonitor>();
    }

    /**
     * Find by state ID .
     *
     * @param context  Context
     * @param stateIDs State ID array
     * @return List
     */
    public static List<RealTimeMonitor> findAllByStateIDs(Context context, int[] stateIDs) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        int size = stateIDs.length;
        if (size > 0) {
            String condition = "";
            if (size == 1) {
                condition = " stateID = '" + stateIDs[0] + "'";
            } else {
                for (int i = 0; i < size; i++) {
                    if (i == 0) {
                        condition += " stateID = '" + stateIDs[i];
                    } else if (i == size - 1) {
                        condition += "' or stateID = '" + stateIDs[i] + "'";
                    } else {
                        condition += "' or stateID = '" + stateIDs[i];
                    }
                }
            }
            condition = "(" + condition + ")" + " and "
                    + " deviceID = '" + ParameterUpdateTool.getInstance().getDeviceSQLID() + "'";
            return db.findAllByWhere(RealTimeMonitor.class, condition);
        }
        return new ArrayList<RealTimeMonitor>();
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
                    + " deviceID = '" + ParameterUpdateTool.getInstance().getDeviceSQLID() + "'";
            return db.findAllByWhere(RealTimeMonitor.class, condition);
        }
        return new ArrayList<RealTimeMonitor>();
    }

    public static void deleteAllByDeviceID(Context context, int deviceID) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.deleteByWhere(RealTimeMonitor.class, " deviceID = '" + deviceID + "'");
    }

}
