package com.inovance.elevatorcontrol.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TabActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import butterknife.InjectView;
import butterknife.Views;
import com.inovance.bluetoothtool.BluetoothHandler;
import com.inovance.bluetoothtool.BluetoothTalk;
import com.inovance.bluetoothtool.BluetoothTool;
import com.inovance.bluetoothtool.SerialUtility;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.config.ParameterUpdateTool;
import com.inovance.elevatorcontrol.daos.DeviceDao;
import com.inovance.elevatorcontrol.handlers.GlobalHandler;
import com.inovance.elevatorcontrol.handlers.SearchBluetoothHandler;
import com.inovance.elevatorcontrol.models.CommunicationCode;
import com.inovance.elevatorcontrol.models.Device;
import com.inovance.elevatorcontrol.models.NormalDevice;
import com.inovance.elevatorcontrol.models.SpecialDevice;
import com.inovance.elevatorcontrol.utils.ParseSerialsUtils;
import com.inovance.elevatorcontrol.views.customspinner.NoDefaultSpinner;
import com.inovance.elevatorcontrol.views.dialogs.CustomDialog;
import com.inovance.elevatorcontrol.web.WebApi;
import com.inovance.elevatorcontrol.web.WebApi.OnGetResultListener;
import com.inovance.elevatorcontrol.web.WebApi.OnRequestFailureListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TabActivity 导航
 */

public class NavigationTabActivity extends TabActivity implements Runnable, OnGetResultListener, OnRequestFailureListener {

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
    private boolean hasGetDeviceType = false;

    /**
     * 用于读取标准设备型号的 Handler
     */
    private GetNormalDeviceTypeHandler getNormalDeviceTypeHandler;

    /**
     * 用于读取专用设备型号的 Handler
     */
    private GetSpecialDeviceTypeHandler getSpecialDeviceTypeHandler;

    /**
     * 用于取得设备型号的通信内容
     */
    private BluetoothTalk[] getNormalDeviceTypeTalk;

    /**
     * 用于读取设备型号的 Task (标准设备或者专用设备)
     */
    private Runnable getDeviceTypeTask;

    /**
     * 读取间隔
     */
    private static final int LOOP_TIME = 1000;

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
     * 读取标准设备信号状态
     */
    private static final int GET_NORMAL_DEVICE_TYPE = 3;

    /**
     * 读取非标准设备信号状态
     */
    private static final int GET_SPECIAL_DEVICE_TYPE = 4;

    /**
     * 当前的 Task 状态
     */
    private int currentTask;

    /**
     * 上一次连接的蓝牙设备
     */
    private BluetoothDevice tempDevice;

    /**
     * 通信次数
     */
    private int talkTime = 0;

    /**
     * 最大重试次数
     */
    private static final int MaxRetryTime = 5;

    /**
     * 专有设备通信码
     */
    private List<CommunicationCode> communicationCodeList = new ArrayList<CommunicationCode>();

    /**
     * 当前正在尝试的通信码索引
     */
    private int specialDeviceCodeIndex = -1;

    private Handler delayHandler = new Handler();

    /**
     * 可供选择的标准设备列表
     */
    private List<NormalDevice> normalDeviceList = new ArrayList<NormalDevice>();

    /**
     * 可供选择的专有设备列表
     */
    private List<SpecialDevice> specialDeviceList = new ArrayList<SpecialDevice>();

    private static final int StartRecogniseDevice = 1;

    private static final int FailedRecogniseDevice = 2;

    /**
     * 连接设备失败
     */
    public boolean failedToConnectDevice = false;

    private Handler messageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (isRunning) {
                switch (msg.what) {
                    case StartRecogniseDevice:
                        Toast.makeText(NavigationTabActivity.this,
                                R.string.recognise_device_wait_text,
                                Toast.LENGTH_SHORT).show();
                        break;
                    case FailedRecogniseDevice:
                        Toast.makeText(NavigationTabActivity.this,
                                R.string.recognise_device_failed_text,
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    };

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
        getNormalDeviceTypeHandler = new GetNormalDeviceTypeHandler(this);
        getSpecialDeviceTypeHandler = new GetSpecialDeviceTypeHandler(this);
        getDeviceTypeTask = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    if (!hasGetDeviceType) {
                        if (currentTask == GET_NORMAL_DEVICE_TYPE) {
                            if (talkTime >= MaxRetryTime) {
                                talkTime = 0;
                                currentTask = GET_SPECIAL_DEVICE_TYPE;
                                specialDeviceCodeIndex = 0;
                            }
                        }
                        if (currentTask == GET_SPECIAL_DEVICE_TYPE) {
                            if (talkTime >= MaxRetryTime) {
                                talkTime = 0;
                                currentTask = GET_SPECIAL_DEVICE_TYPE;
                                specialDeviceCodeIndex++;
                            }
                            Log.v(TAG, specialDeviceCodeIndex +"");
                            Log.v(TAG, specialDeviceList.size() +"");
                            Log.v(TAG, talkTime +"");
                            if (specialDeviceCodeIndex == specialDeviceList.size() - 1 && talkTime >= MaxRetryTime) {
                                isRunning = false;
                                messageHandler.sendEmptyMessage(FailedRecogniseDevice);
                            }
                        }
                        talkTime++;
                        pool.execute(NavigationTabActivity.this);
                        delayHandler.postDelayed(getDeviceTypeTask, LOOP_TIME);
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
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebApi.getInstance().setOnResultListener(this);
        WebApi.getInstance().setOnFailureListener(this);
        WebApi.getInstance().getNormalDeviceList(this);
        WebApi.getInstance().getSpecialDeviceList(this);
        WebApi.getInstance().getSpecialDeviceCodeList(this, BluetoothAdapter.getDefaultAdapter().getAddress());
    }

    /**
     * 显示选择操作类型对话框
     * 调试电梯
     * 烧录 DSP
     */
    public void showSelectOperationDialog() {
        String[] operationArray = getResources().getStringArray(R.array.device_operation_array);
        AlertDialog.Builder builder = new AlertDialog.Builder(NavigationTabActivity.this);
        builder.setTitle(R.string.choice_operation_title);
        builder.setItems(operationArray, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        // 识别设备
                        startGetNormalDeviceTypeTask();
                        break;
                    case 1:
                        // 跳转到程序烧录界面
                        switchTab(3, 2);
                        break;
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
    }

    /**
     * 取得当前连接的标准设备型号
     */
    public void startGetNormalDeviceTypeTask() {
        if (!hasGetDeviceType) {
            isRunning = true;
            talkTime = 0;
            currentTask = GET_NORMAL_DEVICE_TYPE;
            delayHandler.postDelayed(getDeviceTypeTask, LOOP_TIME);
            messageHandler.sendEmptyMessage(StartRecogniseDevice);
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
        WebApi.getInstance().removeListener();
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
                BluetoothTool.getInstance()
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
                        getTabHost().setOnTabChangedListener(null);
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
                        getTabHost().setOnTabChangedListener(null);
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
                        getTabHost().setOnTabChangedListener(null);
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
            if (getTabHost().getCurrentTab() != 2) {
                getTabHost().setCurrentTab(2);
            } else {
                CustomDialog.exitDialog(this).show();
            }
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
                    // 连接设备
                    failedToConnectDevice = false;
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
     * 取得标准设备型号、厂家编号
     */
    private void getNormalDeviceType() {
        if (getNormalDeviceTypeTalk == null) {
            BluetoothTool.getInstance().crcValue = BluetoothTool.CRCValueNone;
            getNormalDeviceTypeTalk = new BluetoothTalk[]{
                    new BluetoothTalk() {
                        @Override
                        public void beforeSend() {
                            this.setSendBuffer(SerialUtility.crc16("0103"
                                    + ApplicationConfig.GetDeviceTypeCode
                                    + "0001"));
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
                                String result = SerialUtility.byte2HexStr(getReceivedBuffer());
                                if (!result.contains(ApplicationConfig.ErrorCode)) {
                                    return ParseSerialsUtils.getIntFromBytes(getReceivedBuffer());
                                }
                            }
                            return null;
                        }
                    }
            };
        }
        if (BluetoothTool.getInstance().isConnected()) {
            BluetoothTool.getInstance()
                    .setHandler(getNormalDeviceTypeHandler)
                    .setCommunications(getNormalDeviceTypeTalk)
                    .send();
        }
    }

    /**
     * 取得专用设备型号
     */
    private void getSpecialDeviceType() {
        if (specialDeviceCodeIndex < communicationCodeList.size()) {
            final CommunicationCode code = communicationCodeList.get(specialDeviceCodeIndex);
            BluetoothTool.getInstance().crcValue = code.getCrcValue();
            // 通信码是否过期
            if (!code.isExpire()) {
                BluetoothTalk[] talks = new BluetoothTalk[1];
                talks[0] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16("0103"
                                + ApplicationConfig.GetDeviceTypeCode
                                + "0001"));
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
                            String result = SerialUtility.byte2HexStr(getReceivedBuffer());
                            if (!result.contains(ApplicationConfig.ErrorCode)) {
                                return result;
                            }
                        }
                        return null;
                    }
                };
                if (BluetoothTool.getInstance().isConnected()) {
                    BluetoothTool.getInstance()
                            .setHandler(getSpecialDeviceTypeHandler)
                            .setCommunications(talks)
                            .send();
                }
            } else {
                talkTime = 0;
                specialDeviceCodeIndex++;
            }
        } else {
            isRunning = false;
            messageHandler.sendEmptyMessage(FailedRecogniseDevice);
        }
    }

    /**
     * 显示标准设备选择框
     *
     * @param type 1000/3000
     */
    private void showNormalDeviceSelect(int type) {
        String typeString = String.valueOf(type);
        final List<NormalDevice> tempNormalDevice = new ArrayList<NormalDevice>();
        List<String> names = new ArrayList<String>();
        for (NormalDevice device : normalDeviceList) {
            if (device.getName().contains(typeString)) {
                tempNormalDevice.add(device);
                names.add(device.getName());
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(NavigationTabActivity.this);
        builder.setTitle(R.string.choice_device_type_title);
        builder.setItems(names.toArray(new String[names.size()]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int position) {
                final NormalDevice device = tempNormalDevice.get(position);
                BluetoothTool.getInstance().setHasSelectDeviceType(true);
                // 检查标准设备功能码、状态码、故障帮助更新状态
                ParameterUpdateTool.getInstance().selectDevice(NavigationTabActivity.this,
                        device.getID(),
                        device.getName(),
                        Device.NormalDevice,
                        new ParameterUpdateTool.OnCheckResultListener() {
                            @Override
                            public void onComplete() {
                                NavigationTabActivity.this.startHomeActivityStatusSyncTask();
                            }

                            @Override
                            public void onFailed(Throwable throwable) {
                                Device temp = DeviceDao.findByName(NavigationTabActivity.this,
                                        device.getName(),
                                        Device.NormalDevice);
                                if (temp != null) {
                                    // 本地存在当前连接设备的参数
                                    Toast.makeText(NavigationTabActivity.this,
                                            R.string.check_parameter_failed_use_local_parameter_message,
                                            Toast.LENGTH_SHORT).show();
                                    // 使用本地保存的参数
                                    NavigationTabActivity.this.startHomeActivityStatusSyncTask();
                                } else {
                                    // 本地不存在当前连接设备的参数
                                    Toast.makeText(NavigationTabActivity.this,
                                            R.string.check_parameter_failed_message,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
    }

    @Override
    public void run() {
        switch (currentTask) {
            case SEARCH_DEVICE:
                BluetoothTool.getInstance()
                        .setSearchHandler(searchBluetoothHandler)
                        .search();
                break;
            case CONNECT_DEVICE:
                if (tempDevice != null && !failedToConnectDevice) {
                    hasGetDeviceType = false;
                    BluetoothTool.getInstance().setHasSelectDeviceType(false);
                    BluetoothTool.getInstance().connectDevice(tempDevice);
                }
                break;
            case GET_NORMAL_DEVICE_TYPE:
                getNormalDeviceType();
                break;
            case GET_SPECIAL_DEVICE_TYPE:
                getSpecialDeviceType();
                break;
        }
    }

    @Override
    public void onResult(String tag, String responseString) {
        if (responseString != null && responseString.length() > 0) {
            if (tag.equalsIgnoreCase(ApplicationConfig.GetNormalDeviceList)) {
                try {
                    JSONArray jsonArray = new JSONArray(responseString);
                    normalDeviceList.clear();
                    int size = jsonArray.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        NormalDevice device = new NormalDevice(object);
                        normalDeviceList.add(device);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (tag.equalsIgnoreCase(ApplicationConfig.GetSpecialDeviceList)) {
                try {
                    JSONArray jsonArray = new JSONArray(responseString);
                    specialDeviceList.clear();
                    int size = jsonArray.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        SpecialDevice device = new SpecialDevice(object);
                        specialDeviceList.add(device);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (tag.equalsIgnoreCase(ApplicationConfig.GetSpecialDeviceCodeList)) {
                try {
                    JSONArray jsonArray = new JSONArray(responseString);
                    int size = jsonArray.length();
                    communicationCodeList = new ArrayList<CommunicationCode>();
                    for (int i = 0; i < size; i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        CommunicationCode code = new CommunicationCode(object);
                        communicationCodeList.add(code);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onFailure(int statusCode, Throwable throwable) {
        Toast.makeText(this, R.string.server_error_text, Toast.LENGTH_SHORT).show();
    }

    // ============================== Get Normal DeviceType Handler ================================================ //

    private class GetNormalDeviceTypeHandler extends BluetoothHandler {

        public GetNormalDeviceTypeHandler(Activity activity) {
            super(activity);
            TAG = GetNormalDeviceTypeHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof Integer) {
                isRunning = false;
                hasGetDeviceType = true;
                BluetoothTool.getInstance().crcValue = BluetoothTool.CRCValueNone;
                showNormalDeviceSelect((Integer) msg.obj);
            }
        }
    }

    // ============================== Get Special DeviceType Handler =============================================== //

    private class GetSpecialDeviceTypeHandler extends BluetoothHandler {

        public GetSpecialDeviceTypeHandler(Activity activity) {
            super(activity);
            TAG = GetSpecialDeviceTypeHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof String) {
                isRunning = false;
                hasGetDeviceType = true;
                CommunicationCode code = communicationCodeList.get(specialDeviceCodeIndex);
                for (final SpecialDevice device : specialDeviceList) {
                    if (code.getCode().equalsIgnoreCase(device.getCode())) {
                        BluetoothTool.getInstance().setHasSelectDeviceType(true);
                        // 检查专有设备功能码、状态码、故障帮助更新状态
                        ParameterUpdateTool.getInstance().selectDevice(NavigationTabActivity.this,
                                device.getId(),
                                device.getName(),
                                Device.SpecialDevice,
                                new ParameterUpdateTool.OnCheckResultListener() {
                                    @Override
                                    public void onComplete() {
                                        NavigationTabActivity.this.startHomeActivityStatusSyncTask();
                                    }

                                    @Override
                                    public void onFailed(Throwable throwable) {
                                        Device temp = DeviceDao.findByName(NavigationTabActivity.this,
                                                device.getName(),
                                                Device.SpecialDevice);
                                        if (temp != null) {
                                            // 本地存在当前连接设备的参数
                                            Toast.makeText(NavigationTabActivity.this,
                                                    R.string.check_parameter_failed_use_local_parameter_message,
                                                    Toast.LENGTH_SHORT).show();
                                            // 使用本地保存的参数
                                            NavigationTabActivity.this.startHomeActivityStatusSyncTask();
                                        } else {
                                            // 本地不存在当前连接设备的参数
                                            Toast.makeText(NavigationTabActivity.this,
                                                    R.string.check_parameter_failed_message,
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                }
            }
        }
    }

    /**
     * 开启HomeActivity Sync Task
     */
    public void startHomeActivityStatusSyncTask() {
        if (hasGetDeviceType) {
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
