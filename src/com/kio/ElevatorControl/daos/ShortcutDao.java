package com.kio.ElevatorControl.daos;

import android.content.Context;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.models.Shortcut;
import net.tsz.afinal.FinalDb;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-31.
 * Time: 9:52.
 */
public class ShortcutDao {

    private static final boolean DEBUG = true;

    public static List<Shortcut> findAll(Context context) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        return db.findAll(Shortcut.class);
    }

}
