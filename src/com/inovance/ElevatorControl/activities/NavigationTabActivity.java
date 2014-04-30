package com.inovance.ElevatorControl.activities;

import android.app.ActionBar;
import android.app.TabActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.widget.*;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HJudgeListener;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.handlers.SearchBluetoothHandler;
import com.inovance.ElevatorControl.views.customspinner.NoDefaultSpinner;
import com.inovance.ElevatorControl.views.customspinner.ViewGroupUtils;
import com.inovance.ElevatorControl.views.dialogs.CustomDialog;
import com.manuelpeinado.refreshactionitem.ProgressIndicatorType;
import com.manuelpeinado.refreshactionitem.RefreshActionItem;
import org.holoeverywhere.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * TabActivity 导航
 */

@SuppressWarnings("deprecation")
public class NavigationTabActivity extends TabActivity implements RefreshActionItem.RefreshActionListener {

    private static final String TAG = NavigationTabActivity.class.getSimpleName();

    private static final String SpecialDeviceName = "M-Tools";

    public RefreshActionItem mRefreshActionItem;

    private NoDefaultSpinner actionbarSpinner;

    private SearchBluetoothHandler searchBluetoothHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_navigation_tab);
        Views.inject(this);
        initTabs();
        searchBluetoothHandler = new SearchBluetoothHandler(this);
        replaceTitleViewWithSpinnerView();
        startHomeActivityStatusSyncTask();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(intent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
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
     * Change Current Tab Index
     *
     * @param tabIndex   New Tab Index
     * @param pagerIndex Pager Index
     */
    public void switchTab(final int tabIndex, final int pagerIndex) {
        this.getTabHost().setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if (tabIndex == 0) {
                    if (getCurrentActivity() instanceof TroubleAnalyzeActivity) {
                        final TroubleAnalyzeActivity troubleAnalyzeActivity = (TroubleAnalyzeActivity) getCurrentActivity();
                        troubleAnalyzeActivity.pageIndex = pagerIndex;
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                troubleAnalyzeActivity.changePagerIndex(pagerIndex);
                            }
                        };
                        Handler handler = new Handler();
                        handler.postDelayed(runnable, 300);
                    }
                }
                if (tabIndex == 1) {
                    if (getCurrentActivity() instanceof ConfigurationActivity) {
                        final ConfigurationActivity configurationActivity = (ConfigurationActivity) getCurrentActivity();
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                configurationActivity.changePagerIndex(pagerIndex);
                            }
                        };
                        Handler handler = new Handler();
                        handler.postDelayed(runnable, 300);
                    }
                }
                if (tabIndex == 3) {
                    if (getCurrentActivity() instanceof FirmwareManageActivity) {
                        final FirmwareManageActivity firmwareManageActivity = (FirmwareManageActivity) getCurrentActivity();
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                firmwareManageActivity.changePagerIndex(pagerIndex);
                            }
                        };
                        Handler handler = new Handler();
                        handler.postDelayed(runnable, 300);
                    }
                }
            }
        });
        this.getTabHost().setCurrentTab(tabIndex);
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
        View newTitleView = getLayoutInflater().inflate(R.layout.custom_spinner_layout, null);
        actionbarSpinner = (NoDefaultSpinner) newTitleView.findViewById(R.id.custom_spinner);
        actionbarSpinner.setBackgroundResource(R.drawable.custom_spinner_background);
        actionbarSpinner.setSpinnerItemLayout(R.layout.custom_white_spinner_item);
        ViewGroupUtils.replaceView(titleView, newTitleView);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(),
                R.layout.custom_white_spinner_item, new String[]{});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionbarSpinner.setAdapter(adapter);
        actionbarSpinner.setVisibility(View.GONE);
    }

    /**
     * 更新蓝牙设备 Spinner 下拉列表
     *
     * @param items 蓝牙设备列表
     */
    public void updateSpinnerDropdownItem(List<BluetoothDevice> deviceArrayList) {
        List<String> specialDevicesName = new ArrayList<String>();
        List<String> normalDevicesName = new ArrayList<String>();
        List<BluetoothDevice> specialDevices = new ArrayList<BluetoothDevice>();
        List<BluetoothDevice> normalDevices = new ArrayList<BluetoothDevice>();
        for (BluetoothDevice device : deviceArrayList) {
            String name = device.getName() + "(" + device.getAddress() + ")";
            if (device.getName().contains(SpecialDeviceName)) {
                specialDevicesName.add(name);
                specialDevices.add(device);
            } else {
                normalDevicesName.add(name);
                normalDevices.add(device);
            }
        }
        List<String> concatenateName = new ArrayList<String>();
        concatenateName.addAll(specialDevicesName);
        concatenateName.addAll(normalDevicesName);
        final List<BluetoothDevice> concatenateDevices = new ArrayList<BluetoothDevice>();
        concatenateDevices.addAll(specialDevices);
        concatenateDevices.addAll(normalDevices);
        String[] devicesName = concatenateName.toArray(new String[concatenateName.size()]);
        if (devicesName.length > 0) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(),
                    R.layout.custom_white_spinner_item, devicesName);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            actionbarSpinner.setAdapter(adapter);
            actionbarSpinner.setVisibility(View.VISIBLE);
            actionbarSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    final BluetoothDevice device = concatenateDevices.get(position);
                    HBluetooth bluetoothInstance = HBluetooth.getInstance(NavigationTabActivity.this);
                    bluetoothInstance.setPrepared(false);
                    bluetoothInstance.setDiscoveryMode(false);
                    bluetoothInstance.setJudgement(new HJudgeListener() {
                        @Override
                        public boolean judge(BluetoothDevice dev) {
                            return dev.equals(device);
                        }
                    });
                    bluetoothInstance.pairDevice(device);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        } else {
            actionbarSpinner.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRefreshButtonClick(RefreshActionItem sender) {
        HBluetooth.getInstance(this)
                .setPrepared(false)
                .setDiscoveryMode(true)
                .setHandler(searchBluetoothHandler)
                .Start();
    }

    /**
     * 开启HomeActivity Sync Task
     */
    public void startHomeActivityStatusSyncTask() {
        switch (getTabHost().getCurrentTab()) {
            case 0: {
                if (getCurrentActivity() instanceof TroubleAnalyzeActivity) {
                    TroubleAnalyzeActivity troubleAnalyzeActivity = (TroubleAnalyzeActivity) getCurrentActivity();
                    troubleAnalyzeActivity.reSyncData();
                }
            }
            break;
            case 1: {
                if (getCurrentActivity() instanceof ConfigurationActivity) {
                    ConfigurationActivity configurationActivity = (ConfigurationActivity) getCurrentActivity();
                    configurationActivity.reSyncData();
                }
            }
            break;
            case 2: {
                if (getCurrentActivity() instanceof HomeActivity) {
                    HomeActivity homeActivity = (HomeActivity) getCurrentActivity();
                    homeActivity.reSyncData();
                }
            }
            break;
        }
    }

}
