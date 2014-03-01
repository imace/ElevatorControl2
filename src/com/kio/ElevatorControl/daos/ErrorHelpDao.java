package com.kio.ElevatorControl.daos;

import android.annotation.SuppressLint;
import android.content.Context;
import com.kio.ElevatorControl.models.ErrorHelp;
import net.tsz.afinal.FinalDb;

import java.util.List;

public class ErrorHelpDao {

    private static final boolean DBDEBUG = true;


    @SuppressLint("DefaultLocale")
    public static ErrorHelp findByDisplay(Context ctx, String display) {
        // (android:label).db
        FinalDb db = FinalDb.create(ctx, ctx.getString(ctx.getApplicationInfo().labelRes) + ".db", DBDEBUG);
        List<ErrorHelp> listep = db.findAllByWhere(ErrorHelp.class, " display = '" + display.toUpperCase() + "' or display='" + display.toLowerCase() + "'");
        if (listep != null && listep.size() > 0) {
            return listep.get(0);
        }
        return null;
    }
}
