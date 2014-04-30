package com.inovance.ElevatorControl.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import butterknife.InjectView;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HHandler;
import com.hbluetooth.HSerial;
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
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.preference.SharedPreferences;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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

    @InjectView(R.id.system_status)
    TextView systemStatusTextView;

    @InjectView(R.id.lock_status)
    TextView lockStatusTextView;

    @InjectView(R.id.error_status)
    TextView errorStatusTextView;

    private HCommunication[] communications;

    private SyncStatusHandler mSyncStatusHandler;

    private List<RealTimeMonitor> monitorLists;

    private String[] elevatorBoxStatus;

    private String[] systemStatus;

    private ShortcutListViewAdapter adapter;

    private Handler handler = new Handler();

    private Runnable syncTask;

    private boolean running = false;

    private List<Shortcut> shortcutList;

    private boolean hasGetDeviceTypeAndNumber = false;

    private GetDeviceTypeAndNumberHandler getDeviceTypeAndNumberHandler;

    private HCommunication[] getDeviceTypeAndNumberCommunications;

    private boolean hasToasted;

    /**
     * 同步间隔
     */
    private static final int SYNC_TIME = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Views.inject(this);
        hasToasted = false;
        getDeviceTypeAndNumberHandler = new GetDeviceTypeAndNumberHandler(this);
        mSyncStatusHandler = new SyncStatusHandler(HomeActivity.this);
        readMonitorStateCode();
        syncTask = new Runnable() {
            @Override
            public void run() {
                if (running) {
                    if (!hasGetDeviceTypeAndNumber) {
                        HomeActivity.this.getDeviceTypeAndNumber();
                    } else {
                        HomeActivity.this.syncElevatorStatus();
                    }
                    handler.postDelayed(this, SYNC_TIME);
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
        if (HBluetooth.getInstance(HomeActivity.this).isPrepared()) {
            reSyncData();
        } else {
            if (!hasToasted) {
                Toast.makeText(this,
                        R.string.not_connect_device_error,
                        android.widget.Toast.LENGTH_SHORT)
                        .show();
                hasToasted = true;
            }
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
     * 取得设备型号、厂家编号
     */
    private void getDeviceTypeAndNumber() {
        if (getDeviceTypeAndNumberCommunications == null) {
            getDeviceTypeAndNumberCommunications = new HCommunication[2];
            for (int i = 0; i < 2; i++) {
                final int index = i;
                getDeviceTypeAndNumberCommunications[i] = new HCommunication() {
                    @Override
                    public void beforeSend() {
                        if (index == 0) {
                            this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103FA080001")));
                        }
                        if (index == 1) {
                            this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103D2090001")));
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
                        if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                            byte[] received = HSerial.trimEnd(getReceivedBuffer());
                            if (received.length == 8) {
                                if (index == 0) {
                                    for (String error : ApplicationConfig.ERROR_CODE_ARRAY) {
                                        if (HSerial.byte2HexStr(received).contains(error)) {
                                            return "type:error";
                                        }
                                    }
                                    return "type:" + ParseSerialsUtils.getIntFromBytes(received);
                                }
                                if (index == 1) {
                                    for (String error : ApplicationConfig.ERROR_CODE_ARRAY) {
                                        if (HSerial.byte2HexStr(received).contains(error)) {
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
        if (HBluetooth.getInstance(this).isPrepared()) {
            getDeviceTypeAndNumberHandler.sendCount = getDeviceTypeAndNumberCommunications.length;
            HBluetooth.getInstance(HomeActivity.this)
                    .setHandler(getDeviceTypeAndNumberHandler)
                    .setCommunications(getDeviceTypeAndNumberCommunications)
                    .Start();
        } else {
            Toast.makeText(this,
                    R.string.not_connect_device_error,
                    android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
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
            mSyncStatusHandler.sendCount = communications.length;
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
        shortcutList = ShortcutDao.findAll(this);
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

    // ============================== Get DeviceType And Number Handler ====================== //
    private class GetDeviceTypeAndNumberHandler extends HHandler {

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
                for (String item : responseStringList) {
                    if (item.contains("type")) {
                        if (item.contains("1000")) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                            AlertDialog dialog = builder.setTitle(R.string.choice_device_type_title)
                                    .setItems(new String[]{ApplicationConfig.deviceType[0],
                                            ApplicationConfig.deviceType[1]},
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {

                                                }
                                            }).create();
                            dialog.show();
                            dialog.setCancelable(false);
                            dialog.setCanceledOnTouchOutside(false);
                        } else if (item.contains("3000")) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                            AlertDialog dialog = builder.setTitle(R.string.choice_device_type_title)
                                    .setItems(new String[]{ApplicationConfig.deviceType[2],
                                            ApplicationConfig.deviceType[3]},
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {

                                                }
                                            })
                                    .create();
                            dialog.show();
                            dialog.setCancelable(false);
                            dialog.setCanceledOnTouchOutside(false);
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                            AlertDialog dialog = builder.setTitle(R.string.choice_device_type_title)
                                    .setItems(ApplicationConfig.deviceType,
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {

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
                HomeActivity.this.hasGetDeviceTypeAndNumber = true;
            } else {
                HomeActivity.this.getDeviceTypeAndNumber();
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
            HomeActivity.this.getDeviceTypeAndNumber();
        }
    }

}
