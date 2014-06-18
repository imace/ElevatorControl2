package com.inovance.ElevatorControl.daos;

import android.content.Context;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.models.Profile;
import net.tsz.afinal.FinalDb;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-17.
 * Time: 17:33.
 */
public class ProfileDao {

    private static final boolean DEBUG = false;

    public static List<Profile> findAll(Context context) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        List<Profile> profileList = db.findAll(Profile.class);
        return profileList != null ? profileList : new ArrayList<Profile>();
    }

    public static void save(Context context, Profile profile) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.saveBindId(profile);
    }

    public static void deleteItem(Context context, Profile profile) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.delete(profile);
    }

}
