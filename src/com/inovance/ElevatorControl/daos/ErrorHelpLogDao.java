package com.inovance.ElevatorControl.daos;

import android.content.Context;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.models.ErrorHelp;
import com.inovance.ElevatorControl.models.ErrorHelpLog;
import net.tsz.afinal.FinalDb;

import java.util.List;

public class ErrorHelpLogDao {

    private static final boolean DEBUG = false;

    /**
     * 失败返回-1
     *
     * @param context Context
     * @param entity  ErrorHelpLog
     * @return int
     */
    public static int Insert(Context context, ErrorHelpLog entity) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        if (db.saveBindId(entity)) {
            return entity.getId();
        }
        return -1;
    }

    public static List<ErrorHelpLog> findAll(Context context) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        List<ErrorHelpLog> result = db.findAll(ErrorHelpLog.class);
        if (result != null) {
            for (ErrorHelpLog lg : result) {
                lg.setCtx(context);
            }
        }
        return result;
    }

    public static ErrorHelp findErrorHelp(Context context, int id) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        return db.findById(id, ErrorHelp.class);
    }

}
