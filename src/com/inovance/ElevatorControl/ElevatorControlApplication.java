package com.inovance.ElevatorControl;

import android.app.Application;
import android.content.SharedPreferences;
import com.bluetoothtool.BluetoothTool;
import com.inovance.ElevatorControl.cache.LruCacheTool;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.daos.ShortcutDao;
import com.inovance.ElevatorControl.models.ConfigFactory;
import com.inovance.ElevatorControl.models.Shortcut;
import com.inovance.ElevatorControl.utils.LogUtils;
import com.inovance.ElevatorControl.utils.TextLocalize;

/**
 * Created by keith on 14-3-8.
 * User keith
 * Date 14-3-8
 * Time 下午9:55
 */

/*
@ReportsCrashes(
        formKey = "crashReport",
        formUri = "http://www.report.com/reportpath"
)
*/
public class ElevatorControlApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        BluetoothTool.getInstance().init(getApplicationContext());
        // 初始化缓存组件
        LruCacheTool.getInstance().initCache(getApplicationContext());
        LogUtils.getInstance().init(getApplicationContext());
        writeDefaultShortcutData();
        ConfigFactory.getInstance().init(getApplicationContext());
        TextLocalize.getInstance().init(getApplicationContext());
        //ACRA.init(this);
    }

    private void writeDefaultShortcutData() {
        SharedPreferences settings = getSharedPreferences(ApplicationConfig.PREFS_NAME, 0);
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
