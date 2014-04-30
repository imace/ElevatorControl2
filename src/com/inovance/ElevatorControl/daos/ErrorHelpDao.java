package com.inovance.ElevatorControl.daos;

import android.annotation.SuppressLint;
import android.content.Context;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.models.ErrorHelp;
import net.tsz.afinal.FinalDb;

import java.util.List;

public class ErrorHelpDao {

    private static final boolean DEBUG = true;

    @SuppressLint("DefaultLocale")
    public static ErrorHelp findByDisplay(Context context, String display) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        List<ErrorHelp> helpList = db.findAllByWhere(ErrorHelp.class, " display = '"
                + display.toUpperCase()
                + "' or display='"
                + display.toLowerCase()
                + "'");
        if (helpList != null && helpList.size() > 0) {
            return helpList.get(0);
        }
        return null;
    }
}
