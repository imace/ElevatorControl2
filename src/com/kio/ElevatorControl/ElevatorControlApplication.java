package com.kio.ElevatorControl;

import com.kio.ElevatorControl.cache.LruCacheTool;
import org.holoeverywhere.HoloEverywhere;
import org.holoeverywhere.app.Application;

/**
 * Created by keith on 14-3-8.
 * User keith
 * Date 14-3-8
 * Time 下午9:55
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
    }
}
