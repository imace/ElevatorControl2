package com.kio.ElevatorControl.activities;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import butterknife.OnClick;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.daos.MenuValuesDao;
import com.kio.ElevatorControl.daos.RestoreFactoryDao;
import com.kio.ElevatorControl.handlers.CoreHandler;
import com.kio.ElevatorControl.views.customspinner.HCustomSpinner;
import com.kio.ElevatorControl.views.customspinner.HDropListener;
import com.kio.ElevatorControl.views.dialogs.CustomDialog;

/**
 * TabActivity 导航
 */

@SuppressWarnings("deprecation")
public class NavigationTabActivity extends TabActivity {

    private HCustomSpinner spinner = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_navigation_tab);
        Views.inject(this);
        initTabs();
        initTitle();
        initBluetooth();
        //initDB();
    }

    /**
     * 标签初始化
     */
    private void initTabs() {
        String[] CONTENTS = MenuValuesDao.getNavigationTabsTexts(this);
        Integer[] ICONS = MenuValuesDao.getNavigationTabsIcons(this);
        Class<?>[] CLAZZ = MenuValuesDao.getNavigationTabsClazz(this);
        if (null != CONTENTS && null != ICONS && CONTENTS.length == ICONS.length) {
            for (int i = 0; i < CONTENTS.length; i++) {
                // 正常添加
                addTab(CONTENTS[i], ICONS[i], CLAZZ[i]);
                if (i == 1) {
                    // 虽然调用了addTab,实际只是占用一个位置,并未真正添加这个标签
                    addTab(getResources().getString(R.string.title_activity_index), R.drawable.tab_home, HomeActivity.class);
                }
            }
            btnMiddle(findViewById(R.id.reset_btn));
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
            View tabIndicator = LayoutInflater.from(this).inflate(R.layout.navigation_tab_indicator, getTabWidget(), false);
            TabHost.TabSpec spec = tabHost.newTabSpec("tab" + key).setIndicator(tabIndicator).setContent(new Intent(this, c));
            ((TextView) tabIndicator.findViewById(R.id.title)).setText(key);
            ((ImageView) tabIndicator.findViewById(R.id.icon)).setImageResource(value);
            tabHost.addTab(spec);
        }
    }

    /**
     * 为中间特殊按钮设置click事件监听 跳转到HomeActivity
     */
    @OnClick(R.id.reset_btn)
    public void btnMiddle(View v) {
        try {
            // 模拟一次点击事件
            this.getTabHost().getTabWidget().getChildTabViewAt(2).performClick();
        } catch (Exception e) {//
            Log.e("NavigationTabActivity.btnMain", e.getMessage());
        }
    }

    private void initBluetooth() {
        // 必须用CoreActivity初始化用CoreActivity
        if (!HBluetooth.getInstance(this).isPrepared()) {
            HBluetooth.getInstance(this).setPrepared(false).setDiscoveryMode(true).setHandler(new CoreHandler(this)).Start();
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
            CustomDialog.exitDialog(this).show();
            return false;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                startActivity(new Intent(this, NavigationTabActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressLint("NewApi")
    private void initTitle() {
        /*
        ActionBar actionBar = this.getActionBar();
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT,
                ActionBar.LayoutParams.FILL_PARENT);
        LayoutInflater layoutInflater = getLayoutInflater();
        RelativeLayout dropdownSpinnerView = (RelativeLayout) layoutInflater.inflate(R.layout.dropdown_title, null);
        assert actionBar != null;
        actionBar.setCustomView(dropdownSpinnerView, layoutParams);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        spinner = ((HCustomSpinner) actionBar.getCustomView().findViewById(R.id.custom_spinner));
        spinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, new String[]{}));
        spinner.setOnPopup(new HDropListener() {

            private Thread thread = null;

            @Override
            public void onPopup() {

            }

            @Override
            public void onRefresh() {
                spinner.setAdapter(new ArrayAdapter<String>(NavigationTabActivity.this, android.R.layout.select_dialog_item, new String[]{}));
                if (thread == null || !thread.isAlive()) {
                    HBluetooth bth = HBluetooth.getInstance(NavigationTabActivity.this).setPrepared(false).setDiscoveryMode(true).setHandler(new CoreHandler(NavigationTabActivity.this));
                    bth.Start();
                }
            }
        });
        actionBar.show();
        */
        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getActionBar().setDisplayShowTitleEnabled(false);
        String[] actions = new String[]{
                "Test"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(),
                R.layout.simple_spinner_dropdown_item, actions);
        ActionBar.OnNavigationListener navigationListener = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                return false;
            }
        };
        getActionBar().setListNavigationCallbacks(adapter, navigationListener);
        adapter.setDropDownViewResource(R.layout.simple_spinner_item);
    }
}
