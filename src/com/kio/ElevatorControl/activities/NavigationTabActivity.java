package com.kio.ElevatorControl.activities;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.TabActivity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import butterknife.OnClick;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HJudgeListener;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.daos.MenuValuesDao;
import com.kio.ElevatorControl.handlers.CoreHandler;
import com.kio.ElevatorControl.handlers.SearchBluetoothHandler;
import com.kio.ElevatorControl.views.customspinner.HCustomSpinner;
import com.kio.ElevatorControl.views.customspinner.NoDefaultSpinner;
import com.kio.ElevatorControl.views.customspinner.ViewGroupUtils;
import com.kio.ElevatorControl.views.dialogs.CustomDialog;
import com.manuelpeinado.refreshactionitem.ProgressIndicatorType;
import com.manuelpeinado.refreshactionitem.RefreshActionItem;

/**
 * TabActivity 导航
 */

@SuppressWarnings("deprecation")
public class NavigationTabActivity extends TabActivity implements RefreshActionItem.RefreshActionListener {

    private static final String TAG = NavigationTabActivity.class.getSimpleName();

    private HCustomSpinner spinner = null;

    public RefreshActionItem mRefreshActionItem;

    private NoDefaultSpinner actionbarSpinnerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_navigation_tab);
        Views.inject(this);
        initTabs();
        replaceTitleViewWithSpinnerView();
        //initBluetooth();
        //initTitle();
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
     * @param key   key
     * @param value value
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

    /**
     * 退出 tab activity的onKeyDown有bug 改用dispatchKeyEvent
     *
     * @param event key event
     * @return boolean
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
        getMenuInflater().inflate(R.menu.actionbar_menu, menu);
        MenuItem item = menu.findItem(R.id.refresh_button);
        assert item != null;
        mRefreshActionItem = (RefreshActionItem) item.getActionView();
        assert mRefreshActionItem != null;
        mRefreshActionItem.setMenuItem(item);
        mRefreshActionItem.setProgressIndicatorType(ProgressIndicatorType.INDETERMINATE);
        mRefreshActionItem.setRefreshActionListener(NavigationTabActivity.this);
        return true;
    }

    private void initBluetooth() {
        // 必须用NavigationTabActivity初始化用NavigationTabActivity
        if (!HBluetooth.getInstance(this).isPrepared()) {
            HBluetooth.getInstance(this)
                    .setPrepared(false)
                    .setDiscoveryMode(true)
                    .setHandler(new CoreHandler(this)).Start();
        }
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

    /**
     * 替换 TitleView 为 SpinnerView
     */
    public void replaceTitleViewWithSpinnerView() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        View titleView = findViewById(titleId);
        actionbarSpinnerView = (NoDefaultSpinner) getLayoutInflater().inflate(R.layout.custom_spinner_layout, null);
        ViewGroupUtils.replaceView(titleView, actionbarSpinnerView);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(),
                android.R.layout.simple_spinner_item, new String[]{});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionbarSpinnerView.setAdapter(adapter);
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
        //updateSpinnerDropdownItem(new String[]{});
    }

    /**
     * 更新蓝牙设备 Spinner 下拉列表
     *
     * @param items 蓝牙设备列表
     */
    public void updateSpinnerDropdownItem(String[] items) {
        final String[] devices = items;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(),
                android.R.layout.simple_spinner_item, devices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionbarSpinnerView.setAdapter(adapter);
        actionbarSpinnerView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                final String devName = devices[i];
                HBluetooth bluetoothSocket = HBluetooth.getInstance(NavigationTabActivity.this);
                bluetoothSocket.setPrepared(false)
                        .setDiscoveryMode(false)
                        .setJudgement(new HJudgeListener() {
                            @Override
                            public boolean judge(BluetoothDevice dev) {
                                String deviceLogName = dev.getName() + "(" + dev.getAddress() + ")";
                                return deviceLogName.trim().equalsIgnoreCase(devName);
                            }
                        }).Start();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public void onRefreshButtonClick(RefreshActionItem sender) {
        if (!HBluetooth.getInstance(this).isPrepared()) {
            HBluetooth.getInstance(this)
                    .setPrepared(false)
                    .setDiscoveryMode(true)
                    .setHandler(new SearchBluetoothHandler(this)).Start();
        }
    }

    /**
     * 开启HomeActivity Sync Task
     */
    public void startHomeActivityStatusSyncTask() {
        if (getTabHost().getCurrentTab() == 2) {
            if (getCurrentActivity() instanceof HomeActivity) {
                HomeActivity homeActivity = (HomeActivity) getCurrentActivity();
                homeActivity.loopSyncElevatorStatusTask();
            }
        }
    }

}
