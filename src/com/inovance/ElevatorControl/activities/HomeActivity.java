package com.inovance.ElevatorControl.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import butterknife.InjectView;
import butterknife.Views;
import com.bluetoothtool.BluetoothHandler;
import com.bluetoothtool.BluetoothTool;
import com.bluetoothtool.BluetoothTalk;
import com.bluetoothtool.SerialUtility;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.adapters.ShortcutListViewAdapter;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.daos.RealTimeMonitorDao;
import com.inovance.ElevatorControl.daos.ShortcutDao;
import com.inovance.ElevatorControl.models.RealTimeMonitor;
import com.inovance.ElevatorControl.models.Shortcut;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import com.inovance.ElevatorControl.views.DoorAnimationView;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class HomeActivity extends Activity implements Runnable{

    private static final String TAG = HomeActivity.class.getSimpleName();

    @InjectView(R.id.list_view)
    ListView mListView;

    @InjectView(R.id.door_animation_view)
    DoorAnimationView doorAnimationView;

    @InjectView(R.id.running_speed)
    TextView runningSpeedTextView;

    @InjectView(R.id.system_status)
    TextView systemStatusTextView;

    @InjectView(R.id.lock_status)
    TextView lockStatusTextView;

    @InjectView(R.id.error_status)
    TextView errorStatusTextView;

    private BluetoothTalk[] communications;

    private SyncStatusHandler mSyncStatusHandler;

    private List<RealTimeMonitor> monitorLists;

    private String[] elevatorBoxStatus;

    private String[] systemStatus;

    private ShortcutListViewAdapter adapter;

    private Handler handler = new Handler();

    private Runnable syncTask;

    private boolean running = false;

    private List<Shortcut> shortcutList;

    private ExecutorService pool = Executors.newSingleThreadExecutor();

    /**
     * 同步间隔
     */
    private static final int SYNC_TIME = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Views.inject(this);
        mSyncStatusHandler = new SyncStatusHandler(HomeActivity.this);
        readMonitorStateCode();
        syncTask = new Runnable() {
            @Override
            public void run() {
                if (running) {
                    if (BluetoothTool.getInstance(HomeActivity.this).isConnected()) {
                        pool.execute(HomeActivity.this);
                        handler.postDelayed(this, SYNC_TIME);
                    }
                }
            }
        };
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < shortcutList.size()) {
                    String[] locationArray = shortcutList.get(position).getCommand().split(":");
                    if (locationArray.length == 3) {
                        int navigationTabIndex = Integer.parseInt(locationArray[0]);
                        int pagerIndex = Integer.parseInt(locationArray[1]);
                        NavigationTabActivity tabActivity = (NavigationTabActivity) HomeActivity.this.getParent();
                        if (tabActivity != null) {
                            tabActivity.switchTab(navigationTabIndex, pagerIndex);
                        }
                    }
                }
            }
        });
    }

    /**
     * 读取状态参数Code
     */
    private void readMonitorStateCode() {
        monitorLists = RealTimeMonitorDao.findByNames(this, new String[]{
                ApplicationConfig.RUNNING_SPEED_NAME,
                ApplicationConfig.ERROR_CODE_NAME,
                ApplicationConfig.STATUS_WORD_NAME,
                ApplicationConfig.CURRENT_FLOOR_NAME,
                ApplicationConfig.SYSTEM_STATUS_NAME
        });
        int size = monitorLists.size();
        communications = new BluetoothTalk[size];
        for (int index = 0; index < size; index++) {
            final int i = index;
            communications[i] = new BluetoothTalk() {
                @Override
                public void beforeSend() {
                    this.setSendBuffer(SerialUtility.crc16(SerialUtility.hexStr2Ints("0103"
                            + monitorLists.get(i).getCode()
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
                        byte[] received = SerialUtility.trimEnd(getReceivedBuffer());
                        try {
                            RealTimeMonitor monitor = (RealTimeMonitor) monitorLists.get(i).clone();
                            monitor.setReceived(received);
                            return monitor;
                        } catch (CloneNotSupportedException e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                }
            };
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setListViewDataSource();
        if (BluetoothTool.getInstance(HomeActivity.this).isConnected()) {
            if (((NavigationTabActivity) getParent()).hasGetDeviceTypeAndNumber) {
                reSyncData();
            }
        } else {
            Toast.makeText(this,
                    R.string.not_connect_device_error,
                    android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;
    }

    public void reSyncData() {
        loopSyncElevatorStatusTask();
    }

    /**
     * 开始同步电梯状态信息 Task
     */
    public void loopSyncElevatorStatusTask() {
        running = true;
        handler.postDelayed(syncTask, SYNC_TIME);
    }

    /**
     * 同步电梯状态信息
     */
    private void syncElevatorStatus() {
        if (BluetoothTool.getInstance(HomeActivity.this).isConnected()) {
            mSyncStatusHandler.sendCount = communications.length;
            BluetoothTool.getInstance(HomeActivity.this)
                    .setHandler(mSyncStatusHandler)
                    .setCommunications(communications)
                    .send();
        } else {
            Toast.makeText(this,
                    R.string.not_connect_device_error,
                    android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void setListViewDataSource() {
        shortcutList = ShortcutDao.findAll(this);
        if (adapter == null) {
            adapter = new ShortcutListViewAdapter(HomeActivity.this, shortcutList);
            mListView.setAdapter(adapter);
        } else {
            adapter.setShortcutList(shortcutList);
        }
    }

    @Override
    public void run() {
        HomeActivity.this.syncElevatorStatus();
    }

    // ==================================== HomeActivity Bluetooth Handler ===================================

    /**
     * 首页电梯实时状态
     */
    private class SyncStatusHandler extends BluetoothHandler {

        public int sendCount;

        public int receiveCount;

        private List<RealTimeMonitor> receivedMonitorList;

        public SyncStatusHandler(Activity activity) {
            super(activity);
            TAG = SyncStatusHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
            receivedMonitorList = new ArrayList<RealTimeMonitor>();
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (sendCount == receiveCount) {
                for (RealTimeMonitor monitor : receivedMonitorList) {
                    if (monitor.getName().equalsIgnoreCase(ApplicationConfig.RUNNING_SPEED_NAME)) {
                        HomeActivity.this.runningSpeedTextView
                                .setText(ParseSerialsUtils.getValueTextFromRealTimeMonitor(monitor)
                                        + monitor.getUnit());
                    }
                    if (monitor.getName().equalsIgnoreCase(ApplicationConfig.ERROR_CODE_NAME)) {
                        String errorCode = ParseSerialsUtils.getErrorCode(monitor.getReceived());
                        if (errorCode.equalsIgnoreCase("E00")) {
                            HomeActivity.this.errorStatusTextView.setTextColor(0xff989898);
                            HomeActivity.this.errorStatusTextView.setText(R.string.home_no_error_text);
                        } else {
                            HomeActivity.this.errorStatusTextView.setTextColor(0xffff594b);
                            HomeActivity.this.errorStatusTextView.setText(errorCode);
                        }
                    }
                    if (monitor.getName().equalsIgnoreCase(ApplicationConfig.STATUS_WORD_NAME)) {
                        int controllerStatus = ParseSerialsUtils.getIntFromBytes(monitor.getReceived());
                        doorAnimationView.setCurrentDirection(controllerStatus);
                    }
                    if (monitor.getName().equalsIgnoreCase(ApplicationConfig.CURRENT_FLOOR_NAME)) {
                        doorAnimationView.setCurrentFloor(ParseSerialsUtils.getIntFromBytes(monitor.getReceived()));
                    }
                    if (monitor.getName().equalsIgnoreCase(ApplicationConfig.SYSTEM_STATUS_NAME)) {
                        int elevatorBoxStatusCode = ParseSerialsUtils.getElevatorBoxStatusCode(monitor);
                        int systemStatusCode = ParseSerialsUtils.getSystemStatusCode(monitor);
                        if (HomeActivity.this.elevatorBoxStatus == null || HomeActivity.this.systemStatus == null) {
                            try {
                                JSONArray jsonArray = new JSONArray(monitor.getJSONDescription());
                                Pattern pattern = Pattern.compile("^\\d*\\-\\d*:.*", Pattern.CASE_INSENSITIVE);
                                int size = jsonArray.length();
                                for (int i = 0; i < size; i++) {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    for (Iterator iterator = jsonObject.keys(); iterator.hasNext(); ) {
                                        String name = (String) iterator.next();
                                        if (pattern.matcher(name).matches()) {
                                            if (name.replaceAll("\\d*\\-\\d*:", "")
                                                    .equalsIgnoreCase(ApplicationConfig.ELEVATOR_BOX_STATUS_NAME)) {
                                                JSONArray subArray = jsonObject.optJSONArray(name);
                                                int subArraySize = subArray.length();
                                                HomeActivity.this.elevatorBoxStatus = new String[subArraySize];
                                                for (int m = 0; m < subArraySize; m++) {
                                                    HomeActivity.this.elevatorBoxStatus[m] = subArray
                                                            .getJSONObject(m)
                                                            .optString("value");
                                                }
                                            }
                                            if (name.replaceAll("\\d*\\-\\d*:", "")
                                                    .equalsIgnoreCase(ApplicationConfig.SYSTEM_STATUS_NAME)) {
                                                JSONArray subArray = jsonObject.optJSONArray(name);
                                                int subArraySize = subArray.length();
                                                HomeActivity.this.systemStatus = new String[subArraySize];
                                                for (int n = 0; n < subArraySize; n++) {
                                                    HomeActivity.this.systemStatus[n] = subArray
                                                            .getJSONObject(n)
                                                            .optString("value");
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        if (elevatorBoxStatusCode < HomeActivity.this.elevatorBoxStatus.length) {
                            HomeActivity.this.lockStatusTextView
                                    .setText(HomeActivity.this.elevatorBoxStatus[elevatorBoxStatusCode]);
                            if (elevatorBoxStatusCode == 1 || elevatorBoxStatusCode == 2) {
                                HomeActivity.this.doorAnimationView.openDoor();
                            }
                            if (elevatorBoxStatusCode == 3 || elevatorBoxStatusCode == 4) {
                                HomeActivity.this.doorAnimationView.closeDoor();
                            }
                        }
                        if (systemStatusCode < HomeActivity.this.systemStatus.length) {
                            HomeActivity.this.systemStatusTextView
                                    .setText(HomeActivity.this.systemStatus[systemStatusCode]);
                        }
                    }
                }
            }
        }

        @Override
        public void onTalkReceive(Message msg) {
            if (msg.obj != null && (msg.obj instanceof RealTimeMonitor)) {
                RealTimeMonitor monitor = (RealTimeMonitor) msg.obj;
                receivedMonitorList.add(monitor);
                receiveCount++;
            }
        }
    }

}
