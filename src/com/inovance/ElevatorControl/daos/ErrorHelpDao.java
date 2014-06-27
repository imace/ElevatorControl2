package com.inovance.ElevatorControl.daos;

import android.content.Context;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.config.ConfigFactory;
import com.inovance.ElevatorControl.models.ErrorHelp;
import net.tsz.afinal.FinalDb;

import java.util.List;

public class ErrorHelpDao {

    private static final boolean DEBUG = false;

    public static ErrorHelp findByDisplay(Context context, String display) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        String condition = "(" + " display = '" + display.toUpperCase() + "' or display='" + display.toLowerCase() + "'" + ")";
        condition = condition + " and deviceID ='" + ConfigFactory.getInstance().getDeviceSQLID() + "'";
        List<ErrorHelp> helpList = db.findAllByWhere(ErrorHelp.class, condition);
        if (helpList != null && helpList.size() > 0) {
            ErrorHelp errorHelp = helpList.get(0);
            String[] reasonArray = errorHelp.getReason().trim().split("#");
            String[] solutionArray = errorHelp.getSolution().trim().split("#");
            String reasonString = "";
            int m = 0;
            int reasonArrayLength = reasonArray.length;
            for (String reason : reasonArray) {
                reasonString += reason.trim();
                if (m != reasonArrayLength - 1) {
                    reasonString += "\n";
                }
                m++;
            }
            String solutionString = "";
            int n = 0;
            int solutionArrayLength = solutionArray.length;
            for (String solution : solutionArray) {
                solutionString += solution.trim();
                if (n != solutionArrayLength - 1) {
                    solutionString += "\n";
                }
                n++;
            }
            errorHelp.setReason(reasonString);
            errorHelp.setSolution(solutionString);
            return helpList.get(0);
        }
        return null;
    }

    public static void deleteAllByDeviceID(Context context, int deviceID) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.deleteByWhere(ErrorHelp.class, " deviceID = '" + deviceID + "'");
    }

}
