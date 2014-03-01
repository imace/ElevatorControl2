package com.kio.ElevatorControl.activities;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import butterknife.OnClick;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.daos.RestoreFactoryDao;
import com.kio.ElevatorControl.daos.ValuesDao;
import com.kio.ElevatorControl.handlers.CoreHandler;
import com.kio.ElevatorControl.views.customsinnper.HCustomSinnper;
import com.kio.ElevatorControl.views.customsinnper.HDropListener;
import com.kio.ElevatorControl.views.dialogs.CustomDialoger;

@SuppressWarnings("deprecation")
public class CoreActivity extends TabActivity {

    private HCustomSinnper spinner = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_core);
        Views.inject(this);
        initTabs();
        initTitle();
        initbluetooth();
        initDB();
    }

    /**
     * 标签初始化
     */
    private void initTabs() {
        String[] CONTENTS = ValuesDao.getRootTabsTexts(this);
        Integer[] ICONS = ValuesDao.getRootTabsIcons(this);
        Class<?>[] CLAZZ = ValuesDao.getRootTabsClazz(this);
        if (null != CONTENTS && null != ICONS && CONTENTS.length == ICONS.length) {
            for (int i = 0; i < CONTENTS.length; i++) {
                // 正常添加
                addTab(CONTENTS[i], ICONS[i], CLAZZ[i]);
                if (i == 1) {
                    // 虽然调用了addTab,实际只是占用一个位置,并未真正添加这个标签
                    addTab("index", R.drawable.group_save, IndexActivity.class);
                }
            }
            btnMiddle(findViewById(R.id.resetbtn));
        }
    }


    /**
     * 新建并添加一个Tab到tabHost
     *
     * @param key
     * @param value
     * @param c     Intent新建实例使用
     */
    private void addTab(String key, int value, Class<?> c) {
        TabHost tabHost = this.getTabHost();
        if (null != tabHost) {
            View tabIndicator = LayoutInflater.from(this).inflate(R.layout.tab_indicator, getTabWidget(), false);
            TabHost.TabSpec spec = tabHost.newTabSpec("tab" + key).setIndicator(tabIndicator).setContent(new Intent(this, c));
            ((TextView) tabIndicator.findViewById(R.id.title)).setText(key);
            ((ImageView) tabIndicator.findViewById(R.id.icon)).setImageResource(value);
            tabHost.addTab(spec);
        }
    }


    /**
     * 为中间特殊按钮设置click事件监听 跳转到indexactivity
     */
    @OnClick(R.id.resetbtn)
    public void btnMiddle(View v) {
        try {
            // 模拟一次点击事件
            this.getTabHost().getTabWidget().getChildTabViewAt(2).performClick();
        } catch (Exception e) {//
            Log.e("CoreActivity.btnMain", e.getMessage());
        }
    }


    private void initbluetooth() {
        // 必须用CoreActivity初始化用CoreActivity
        if (!HBluetooth.getInstance(this).isPrepared()) {
            HBluetooth.getInstance(this).setPrepared(false).setDiscoveryMode(true).setHandler(new CoreHandler(this)).HStart();
        }
    }


    /**
     * 如果如果第一次运行本程序 db在此处初始化
     */
    private void initDB() {
        if (RestoreFactoryDao.dbEmpty(this)) {
            RestoreFactoryDao.dbInit(this);
        }
    }


    /**
     * 退出 tabactivity的onKeyDown有bug 改用dispatchKeyEvent
     *
     * @param keyCode
     * @param event
     * @return
     */
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            CustomDialoger.exitDialog(this).show();
            return false;
        }
        return super.dispatchKeyEvent(event);
    }

    ;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                startActivity(new Intent(this, CoreActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @SuppressLint("NewApi")
    private void initTitle() {
        ActionBar actionBar = this.getActionBar();
        actionBar.setCustomView(R.layout.dropdowntitle);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);

        spinner = ((HCustomSinnper) actionBar.getCustomView().findViewById(R.id.custom_sinnper));
        spinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, new String[]{}));
        spinner.setOnpop(new HDropListener() {

            private Thread th = null;


            @Override
            public void onPopup() {
            }


            @Override
            public void onRefresh() {
                spinner.setAdapter(new ArrayAdapter<String>(CoreActivity.this, android.R.layout.select_dialog_item, new String[]{}));
                if (th == null || !th.isAlive()) {
                    HBluetooth bth = HBluetooth.getInstance(CoreActivity.this).setPrepared(false).setDiscoveryMode(true).setHandler(new CoreHandler(CoreActivity.this));
                    bth.HStart();
                }
            }
        });
        actionBar.show();
    }

}
