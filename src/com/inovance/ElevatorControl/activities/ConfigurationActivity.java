package com.inovance.elevatorcontrol.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.inovance.bluetoothtool.BluetoothTalk;
import com.inovance.bluetoothtool.BluetoothTool;
import com.inovance.bluetoothtool.SerialUtility;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.adapters.ConfigurationAdapter;
import com.inovance.elevatorcontrol.adapters.ParameterStatusAdapter;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.daos.ParameterSettingsDao;
import com.inovance.elevatorcontrol.daos.RealTimeMonitorDao;
import com.inovance.elevatorcontrol.factory.ParameterFactory;
import com.inovance.elevatorcontrol.handlers.ConfigurationHandler;
import com.inovance.elevatorcontrol.handlers.UnlockHandler;
import com.inovance.elevatorcontrol.models.ObjectListHolder;
import com.inovance.elevatorcontrol.models.ParameterSettings;
import com.inovance.elevatorcontrol.models.ParameterStatusItem;
import com.inovance.elevatorcontrol.models.RealTimeMonitor;
import com.inovance.elevatorcontrol.utils.LogUtils;
import com.inovance.elevatorcontrol.utils.ParseSerialsUtils;
import com.inovance.elevatorcontrol.views.fragments.ConfigurationFragment;
import com.viewpagerindicator.TabPageIndicator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import butterknife.InjectView;
import butterknife.Views;

/**
 * 标签卡 电梯调试
 *
 * @author jch
 */
public class ConfigurationActivity extends FragmentActivity implements Runnable {

    private static final String TAG = ConfigurationActivity.class.getSimpleName();

    /**
     * 当前 Viewpager Index
     */
    public int pageIndex;

    /**
     * 获取实时状态 Handler
     */
    private ConfigurationHandler configurationHandler;

    /**
     * 获取实时状态通信内容
     */
    private BluetoothTalk[] getRealTimeStateCommunications;

    /**
     * 获取高压输入端子值 Handler
     */
    private GetHVInputTerminalValueHandler getHVInputTerminalValueHandler;

    /**
     * 获取高压输入端子状态 Handler
     */
    private GetHVInputTerminalStateHandler getHVInputTerminalStateHandler;

    /**
     * 获取高压输入端子值通信内容
     */
    private BluetoothTalk[] getHVInputTerminalValueCommunications;

    /**
     * 获取高压输入端子状态通信内容
     */
    private BluetoothTalk[] getHVInputTerminalStateCommunications;

    /**
     * 获取输入端子值 Handler
     */
    private GetInputTerminalValueHandler getInputTerminalValueHandler;

    /**
     * 获取输入端子状态 Handler
     */
    private GetInputTerminalStateHandler getInputTerminalStateHandler;

    /**
     * 获取输入端子值通信内容
     */
    private BluetoothTalk[] getInputTerminalValueCommunications;

    /**
     * 获取输入端子状态通信内容
     */
    private BluetoothTalk[] getInputTerminalStateCommunications;

    /**
     * 读取电梯状态 Handler
     */
    private ElevatorStatusHandler getElevatorStatusHandler;

    /**
     * 读取电梯运行状态通信内容
     */
    private BluetoothTalk[] getElevatorStateCommunications;

    /**
     * 恢复出厂参数设置 Handler
     */
    private RestoreFactoryHandler restoreFactoryHandler;

    /**
     * 恢复电梯状态通信内容
     */
    private BluetoothTalk[] restoreElevatorCommunications;

    /**
     * 获取输出端子值 Handler
     */
    private GetOutputTerminalValueHandler getOutputTerminalValueHandler;

    /**
     * 获取输出端子状态 Handler
     */
    private GetOutputTerminalStateHandler getOutputTerminalStateHandler;

    /**
     * 获取输出端子值通信内容
     */
    private BluetoothTalk[] getOutputTerminalValueCommunications;

    /**
     * 获取输出端子状态通信内容
     */
    private BluetoothTalk[] getOutputTerminalStateCommunications;

    /**
     * 获取系统状态 Handler
     */
    private GetSystemStateHandler getSystemStateHandler;

    /**
     * 获取系统状态通信内容
     */
    private BluetoothTalk[] getSystemStateCommunications;

    /**
     * 获取轿顶板输入状态
     */
    private GetCeilingInputStateHandler getCeilingInputStateHandler;

    /**
     * 获取轿顶板输入状态通信内容
     */
    private BluetoothTalk[] getCeilingInputStateCommunications;

    /**
     * 获取轿顶板输出状态
     */
    private GetCeilingOutputStateHandler getCeilingOutputStateHandler;

    /**
     * 获取轿顶板输出状态通信内容
     */
    private BluetoothTalk[] getCeilingOutputStateCommunications;

    /**
     * 读取等待信息
     */
    private TextView waitTextView;

    /**
     * 端子状态信息列表
     */
    private ListView terminalListView;

    /**
     * 同步实时状态 Task
     */
    private Runnable syncTask;

    /**
     * 当前 Loop 是否运行
     */
    private boolean isRunning;

    /**
     * 同步 Handler 用于不断循环读取
     */
    private Handler syncHandler = new Handler();

    /**
     * 同步时间间隔
     */
    private static final int SYNC_TIME = 3000;

    /**
     * 是否正在同步实时状态
     */
    public boolean isSyncing = false;

    private static final int NO_TASK = -1;

    /**
     * 读取实时状态
     */
    private static final int GET_MONITOR_STATE = 1;

    /**
     * 读取高压输入端子值
     */
    private static final int GET_HV_INPUT_TERMINAL_VALUE = 2;

    /**
     * 读取高压输入端子状态
     */
    private static final int GET_HV_INPUT_TERMINAL_STATE = 3;

    /**
     * 读取输入端子值
     */
    private static final int GET_INPUT_TERMINAL_VALUE = 4;

    /**
     * 读取输入端子状态
     */
    private static final int GET_INPUT_TERMINAL_STATE = 5;

    /**
     * 读取输出端子值
     */
    private static final int GET_OUTPUT_TERMINAL_VALUE = 6;

    /**
     * 读取输出端子状态
     */
    private static final int GET_OUTPUT_TERMINAL_STATE = 7;

    /**
     * 读取系统状态
     */
    private static final int GET_SYSTEM_STATE = 8;

    /**
     * 读取轿顶板输入状态
     */
    private static final int GET_CEILING_OUTPUT_STATE = 9;

    /**
     * 读取轿顶板输出状态
     */
    private static final int GET_CEILING_INPUT_STATE = 10;

    /**
     * 读取电梯运行状态
     */
    private static final int GET_ELEVATOR_STATE = 11;

    /**
     * 恢复电梯出厂状态
     */
    private static final int RESTORE_ELEVATOR_FACTORY = 12;

    /**
     * 当前执行的任务
     */
    private int currentTask;

    private ExecutorService pool = Executors.newSingleThreadExecutor();

    /**
     * View Pager
     */
    @InjectView(R.id.pager)
    public ViewPager pager;

    /**
     * View Pager Indicator
     */
    @InjectView(R.id.indicator)
    protected TabPageIndicator indicator;

    /**
     * View Pager Adapter
     */
    public ConfigurationAdapter mConfigurationAdapter;

    /**
     * 用于通信的实时监控列表
     */
    private List<RealTimeMonitor> talkStateList = new ArrayList<RealTimeMonitor>();

    /**
     * 用于显示的实时监控列表
     */
    public List<RealTimeMonitor> showStateList = new ArrayList<RealTimeMonitor>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        Views.inject(this);
        // 从数据库加载数据
        reloadDataFromDataBase();
        mConfigurationAdapter = new ConfigurationAdapter(this, showStateList);
        // 同步高压输入端子状态
        getHVInputTerminalValueHandler = new GetHVInputTerminalValueHandler(this);
        getHVInputTerminalStateHandler = new GetHVInputTerminalStateHandler(this);
        // 同步输入端子状态
        getInputTerminalValueHandler = new GetInputTerminalValueHandler(this);
        getInputTerminalStateHandler = new GetInputTerminalStateHandler(this);
        // 同步输出端子状态
        getOutputTerminalValueHandler = new GetOutputTerminalValueHandler(this);
        getOutputTerminalStateHandler = new GetOutputTerminalStateHandler(this);
        // 同步系统状态
        getSystemStateHandler = new GetSystemStateHandler(this);
        // 同步轿顶板输入状态
        getCeilingInputStateHandler = new GetCeilingInputStateHandler(this);
        // 同步轿顶板输出状态
        getCeilingOutputStateHandler = new GetCeilingOutputStateHandler(this);
        // 读取电梯状态
        getElevatorStatusHandler = new ElevatorStatusHandler(this);
        // 恢复出厂设置
        restoreFactoryHandler = new RestoreFactoryHandler(this);
        pager.setAdapter(mConfigurationAdapter);
        pager.setOffscreenPageLimit(4);
        indicator.setViewPager(pager);
        configurationHandler = new ConfigurationHandler(this);
        indicator.setOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int arg0) {

            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {

            }

            @Override
            public void onPageSelected(int index) {
                pageIndex = index;
            }
        });
        // 同步实时状态
        syncTask = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    if (BluetoothTool.getInstance().isPrepared()) {
                        if (!isSyncing) {
                            pool.execute(ConfigurationActivity.this);
                        }
                        syncHandler.postDelayed(syncTask, SYNC_TIME);
                    }
                }
            }
        };
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

    @Override
    protected void onResume() {
        super.onResume();
        if (BluetoothTool.getInstance().isPrepared()) {
            isRunning = true;
            isSyncing = false;
            currentTask = GET_MONITOR_STATE;
            syncHandler.postDelayed(syncTask, SYNC_TIME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isRunning = false;
    }

    /**
     * Change Pager Index
     *
     * @param index Index
     */
    public void changePagerIndex(int index) {
        if (index != pager.getCurrentItem()) {
            pager.setCurrentItem(index);
        }
    }

    public void reSyncData() {
        getRealTimeStateCommunications = null;
        getHVInputTerminalValueCommunications = null;
        getHVInputTerminalStateCommunications = null;
        getInputTerminalValueCommunications = null;
        getInputTerminalStateCommunications = null;
        getOutputTerminalValueCommunications = null;
        getOutputTerminalStateCommunications = null;
        getSystemStateCommunications = null;
        getCeilingInputStateCommunications = null;
        getCeilingOutputStateCommunications = null;
        getElevatorStateCommunications = null;
        restoreElevatorCommunications = null;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ConfigurationFragment monitorFragment = mConfigurationAdapter.getItem(0);
                if (monitorFragment != null) {
                    reloadDataFromDataBase();
                    monitorFragment.reloadDataSource(showStateList);
                }
                ConfigurationFragment groupFragment = mConfigurationAdapter.getItem(1);
                if (groupFragment != null) {
                    groupFragment.reloadDataSource();
                }
            }
        });
        isSyncing = false;
        currentTask = GET_MONITOR_STATE;
    }

    /**
     * 重新从数据库加载数据
     */
    private void reloadDataFromDataBase() {
        talkStateList = RealTimeMonitorDao.findAllByStateIDs(this, ApplicationConfig.MonitorStateCode);
        // 输入端子 ID
        int inputStateID = ApplicationConfig.MonitorStateCode[5];
        // 输出端子 ID
        int outputStateID = ApplicationConfig.MonitorStateCode[6];
        List<RealTimeMonitor> tempInputMonitor = new ArrayList<RealTimeMonitor>();
        List<RealTimeMonitor> tempOutputMonitor = new ArrayList<RealTimeMonitor>();
        showStateList = new ArrayList<RealTimeMonitor>();
        for (RealTimeMonitor item : talkStateList) {
            if (item.getStateID() == inputStateID) {
                tempInputMonitor.add(item);
            } else if (item.getStateID() == outputStateID) {
                tempOutputMonitor.add(item);
            } else {
                showStateList.add(item);
            }
        }
        // 根据 Sort 排序
        Collections.sort(tempInputMonitor, new SortComparator());
        Collections.sort(tempOutputMonitor, new SortComparator());
        // 取得输入、输出端子位置索引
        if (tempInputMonitor.size() > 0) {
            showStateList.add(tempInputMonitor.get(0));
        }
        if (tempOutputMonitor.size() > 0) {
            showStateList.add(tempOutputMonitor.get(0));
        }
    }

    /**
     * 恢复出厂设置
     */
    public void restoreFactory() {
        isSyncing = false;
        currentTask = GET_ELEVATOR_STATE;
    }

    /**
     * 读取电梯状态
     */
    private void getElevatorState() {
        if (getElevatorStateCommunications == null) {
            final RealTimeMonitor monitor = RealTimeMonitorDao.findByStateID(this, ApplicationConfig.RunningStatusType);
            if (monitor != null) {
                getElevatorStateCommunications = new BluetoothTalk[]{
                        new BluetoothTalk() {
                            @Override
                            public void beforeSend() {
                                this.setSendBuffer(SerialUtility.crc16("0103"
                                        + monitor.getCode()
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
                                    byte[] data = SerialUtility.trimEnd(getReceivedBuffer());
                                    if (data.length == 8) {
                                        monitor.setReceived(data);
                                        return monitor;
                                    }
                                }
                                return null;
                            }
                        }
                };
            }
        }
        if (getElevatorStateCommunications != null) {
            if (BluetoothTool.getInstance().isPrepared()) {
                BluetoothTool.getInstance()
                        .setCommunications(getElevatorStateCommunications)
                        .setHandler(getElevatorStatusHandler)
                        .startTask();
            }
        }
    }

    /**
     * 已经读取到电梯运行状态
     *
     * @param monitor RealTimeMonitor
     */
    private void onGetElevatorState(RealTimeMonitor monitor) {
        int state = ParseSerialsUtils.getElevatorStatus(monitor);
        // 电梯停机状态
        if (state == 3) {
            isSyncing = false;
            currentTask = RESTORE_ELEVATOR_FACTORY;
        } else {
            isSyncing = false;
            currentTask = NO_TASK;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.cannot_restore_elevator_factory, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * 写入恢复出厂参数
     */
    private void startRestoreElevatorFactory() {
        if (restoreElevatorCommunications == null) {
            final RealTimeMonitor monitor = RealTimeMonitorDao.findByStateID(this, ApplicationConfig.RestoreFactoryStateCode);
            if (monitor != null) {
                restoreElevatorCommunications = new BluetoothTalk[]{
                        new BluetoothTalk() {
                            @Override
                            public void beforeSend() {

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
                                    // 写入恢复出厂设置日志
                                    LogUtils.getInstance().write(ApplicationConfig.LogRestoreFactory,
                                            SerialUtility.byte2HexStr(getSendBuffer()),
                                            SerialUtility.byte2HexStr(received));
                                    monitor.setReceived(received);
                                    return monitor;
                                }
                                return null;
                            }
                        }
                };
            }
        }
        if (restoreElevatorCommunications != null) {
            if (BluetoothTool.getInstance().isPrepared()) {
                BluetoothTool.getInstance()
                        .setCommunications(restoreElevatorCommunications)
                        .setHandler(restoreFactoryHandler)
                        .startTask();
            }
        }
    }

    /**
     * 读取实时状态
     */
    public void getRealTimeMonitorState() {
        if (getRealTimeStateCommunications == null) {
            getRealTimeStateCommunications = new BluetoothTalk[talkStateList.size()];
            int commandSize = talkStateList.size();
            for (int index = 0; index < commandSize; index++) {
                final String code = talkStateList.get(index).getCode();
                final RealTimeMonitor monitor = talkStateList.get(index);
                getRealTimeStateCommunications[index] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16("0103"
                                + code
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
                            byte[] received = SerialUtility.trimEnd(getReceivedBuffer());
                            monitor.setReceived(received);
                            return monitor;
                        }
                        return null;
                    }
                };
            }
        }
        if (BluetoothTool.getInstance().isPrepared()) {
            isSyncing = true;
            configurationHandler.sendCount = getRealTimeStateCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(configurationHandler)
                    .setCommunications(getRealTimeStateCommunications)
                    .startTask();
        }
    }

    /**
     * 查看高压输入端子状态
     *
     * @param index RealTimeMonitor index
     */
    public void viewHVInputTerminalStatus(int index) {
        showTerminalStatusDialog(showStateList.get(index));
        isSyncing = false;
        getHVInputTerminalValueHandler.index = index;
        currentTask = GET_HV_INPUT_TERMINAL_VALUE;
    }

    /**
     * 读取高压输入端子值
     */
    public void getHVInputTerminalValue() {
        if (getHVInputTerminalValueCommunications == null) {
            final RealTimeMonitor monitor = RealTimeMonitorDao.findByStateID(this, ApplicationConfig.MonitorStateCode[14]);
            if (monitor != null) {
                getHVInputTerminalValueCommunications = new BluetoothTalk[1];
                getHVInputTerminalValueCommunications[0] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16("0103"
                                + monitor.getCode()
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
                            byte[] received = SerialUtility.trimEnd(getReceivedBuffer());
                            monitor.setReceived(received);
                            return monitor;
                        }
                        return null;
                    }
                };
            }
        }
        if (BluetoothTool.getInstance().isPrepared()) {
            isSyncing = true;
            getHVInputTerminalValueHandler.sendCount = getHVInputTerminalValueCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(getHVInputTerminalValueHandler)
                    .setCommunications(getHVInputTerminalValueCommunications)
                    .startTask();
        }
    }

    /**
     * 开始读取高压输入端子状态通信
     */
    private void getHVInputTerminalState() {
        if (getHVInputTerminalStateCommunications == null) {
            final List<ParameterSettings> terminalList = ParameterSettingsDao.findByType(this, ApplicationConfig.HVInputTerminalType);
            final int size = terminalList.size();
            final int count = size <= 10 ? 1 : ((size - size % 10) / 10 + (size % 10 == 0 ? 0 : 1));
            getHVInputTerminalStateCommunications = new BluetoothTalk[count];
            for (int i = 0; i < count; i++) {
                final int position = i;
                final ParameterSettings firstItem = terminalList.get(position * 10);
                final int length = size <= 10 ? size : (size % 10 == 0 ? 10 : ((position == count - 1) ? size % 10 : 10));
                getHVInputTerminalStateCommunications[i] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16("0103"
                                + ParseSerialsUtils.getCalculatedCode(firstItem)
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
                                List<ParameterSettings> tempList = new ArrayList<ParameterSettings>();
                                for (int j = 0; j < length; j++) {
                                    if (position * 10 + j < terminalList.size()) {
                                        ParameterSettings item = terminalList.get(position * 10 + j);
                                        byte[] tempData = SerialUtility.crc16("01030002"
                                                + SerialUtility.byte2HexStr(new byte[]{data[4 + j * 2], data[5 + j * 2]}));
                                        item.setReceived(tempData);
                                        tempList.add(item);
                                    }
                                }
                                ObjectListHolder holder = new ObjectListHolder();
                                holder.setParameterSettingsList(tempList);
                                return holder;
                            }
                        }
                        return null;
                    }
                };
            }
        }
        if (BluetoothTool.getInstance().isPrepared()) {
            isSyncing = true;
            getHVInputTerminalStateHandler.sendCount = getHVInputTerminalStateCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(getHVInputTerminalStateHandler)
                    .setCommunications(getHVInputTerminalStateCommunications)
                    .startTask();
        }
    }

    /**
     * 查看输入端子状态
     *
     * @param index List view item index
     */
    public void viewInputTerminalStatus(int index) {
        showTerminalStatusDialog(showStateList.get(index));
        isSyncing = false;
        getInputTerminalValueHandler.index = index;
        currentTask = GET_INPUT_TERMINAL_VALUE;
    }

    /**
     * 读取输入端子值
     */
    public void getInputTerminalValue() {
        if (getInputTerminalValueCommunications == null) {
            List<RealTimeMonitor> monitorList = RealTimeMonitorDao.findAllByStateID(this,
                    ApplicationConfig.MonitorStateCode[5]);
            Collections.sort(monitorList, new SortComparator());
            int size = monitorList.size();
            getInputTerminalValueCommunications = new BluetoothTalk[size];
            for (int index = 0; index < size; index++) {
                final String code = monitorList.get(index).getCode();
                final RealTimeMonitor monitor = monitorList.get(index);
                getInputTerminalValueCommunications[index] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16("0103"
                                + code
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
                            byte[] received = SerialUtility.trimEnd(getReceivedBuffer());
                            monitor.setReceived(received);
                            return monitor;
                        }
                        return null;
                    }
                };
            }
        }
        if (BluetoothTool.getInstance().isPrepared()) {
            isSyncing = true;
            getInputTerminalValueHandler.sendCount = getInputTerminalValueCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(getInputTerminalValueHandler)
                    .setCommunications(getInputTerminalValueCommunications)
                    .startTask();
        }
    }

    /**
     * 开始读取输入端子状态通信
     */
    private void getInputTerminalState() {
        if (getInputTerminalStateCommunications == null) {
            final List<ParameterSettings> terminalList = ParameterSettingsDao.findByType(this,
                    ApplicationConfig.InputTerminalType);
            final int size = terminalList.size();
            final int count = size <= 10 ? 1 : ((size - size % 10) / 10 + (size % 10 == 0 ? 0 : 1));
            getInputTerminalStateCommunications = new BluetoothTalk[count];
            for (int i = 0; i < count; i++) {
                final int position = i;
                final ParameterSettings firstItem = terminalList.get(position * 10);
                final int length = size <= 10 ? size : (size % 10 == 0 ? 10 : ((position == count - 1) ? size % 10 : 10));
                getInputTerminalStateCommunications[i] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16("0103"
                                + ParseSerialsUtils.getCalculatedCode(firstItem)
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
                                List<ParameterSettings> tempList = new ArrayList<ParameterSettings>();
                                for (int j = 0; j < length; j++) {
                                    if (position * 10 + j < terminalList.size()) {
                                        ParameterSettings item = terminalList.get(position * 10 + j);
                                        byte[] tempData = SerialUtility.crc16("01030002"
                                                + SerialUtility.byte2HexStr(new byte[]{data[4 + j * 2], data[5 + j * 2]}));
                                        item.setReceived(tempData);
                                        tempList.add(item);
                                    }
                                }
                                ObjectListHolder holder = new ObjectListHolder();
                                holder.setParameterSettingsList(tempList);
                                return holder;
                            }
                        }
                        return null;
                    }
                };
            }
        }
        if (BluetoothTool.getInstance().isPrepared()) {
            isSyncing = true;
            getInputTerminalStateHandler.sendCount = getInputTerminalStateCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(getInputTerminalStateHandler)
                    .setCommunications(getInputTerminalStateCommunications)
                    .startTask();
        }
    }

    /**
     * 查看输出端子状态
     *
     * @param index RealTimeMonitor index
     */
    public void viewOutputTerminalStatus(int index) {
        showTerminalStatusDialog(showStateList.get(index));
        isSyncing = false;
        getOutputTerminalValueHandler.index = index;
        currentTask = GET_OUTPUT_TERMINAL_VALUE;
    }

    /**
     * 读取输出端子值
     */
    public void getOutputTerminalValue() {
        if (getOutputTerminalValueCommunications == null) {
            List<RealTimeMonitor> monitorList = RealTimeMonitorDao.findAllByStateID(this,
                    ApplicationConfig.MonitorStateCode[6]);
            Collections.sort(monitorList, new SortComparator());
            int size = monitorList.size();
            getOutputTerminalValueCommunications = new BluetoothTalk[size];
            for (int index = 0; index < size; index++) {
                final String code = monitorList.get(index).getCode();
                final RealTimeMonitor monitor = monitorList.get(index);
                getOutputTerminalValueCommunications[index] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16("0103"
                                + code
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
                            byte[] received = SerialUtility.trimEnd(getReceivedBuffer());
                            monitor.setReceived(received);
                            return monitor;
                        }
                        return null;
                    }
                };
            }
        }
        if (BluetoothTool.getInstance().isPrepared()) {
            isSyncing = true;
            getOutputTerminalValueHandler.sendCount = getOutputTerminalValueCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(getOutputTerminalValueHandler)
                    .setCommunications(getOutputTerminalValueCommunications)
                    .startTask();
        }
    }

    /**
     * 开始读取输出端子状态通信
     */
    private void getOutputTerminalState() {
        if (getOutputTerminalStateCommunications == null) {
            final List<ParameterSettings> terminalList = ParameterSettingsDao.findByType(this, ApplicationConfig.OutputTerminalType);
            final int size = terminalList.size();
            final int count = size <= 10 ? 1 : ((size - size % 10) / 10 + (size % 10 == 0 ? 0 : 1));
            getOutputTerminalStateCommunications = new BluetoothTalk[count];
            for (int i = 0; i < count; i++) {
                final int position = i;
                final ParameterSettings firstItem = terminalList.get(position * 10);
                final int length = size <= 10 ? size : (size % 10 == 0 ? 10 : ((position == count - 1) ? size % 10 : 10));
                getOutputTerminalStateCommunications[i] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16("0103"
                                + ParseSerialsUtils.getCalculatedCode(firstItem)
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
                                List<ParameterSettings> tempList = new ArrayList<ParameterSettings>();
                                for (int j = 0; j < length; j++) {
                                    if (position * 10 + j < terminalList.size()) {
                                        ParameterSettings item = terminalList.get(position * 10 + j);
                                        byte[] tempData = SerialUtility.crc16("01030002"
                                                + SerialUtility.byte2HexStr(new byte[]{data[4 + j * 2], data[5 + j * 2]}));
                                        item.setReceived(tempData);
                                        tempList.add(item);
                                    }
                                }
                                ObjectListHolder holder = new ObjectListHolder();
                                holder.setParameterSettingsList(tempList);
                                return holder;
                            }
                        }
                        return null;
                    }
                };
            }
        }
        if (BluetoothTool.getInstance().isPrepared()) {
            isSyncing = true;
            getOutputTerminalStateHandler.sendCount = getOutputTerminalStateCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(getOutputTerminalStateHandler)
                    .setCommunications(getOutputTerminalStateCommunications)
                    .startTask();
        }
    }

    /**
     * 获取系统状态
     */
    private void getSystemState() {
        if (getSystemStateCommunications == null) {
            final RealTimeMonitor monitor = RealTimeMonitorDao.findByStateID(this, ApplicationConfig.MonitorStateCode[12]);
            if (monitor != null) {
                getSystemStateCommunications = new BluetoothTalk[]{new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16("0103"
                                + monitor.getCode()
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
                            byte[] received = SerialUtility.trimEnd(getReceivedBuffer());
                            monitor.setReceived(received);
                            return monitor;
                        }
                        return null;
                    }
                }};
            }
        }
        if (BluetoothTool.getInstance().isPrepared()) {
            isSyncing = true;
            getSystemStateHandler.sendCount = getSystemStateCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(getSystemStateHandler)
                    .setCommunications(getSystemStateCommunications)
                    .startTask();
        }
    }

    /**
     * 查看系统状态
     *
     * @param index RealTimeMonitor index
     */
    public void viewSystemTerminalStatus(int index) {
        showTerminalStatusDialog(showStateList.get(index));
        isSyncing = false;
        currentTask = GET_SYSTEM_STATE;
    }

    /**
     * 获取轿顶板输入状态
     */
    private void getCeilingInputState() {
        if (getCeilingInputStateCommunications == null) {
            final RealTimeMonitor monitor = RealTimeMonitorDao.findByStateID(this, ApplicationConfig.MonitorStateCode[10]);
            if (monitor != null) {
                getCeilingInputStateCommunications = new BluetoothTalk[]{new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16("0103"
                                + monitor.getCode()
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
                            byte[] received = SerialUtility.trimEnd(getReceivedBuffer());
                            monitor.setReceived(received);
                            return monitor;
                        }
                        return null;
                    }
                }};
            }
        }
        if (BluetoothTool.getInstance().isPrepared()) {
            isSyncing = true;
            getCeilingInputStateHandler.sendCount = getCeilingInputStateCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(getCeilingInputStateHandler)
                    .setCommunications(getCeilingInputStateCommunications)
                    .startTask();
        }
    }

    /**
     * 查看轿顶板输入状态
     *
     * @param index RealTimeMonitor index
     */
    public void viewCeilingInputStatus(int index) {
        showTerminalStatusDialog(showStateList.get(index));
        isSyncing = false;
        currentTask = GET_CEILING_INPUT_STATE;
    }

    /**
     * 获取轿顶板输出状态
     */
    private void getCeilingOutputState() {
        if (getCeilingOutputStateCommunications == null) {
            final RealTimeMonitor monitor = RealTimeMonitorDao.findByStateID(this, ApplicationConfig.MonitorStateCode[11]);
            if (monitor != null) {
                getCeilingOutputStateCommunications = new BluetoothTalk[]{new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16("0103"
                                + monitor.getCode()
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
                            byte[] received = SerialUtility.trimEnd(getReceivedBuffer());
                            monitor.setReceived(received);
                            return monitor;
                        }
                        return null;
                    }
                }};
            }
        }
        if (BluetoothTool.getInstance().isPrepared()) {
            isSyncing = true;
            getCeilingOutputStateHandler.sendCount = getCeilingOutputStateCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(getCeilingOutputStateHandler)
                    .setCommunications(getCeilingOutputStateCommunications)
                    .startTask();
        }
    }

    /**
     * 查看轿顶板输出状态
     *
     * @param index RealTimeMonitor index
     */
    public void viewCeilingOutputStatus(int index) {
        showTerminalStatusDialog(showStateList.get(index));
        isSyncing = false;
        currentTask = GET_CEILING_OUTPUT_STATE;
    }

    /**
     * 显示端子状态对话框
     */
    private void showTerminalStatusDialog(RealTimeMonitor monitor) {
        View dialogView = getLayoutInflater().inflate(R.layout.terminal_status_dialog, null);
        waitTextView = (TextView) dialogView.findViewById(R.id.wait_text);
        terminalListView = (ListView) dialogView.findViewById(R.id.list_view);
        AlertDialog.Builder builder = new AlertDialog.Builder(ConfigurationActivity.this,
                R.style.GlobalDialogStyle)
                .setView(dialogView)
                .setTitle(monitor.getName())
                .setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        isSyncing = false;
                        currentTask = GET_MONITOR_STATE;
                        getHVInputTerminalStateHandler.statusAdapter = null;
                        getInputTerminalStateHandler.statusAdapter = null;
                        getOutputTerminalStateHandler.statusAdapter = null;
                        getSystemStateHandler.statusAdapter = null;
                        getCeilingInputStateHandler.statusAdapter = null;
                        getCeilingOutputStateHandler.statusAdapter = null;
                    }
                });
        // 端子状态信息 Dialog
        AlertDialog terminalDialog = builder.create();
        terminalDialog.show();
        terminalDialog.setCancelable(false);
        terminalDialog.setCanceledOnTouchOutside(false);
    }

    @Override
    public void run() {
        switch (currentTask) {
            // 读取实时状态值
            case GET_MONITOR_STATE:
                getRealTimeMonitorState();
                break;
            // 读取高压输入端子值
            case GET_HV_INPUT_TERMINAL_VALUE:
                getHVInputTerminalValue();
                break;
            // 读取高压输入端子状态
            case GET_HV_INPUT_TERMINAL_STATE:
                getHVInputTerminalState();
                break;
            // 读取输入端子值
            case GET_INPUT_TERMINAL_VALUE:
                getInputTerminalValue();
                break;
            // 读取输入端子状态
            case GET_INPUT_TERMINAL_STATE:
                getInputTerminalState();
                break;
            // 读取输出端子值
            case GET_OUTPUT_TERMINAL_VALUE:
                getOutputTerminalValue();
                break;
            // 读取输出端子状态
            case GET_OUTPUT_TERMINAL_STATE:
                getOutputTerminalState();
                break;
            // 读取系统状态
            case GET_SYSTEM_STATE:
                getSystemState();
                break;
            // 读取轿顶板输入状态
            case GET_CEILING_INPUT_STATE:
                getCeilingInputState();
                break;
            // 读取轿顶板输出状态
            case GET_CEILING_OUTPUT_STATE:
                getCeilingOutputState();
                break;
            // 读取电梯运行状态
            case GET_ELEVATOR_STATE:
                getElevatorState();
                break;
            // 恢复出厂设置
            case RESTORE_ELEVATOR_FACTORY:
                startRestoreElevatorFactory();
                break;
        }
    }

    // ================================ 高压输入端子状态 Handler  ===================================== //

    private class GetHVInputTerminalStateHandler extends UnlockHandler {

        /**
         * 发送的指令数
         */
        public int sendCount;

        /**
         * 接收到得指令数
         */
        private int receiveCount;

        public RealTimeMonitor monitor;

        public ParameterStatusAdapter statusAdapter;

        private List<ObjectListHolder> holderList;

        public GetHVInputTerminalStateHandler(Activity activity) {
            super(activity);
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
            holderList = new ArrayList<ObjectListHolder>();
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (sendCount == receiveCount) {
                if (ConfigurationActivity.this.waitTextView != null
                        && ConfigurationActivity.this.terminalListView != null) {
                    List<ParameterSettings> settingsList = new ArrayList<ParameterSettings>();
                    for (ObjectListHolder holder : holderList) {
                        settingsList.addAll(holder.getParameterSettingsList());
                    }
                    byte[] newByte = new byte[]{monitor.getReceived()[4], monitor.getReceived()[5]};
                    boolean[] bitValues = ParseSerialsUtils.getBooleanValueArray(newByte);
                    int length = bitValues.length;
                    List<ParameterStatusItem> statusList = new ArrayList<ParameterStatusItem>();
                    for (ParameterSettings settings : settingsList) {
                        int indexValue = ParseSerialsUtils.getIntFromBytes(settings.getReceived());
                        if (indexValue < length && indexValue >= 0) {
                            try {
                                JSONArray jsonArray = new JSONArray(settings.getJSONDescription());
                                int size = jsonArray.length();
                                String[] valueStringArray = new String[size];
                                for (int i = 0; i < size; i++) {
                                    JSONObject value = jsonArray.getJSONObject(i);
                                    valueStringArray[i] = indexValue + ":" + value.optString("value");
                                }
                                if (indexValue < valueStringArray.length) {
                                    ParameterStatusItem item = new ParameterStatusItem();
                                    item.setName(settings.getName().replace("功能选择", "端子   ") + valueStringArray[indexValue]);
                                    item.setStatus(bitValues[indexValue]);
                                    if (indexValue < bitValues.length) {
                                        item.setStatus(bitValues[indexValue]);
                                    }
                                    statusList.add(item);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    // 更新 AlertDialog ListView
                    if (statusAdapter == null) {
                        statusAdapter = new ParameterStatusAdapter(ConfigurationActivity.this, statusList);
                        ConfigurationActivity.this.terminalListView.setAdapter(statusAdapter);
                        ConfigurationActivity.this.waitTextView.setVisibility(View.GONE);
                        ConfigurationActivity.this.terminalListView.setVisibility(View.VISIBLE);
                    } else {
                        statusAdapter.setStatusList(statusList);
                    }
                }
                ConfigurationActivity.this.currentTask = GET_HV_INPUT_TERMINAL_VALUE;
            } else {
                ConfigurationActivity.this.currentTask = GET_HV_INPUT_TERMINAL_STATE;
            }
            ConfigurationActivity.this.isSyncing = false;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof ObjectListHolder) {
                holderList.add((ObjectListHolder) msg.obj);
                receiveCount++;
            }
        }
    }

    // =============================== Get Input Terminal Status Handler ======================================== //

    private class GetInputTerminalStateHandler extends UnlockHandler {

        /**
         * 发送的指令数
         */
        public int sendCount;

        public ParameterStatusAdapter statusAdapter;

        /**
         * 接收到得指令数
         */
        private int receiveCount;

        public RealTimeMonitor monitor;

        private List<ObjectListHolder> holderList;

        public GetInputTerminalStateHandler(android.app.Activity activity) {
            super(activity);
            TAG = GetInputTerminalStateHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
            holderList = new ArrayList<ObjectListHolder>();
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (sendCount == receiveCount) {
                if (ConfigurationActivity.this.waitTextView != null
                        && ConfigurationActivity.this.terminalListView != null) {

                    List<ParameterSettings> settingsList = new ArrayList<ParameterSettings>();
                    for (ObjectListHolder holder : holderList) {
                        settingsList.addAll(holder.getParameterSettingsList());
                    }
                    boolean[] bitValues = new boolean[monitor.getCombineBytes().length * 8];
                    int dataIndex = 0;
                    for (byte data : monitor.getCombineBytes()) {
                        boolean[] valueArray = ParseSerialsUtils.byteToBoolArray(data);
                        System.arraycopy(valueArray, 0, bitValues, dataIndex * 8, valueArray.length);
                        dataIndex++;
                    }
                    // 解析端子状态值
                    List<ParameterStatusItem> statusList = ParameterFactory
                            .getParameter()
                            .getInputTerminalStateList(bitValues, settingsList);
                    // 更新 AlertDialog ListView
                    if (statusAdapter == null) {
                        statusAdapter = new ParameterStatusAdapter(ConfigurationActivity.this, statusList);
                        ConfigurationActivity.this.terminalListView.setAdapter(statusAdapter);
                        ConfigurationActivity.this.waitTextView.setVisibility(View.GONE);
                        ConfigurationActivity.this.terminalListView.setVisibility(View.VISIBLE);
                    } else {
                        statusAdapter.setStatusList(statusList);
                    }
                }
                ConfigurationActivity.this.currentTask = GET_INPUT_TERMINAL_VALUE;
            } else {
                ConfigurationActivity.this.currentTask = GET_INPUT_TERMINAL_STATE;
            }
            ConfigurationActivity.this.isSyncing = false;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof ObjectListHolder) {
                holderList.add((ObjectListHolder) msg.obj);
                receiveCount++;
            }
        }
    }

    // ================================= Get Output Terminal Status Handler ======================================= //

    private class GetOutputTerminalStateHandler extends UnlockHandler {

        public int sendCount;

        private int receiveCount;

        public RealTimeMonitor monitor;

        private List<ObjectListHolder> holderList;

        public ParameterStatusAdapter statusAdapter;

        public GetOutputTerminalStateHandler(android.app.Activity activity) {
            super(activity);
            TAG = GetInputTerminalStateHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
            holderList = new ArrayList<ObjectListHolder>();
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (sendCount == receiveCount) {
                List<ParameterSettings> settingsList = new ArrayList<ParameterSettings>();
                for (ObjectListHolder holder : holderList) {
                    settingsList.addAll(holder.getParameterSettingsList());
                }
                if (ConfigurationActivity.this.waitTextView != null
                        && ConfigurationActivity.this.terminalListView != null) {
                    boolean[] bitValues = new boolean[monitor.getCombineBytes().length * 8];
                    int dataIndex = 0;
                    for (byte data : monitor.getCombineBytes()) {
                        boolean[] valueArray = ParseSerialsUtils.byteToBoolArray(data);
                        System.arraycopy(valueArray, 0, bitValues, dataIndex * 8, valueArray.length);
                        dataIndex++;
                    }
                    List<ParameterStatusItem> statusList = ParameterFactory
                            .getParameter()
                            .getOutputTerminalStateList(bitValues, settingsList);
                    // 更新 AlertDialog ListView
                    if (statusAdapter == null) {
                        statusAdapter = new ParameterStatusAdapter(ConfigurationActivity.this, statusList);
                        ConfigurationActivity.this.terminalListView.setAdapter(statusAdapter);
                        ConfigurationActivity.this.waitTextView.setVisibility(View.GONE);
                        ConfigurationActivity.this.terminalListView.setVisibility(View.VISIBLE);
                    } else {
                        statusAdapter.setStatusList(statusList);
                    }
                }
                ConfigurationActivity.this.currentTask = GET_OUTPUT_TERMINAL_VALUE;
            } else {
                ConfigurationActivity.this.currentTask = GET_OUTPUT_TERMINAL_STATE;
            }
            ConfigurationActivity.this.isSyncing = false;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkError(msg);
            if (msg.obj != null && msg.obj instanceof ObjectListHolder) {
                holderList.add((ObjectListHolder) msg.obj);
                receiveCount++;
            }
        }
    }

    // ================================= Get HV Input Terminal Value Handler ===================================== //

    private class GetHVInputTerminalValueHandler extends UnlockHandler {

        public int sendCount;

        public int index;

        private int receiveCount;

        private RealTimeMonitor monitor;

        public GetHVInputTerminalValueHandler(Activity activity) {
            super(activity);
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
            monitor = null;
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (sendCount == receiveCount && monitor != null) {
                getHVInputTerminalStateHandler.monitor = monitor;
                ConfigurationActivity.this.currentTask = GET_HV_INPUT_TERMINAL_STATE;
            } else {
                ConfigurationActivity.this.currentTask = GET_HV_INPUT_TERMINAL_VALUE;
            }
            ConfigurationActivity.this.isSyncing = false;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
                monitor = (RealTimeMonitor) msg.obj;
                receiveCount++;
            }
        }
    }

    // ================================= Get Input Terminal Value Handler ======================================= //

    private class GetInputTerminalValueHandler extends UnlockHandler {

        public int sendCount;

        public int index;

        private int receiveCount;

        private List<RealTimeMonitor> monitorList = new ArrayList<RealTimeMonitor>();

        public GetInputTerminalValueHandler(Activity activity) {
            super(activity);
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
                if (monitorList.size() > 0) {
                    RealTimeMonitor monitor = monitorList.get(monitorList.size() - 1);
                    monitor.setCombineBytes(ConfigurationHandler.getCombineBytes(monitorList));
                    getInputTerminalStateHandler.monitor = monitor;
                    ConfigurationActivity.this.currentTask = GET_INPUT_TERMINAL_STATE;
                }
            } else {
                ConfigurationActivity.this.currentTask = GET_INPUT_TERMINAL_VALUE;
            }
            ConfigurationActivity.this.isSyncing = false;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
                monitorList.add((RealTimeMonitor) msg.obj);
                receiveCount++;
            }
        }
    }

    // ================================= Get Output Terminal Value Handler ====================================== //

    private class GetOutputTerminalValueHandler extends UnlockHandler {

        public int sendCount;

        public int index;

        private int receiveCount;

        private List<RealTimeMonitor> monitorList = new ArrayList<RealTimeMonitor>();

        public GetOutputTerminalValueHandler(Activity activity) {
            super(activity);
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
                if (monitorList.size() > 0) {
                    RealTimeMonitor monitor = monitorList.get(monitorList.size() - 1);
                    monitor.setCombineBytes(ConfigurationHandler.getCombineBytes(monitorList));
                    getOutputTerminalStateHandler.monitor = monitor;
                    ConfigurationActivity.this.currentTask = GET_OUTPUT_TERMINAL_STATE;
                }
            } else {
                ConfigurationActivity.this.currentTask = GET_OUTPUT_TERMINAL_VALUE;
            }
            ConfigurationActivity.this.isSyncing = false;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
                monitorList.add((RealTimeMonitor) msg.obj);
                receiveCount++;
            }
        }
    }

    // ============================================ Get elevator state handler =================================== //

    private class ElevatorStatusHandler extends UnlockHandler {

        private RealTimeMonitor monitor;

        public ElevatorStatusHandler(Activity activity) {
            super(activity);
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            monitor = null;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
                monitor = (RealTimeMonitor) msg.obj;
                // 读取到电梯运行状态
                ConfigurationActivity.this.onGetElevatorState(monitor);
            }
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
        }
    }

    // ============================================= Restore factory handler ===================================== //

    private class RestoreFactoryHandler extends UnlockHandler {

        public RestoreFactoryHandler(Activity activity) {
            super(activity);
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
                RealTimeMonitor monitor = (RealTimeMonitor) msg.obj;
                String valueString = SerialUtility.byte2HexStr(monitor.getReceived());
                boolean writeSuccessful = true;
                String result = ParseSerialsUtils.getErrorString(valueString);
                if (result != null) {
                    writeSuccessful = false;
                }
                final String tips;
                if (writeSuccessful) {
                    tips = getResources().getString(R.string.restore_factory_successful);
                } else {
                    tips = getResources().getString(R.string.restore_factory_failed);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ConfigurationActivity.this, tips, Toast.LENGTH_SHORT).show();
                    }
                });
                isSyncing = false;
                currentTask = NO_TASK;
            }
        }
    }

    // ====================================== Get system state handler ========================================= //

    private class GetSystemStateHandler extends UnlockHandler {

        public int sendCount;

        private int receiveCount;

        private RealTimeMonitor monitor;

        public ParameterStatusAdapter statusAdapter;

        public GetSystemStateHandler(Activity activity) {
            super(activity);
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
                receiveCount = 1;
                monitor = (RealTimeMonitor) msg.obj;
            }
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (sendCount == receiveCount && monitor != null) {
                byte[] data = monitor.getReceived();
                List<ParameterStatusItem> statusList = new ArrayList<ParameterStatusItem>();
                try {
                    JSONArray valuesArray = new JSONArray(monitor.getJSONDescription());
                    int size = valuesArray.length();
                    Pattern pattern = Pattern.compile("^\\d*\\-\\d*:.*", Pattern.CASE_INSENSITIVE);
                    for (int i = 0; i < size; i++) {
                        JSONObject value = valuesArray.getJSONObject(i);
                        ParameterStatusItem status = new ParameterStatusItem();
                        for (Iterator iterator = value.keys(); iterator.hasNext(); ) {
                            String name = (String) iterator.next();
                            if (name.equalsIgnoreCase("value")) {
                                if (!value.optString("value").contains(ApplicationConfig.RETAIN_NAME)) {
                                    status.setName(value.optString("value"));
                                }
                            }
                            if (name.equalsIgnoreCase("id")) {
                                status.setStatus(ParseSerialsUtils
                                        .getIntValueFromBytesInSection(new byte[]{data[4], data[5]},
                                                new int[]{Integer.parseInt(value.optString("id"))}) == 1);
                            }
                            if (pattern.matcher(name).matches()) {
                                String[] intStringArray = name.split(":")[0].split("-");
                                status.setName(name.replaceAll("\\d*\\-\\d*:", ""));
                                JSONArray subArray = value.optJSONArray(name);
                                int intValue = ParseSerialsUtils
                                        .getIntValueFromBytesInSection(new byte[]{data[4], data[5]}, new int[]{
                                                Integer.parseInt(intStringArray[0]),
                                                Integer.parseInt(intStringArray[1])
                                        });
                                int subArraySize = subArray.length();
                                for (int j = 0; j < subArraySize; j++) {
                                    int index = Integer.parseInt(subArray.getJSONObject(j).optString("id"));
                                    if (index == intValue) {
                                        status.setStatusString(subArray.getJSONObject(j).optString("value"));
                                    }
                                }
                            }
                        }
                        if (status.getName() != null) {
                            statusList.add(status);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (statusAdapter == null) {
                    statusAdapter = new ParameterStatusAdapter(ConfigurationActivity.this, statusList);
                    ConfigurationActivity.this.terminalListView.setAdapter(statusAdapter);
                    ConfigurationActivity.this.waitTextView.setVisibility(View.GONE);
                    ConfigurationActivity.this.terminalListView.setVisibility(View.VISIBLE);
                } else {
                    statusAdapter.setStatusList(statusList);
                }
            }
            ConfigurationActivity.this.currentTask = GET_SYSTEM_STATE;
            ConfigurationActivity.this.isSyncing = false;
        }
    }


    // =================================== Get ceiling input state handler ===================================== //

    private class GetCeilingInputStateHandler extends UnlockHandler {

        public int sendCount;

        private int receiveCount;

        private RealTimeMonitor monitor;

        public ParameterStatusAdapter statusAdapter;

        public GetCeilingInputStateHandler(Activity activity) {
            super(activity);
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
                receiveCount = 1;
                monitor = (RealTimeMonitor) msg.obj;
            }
        }


        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (sendCount == receiveCount && monitor != null) {
                byte[] data = monitor.getReceived();
                List<ParameterStatusItem> statusList = new ArrayList<ParameterStatusItem>();
                boolean[] booleanArray = ParseSerialsUtils.getBooleanValueArray(new byte[]{data[4], data[5]});
                int bitsSize = booleanArray.length;
                try {
                    JSONArray valuesArray = new JSONArray(monitor.getJSONDescription());
                    int size = valuesArray.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject value = valuesArray.getJSONObject(i);
                        if (i < bitsSize) {
                            if (!value.optString("value").contains(ApplicationConfig.RETAIN_NAME)) {
                                ParameterStatusItem status = new ParameterStatusItem();
                                status.setName(value.optString("value"));
                                status.setStatus(booleanArray[Integer.parseInt(value.optString("id"))]);
                                statusList.add(status);
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (statusAdapter == null) {
                    statusAdapter = new ParameterStatusAdapter(ConfigurationActivity.this, statusList);
                    ConfigurationActivity.this.terminalListView.setAdapter(statusAdapter);
                    ConfigurationActivity.this.waitTextView.setVisibility(View.GONE);
                    ConfigurationActivity.this.terminalListView.setVisibility(View.VISIBLE);
                } else {
                    statusAdapter.setStatusList(statusList);
                }
            }
            ConfigurationActivity.this.currentTask = GET_CEILING_INPUT_STATE;
            ConfigurationActivity.this.isSyncing = false;
        }
    }

    // =================================== Get ceiling output state handler ==================================== //

    private class GetCeilingOutputStateHandler extends UnlockHandler {

        public int sendCount;

        private int receiveCount;

        private RealTimeMonitor monitor;

        public ParameterStatusAdapter statusAdapter;

        public GetCeilingOutputStateHandler(Activity activity) {
            super(activity);
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
                receiveCount = 1;
                monitor = (RealTimeMonitor) msg.obj;
            }
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (sendCount == receiveCount && monitor != null) {
                byte[] data = monitor.getReceived();
                List<ParameterStatusItem> statusList = new ArrayList<ParameterStatusItem>();
                boolean[] booleanArray = ParseSerialsUtils.getBooleanValueArray(new byte[]{data[4], data[5]});
                int bitsSize = booleanArray.length;
                try {
                    JSONArray valuesArray = new JSONArray(monitor.getJSONDescription());
                    int size = valuesArray.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject value = valuesArray.getJSONObject(i);
                        if (i < bitsSize) {
                            if (!value.optString("value").contains(ApplicationConfig.RETAIN_NAME)) {
                                ParameterStatusItem status = new ParameterStatusItem();
                                status.setName(value.optString("value"));
                                status.setStatus(booleanArray[Integer.parseInt(value.optString("id"))]);
                                statusList.add(status);
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (statusAdapter == null) {
                    statusAdapter = new ParameterStatusAdapter(ConfigurationActivity.this, statusList);
                    ConfigurationActivity.this.terminalListView.setAdapter(statusAdapter);
                    ConfigurationActivity.this.waitTextView.setVisibility(View.GONE);
                    ConfigurationActivity.this.terminalListView.setVisibility(View.VISIBLE);
                } else {
                    statusAdapter.setStatusList(statusList);
                }
            }
            ConfigurationActivity.this.currentTask = GET_CEILING_OUTPUT_STATE;
            ConfigurationActivity.this.isSyncing = false;
        }
    }
}
