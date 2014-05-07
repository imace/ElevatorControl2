package com.inovance.ElevatorControl.activities;

import android.app.AlertDialog;
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
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import com.viewpagerindicator.TabPageIndicator;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 大标签卡 电梯调试
 *
 * @author jch
 */
public class ConfigurationActivity extends Activity {

    private static final String TAG = ConfigurationActivity.class.getSimpleName();

    public int pageIndex;

    private ConfigurationHandler configurationHandler;

    private BluetoothTalk[] communications;

    private GetXTerminalStatusHandler getXTerminalStatusHandler;

    private GetYTerminalStatusHandler getYTerminalStatusHandler;

    private BluetoothTalk[] getXTerminalCommunications;

    private BluetoothTalk[] getYTerminalCommunications;

    private AlertDialog terminalDialog;

    private TextView waitTextView;

    private ListView terminalListView;

    private Runnable syncStatusTask;

    private boolean isRunning;

    private Handler syncHandler = new Handler();

    private static final int SYNC_TIME = 3000;

    public boolean isSyncing = false;

    private boolean isReadingTerminalStatus = false;

    /**
     * 注入页面元素
     */
    @InjectView(R.id.pager)
    public ViewPager pager;

    @InjectView(R.id.indicator)
    protected TabPageIndicator indicator;

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
                    if (BluetoothTool.getInstance(ConfigurationActivity.this).isConnected()) {
                        if (!isSyncing && !isReadingTerminalStatus) {
                            ConfigurationActivity.this.reSyncData();
                            syncHandler.postDelayed(syncStatusTask, SYNC_TIME);
                        }
                    }
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BluetoothTool.getInstance(this).isConnected()) {
            if (((NavigationTabActivity) getParent()).hasGetDeviceTypeAndNumber) {
                isRunning = true;
                isReadingTerminalStatus = false;
                isSyncing = false;
                syncHandler.postDelayed(syncStatusTask, SYNC_TIME);
            }
        } else {
            handler.sendEmptyMessage(0);
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
        switch (pageIndex) {
            case 0:
                loadMonitorView();
                break;
        }
    }

    /**
     * 实时监控,加载内容 实时监控列表比较特殊,要在标签切换之后刷新 而标签切换只在一个activity中进行的
     * 于是放在activity中加载,另外三个标签都是静态内容放在Fragment中加载
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
        if (BluetoothTool.getInstance(this).isConnected()) {
            configurationHandler.sendCount = communications.length;
            ConfigurationActivity.this.isSyncing = true;
            BluetoothTool.getInstance(this)
                    .setHandler(configurationHandler)
                    .setCommunications(communications)
                    .send();
        } else {
            handler.sendEmptyMessage(0);
        }
    }

    /**
     * 查看输入端状态
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
                .setPositiveButton(R.string.dialog_btn_ok, null);
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
            if (BluetoothTool.getInstance(this).isConnected()) {
                ConfigurationActivity.this.isReadingTerminalStatus = true;
                getXTerminalStatusHandler.sendCount = getXTerminalCommunications.length;
                BluetoothTool.getInstance(this)
                        .setHandler(getXTerminalStatusHandler)
                        .setCommunications(getXTerminalCommunications)
                        .send();
            } else {
                Toast.makeText(ConfigurationActivity.this,
                        R.string.not_connect_device_error,
                        android.widget.Toast.LENGTH_SHORT)
                        .show();
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
                .setPositiveButton(R.string.dialog_btn_ok, null);
        terminalDialog = builder.create();
        terminalDialog.show();
        terminalDialog.setCancelable(false);
        terminalDialog.setCanceledOnTouchOutside(false);
        getYTerminalStatusHandler.monitor = monitor;
        startGetYTerminalCommunications();
    }

    /**
     * Start Get Y Terminal Communications
     */
    private void startGetYTerminalCommunications() {
        if (getYTerminalCommunications != null) {
            if (BluetoothTool.getInstance(ConfigurationActivity.this).isConnected()) {
                getYTerminalStatusHandler.sendCount = getYTerminalCommunications.length;
                ConfigurationActivity.this.isReadingTerminalStatus = true;
                BluetoothTool.getInstance(ConfigurationActivity.this)
                        .setHandler(getYTerminalStatusHandler)
                        .setCommunications(getYTerminalCommunications)
                        .send();
            } else {
                Toast.makeText(this,
                        R.string.not_connect_device_error,
                        android.widget.Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    /**
     * Bluetooth socket error handler
     */
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0: {
                    Toast.makeText(ConfigurationActivity.this,
                            R.string.not_connect_device_error,
                            android.widget.Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            }
            super.handleMessage(msg);
        }

    };

    // =============================== Get X Terminal Status Handler ====================================== //
    private class GetXTerminalStatusHandler extends BluetoothHandler {

        public int sendCount;

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
                    boolean[] bitValues = SerialUtility.byteArray2BitArray(monitor.getCombineBytes());
                    int length = bitValues.length;
                    List<ParameterStatusItem> statusList = new ArrayList<ParameterStatusItem>();
                    for (ParameterSettings settings : settingsList) {
                        int indexValue = ParseSerialsUtils.getIntFromBytes(settings.getReceived());
                        if (indexValue > 31 && indexValue < 64) {
                            indexValue -= 32;
                        } else if (indexValue >= 64 && indexValue < 96) {
                            indexValue -= 32;
                        } else if (indexValue >= 96) {
                            indexValue -= 64;
                        }
                        if (indexValue < length && indexValue >= 0) {
                            ParameterStatusItem item = new ParameterStatusItem();
                            item.setName(settings.getName());
                            item.setStatus(bitValues[indexValue]);
                            statusList.add(item);
                        }
                    }
                    ParameterStatusAdapter adapter = new ParameterStatusAdapter(ConfigurationActivity.this, statusList);
                    ConfigurationActivity.this.terminalListView.setAdapter(adapter);
                    ConfigurationActivity.this.waitTextView.setVisibility(View.GONE);
                    ConfigurationActivity.this.terminalListView.setVisibility(View.VISIBLE);
                    ConfigurationActivity.this.isSyncing = false;
                    ConfigurationActivity.this.isReadingTerminalStatus = false;
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
                    boolean[] bitValues = SerialUtility.byteArray2BitArray(monitor.getCombineBytes());
                    int length = bitValues.length;
                    List<ParameterStatusItem> statusList = new ArrayList<ParameterStatusItem>();
                    for (ParameterSettings settings : settingsList) {
                        int indexValue = ParseSerialsUtils.getIntFromBytes(settings.getReceived());
                        if (indexValue >= 0 && indexValue < length) {
                            ParameterStatusItem item = new ParameterStatusItem();
                            item.setName(settings.getName());
                            item.setStatus(bitValues[indexValue]);
                            statusList.add(item);
                        }
                    }
                    ParameterStatusAdapter adapter = new ParameterStatusAdapter(ConfigurationActivity.this, statusList);
                    ConfigurationActivity.this.terminalListView.setAdapter(adapter);
                    ConfigurationActivity.this.waitTextView.setVisibility(View.GONE);
                    ConfigurationActivity.this.terminalListView.setVisibility(View.VISIBLE);
                    ConfigurationActivity.this.isSyncing = false;
                    ConfigurationActivity.this.isReadingTerminalStatus = false;
                    syncHandler.postDelayed(syncStatusTask, SYNC_TIME);
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
