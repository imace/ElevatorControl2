package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.models.RootTabs;

/**
 * 固件管理
 */
public class FirmwareManageActivity extends FragmentActivity {

    protected String[] CONTENTS = null;
    protected Integer[] ICONS = null;

    /**
     * 注入页面元素
     */
    //初始化一级标签
    private void initTabConstant() {
        RootTabs uic = RootTabs.getTabInstance(this);
        CONTENTS = uic.getTexts();
        ICONS = uic.getIcons();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firmware_manage);
        //注入以后才能使用@InjectView定义的对象
        //Views.inject(this);
    }
}
