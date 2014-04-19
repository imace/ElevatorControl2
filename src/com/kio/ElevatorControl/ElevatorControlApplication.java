package com.kio.ElevatorControl;

import com.kio.ElevatorControl.cache.LruCacheTool;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.daos.ShortcutDao;
import com.kio.ElevatorControl.models.Shortcut;
import org.holoeverywhere.HoloEverywhere;
import org.holoeverywhere.app.Application;
import org.holoeverywhere.preference.SharedPreferences;

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

    static {
        HoloEverywhere.DEBUG = true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化缓存组件
        LruCacheTool.getInstance().initCache(getApplicationContext());
        writeDefaultShortcutData();
        //ACRA.init(this);
    }

    /**
     * 检查错误故障信息是否更新
     */
    private void checkErrorHelpListUpdate() {

    }

    private void writeDefaultShortcutData() {
        SharedPreferences settings = getSharedPreferences(ApplicationConfig.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        boolean hasWriteDefaultData = settings.getBoolean("hasWriteDefaultData", false);
        if (!hasWriteDefaultData) {
            for (int i = 0; i < 4; i++) {
                Shortcut shortcut = new Shortcut();
                shortcut.setName("快捷菜单");
                ShortcutDao.saveItem(getApplicationContext(), shortcut);
            }
            editor.putBoolean("hasWriteDefaultData", true);
            editor.commit();
        }
    }
}
