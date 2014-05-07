package com.inovance.ElevatorControl;

import com.inovance.ElevatorControl.cache.LruCacheTool;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.daos.ShortcutDao;
import com.inovance.ElevatorControl.models.Shortcut;
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
        /*
        String JSON = AssetUtils.readDefaultFunCode(getApplicationContext(), "NICE3000+_FunCode.json");
        try {
            JSONArray groups = new JSONArray(JSON);
            // 遍历group
            int size = groups.length();
            int position = 0;
            for (int i = 0; i < size; i++) {
                JSONObject groupsJSONObject = groups.getJSONObject(i);
                JSONArray settingJson = groupsJSONObject.getJSONArray("parameterSettings");
                int length = settingJson.length();
                for (int j = 0; j < length; j++) {
                    Log.v("AAABBB", String.valueOf(position));
                    JSONObject jsonObject = settingJson.getJSONObject(j);
                    jsonObject.put("mode", ApplicationConfig.tempA[position]);
                    position++;
                }
            }
            Log.v("AAABBB", groups.toString());
            File fileName = new File(getApplicationContext().getExternalCacheDir().getPath()
                    + "/test");
            if (!fileName.exists()) {
                fileName.createNewFile();
            }
            FileOutputStream outputStream = new FileOutputStream(fileName);
            outputStream.write(groups.toString().getBytes());
            outputStream.close();
        } catch (Exception e) {
            //
        }
        */
    }

    private void writeDefaultShortcutData() {
        SharedPreferences settings = getSharedPreferences(ApplicationConfig.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        boolean hasWriteDefaultData = settings.getBoolean("hasWriteDefaultData", false);
        if (!hasWriteDefaultData) {
            for (int i = 0; i < 4; i++) {
                Shortcut shortcut = new Shortcut();
                shortcut.setName("快捷菜单");
                shortcut.setCommand("0:0:0");
                ShortcutDao.saveItem(getApplicationContext(), shortcut);
            }
            editor.putBoolean("hasWriteDefaultData", true);
            editor.commit();
        }
    }
}
