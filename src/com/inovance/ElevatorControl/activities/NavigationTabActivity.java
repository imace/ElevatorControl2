package com.inovance.ElevatorControl.activities;

import android.app.Activity;
import android.app.TabActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import butterknife.InjectView;
import butterknife.Views;
import com.bluetoothtool.BluetoothHandler;
import com.bluetoothtool.BluetoothTalk;
import com.bluetoothtool.BluetoothTool;
import com.bluetoothtool.SerialUtility;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.handlers.SearchBluetoothHandler;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import com.inovance.ElevatorControl.views.customspinner.NoDefaultSpinner;
import com.inovance.ElevatorControl.views.dialogs.CustomDialog;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TabActivity 导航
 */

@SuppressWarnings("deprecation")
public class NavigationTabActivity extends TabActivity {

    private static final String TAG = NavigationTabActivity.class.getSimpleName();

    private static final String SpecialDeviceName = "M-Tools";

    private SearchBluetoothHandler searchBluetoothHandler;

    public boolean hasGetDeviceTypeAndNumber = false;

    private GetDeviceTypeAndNumberHandler getDeviceTypeAndNumberHandler;

    private BluetoothTalk[] getDeviceTypeAndNumberCommunications;

    private Runnable getDeviceTypeNumberTask;

    private static final int LOOP_TIME = 2000;

    private boolean running = false;

    @InjectView(R.id.search_device_tips)
    TextView searchDeviceTipsView;

    @InjectView(R.id.custom_spinner)
    NoDefaultSpinner deviceListSpinner;

    @InjectView(R.id.research_devices_button)
    public View researchDevicesButton;

    @InjectView(R.id.refresh_button_icon)
    ImageView refreshButtonIcon;

    @InjectView(R.id.refresh_button_progress)
    ProgressBar refreshButtonProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_tab);
        Views.inject(this);
        initTabs();
        deviceListSpinner.setBackgroundResource(R.drawable.custom_spinner_background);
        deviceListSpinner.setSpinnerItemLayout(R.layout.custom_white_spinner_item);
        searchBluetoothHandler = new SearchBluetoothHandler(this);
        getDeviceTypeAndNumberHandler = new GetDeviceTypeAndNumberHandler(this);
        getDeviceTypeNumberTask = new Runnable() {
            @Override
            public void run() {
                if (running) {
                    if (!hasGetDeviceTypeAndNumber) {
                        if (BluetoothTool.getInstance(NavigationTabActivity.this).isConnected()) {
                            NavigationTabActivity.this.getDeviceTypeAndNumber();
                            getDeviceTypeAndNumberHandler.postDelayed(getDeviceTypeNumberTask, LOOP_TIME);
                        }
                    }
                }
            }
        };
        showRefreshButtonProgress(false);
        researchDevicesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRefreshButtonProgress(true);
                searchDeviceTipsView.setVisibility(View.GONE);
                deviceListSpinner.setVisibility(View.VISIBLE);
                BluetoothTool.getInstance(NavigationTabActivity.this)
                        .setSearchHandler(searchBluetoothHandler)
                        .search();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(intent);
        } else {
            if (BluetoothTool.getInstance(NavigationTabActivity.this).isConnected()) {
                startGetDeviceTypeAndNumberTask();
            }
        }
    }

    /**
     * Start Get Device Type And Number Task
     */
    public void startGetDeviceTypeAndNumberTask() {
        if (!hasGetDeviceTypeAndNumber) {
            NavigationTabActivity.this.getDeviceTypeAndNumber();
            running = true;
            getDeviceTypeAndNumberHandler.postDelayed(getDeviceTypeNumberTask, LOOP_TIME);
        }
    }

    /**
     * Show refresh button progress
     *
     * @param shown shown
     */
    public void showRefreshButtonProgress(boolean shown) {
        if (shown) {
            refreshButtonIcon.setVisibility(View.GONE);
            refreshButtonProgress.setVisibility(View.VISIBLE);
        } else {
            refreshButtonIcon.setVisibility(View.VISIBLE);
            refreshButtonProgress.setVisibility(View.GONE);
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

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            CustomDialog.exitDialog(this).show();
            return false;
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * 更新蓝牙设备 Spinner 下拉列表
     *
     * @param items 蓝牙设备列表
     */
    public void updateSpinnerDropdownItem(List<BluetoothDevice> deviceList) {
        Set<BluetoothDevice> bluetoothDeviceSet = new HashSet<BluetoothDevice>();
        bluetoothDeviceSet.addAll(deviceList);
        deviceList = new ArrayList<BluetoothDevice>();
        deviceList.addAll(bluetoothDeviceSet);
        List<String> specialDevicesName = new ArrayList<String>();
        List<String> normalDevicesName = new ArrayList<String>();
        List<BluetoothDevice> specialDevices = new ArrayList<BluetoothDevice>();
        List<BluetoothDevice> normalDevices = new ArrayList<BluetoothDevice>();
        for (BluetoothDevice device : deviceList) {
            String deviceName = device.getName();
            if (deviceName == null) {
                deviceName = "NULL";
            }
            String name = deviceName + "(" + device.getAddress() + ")";
            if (deviceName.contains(SpecialDeviceName)) {
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
            deviceListSpinner.setAdapter(adapter);
            deviceListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    BluetoothDevice device = concatenateDevices.get(position);
                    BluetoothTool.getInstance(NavigationTabActivity.this)
                            .connectDevice(device);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }
    }

    /**
     * 取得设备型号、厂家编号
     */
    private void getDeviceTypeAndNumber() {
        if (getDeviceTypeAndNumberCommunications == null) {
            getDeviceTypeAndNumberCommunications = new BluetoothTalk[2];
            for (int i = 0; i < 2; i++) {
                final int index = i;
                getDeviceTypeAndNumberCommunications[i] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        if (index == 0) {
                            this.setSendBuffer(SerialUtility.crc16(SerialUtility.hexStr2Ints("0103FA080001")));
                        }
                        if (index == 1) {
                            this.setSendBuffer(SerialUtility.crc16(SerialUtility.hexStr2Ints("0103D2090001")));
                        }
                    }

                    @Override
                    public void afterSend() {

                    }

                    @Override
                    public void beforeReceive() {

                    }

                    @Override
                    public void afterReceive() {

                    }

                    @Override
                    public Object onParse() {
                        if (SerialUtility.isCRC16Valid(getReceivedBuffer())) {
                            byte[] received = SerialUtility.trimEnd(getReceivedBuffer());
                            if (received.length == 8) {
                                if (index == 0) {
                                    for (String error : ApplicationConfig.ERROR_CODE_ARRAY) {
                                        if (SerialUtility.byte2HexStr(received).contains(error)) {
                                            return "type:error";
                                        }
                                    }
                                    return "type:" + ParseSerialsUtils.getIntFromBytes(received);
                                }
                                if (index == 1) {
                                    for (String error : ApplicationConfig.ERROR_CODE_ARRAY) {
                                        if (SerialUtility.byte2HexStr(received).contains(error)) {
                                            return "number:1000";
                                        }
                                    }
                                    return "number:" + ParseSerialsUtils.getIntFromBytes(received);
                                }
                            }
                        }
                        return null;
                    }
                };
            }
        }
        if (BluetoothTool.getInstance(this).isConnected()) {
            getDeviceTypeAndNumberHandler.sendCount = getDeviceTypeAndNumberCommunications.length;
            BluetoothTool.getInstance(NavigationTabActivity.this)
                    .setHandler(getDeviceTypeAndNumberHandler)
                    .setCommunications(getDeviceTypeAndNumberCommunications)
                    .send();
        } else {
            Toast.makeText(this,
                    R.string.not_connect_device_error,
                    android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
    }

    // ============================== Get DeviceType And Number Handler ====================== //
    private class GetDeviceTypeAndNumberHandler extends BluetoothHandler {

        public int sendCount;

        private int receiveCount;

        private List<String> responseStringList;

        public GetDeviceTypeAndNumberHandler(Activity activity) {
            super(activity);
            TAG = GetDeviceTypeAndNumberHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
            responseStringList = new ArrayList<String>();
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (sendCount == receiveCount) {
                NavigationTabActivity.this.hasGetDeviceTypeAndNumber = true;
                for (String item : responseStringList) {
                    if (item.contains("type")) {
                        if (item.contains("1000")) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(NavigationTabActivity.this);
                            AlertDialog dialog = builder.setTitle(R.string.choice_device_type_title)
                                    .setItems(new String[]{ApplicationConfig.deviceType[0],
                                            ApplicationConfig.deviceType[1]},
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    // 选定设备型号后开始同步参数
                                                    NavigationTabActivity.this.startHomeActivityStatusSyncTask();
                                                }
                                            }).create();
                            dialog.show();
                            dialog.setCancelable(false);
                            dialog.setCanceledOnTouchOutside(false);
                        } else if (item.contains("3000")) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(NavigationTabActivity.this);
                            AlertDialog dialog = builder.setTitle(R.string.choice_device_type_title)
                                    .setItems(new String[]{ApplicationConfig.deviceType[2],
                                            ApplicationConfig.deviceType[3]},
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    // 选定设备型号后开始同步参数
                                                    NavigationTabActivity.this.startHomeActivityStatusSyncTask();
                                                }
                                            })
                                    .create();
                            dialog.show();
                            dialog.setCancelable(false);
                            dialog.setCanceledOnTouchOutside(false);
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(NavigationTabActivity.this);
                            AlertDialog dialog = builder.setTitle(R.string.choice_device_type_title)
                                    .setItems(ApplicationConfig.deviceType,
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    // 选定设备型号后开始同步参数
                                                    NavigationTabActivity.this.startHomeActivityStatusSyncTask();
                                                }
                                            })
                                    .create();
                            dialog.show();
                            dialog.setCancelable(false);
                            dialog.setCanceledOnTouchOutside(false);
                        }
                    }
                    if (item.contains("number")) {
                        SharedPreferences settings = getSharedPreferences(ApplicationConfig.PREFS_NAME, 0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putInt(ApplicationConfig.DeviceNumberValue, Integer.parseInt(item.split(":")[1]));
                        editor.commit();
                    }
                }
            }
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof String) {
                responseStringList.add((String) msg.obj);
                receiveCount++;
            }
        }

        @Override
        public void onTalkError(Message msg) {
            super.onTalkError(msg);
        }
    }

    /**
     * 开启HomeActivity Sync Task
     */
    public void startHomeActivityStatusSyncTask() {
        if (hasGetDeviceTypeAndNumber) {
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

}
