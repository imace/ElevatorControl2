package com.inovance.ElevatorControl.activities;

import android.app.Activity;
import android.app.AlertDialog;
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
import com.inovance.ElevatorControl.handlers.GlobalHandler;
import com.inovance.ElevatorControl.handlers.SearchBluetoothHandler;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import com.inovance.ElevatorControl.views.customspinner.NoDefaultSpinner;
import com.inovance.ElevatorControl.views.dialogs.CustomDialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TabActivity 导航
 */

public class NavigationTabActivity extends TabActivity implements Runnable {

    private static final String TAG = NavigationTabActivity.class.getSimpleName();

    /**
     * 优先显示的设备名称
     */
    private static final String SpecialDeviceName = "M-Tools";

    /**
     * 用于搜索蓝牙的 Handler
     */
    private SearchBluetoothHandler searchBluetoothHandler;

    /**
     * 是否已读取到设备型号和名称
     */
    private boolean hasGetDeviceTypeAndNumber = false;

    /**
     * 用于读取设备型号的 Handler
     */
    private GetDeviceTypeAndNumberHandler getDeviceTypeAndNumberHandler;

    /**
     * 用于取得设备型号的通信内容
     */
    private BluetoothTalk[] getDeviceTypeAndNumberCommunications;

    /**
     * 用于读取设备型号的 Task
     */
    private Runnable getDeviceTypeNumberTask;

    /**
     * 读取间隔
     */
    private static final int LOOP_TIME = 2000;

    /**
     * 是否暂停读取
     */
    private boolean isRunning = false;

    /**
     * 正在搜索蓝牙设备提示
     */
    @InjectView(R.id.search_device_tips)
    TextView searchDeviceTipsView;

    /**
     * 搜索到得设备列表显示
     */
    @InjectView(R.id.custom_spinner)
    NoDefaultSpinner deviceListSpinner;

    /**
     * 重新搜索蓝牙设备按钮
     */
    @InjectView(R.id.research_devices_button)
    public View researchDevicesButton;

    /**
     * 重新搜索设备按钮图标ImageView
     */
    @InjectView(R.id.refresh_button_icon)
    ImageView refreshButtonIcon;

    /**
     * 重新搜索设备按钮ProgressBar
     */
    @InjectView(R.id.refresh_button_progress)
    ProgressBar refreshButtonProgress;

    /**
     * 故障分析 Tab Icon ImageView
     */
    public ImageView troubleAnalyzeIcon;

    /**
     * 已经搜索到的蓝牙设备列表
     */
    private List<BluetoothDevice> tempDeviceList;

    /**
     * 已经搜索到的蓝牙设备列表名称
     */
    private String[] tempDevicesName;

    /**
     * 搜索蓝牙设备状态
     */
    private static final int SEARCH_DEVICE = 1;

    /**
     * 连接蓝牙设备状态
     */
    private static final int CONNECT_DEVICE = 2;

    /**
     * 取得设备型号状态
     */
    private static final int GET_DEVICE_TYPE = 3;

    /**
     * 当前的 Task 状态
     */
    private int currentTask;

    /**
     * 上一次连接的蓝牙设备
     */
    private BluetoothDevice tempDevice;

    private static final int REQUEST_BLUETOOTH_ENABLE = 1;

    private ExecutorService pool = Executors.newSingleThreadExecutor();

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
                if (isRunning) {
                    if (!hasGetDeviceTypeAndNumber) {
                        if (BluetoothTool.getInstance(NavigationTabActivity.this).isConnected()) {
                            currentTask = GET_DEVICE_TYPE;
                            pool.execute(NavigationTabActivity.this);
                            getDeviceTypeAndNumberHandler.postDelayed(getDeviceTypeNumberTask, LOOP_TIME);
                        }
                    }
                }
            }
        };
        showRefreshButtonProgress(false);
        /**
         * 绑定搜索按钮动作
         */
        researchDevicesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRefreshButtonProgress(true);
                searchDeviceTipsView.setText(R.string.searching_device_text);
                searchDeviceTipsView.setVisibility(View.VISIBLE);
                deviceListSpinner.setVisibility(View.GONE);
                currentTask = SEARCH_DEVICE;
                pool.execute(NavigationTabActivity.this);
            }
        });
        GlobalHandler.getInstance(NavigationTabActivity.this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLUETOOTH_ENABLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BLUETOOTH_ENABLE) {
            if (resultCode == RESULT_CANCELED) {
                BluetoothTool.getInstance(this).kill();
                Intent intent = new Intent(this, CheckAuthorizationActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("Exit", true);
                startActivity(intent);
                finish();
            }
        }
    }

    /**
     * 取得当前连接的蓝牙设备型号
     */
    public void startGetDeviceTypeAndNumberTask() {
        if (!hasGetDeviceTypeAndNumber) {
            isRunning = true;
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
                ImageView tabIcon = ((ImageView) tabIndicator.findViewById(R.id.icon));
                tabIcon.setImageResource(icons[index]);
                if (index == 0) {
                    troubleAnalyzeIcon = tabIcon;
                }
                TabHost.TabSpec tabSpec = tabHost.newTabSpec("tab" + title)
                        .setIndicator(tabIndicator)
                        .setContent(new Intent(this, classes[index]));
                tabHost.addTab(tabSpec);
                index++;
            }
        }
        tabHost.setCurrentTab(2);
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String s) {
                BluetoothTool.getInstance(NavigationTabActivity.this)
                        .setHandler(null);
            }
        });
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
     * 绑定导航按键动作
     *
     * @param event KeyEvent
     * @return True or false
     */
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
        tempDeviceList = new ArrayList<BluetoothDevice>();
        tempDeviceList.addAll(specialDevices);
        tempDeviceList.addAll(normalDevices);
        tempDevicesName = concatenateName.toArray(new String[concatenateName.size()]);
        if (tempDevicesName.length > 0) {
            setSpinnerDataSource();
            deviceListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    tempDevice = tempDeviceList.get(position);
                    currentTask = CONNECT_DEVICE;
                    pool.execute(NavigationTabActivity.this);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }
    }

    /**
     * Set spinner data source
     */
    public void setSpinnerDataSource() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(),
                R.layout.custom_white_spinner_item, tempDevicesName);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceListSpinner.setAdapter(adapter);
        searchDeviceTipsView.setVisibility(View.GONE);
        deviceListSpinner.setVisibility(View.VISIBLE);
    }

    public void updateSearchResult() {
        if (tempDevicesName.length == 0) {
            searchDeviceTipsView.setText(R.string.found_none_device_text);
            searchDeviceTipsView.setVisibility(View.VISIBLE);
            deviceListSpinner.setVisibility(View.GONE);
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
        }
    }

    @Override
    public void run() {
        switch (currentTask) {
            case SEARCH_DEVICE: {
                BluetoothTool.getInstance(NavigationTabActivity.this)
                        .setSearchHandler(searchBluetoothHandler)
                        .search();
            }
            break;
            case CONNECT_DEVICE: {
                if (tempDevice != null) {
                    BluetoothTool.getInstance(NavigationTabActivity.this).connectDevice(tempDevice);
                }
            }
            break;
            case GET_DEVICE_TYPE: {
                NavigationTabActivity.this.getDeviceTypeAndNumber();
            }
            break;
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
                hasGetDeviceTypeAndNumber = true;
                isRunning = false;
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
                                                    BluetoothTool.getInstance(NavigationTabActivity.this)
                                                            .setHasSelectDeviceType(true);
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
                                                    BluetoothTool.getInstance(NavigationTabActivity.this)
                                                            .setHasSelectDeviceType(true);
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
                                                    BluetoothTool.getInstance(NavigationTabActivity.this)
                                                            .setHasSelectDeviceType(true);
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
