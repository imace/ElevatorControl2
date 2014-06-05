package com.inovance.ElevatorControl.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import butterknife.InjectView;
import butterknife.Views;
import com.bluetoothtool.BluetoothHandler;
import com.bluetoothtool.BluetoothTalk;
import com.bluetoothtool.BluetoothTool;
import com.bluetoothtool.SerialUtility;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.adapters.ConfigurationAdapter;
import com.inovance.ElevatorControl.adapters.ParameterStatusAdapter;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.daos.ParameterSettingsDao;
import com.inovance.ElevatorControl.daos.RealTimeMonitorDao;
import com.inovance.ElevatorControl.handlers.ConfigurationHandler;
import com.inovance.ElevatorControl.models.ObjectListHolder;
import com.inovance.ElevatorControl.models.ParameterSettings;
import com.inovance.ElevatorControl.models.ParameterStatusItem;
import com.inovance.ElevatorControl.models.RealTimeMonitor;
import com.inovance.ElevatorControl.utils.LogUtils;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import com.viewpagerindicator.TabPageIndicator;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 标签卡 电梯调试
 *
 * @author jch
 */
public class ConfigurationActivity extends Activity implements Runnable {

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
    private BluetoothTalk[] communications;

    /**
     * 获取 X 端子状态 Handler
     */
    private GetXTerminalStatusHandler getXTerminalStatusHandler;

    /**
     * 获取 Y 端子状态 Handler
     */
    private GetYTerminalStatusHandler getYTerminalStatusHandler;

    /**
     * 获取 X 端子状态通信内容
     */
    private BluetoothTalk[] getXTerminalCommunications;

    /**
     * 获取 Y 端子状态通信内容
     */
    private BluetoothTalk[] getYTerminalCommunications;

    /**
     * 端子状态信息 Dialog
     */
    private AlertDialog terminalDialog;

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
    private Runnable syncStatusTask;

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
    private static final int SYNC_TIME = 2000;

    /**
     * 读取实时状态
     */
    private static final int GET_SYSTEM_STATUS = 1;

    /**
     * 读取 X 端子状态
     */
    private static final int SEE_X_TERMINAL_STATUS = 2;

    /**
     * 读取 Y 端子状态
     */
    private static final int SEE_Y_TERMINAL_STATUS = 3;

    /**
     * 当前执行的 Task 种类
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
     * 是否正在同步系统状态
     */
    public boolean isSyncingSystemStatus = false;

    /**
     * 是否正在同步端子状态
     */
    private boolean isSyncingTerminalStatus = false;

    /**
     * View Pager Adapter
     */
    public ConfigurationAdapter mConfigurationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        Views.inject(this);
        mConfigurationAdapter = new ConfigurationAdapter(this);
        getXTerminalStatusHandler = new GetXTerminalStatusHandler(this);
        getYTerminalStatusHandler = new GetYTerminalStatusHandler(this);
        pager.setAdapter(mConfigurationAdapter);
        pager.setOffscreenPageLimit(3);
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
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        switch (pageIndex) {
                            case 0:
                                loadMonitorView();
                                break;
                        }
                    }
                };
                Thread thread = new Thread(runnable);
                thread.start();
            }
        });
        syncStatusTask = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    if (BluetoothTool.getInstance(ConfigurationActivity.this).isPrepared()) {
                        pool.execute(ConfigurationActivity.this);
                        syncHandler.postDelayed(syncStatusTask, SYNC_TIME);
                    }
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BluetoothTool.getInstance(this).isPrepared()) {
            if (((NavigationTabActivity) getParent()).hasGetDeviceTypeAndNumber) {
                isRunning = true;
                currentTask = GET_SYSTEM_STATUS;
                syncHandler.postDelayed(syncStatusTask, SYNC_TIME);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isRunning = false;
        BluetoothTool.getInstance(ConfigurationActivity.this).setHandler(null);
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
        switch (pageIndex) {
            case 0:
                loadMonitorView();
                break;
        }
    }

    /**
     * 读取实时状态
     */
    public void loadMonitorView() {
        if (communications == null) {
            List<RealTimeMonitor> monitorList = RealTimeMonitorDao.findByNames(this, ApplicationConfig.stateFilters);
            communications = new BluetoothTalk[monitorList.size()];
            int commandSize = monitorList.size();
            for (int index = 0; index < commandSize; index++) {
                final String code = monitorList.get(index).getCode();
                final RealTimeMonitor monitor = monitorList.get(index);
                communications[index] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16(SerialUtility.hexStr2Ints("0103" + code + "0001")));
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
        if (BluetoothTool.getInstance(this).isPrepared()) {
            configurationHandler.sendCount = communications.length;
            isSyncingSystemStatus = true;
            BluetoothTool.getInstance(this)
                    .setHandler(configurationHandler)
                    .setCommunications(communications)
                    .send();
        }
    }

    /**
     * 恢复出厂设置
     */
    public void restoreFactory() {
        BluetoothTalk[] communications = new BluetoothTalk[1];
        communications[0] = new BluetoothTalk() {
            @Override
            public void beforeSend() {
                this.setSendBuffer(SerialUtility.crc16(SerialUtility.hexStr2Ints("010660030001")));
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
                    // 写入恢复出厂设置日志
                    LogUtils.getInstance().write(ApplicationConfig.LogRestoreFactory,
                            SerialUtility.byte2HexStr(getSendBuffer()),
                            SerialUtility.byte2HexStr(data));
                }
                return null;
            }
        };
        if (BluetoothTool.getInstance(this).isPrepared()) {
            BluetoothTool.getInstance(this)
                    .setHandler(null)
                    .setCommunications(communications)
                    .send();
        }
    }

    /**
     * 查看输入端子状态
     *
     * @param monitor RealTimeMonitor
     */
    public void seeInputTerminalStatus(RealTimeMonitor monitor) {
        if (getXTerminalCommunications == null) {
            final List<ParameterSettings> terminalList = ParameterSettingsDao.findByType(this, 3);
            getXTerminalCommunications = new BluetoothTalk[4];
            for (int i = 0; i < 4; i++) {
                int startIndex = 0;
                int length = 0;
                if (i == 0) {
                    startIndex = 0;
                    length = 10;
                }
                if (i == 1) {
                    startIndex = 10;
                    length = 10;
                }
                if (i == 2) {
                    startIndex = 20;
                    length = 4;
                }
                if (i == 3) {
                    startIndex = 24;
                    length = 3;
                }
                final int commandLength = length;
                final int startPosition = startIndex;
                final ParameterSettings firstItem = terminalList.get(startIndex);
                final String lengthHex = String.format("%04x", length);
                getXTerminalCommunications[i] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16(SerialUtility
                                .hexStr2Ints("0103"
                                        + ParseSerialsUtils.getCalculatedCode(firstItem)
                                        + lengthHex
                                        + "0001")));
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
                            if (commandLength * 2 == bytesLength) {
                                List<ParameterSettings> tempList = new ArrayList<ParameterSettings>();
                                for (int j = 0; j < commandLength; j++) {
                                    int index = startPosition + j;
                                    if (index < terminalList.size()) {
                                        ParameterSettings item = terminalList.get(index);
                                        byte[] tempData = SerialUtility.crc16(SerialUtility.hexStr2Ints("01030002"
                                                + SerialUtility.byte2HexStr(new byte[]{data[4 + j * 2], data[5 + j * 2]})));
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
        View dialogView = getLayoutInflater().inflate(R.layout.terminal_status_dialog, null);
        waitTextView = (TextView) dialogView.findViewById(R.id.wait_text);
        terminalListView = (ListView) dialogView.findViewById(R.id.list_view);
        AlertDialog.Builder builder = new AlertDialog.Builder(ConfigurationActivity.this,
                R.style.CustomDialogStyle)
                .setView(dialogView)
                .setTitle(monitor.getName())
                .setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        isSyncingSystemStatus = false;
                        currentTask = GET_SYSTEM_STATUS;
                        syncHandler.postDelayed(syncStatusTask, SYNC_TIME);
                    }
                });
        terminalDialog = builder.create();
        terminalDialog.show();
        terminalDialog.setCancelable(false);
        terminalDialog.setCanceledOnTouchOutside(false);
        getXTerminalStatusHandler.monitor = monitor;
        startGetXTerminalCommunications();
    }

    /**
     * Start Get X Terminal Status Communications
     */
    private void startGetXTerminalCommunications() {
        if (getXTerminalCommunications != null) {
            if (BluetoothTool.getInstance(this).isPrepared()) {
                isSyncingTerminalStatus = false;
                currentTask = SEE_X_TERMINAL_STATUS;
                syncHandler.postDelayed(syncStatusTask, SYNC_TIME);
            }
        }
    }

    /**
     * 查看输出端子状态
     *
     * @param monitor RealTimeMonitor
     */
    public void seeOutputTerminalStatus(RealTimeMonitor monitor) {
        if (getYTerminalCommunications == null) {
            final List<ParameterSettings> terminalList = ParameterSettingsDao.findByType(this, 4);
            final int size = terminalList.size();
            final int count = size <= 10 ? 1 : ((size - size % 10) / 10 + (size % 10 == 0 ? 0 : 1));
            getYTerminalCommunications = new BluetoothTalk[count];
            for (int i = 0; i < count; i++) {
                final int position = i;
                final ParameterSettings firstItem = terminalList.get(position * 10);
                final int length = size <= 10 ? size : (size % 10 == 0 ? 10 : ((position == count - 1) ? size % 10 : 10));
                getYTerminalCommunications[i] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16(SerialUtility
                                .hexStr2Ints("0103"
                                        + ParseSerialsUtils.getCalculatedCode(firstItem)
                                        + String.format("%04x", length)
                                        + "0001")));
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
                                        byte[] tempData = SerialUtility.crc16(SerialUtility.hexStr2Ints("01030002"
                                                + SerialUtility.byte2HexStr(new byte[]{data[4 + j * 2], data[5 + j * 2]})));
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
        View dialogView = getLayoutInflater().inflate(R.layout.terminal_status_dialog, null);
        waitTextView = (TextView) dialogView.findViewById(R.id.wait_text);
        terminalListView = (ListView) dialogView.findViewById(R.id.list_view);
        AlertDialog.Builder builder = new AlertDialog.Builder(ConfigurationActivity.this,
                R.style.CustomDialogStyle)
                .setView(dialogView)
                .setTitle(monitor.getName())
                .setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        isSyncingSystemStatus = false;
                        currentTask = GET_SYSTEM_STATUS;
                        syncHandler.postDelayed(syncStatusTask, SYNC_TIME);
                    }
                });
        terminalDialog = builder.create();
        terminalDialog.show();
        terminalDialog.setCancelable(false);
        terminalDialog.setCanceledOnTouchOutside(false);
        getYTerminalStatusHandler.monitor = monitor;
        startGetYTerminalCommunications();
    }

    /**
     * 开始读取Y端子状态
     */
    private void startGetYTerminalCommunications() {
        if (getYTerminalCommunications != null) {
            if (BluetoothTool.getInstance(this).isPrepared()) {
                isSyncingTerminalStatus = false;
                currentTask = SEE_Y_TERMINAL_STATUS;
                syncHandler.postDelayed(syncStatusTask, SYNC_TIME);
            }
        }
    }

    @Override
    public void run() {
        switch (currentTask) {
            /**
             * 读取实时状态
             */
            case GET_SYSTEM_STATUS: {
                if (!isSyncingTerminalStatus) {
                    ConfigurationActivity.this.reSyncData();
                }
            }
            break;
            /**
             * 读取 X 端子状态
             */
            case SEE_X_TERMINAL_STATUS: {
                if (!isSyncingTerminalStatus) {
                    getXTerminalStatusHandler.sendCount = getXTerminalCommunications.length;
                    isSyncingTerminalStatus = true;
                    BluetoothTool.getInstance(this)
                            .setHandler(getXTerminalStatusHandler)
                            .setCommunications(getXTerminalCommunications)
                            .send();
                }
            }
            break;
            /**
             * 读取 Y 端子状态
             */
            case SEE_Y_TERMINAL_STATUS: {
                if (!isSyncingTerminalStatus) {
                    getYTerminalStatusHandler.sendCount = getYTerminalCommunications.length;
                    isSyncingTerminalStatus = true;
                    BluetoothTool.getInstance(ConfigurationActivity.this)
                            .setHandler(getYTerminalStatusHandler)
                            .setCommunications(getYTerminalCommunications)
                            .send();
                }
            }
            break;
        }

    }

    // =============================== Get X Terminal Status Handler ====================================== //
    private class GetXTerminalStatusHandler extends BluetoothHandler {

        /**
         * 发送的指令数
         */
        public int sendCount;

        /**
         * 接收到得指令数
         */
        private int receiveCount;

        public RealTimeMonitor monitor;

        private List<ObjectListHolder> holderList;

        public GetXTerminalStatusHandler(android.app.Activity activity) {
            super(activity);
            TAG = GetXTerminalStatusHandler.class.getSimpleName();
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
                    boolean[] bitValues = ParseSerialsUtils.getBooleanValueArray(monitor.getCombineBytes());
                    boolean[] HVBoolValueArray = ParseSerialsUtils.getBooleanValueArray(monitor.getHVInputTerminalBytes());
                    int length = bitValues.length;
                    List<ParameterStatusItem> statusList = new ArrayList<ParameterStatusItem>();
                    for (ParameterSettings settings : settingsList) {
                        int indexValue = ParseSerialsUtils.getIntFromBytes(settings.getReceived());
                        int tempIndex = indexValue;
                        String openStatus = "(常开)";
                        if (indexValue > 31 && indexValue < 64) {
                            openStatus = "(常闭)";
                            indexValue -= 32;
                        } else if (indexValue >= 64 && indexValue < 96) {
                            openStatus = "(常开)";
                            indexValue -= 32;
                        } else if (indexValue >= 96) {
                            openStatus = "(常闭)";
                            indexValue -= 64;
                        }
                        if (indexValue < length && indexValue >= 0) {
                            try {
                                JSONArray jsonArray = new JSONArray(settings.getJSONDescription());
                                int size = jsonArray.length();
                                String[] valueStringArray = new String[size];
                                for (int i = 0; i < size; i++) {
                                    JSONObject value = jsonArray.getJSONObject(i);
                                    valueStringArray[i] = tempIndex + ":" + value.optString("value");
                                }
                                if (indexValue < valueStringArray.length) {
                                    ParameterStatusItem item = new ParameterStatusItem();
                                    item.setName(settings.getName().replace("功能选择", "端子   ")
                                            + valueStringArray[indexValue] + openStatus);
                                    item.setStatus(bitValues[indexValue]);
                                    if (indexValue < HVBoolValueArray.length) {
                                        if (item.getName().contains("X25")
                                                || item.getName().contains("X26")
                                                || item.getName().contains("X27")) {
                                            item.setStatus(HVBoolValueArray[indexValue]);
                                        }
                                    }
                                    statusList.add(item);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    /**
                     * 更新 AlertDialog ListView
                     */
                    ParameterStatusAdapter adapter = new ParameterStatusAdapter(ConfigurationActivity.this, statusList);
                    ConfigurationActivity.this.terminalListView.setAdapter(adapter);
                    ConfigurationActivity.this.waitTextView.setVisibility(View.GONE);
                    ConfigurationActivity.this.terminalListView.setVisibility(View.VISIBLE);
                    ConfigurationActivity.this.isSyncingTerminalStatus = false;
                    /**
                     * 重启启动获取实时状态 Task
                     */
                    syncHandler.postDelayed(syncStatusTask, SYNC_TIME);
                }
            } else {
                ConfigurationActivity.this.startGetXTerminalCommunications();
            }
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof ObjectListHolder) {
                holderList.add((ObjectListHolder) msg.obj);
                receiveCount++;
            }
        }

        @Override
        public void onTalkError(Message msg) {
            super.onTalkError(msg);
        }
    }

    // =============================== Get Y Terminal Status Handler ====================================== //
    private class GetYTerminalStatusHandler extends BluetoothHandler {

        private int sendCount;

        private int receiveCount;

        public RealTimeMonitor monitor;

        private List<ObjectListHolder> holderList;

        public GetYTerminalStatusHandler(android.app.Activity activity) {
            super(activity);
            TAG = GetXTerminalStatusHandler.class.getSimpleName();
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
                    boolean[] bitValues = ParseSerialsUtils.getBooleanValueArray(monitor.getCombineBytes());
                    int length = bitValues.length;
                    List<ParameterStatusItem> statusList = new ArrayList<ParameterStatusItem>();
                    for (ParameterSettings settings : settingsList) {
                        int indexValue = ParseSerialsUtils.getIntFromBytes(settings.getReceived());
                        if (indexValue >= 0 && indexValue < length) {
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
                                    item.setName(settings.getName().replace("功能选择", "端子   ")
                                            + valueStringArray[indexValue]);
                                    item.setStatus(bitValues[indexValue]);
                                    statusList.add(item);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    /**
                     * 更新 AlertDialog ListView
                     */
                    ParameterStatusAdapter adapter = new ParameterStatusAdapter(ConfigurationActivity.this, statusList);
                    ConfigurationActivity.this.terminalListView.setAdapter(adapter);
                    ConfigurationActivity.this.waitTextView.setVisibility(View.GONE);
                    ConfigurationActivity.this.terminalListView.setVisibility(View.VISIBLE);
                    ConfigurationActivity.this.isSyncingTerminalStatus = false;
                }
            } else {
                ConfigurationActivity.this.startGetYTerminalCommunications();
            }
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkError(msg);
            if (msg.obj != null && msg.obj instanceof ObjectListHolder) {
                holderList.add((ObjectListHolder) msg.obj);
                receiveCount++;
            }
        }

        @Override
        public void onTalkError(Message msg) {
            super.onTalkError(msg);
        }
    }

}
