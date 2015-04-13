package com.inovance.elevatorcontrol.daos;

import android.content.Context;

import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.models.Profile;

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

    public static boolean checkExistence(Context context, String fileName) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        String condition = "fileName = '" + fileName + "'";
        List<Profile> result = db.findAllByWhere(Profile.class, condition);
        return result != null && result.size() > 0;
    }

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
