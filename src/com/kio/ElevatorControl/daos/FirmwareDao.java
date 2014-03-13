package com.kio.ElevatorControl.daos;

import android.content.Context;
import com.kio.ElevatorControl.models.Firmware;
import net.tsz.afinal.FinalDb;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-13.
 * Time: 13:13.
 */
public class FirmwareDao {

    private static final boolean DEBUG = true;

    public static List<Firmware> findAll(Context context) {
        FinalDb db = FinalDb.create(context,
                context.getString(context.getApplicationInfo().labelRes) + ".db",
                DEBUG);
        return db.findAll(Firmware.class);
    }

}
