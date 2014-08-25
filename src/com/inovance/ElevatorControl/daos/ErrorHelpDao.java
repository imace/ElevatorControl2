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
        if (display.equalsIgnoreCase("E00")) {
            return null;
        }
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
        } else {
            // 未知故障
            ErrorHelp errorHelp = new ErrorHelp();
            errorHelp.setName("未知故障");
            errorHelp.setDisplay(display);
            errorHelp.setLevel("未知");
            errorHelp.setReason("未知");
            errorHelp.setSolution("未知");
            return errorHelp;
        }
    }

    public static void deleteAllByDeviceID(Context context, int deviceID) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.deleteByWhere(ErrorHelp.class, " deviceID = '" + deviceID + "'");
    }

}
