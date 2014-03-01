package com.kio.ElevatorControl.daos;

import android.content.Context;
import com.kio.ElevatorControl.models.ErrorHelp;
import com.kio.ElevatorControl.models.ErrorHelpLog;
import net.tsz.afinal.FinalDb;

import java.util.List;

public class ErrorHelpLogDao {
    private static final boolean DBDEBUG = true;

    /**
     * 失败返回-1
     *
     * @param ctx
     * @param entity
     * @return
     */
    public static int Insert(Context ctx, ErrorHelpLog entity) {
        // (android:label).db
        FinalDb db = FinalDb.create(ctx,
                ctx.getString(ctx.getApplicationInfo().labelRes) + ".db",
                DBDEBUG);
        if (db.saveBindId(entity)) {
            return entity.getId();
        }
        return -1;
    }

    public static List<ErrorHelpLog> findAll(Context ctx) {
        // (android:label).db
        FinalDb db = FinalDb.create(ctx,
                ctx.getString(ctx.getApplicationInfo().labelRes) + ".db",
                DBDEBUG);
        List<ErrorHelpLog> result = db.findAll(ErrorHelpLog.class);
        if (result != null) {
            for (ErrorHelpLog lg : result) {
                lg.setCtx(ctx);
            }
        }
        return result;
    }

    public static ErrorHelp findErrorHelp(Context ctx, int id) {
        // (android:label).db
        FinalDb db = FinalDb.create(ctx,
                ctx.getString(ctx.getApplicationInfo().labelRes) + ".db",
                DBDEBUG);
        return db.findById(id, ErrorHelp.class);
    }

}
