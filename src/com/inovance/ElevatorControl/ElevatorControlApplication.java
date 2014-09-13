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
import org.acra.ACRA;
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

public class ElevatorControlApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        BluetoothTool.getInstance().init(getApplicationContext());
        // 初始化缓存组件
        LruCacheTool.getInstance().initCache(getApplicationContext());
        LogUtils.getInstance().init(getApplicationContext());
        ParameterUpdateTool.getInstance().init(getApplicationContext());
        TextLocalize.getInstance().init(getApplicationContext());
        writeDefaultShortcutData();
        //ACRA.init(this);
    }

    private void writeDefaultShortcutData() {
        SharedPreferences settings = getSharedPreferences(ApplicationConfig.PREFERENCE_FILE_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        boolean hasWriteDefaultData = settings.getBoolean("hasWriteDefaultData", false);
        if (!hasWriteDefaultData) {
            Shortcut shortcut;

            shortcut = new Shortcut();
            shortcut.setName("当前故障");
            shortcut.setCommand("0:0:0");
            ShortcutDao.saveItem(getApplicationContext(), shortcut);

            shortcut = new Shortcut();
            shortcut.setName("故障查询");
            shortcut.setCommand("0:2:0");
            ShortcutDao.saveItem(getApplicationContext(), shortcut);

            shortcut = new Shortcut();
            shortcut.setName("参数设置");
            shortcut.setCommand("1:1:0");
            ShortcutDao.saveItem(getApplicationContext(), shortcut);

            shortcut = new Shortcut();
            shortcut.setName("程序申请");
            shortcut.setCommand("3:0:0");
            ShortcutDao.saveItem(getApplicationContext(), shortcut);

            editor.putBoolean("hasWriteDefaultData", true);
            editor.commit();
        }
    }
}
