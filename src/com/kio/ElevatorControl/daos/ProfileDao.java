package com.kio.ElevatorControl.daos;

import android.content.Context;
import com.kio.ElevatorControl.models.Profile;
import net.tsz.afinal.FinalDb;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-17.
 * Time: 17:33.
 */
public class ProfileDao {

    private static final boolean DEBUG = true;

    public static List<Profile> findAll(Context context) {
        FinalDb db = FinalDb.create(context,
                context.getString(context.getApplicationInfo().labelRes) + ".db",
                DEBUG);
        return db.findAll(Profile.class);
    }

}
