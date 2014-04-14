package com.kio.ElevatorControl.activities;

import android.app.ActionBar;
import android.app.TabActivity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HJudgeListener;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.handlers.SearchBluetoothHandler;
import com.kio.ElevatorControl.views.customspinner.NoDefaultSpinner;
import com.kio.ElevatorControl.views.customspinner.ViewGroupUtils;
import com.kio.ElevatorControl.views.dialogs.CustomDialog;
import com.manuelpeinado.refreshactionitem.ProgressIndicatorType;
import com.manuelpeinado.refreshactionitem.RefreshActionItem;
import org.holoeverywhere.widget.TextView;

/**
 * TabActivity 导航
 */

@SuppressWarnings("deprecation")
public class NavigationTabActivity extends TabActivity implements RefreshActionItem.RefreshActionListener {

    private static final String TAG = NavigationTabActivity.class.getSimpleName();

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
        startHomeActivityStatusSyncTask();
    }

    /**
     * 标签初始化
     */
    private void initTabs() {
        TabHost tabHost = this.getTabHost();
        tabHost.setup();
        if (tabHost != null) {
            LinearLayout tabIndicator = (LinearLayout) LayoutInflater.from(this)
                    .inflate(R.layout.navigation_tab_indicator, getTabWidget(), false);
            String[] tabsText = getResources().getStringArray(R.array.navigation_tab_text);
            int[] icons = new int[]{R.drawable.tab_trouble_analyze,
                    R.drawable.tab_configuration,
                    R.drawable.tab_home,
                    R.drawable.tab_firmware_manage,
                    R.drawable.tab_help_system};
            Class<?>[] classes = new Class[]{TroubleAnalyzeActivity.class,
                    ConfigurationActivity.class,
                    HomeActivity.class,
                    FirmwareManageActivity.class,
                    HelpSystemActivity.class};
            int index = 0;
            for (String title : tabsText) {
                tabIndicator = (LinearLayout) LayoutInflater.from(this)
                        .inflate(R.layout.navigation_tab_indicator, getTabWidget(), false);
                ((TextView) tabIndicator.findViewById(R.id.title)).setText(title);
                ((ImageView) tabIndicator.findViewById(R.id.icon)).setImageResource(icons[index]);
                TabHost.TabSpec tabSpec = tabHost.newTabSpec("tab" + title)
                        .setIndicator(tabIndicator)
                        .setContent(new Intent(this, classes[index]));
                tabHost.addTab(tabSpec);
                index++;
            }
        }
        tabHost.setCurrentTab(2);
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
