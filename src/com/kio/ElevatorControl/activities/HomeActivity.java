package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;
import butterknife.InjectView;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HHandler;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.adapters.ShortcutListViewAdapter;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.daos.RealTimeMonitorDao;
import com.kio.ElevatorControl.daos.ShortcutDao;
import com.kio.ElevatorControl.models.RealTimeMonitor;
import com.kio.ElevatorControl.models.Shortcut;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;
import com.kio.ElevatorControl.views.DoorAnimationView;
import com.kio.ElevatorControl.views.TypefaceTextView;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class HomeActivity extends Activity {

    private static final String TAG = HomeActivity.class.getSimpleName();

    @InjectView(R.id.list_view)
    ListView mListView;

    @InjectView(R.id.door_animation_view)
    DoorAnimationView doorAnimationView;

    @InjectView(R.id.running_speed)
    TextView runningSpeedTextView;

    @InjectView(R.id.lock_status)
    TextView lockStatusTextView;

    @InjectView(R.id.error_status)
    TextView errorStatusTextView;

    @InjectView(R.id.current_floor)
    TypefaceTextView currentFloorTextView;

    @InjectView(R.id.current_direction)
    ImageView currentDirectionImageView;

    private HCommunication[] communications;

    private SyncStatusHandler mSyncStatusHandler;

    private List<RealTimeMonitor> monitorLists;

    private String[] elevatorBoxStatus;

    private ShortcutListViewAdapter adapter;

    private Handler handler = new Handler();

    private Runnable syncTask;

    private boolean running = false;

    /**
     * 同步间隔
     */
    private static final int SYNC_TIME = 1000;

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
                    HomeActivity.this.syncElevatorStatus();
                    handler.postDelayed(this, SYNC_TIME);
                }
            }
        };
    }

    /**
     * 读取状态参数Code
     */
    private void readMonitorStateCode() {
        monitorLists = RealTimeMonitorDao.findByNames(this, new String[]{
                ApplicationConfig.RUNNING_SPEED_NAME,
                ApplicationConfig.ERROR_CODE_NAME,
                ApplicationConfig.CURRENT_FLOOR_NAME,
                ApplicationConfig.SYSTEM_STATUS_NAME
        });
        int size = monitorLists.size();
        communications = new HCommunication[size];
        for (int index = 0; index < size; index++) {
            final int i = index;
            communications[i] = new HCommunication() {
                @Override
                public void beforeSend() {
                    this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103"
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
                    if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                        byte[] received = HSerial.trimEnd(getReceivedBuffer());
                        RealTimeMonitor monitor = null;
                        try {
                            monitor = (RealTimeMonitor) monitorLists.get(i).clone();
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
        reSyncData();
        HBluetooth.getInstance(this).prepare();
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
        if (HBluetooth.getInstance(HomeActivity.this).isPrepared()) {
            running = true;
            handler.postDelayed(syncTask, SYNC_TIME);
        } else {
            Toast.makeText(this,
                    R.string.not_connect_device_error,
                    android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * 同步电梯状态信息
     */
    private void syncElevatorStatus() {
        if (HBluetooth.getInstance(HomeActivity.this).isPrepared()) {
            HBluetooth.getInstance(HomeActivity.this)
                    .setHandler(mSyncStatusHandler)
                    .setCommunications(communications)
                    .Start();
        } else {
            Toast.makeText(this,
                    R.string.not_connect_device_error,
                    android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void setListViewDataSource() {
        List<Shortcut> shortcutList = ShortcutDao.findAll(this);
        if (adapter == null) {
            adapter = new ShortcutListViewAdapter(HomeActivity.this, shortcutList);
            mListView.setAdapter(adapter);
        } else {
            adapter.setShortcutList(shortcutList);
        }
    }

    // ==================================== HomeActivity Bluetooth Handler ===================================

    /**
     * 首页电梯实时状态
     */
    private class SyncStatusHandler extends HHandler {

        public SyncStatusHandler(Activity activity) {
            super(activity);
            TAG = SyncStatusHandler.class.getSimpleName();
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
            if (msg.obj != null && (msg.obj instanceof RealTimeMonitor)) {
                RealTimeMonitor monitor = (RealTimeMonitor) msg.obj;
                if (monitor.getName().equalsIgnoreCase(ApplicationConfig.RUNNING_SPEED_NAME)) {
                    HomeActivity.this.runningSpeedTextView
                            .setText(ParseSerialsUtils.getValueTextFromRealTimeMonitor(monitor)
                                    + monitor.getUnit());
                } else if (monitor.getName().equalsIgnoreCase(ApplicationConfig.ERROR_CODE_NAME)) {
                    String errorCode = ParseSerialsUtils.getErrorCode(monitor.getReceived());
                    if (errorCode.equalsIgnoreCase("E00")){
                        HomeActivity.this.errorStatusTextView.setTextColor(0xff989898);
                        HomeActivity.this.errorStatusTextView.setText(R.string.home_no_error_text);
                    }
                    else {
                        HomeActivity.this.errorStatusTextView.setTextColor(0xffff594b);
                        HomeActivity.this.errorStatusTextView.setText(errorCode);
                    }
                } else if (monitor.getName().equalsIgnoreCase(ApplicationConfig.CURRENT_FLOOR_NAME)) {
                    HomeActivity.this.currentFloorTextView
                            .setText(String.valueOf(ParseSerialsUtils.getIntFromBytes(monitor.getReceived())));
                } else if (monitor.getName().equalsIgnoreCase(ApplicationConfig.SYSTEM_STATUS_NAME)) {
                    int elevatorBoxStatusCode = ParseSerialsUtils.getElevatorBoxStatusCode(monitor);
                    if (HomeActivity.this.elevatorBoxStatus == null) {
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
                                            for (int j = 0; j < subArraySize; j++) {
                                                HomeActivity.this.elevatorBoxStatus[j] = subArray
                                                        .getJSONObject(j)
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
                        if (!HomeActivity.this.elevatorBoxStatus[elevatorBoxStatusCode]
                                .contains(ApplicationConfig.RETAIN_NAME)) {
                            HomeActivity.this.lockStatusTextView
                                    .setText(HomeActivity.this.elevatorBoxStatus[elevatorBoxStatusCode]);
                        }
                        if (elevatorBoxStatusCode == 1 || elevatorBoxStatusCode == 2) {
                            HomeActivity.this.doorAnimationView.openDoor();
                        }
                        if (elevatorBoxStatusCode == 3 || elevatorBoxStatusCode == 4) {
                            HomeActivity.this.doorAnimationView.closeDoor();
                        }
                    }
                }
            }
        }
    }

}
