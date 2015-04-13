package com.inovance.elevatorcontrol;

import android.app.Application;
import android.content.SharedPreferences;

import com.inovance.bluetoothtool.BluetoothTool;
import com.inovance.elevatorcontrol.cache.LruCacheTool;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.config.ParameterUpdateTool;
import com.inovance.elevatorcontrol.daos.ShortcutDao;
import com.inovance.elevatorcontrol.models.Shortcut;
import com.inovance.elevatorcontrol.utils.LogUtils;
import com.inovance.elevatorcontrol.utils.TextLocalize;
import com.inovance.elevatorcontrol.utils.UpdateApplication;
import com.inovance.elevatorcontrol.utils.UserSession;

import org.acra.annotation.ReportsCrashes;

/**
 * Created by keith on 14-3-8.
 * User keith
 * Date 14-3-8
 * Time 下午9:55
 */

@ReportsCrashes(
        formKey = "",
        formUri = ApplicationConfig.ReportsCrashesAPI
)

// TODO Some Handler class should be static to avoid Handler leak.
// TODO Some Class should reduce or refactoring.

public class ElevatorControlApplication extends Application {

    private static final String WRITE_DEFAULT_DATA = "hasWriteDefaultData";

    @Override
    public void onCreate() {
        super.onCreate();
        UserSession.getInstance().init(getApplicationContext());
        if (!UserSession.getInstance().isSessionAvailable()) {
            UserSession.getInstance().destroySession();
        }
        BluetoothTool.getInstance().init(getApplicationContext());
        UpdateApplication.getInstance().init(getApplicationContext());
        LruCacheTool.getInstance().initCache(getApplicationContext());
        LogUtils.getInstance().init(getApplicationContext());
        ParameterUpdateTool.getInstance().init(getApplicationContext());
        TextLocalize.getInstance().init(getApplicationContext());
        writeDefaultShortcutData();
        //ACRA.init(this);
    }

    /**
     * Write default shortcut data
     */
    private void writeDefaultShortcutData() {
        SharedPreferences settings = getSharedPreferences(ApplicationConfig.PREFERENCE_FILE_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        boolean hasWriteDefaultData = settings.getBoolean(WRITE_DEFAULT_DATA, false);
        if (!hasWriteDefaultData) {
            String[] shortcutName = getResources().getStringArray(R.array.shortcut_default);

            Shortcut shortcut;

            shortcut = new Shortcut();
            shortcut.setName(shortcutName[0]);
            shortcut.setCommand("0:0:0");
            ShortcutDao.saveItem(getApplicationContext(), shortcut);

            shortcut = new Shortcut();
            shortcut.setName(shortcutName[1]);
            shortcut.setCommand("0:2:0");
            ShortcutDao.saveItem(getApplicationContext(), shortcut);

            shortcut = new Shortcut();
            shortcut.setName(shortcutName[2]);
            shortcut.setCommand("1:1:0");
            ShortcutDao.saveItem(getApplicationContext(), shortcut);

            shortcut = new Shortcut();
            shortcut.setName(shortcutName[3]);
            shortcut.setCommand("3:0:0");
            ShortcutDao.saveItem(getApplicationContext(), shortcut);

            editor.putBoolean(WRITE_DEFAULT_DATA, true);
            editor.apply();
        }
    }
}
