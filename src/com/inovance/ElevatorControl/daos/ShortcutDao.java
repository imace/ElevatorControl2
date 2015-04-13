package com.inovance.elevatorcontrol.daos;

import android.content.Context;

import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.models.Shortcut;

import net.tsz.afinal.FinalDb;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-31.
 * Time: 9:52.
 */
public class ShortcutDao {

    private static final boolean DEBUG = false;

    public static List<Shortcut> findAll(Context context) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        return db.findAll(Shortcut.class);
    }

    public static void saveItem(Context context, Shortcut shortcut) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.saveBindId(shortcut);
    }

    public static void deleteItem(Context context, Shortcut shortcut) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.delete(shortcut);
    }

    public static void updateItem(Context context, Shortcut shortcut) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.update(shortcut);
    }
}
