package com.inovance.elevatorcontrol.window;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.primitives.Ints;
import com.inovance.bluetoothtool.BluetoothTalk;
import com.inovance.bluetoothtool.BluetoothTool;
import com.inovance.bluetoothtool.SerialUtility;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.daos.ParameterSettingsDao;
import com.inovance.elevatorcontrol.daos.RealTimeMonitorDao;
import com.inovance.elevatorcontrol.handlers.UnlockHandler;
import com.inovance.elevatorcontrol.models.ObjectListHolder;
import com.inovance.elevatorcontrol.models.ParameterSettings;
import com.inovance.elevatorcontrol.models.RealTimeMonitor;
import com.inovance.elevatorcontrol.utils.LogUtils;
import com.inovance.elevatorcontrol.utils.ParseSerialsUtils;
import com.viewpagerindicator.TabPageIndicator;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CallFloorWindow extends FragmentActivity implements CallInsideFragment.OnCallFloorListener,
        CallOutsideFragment.OnCallUpListener,
        CallOutsideFragment.OnCallDownListener,
        Runnable {

    private static final String TAG = CallFloorWindow.class.getSimpleName();

    private enum CallDirection {
        CallDirectionUp,
        CallDirectionDown
    }

    private ViewPager mViewPager;

    private CallFloorAdapter callFloorAdapter;

    /**
     * 楼层状态指示
     */
    private TextView currentFloorTextView;

    /**
     * 取得电梯最高层、最底层通信内容
     */
    private BluetoothTalk[] getFloorsCommunications;

    /**
     * 获取电梯最高层、最底层 Handler
     */
    private GetFloorsHandler getFloorsHandler;

    /**
     * 召唤楼层 Handler
     */
    private CallInsideHandler callInsideHandler;

    /**
     * 内召、外召 Handler
     */
    private CallOutsideHandler callOutsideHandler;

    private static final int NONE_TASK = -1;

    private static final int GET_FLOORS = 1;

    private static final int CALL_INSIDE_FLOOR = 2;

    private static final int CALL_OUTSIDE_FLOOR = 3;

    private static final int GET_CALL_INSIDE_STATUS = 4;

    private static final int GET_CALL_OUTSIDE_STATUS = 5;

    private int currentTask;

    /**
     * 用于同步的 Handler
     */
    private Handler syncHandler = new Handler();

    /**
     * 同步电梯状态 Runnable
     */
    private Runnable syncTask;

    /**
     * 是否暂停同步数据
     */
    private boolean running = false;

    /**
     * 是否正在同步电梯召唤信息
     */
    private boolean isSyncing = false;

    private ExecutorService pool = Executors.newSingleThreadExecutor();

    /**
     * 同步间隔
     */
    private static final int SYNC_TIME = 600;

    /**
     * 最高层和最低层
     */
    private int[] floors;

    /**
     * 选中的楼层
     */
    private int selectedFloor;

    /**
     * 上召或下召
     */
    private CallDirection direction;

    /**
     * 当前楼层
     */
    private RealTimeMonitor currentFloorMonitor;

    /**
     * 读取当前楼层和楼层外召信息
     */
    private BluetoothTalk[] getCallInsideStatusCommunications;

    /**
     * 读取当前楼层和楼层外召信息
     */
    private BluetoothTalk[] getCallOutsideStatusCommunications;

    /**
     * 读取当前楼层和楼层内召信息 Handler
     */
    private GetCallInsideStatusHandler getCallInsideStatusHandler;

    /**
     * 读取当前楼层和楼层外召信息 Handler
     */
    private GetCallOutsideStatusHandler getCallOutsideStatusHandler;

    /**
     * 用于内召楼层的指令列表
     */
    private List<RealTimeMonitor> callInsideMonitorList = new ArrayList<RealTimeMonitor>();

    /**
     * 用于外召楼层的指令列表
     */
    private List<RealTimeMonitor> callOutsideMonitorList = new ArrayList<RealTimeMonitor>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.call_floor_window);
        getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.gravity = Gravity.BOTTOM;

        mViewPager = (ViewPager) findViewById(R.id.pager);
        currentFloorTextView = (TextView) findViewById(R.id.status_info);

        mViewPager.setOffscreenPageLimit(2);
        callFloorAdapter = new CallFloorAdapter(this);
        mViewPager.setAdapter(callFloorAdapter);
        mViewPager.setCurrentItem(0);

        TabPageIndicator indicator = (TabPageIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mViewPager);

        indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int position) {
                if (floors != null) {
                    updateFragmentStatus(floors);
                }
                switch (position) {
                    case 0:
                        selectedFloor = -1;
                        isSyncing = false;
                        currentTask = GET_CALL_INSIDE_STATUS;
                        break;
                    case 1:
                        selectedFloor = -1;
                        isSyncing = false;
                        currentTask = GET_CALL_OUTSIDE_STATUS;
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        findViewById(R.id.cancel_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_OK);
                finish();
            }
        });

        getFloorsHandler = new GetFloorsHandler(this);
        callInsideHandler = new CallInsideHandler(this);
        callOutsideHandler = new CallOutsideHandler(this);
        getCallInsideStatusHandler = new GetCallInsideStatusHandler(this);
        getCallOutsideStatusHandler = new GetCallOutsideStatusHandler(this);

        syncTask = new Runnable() {
            @Override
            public void run() {
                if (running) {
                    if (BluetoothTool.getInstance().isPrepared()) {
                        if (!isSyncing) {
                            pool.execute(CallFloorWindow.this);
                        }
                        syncHandler.postDelayed(this, SYNC_TIME);
                    }
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(R.anim.slide_up_dialog, R.anim.slide_out_down);

        currentFloorMonitor = RealTimeMonitorDao.findByStateID(this, ApplicationConfig.CurrentFloorType);

        callInsideMonitorList = RealTimeMonitorDao.findAllByStateID(this, ApplicationConfig.MoveInsideInformationType);
        Collections.sort(callInsideMonitorList, new SortComparator());

        callOutsideMonitorList = RealTimeMonitorDao.findAllByStateID(this, ApplicationConfig.MoveOutsideInformationType);
        Collections.sort(callOutsideMonitorList, new SortComparator());

        // 获取楼层数据
        createGetFloorsCommunication();

        // 获取电梯内召信息
        createGetCallInsideStatusCommunications();

        // 获取电梯外召信息
        createGetCallOutsideStatusCommunications();

        if (BluetoothTool.getInstance().isPrepared()) {
            running = true;
            currentTask = GET_FLOORS;
            syncHandler.postDelayed(syncTask, SYNC_TIME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.slide_up_dialog, R.anim.slide_out_down);
        running = false;
    }

    /**
     * Update CallInsideFragment and CallOutsideFragment floors
     *
     * @param floors floors
     */
    private void updateFragmentStatus(int[] floors) {
        Fragment fragment = callFloorAdapter.getItem(mViewPager.getCurrentItem());
        if (fragment != null) {
            if (fragment instanceof CallInsideFragment) {
                CallInsideFragment insideCallFragment = (CallInsideFragment) fragment;
                insideCallFragment.updateFloors(floors);
                insideCallFragment.setOnCallFloorListener(this);
            }
            if (fragment instanceof CallOutsideFragment) {
                CallOutsideFragment outsideCallFragment = (CallOutsideFragment) fragment;
                outsideCallFragment.updateFloors(floors);
                outsideCallFragment.setOnCallUpListener(this);
                outsideCallFragment.setOnCallDownListener(this);
            }
        }
    }

    /**
     * 生成用于读取电梯楼层信息的通信内容
     */
    private void createGetFloorsCommunication() {
        List<ParameterSettings> settingsList = ParameterSettingsDao.findAllByCodes(this, new String[]{"F600", "F601"});
        getFloorsCommunications = new BluetoothTalk[settingsList.size()];
        int index = 0;
        for (final ParameterSettings settings : settingsList) {
            getFloorsCommunications[index] = new BluetoothTalk() {
                @Override
                public void beforeSend() {
                    this.setSendBuffer(SerialUtility.crc16("0103"
                            + settings.getCode()
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
                        settings.setReceived(getReceivedBuffer());
                        return settings;
                    }
                    return null;
                }
            };
            index++;
        }
    }

    /**
     * 生成用于读取电梯内召信息的通信内容
     */
    private void createGetCallInsideStatusCommunications() {
        if (getCallInsideStatusCommunications == null) {
            int size = callInsideMonitorList.size();
            final int count = size <= 10 ? 1 : ((size - size % 10) / 10 + (size % 10 == 0 ? 0 : 1));
            getCallInsideStatusCommunications = new BluetoothTalk[count + 1];
            for (int i = 0; i < count; i++) {
                final int position = i;
                final RealTimeMonitor firstItem = callInsideMonitorList.get(position * 10);
                final int length = size <= 10 ? size : (size % 10 == 0 ? 10 : ((position == count - 1) ? size % 10 : 10));
                getCallInsideStatusCommunications[i] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16("0103"
                                + firstItem.getCode()
                                + String.format("%04x", length)));
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
                            byte[] data = SerialUtility.trimEnd(getReceivedBuffer());
                            short bytesLength = ByteBuffer.wrap(new byte[]{data[2], data[3]}).getShort();
                            if (length * 2 == bytesLength) {
                                List<RealTimeMonitor> tempList = new ArrayList<RealTimeMonitor>();
                                for (int j = 0; j < length; j++) {
                                    if (position * 10 + j < callInsideMonitorList.size()) {
                                        RealTimeMonitor item = callInsideMonitorList.get(position * 10 + j);
                                        byte[] tempData = SerialUtility.crc16("01030002"
                                                + SerialUtility.byte2HexStr(new byte[]{data[4 + j * 2], data[5 + j * 2]}));
                                        item.setReceived(tempData);
                                        tempList.add(item);
                                    }
                                }
                                ObjectListHolder holder = new ObjectListHolder();
                                holder.setRealTimeMonitorList(tempList);
                                return holder;
                            }
                        }
                        return null;
                    }
                };
            }
            getCallInsideStatusCommunications[count] = new BluetoothTalk() {
                @Override
                public void beforeSend() {
                    this.setSendBuffer(SerialUtility.crc16("0103"
                            + currentFloorMonitor.getCode()
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
                        ObjectListHolder holder = new ObjectListHolder();
                        currentFloorMonitor.setReceived(getReceivedBuffer());
                        List<RealTimeMonitor> tempList = new ArrayList<RealTimeMonitor>();
                        tempList.add(currentFloorMonitor);
                        holder.setRealTimeMonitorList(tempList);
                        return holder;
                    }
                    return null;
                }
            };
        }
    }

    /**
     * 生成用于读取电梯外召信息的通信内容
     */
    private void createGetCallOutsideStatusCommunications() {
        if (getCallOutsideStatusCommunications == null) {
            int size = callOutsideMonitorList.size();
            final int count = size <= 10 ? 1 : ((size - size % 10) / 10 + (size % 10 == 0 ? 0 : 1));
            getCallOutsideStatusCommunications = new BluetoothTalk[count + 1];
            for (int i = 0; i < count; i++) {
                final int position = i;
                final RealTimeMonitor firstItem = callOutsideMonitorList.get(position * 10);
                final int length = size <= 10 ? size : (size % 10 == 0 ? 10 : ((position == count - 1) ? size % 10 : 10));
                getCallOutsideStatusCommunications[i] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16("0103"
                                + firstItem.getCode()
                                + String.format("%04x", length)));
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
                            byte[] data = SerialUtility.trimEnd(getReceivedBuffer());
                            short bytesLength = ByteBuffer.wrap(new byte[]{data[2], data[3]}).getShort();
                            if (length * 2 == bytesLength) {
                                List<RealTimeMonitor> tempList = new ArrayList<RealTimeMonitor>();
                                for (int j = 0; j < length; j++) {
                                    if (position * 10 + j < callOutsideMonitorList.size()) {
                                        RealTimeMonitor item = callOutsideMonitorList.get(position * 10 + j);
                                        byte[] tempData = SerialUtility.crc16("01030002"
                                                + SerialUtility.byte2HexStr(new byte[]{data[4 + j * 2], data[5 + j * 2]}));
                                        item.setReceived(tempData);
                                        tempList.add(item);
                                    }
                                }
                                ObjectListHolder holder = new ObjectListHolder();
                                holder.setRealTimeMonitorList(tempList);
                                return holder;
                            }
                        }
                        return null;
                    }
                };
            }
            getCallOutsideStatusCommunications[count] = new BluetoothTalk() {
                @Override
                public void beforeSend() {
                    this.setSendBuffer(SerialUtility.crc16("0103"
                            + currentFloorMonitor.getCode()
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
                        ObjectListHolder holder = new ObjectListHolder();
                        currentFloorMonitor.setReceived(getReceivedBuffer());
                        List<RealTimeMonitor> tempList = new ArrayList<RealTimeMonitor>();
                        tempList.add(currentFloorMonitor);
                        holder.setRealTimeMonitorList(tempList);
                        return holder;
                    }
                    return null;
                }
            };
        }
    }

    /**
     * 读取电梯最高层和最底层
     */
    private void startGetFloorsCommunication() {
        isSyncing = true;
        if (getFloorsCommunications != null) {
            if (BluetoothTool.getInstance().isPrepared()) {
                getFloorsHandler.sendCount = getFloorsCommunications.length;
                BluetoothTool.getInstance()
                        .setHandler(getFloorsHandler)
                        .setCommunications(getFloorsCommunications)
                        .startTask();
            }
        }
    }

    /**
     * 内召楼层
     */
    private void startCallInsideCommunication() {
        int index = 0;
        for (final RealTimeMonitor monitor : callInsideMonitorList) {
            if (selectedFloor >= (index * 8 + 1) && selectedFloor <= (index + 1) * 8) {
                int callIndex = selectedFloor - (index * 8 + 1);
                final String callCode = monitor.getCode() + ApplicationConfig.MoveSideCallCode[callIndex];
                BluetoothTalk[] communications = new BluetoothTalk[]{
                        new BluetoothTalk() {
                            @Override
                            public void beforeSend() {
                                this.setSendBuffer(SerialUtility.crc16("0106"
                                        + callCode));
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
                                    monitor.setReceived(getReceivedBuffer());
                                    return monitor;
                                }
                                return null;
                            }
                        }
                };
                if (BluetoothTool.getInstance().isPrepared()) {
                    isSyncing = true;
                    currentTask = CALL_INSIDE_FLOOR;
                    callInsideHandler.writeCode = callCode;
                    this.callInsideHandler.floor = selectedFloor;
                    BluetoothTool.getInstance()
                            .setHandler(callInsideHandler)
                            .setCommunications(communications)
                            .startTask();
                }
                break;
            }
            index++;
        }
    }

    /**
     * 外召楼层
     */
    private void startCallOutsideCommunication() {
        int index = 0;
        for (final RealTimeMonitor monitor : callOutsideMonitorList) {
            if (selectedFloor >= (index * 4 + 1) && selectedFloor <= ((index + 1) * 4)) {
                int minus = selectedFloor - (index * 4 + 1);
                int callIndex = 0;
                switch (direction) {
                    case CallDirectionUp:
                        callIndex = minus * 2;
                        break;
                    case CallDirectionDown:
                        callIndex = 2 * minus + 1;
                        break;
                }
                final String callCode = monitor.getCode() + ApplicationConfig.MoveSideCallCode[callIndex];
                BluetoothTalk[] communications = new BluetoothTalk[]{
                        new BluetoothTalk() {
                            @Override
                            public void beforeSend() {
                                this.setSendBuffer(SerialUtility.crc16("0106"
                                        + callCode));
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
                                    monitor.setReceived(getReceivedBuffer());
                                    return monitor;
                                }
                                return null;
                            }
                        }
                };
                if (BluetoothTool.getInstance().isPrepared()) {
                    isSyncing = true;
                    callOutsideHandler.writeCode = callCode;
                    callOutsideHandler.floor = selectedFloor;
                    callOutsideHandler.isUp = direction == CallDirection.CallDirectionUp;
                    BluetoothTool.getInstance()
                            .setHandler(callOutsideHandler)
                            .setCommunications(communications)
                            .startTask();
                }
                break;
            }
            index++;
        }
    }

    /**
     * 读取电梯内召和当前楼层信息
     */
    private void startGetCallInsideStatus() {
        if (BluetoothTool.getInstance().isPrepared()) {
            isSyncing = true;
            getCallInsideStatusHandler.sendCount = getCallInsideStatusCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(getCallInsideStatusHandler)
                    .setCommunications(getCallInsideStatusCommunications)
                    .startTask();
        }
    }

    /**
     * 读取电梯外召和当前楼层信息
     */
    private void startGetCallOutsideStatus() {
        if (BluetoothTool.getInstance().isPrepared()) {
            isSyncing = true;
            createGetCallOutsideStatusCommunications();
            getCallOutsideStatusHandler.sendCount = getCallOutsideStatusCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(getCallOutsideStatusHandler)
                    .setCommunications(getCallOutsideStatusCommunications)
                    .startTask();
        }
    }

    /**
     * 响应读取到的内召信息和当前楼层
     *
     * @param currentFloor 当前楼层
     * @param calledFloors 召唤的楼层
     */
    private void handlerCallInsideStatus(int currentFloor, int[] calledFloors) {
        currentFloorTextView.setText(getString(R.string.current_floor_text) + String.valueOf(currentFloor));
        Fragment fragment = callFloorAdapter.getItem(mViewPager.getCurrentItem());
        if (fragment != null && fragment instanceof CallInsideFragment) {
            CallInsideFragment callInsideFragment = (CallInsideFragment) fragment;
            callInsideFragment.updateFloorCallStatus(calledFloors);
        }
    }

    /**
     * 响应读取到的外召信息和当前楼层
     *
     * @param currentFloor    当前楼层
     * @param floorCallStatus 楼层召唤信息
     */
    private void handlerCallOutsideStatus(int currentFloor, List<Integer[]> statusList) {
        currentFloorTextView.setText(getString(R.string.current_floor_text) + String.valueOf(currentFloor));
        Fragment fragment = callFloorAdapter.getItem(mViewPager.getCurrentItem());
        if (fragment != null && fragment instanceof CallOutsideFragment) {
            CallOutsideFragment callOutsideFragment = (CallOutsideFragment) fragment;
            callOutsideFragment.updateFloorCallStatus(statusList);
        }
    }

    /**
     * 内召楼层
     *
     * @param floors 电梯最高层和最低层
     * @param index  选中位置的 Index
     */
    @Override
    public void onCallFloor(int[] floors, int index) {
        Fragment fragment = callFloorAdapter.getItem(mViewPager.getCurrentItem());
        if (fragment != null && fragment instanceof CallInsideFragment) {
            CallInsideFragment callInsideFragment = (CallInsideFragment) fragment;
            callInsideFragment.updateSelectedIndex(index);
        }

        selectedFloor = Math.min(floors[0], floors[1]) + index;
        isSyncing = false;
        currentTask = CALL_INSIDE_FLOOR;
    }

    /**
     * 上召楼层
     *
     * @param floors 电梯最高层和最低层
     * @param index  选中位置的 Index
     */
    @Override
    public void onCallUp(int[] floors, int index) {
        selectedFloor = Math.min(floors[0], floors[1]) + index;
        direction = CallDirection.CallDirectionUp;
        isSyncing = false;
        currentTask = CALL_OUTSIDE_FLOOR;
    }

    /**
     * 下召楼层
     *
     * @param floors 电梯最高层和最低层
     * @param index  选中位置的 Index
     */
    @Override
    public void onCallDown(int[] floors, int index) {
        selectedFloor = Math.min(floors[0], floors[1]) + index;
        direction = CallDirection.CallDirectionDown;
        isSyncing = false;
        currentTask = CALL_OUTSIDE_FLOOR;
    }

    @Override
    public void run() {
        switch (currentTask) {
            case GET_FLOORS:
                // 读取电梯楼层信息
                startGetFloorsCommunication();
                break;
            case GET_CALL_INSIDE_STATUS:
                // 读取电梯内召和当前楼层信息
                startGetCallInsideStatus();
                break;
            case GET_CALL_OUTSIDE_STATUS:
                // 读取电梯外召和当前楼层信息
                startGetCallOutsideStatus();
                break;
            case CALL_INSIDE_FLOOR:
                // 内召楼层
                if (selectedFloor != -1) {
                    startCallInsideCommunication();
                }
                break;
            case CALL_OUTSIDE_FLOOR:
                // 外召楼层
                if (selectedFloor != -1) {
                    startCallOutsideCommunication();
                }
                break;
        }
    }

    private class GetFloorsHandler extends UnlockHandler {

        public int sendCount;

        public int receiveCount;

        private List<ParameterSettings> settingsList;

        public GetFloorsHandler(Activity activity) {
            super(activity);
            TAG = GetFloorsHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
            settingsList = new ArrayList<ParameterSettings>();
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (sendCount == receiveCount && settingsList.size() == 2) {
                byte[] data1 = settingsList.get(0).getReceived();
                byte[] data2 = settingsList.get(1).getReceived();
                int top = ByteBuffer.wrap(new byte[]{data1[4], data1[5]}).getShort();
                int bottom = ByteBuffer.wrap(new byte[]{data2[4], data2[5]}).getShort();
                floors = new int[]{bottom, top};
                updateFragmentStatus(floors);
                int index = mViewPager.getCurrentItem();
                switch (index) {
                    case 0:
                        selectedFloor = -1;
                        currentTask = GET_CALL_INSIDE_STATUS;
                        break;
                    case 1:
                        selectedFloor = -1;
                        currentTask = GET_CALL_OUTSIDE_STATUS;
                        break;
                }
            }
            isSyncing = false;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof ParameterSettings) {
                settingsList.add((ParameterSettings) msg.obj);
                receiveCount++;
            }
        }
    }

    private class CallInsideHandler extends UnlockHandler {

        private RealTimeMonitor monitor;

        public String writeCode;

        public int floor;

        public CallInsideHandler(Activity activity) {
            super(activity);
            TAG = CallInsideHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            monitor = null;
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (monitor != null) {
                String receive = SerialUtility.byte2HexStr(monitor.getReceived());
                String checkResult = ParseSerialsUtils.getErrorString(receive);
                if (checkResult != null) {
                    Toast.makeText(CallFloorWindow.this,
                            checkResult,
                            Toast.LENGTH_SHORT)
                            .show();
                } else {
                    if (receive.contains(writeCode)) {
                        // 写入内召日志
                        LogUtils.getInstance().write(ApplicationConfig.LogMoveInside, writeCode, receive, floor);
                    } else {
                        Toast.makeText(CallFloorWindow.this,
                                R.string.call_inside_failed_text,
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                }
                selectedFloor = -1;
                currentTask = GET_CALL_INSIDE_STATUS;
            }
            isSyncing = false;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
                monitor = (RealTimeMonitor) msg.obj;
            }
        }
    }

    private class CallOutsideHandler extends UnlockHandler {

        private RealTimeMonitor monitor;

        public String writeCode;

        public int floor;

        public boolean isUp;

        public CallOutsideHandler(Activity activity) {
            super(activity);
            TAG = CallOutsideHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            monitor = null;
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (monitor != null) {
                String receive = SerialUtility.byte2HexStr(monitor.getReceived());
                String checkResult = ParseSerialsUtils.getErrorString(receive);
                if (checkResult != null) {
                    Toast.makeText(CallFloorWindow.this,
                            checkResult,
                            Toast.LENGTH_SHORT)
                            .show();
                } else {
                    if (receive.contains(writeCode)) {
                        // 写入外召日志
                        LogUtils.getInstance().write(ApplicationConfig.LogMoveOutside,
                                writeCode,
                                receive,
                                floor, isUp ? 1 : 2);
                    } else {
                        Toast.makeText(CallFloorWindow.this,
                                R.string.call_outside_failed_text,
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                }
                selectedFloor = -1;
                currentTask = GET_CALL_OUTSIDE_STATUS;
            }
            isSyncing = false;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
                monitor = (RealTimeMonitor) msg.obj;
            }
        }

    }

    private class GetCallInsideStatusHandler extends UnlockHandler {

        public int sendCount;

        private int receiveCount;

        private List<RealTimeMonitor> monitorList;

        public GetCallInsideStatusHandler(Activity activity) {
            super(activity);
            TAG = GetCallInsideStatusHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
            monitorList = new ArrayList<RealTimeMonitor>();
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (sendCount == receiveCount) {
                List<Integer> calledList = new ArrayList<Integer>();
                int currentFloor = Math.min(floors[0], floors[1]);
                for (RealTimeMonitor monitor : monitorList) {
                    if (monitor.getStateID() == ApplicationConfig.CurrentFloorType) {
                        currentFloor = ParseSerialsUtils.getIntFromBytes(monitor.getReceived());
                    } else {
                        boolean[] booleanArray = ParseSerialsUtils.getBooleanValueArray(new byte[]{
                                monitor.getReceived()[5]
                        });
                        String[] scopes = monitor.getScope().split("~");
                        int min = Integer.parseInt(scopes[0]);
                        for (int i = 0; i < booleanArray.length; i++) {
                            if (booleanArray[i]) {
                                calledList.add(min + i);
                            }
                        }
                    }
                }
                int[] calledFloor = Ints.toArray(calledList);
                handlerCallInsideStatus(currentFloor, calledFloor);
            }
            currentTask = GET_CALL_INSIDE_STATUS;
            isSyncing = false;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof ObjectListHolder) {
                monitorList.addAll(((ObjectListHolder) msg.obj).getRealTimeMonitorList());
                receiveCount++;
            }
        }

    }

    private class GetCallOutsideStatusHandler extends UnlockHandler {

        public int sendCount;

        private int receiveCount;

        private List<RealTimeMonitor> monitorList;

        public GetCallOutsideStatusHandler(Activity activity) {
            super(activity);
            TAG = GetCallOutsideStatusHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
            monitorList = new ArrayList<RealTimeMonitor>();
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof ObjectListHolder) {
                monitorList.addAll(((ObjectListHolder) msg.obj).getRealTimeMonitorList());
                receiveCount++;
            }
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (sendCount == receiveCount) {
                int currentFloor = Math.min(floors[0], floors[1]);
                int size = monitorList.size();
                List<Integer[]> statusList = new ArrayList<Integer[]>();
                for (int m = 0; m < size; m++) {
                    RealTimeMonitor monitor = monitorList.get(m);
                    if (monitor.getStateID() == ApplicationConfig.CurrentFloorType) {
                        currentFloor = ParseSerialsUtils.getIntFromBytes(monitor.getReceived());
                    } else {
                        boolean[] result = ParseSerialsUtils.getBooleanValueArray(new byte[]{
                                monitor.getReceived()[5]
                        });
                        if (result.length == 8) {
                            String[] scopes = monitor.getScope().split("~");
                            int min = Integer.parseInt(scopes[0]);
                            for (int n = 0; n < 4; n++) {
                                int isUp = result[n * 2] ? 1 : 0;
                                int isDown = result[n * 2 + 1] ? 1 : 0;
                                // 被上召或者下召
                                if (isUp == 1 || isDown == 1) {
                                    Integer[] status = new Integer[]{min + n, isUp, isDown};
                                    statusList.add(status);
                                }
                            }
                        }
                    }
                }
                handlerCallOutsideStatus(currentFloor, statusList);
            }
            currentTask = GET_CALL_OUTSIDE_STATUS;
            isSyncing = false;
        }
    }

    private class SortComparator implements Comparator<RealTimeMonitor> {

        @Override
        public int compare(RealTimeMonitor object1, RealTimeMonitor object2) {
            if (object1.getSort() < object2.getSort()) {
                return -1;
            } else if (object1.getSort() > object2.getSort()) {
                return 1;
            } else {
                return 0;
            }
        }

    }
}