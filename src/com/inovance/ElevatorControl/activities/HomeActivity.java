package com.inovance.ElevatorControl.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import butterknife.InjectView;
import butterknife.Views;
import com.bluetoothtool.BluetoothTalk;
import com.bluetoothtool.BluetoothTool;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * 首页
 */
public class HomeActivity extends Activity implements Runnable {

    private static final String TAG = HomeActivity.class.getSimpleName();

    /**
     * 快捷菜单 List View
     */
    @InjectView(R.id.list_view)
    ListView mListView;

    /**
     * 开关门动画视图
     */
    @InjectView(R.id.door_animation_view)
    DoorAnimationView doorAnimationView;

    /**
     * 当前电梯运行速度
     */
    @InjectView(R.id.running_speed)
    TextView runningSpeedTextView;

    /**
     * 系统状态
     */
    @InjectView(R.id.system_status)
    TextView systemStatusTextView;

    /**
     * 锁梯状态
     */
    @InjectView(R.id.lock_status)
    TextView lockStatusTextView;

    /**
     * 故障
     */
    @InjectView(R.id.error_status)
    TextView errorStatusTextView;

    /**
     * 获取当前要显示的状态信息的通信内容
     */
    private BluetoothTalk[] communications;

    /**
     * 要获取的状态信息列表
     */
    private List<RealTimeMonitor> monitorLists;

    /**
     * 电梯所有状态数组
     */
    private String[] elevatorBoxStatus;

    /**
     * 系统所有状态数组
     */
    private String[] systemStatus;

    /**
     * 快捷菜单 List View Adapter
     */
    private ShortcutListViewAdapter adapter;

    /**
     * 用于 Loop Task 的 Handler
     */
    private Handler handler = new Handler();

    /**
     * 同步 Task
     */
    private Runnable syncTask;

    /**
     * 是否正在运行同步 Task
     */
    private boolean running = false;

    /**
     * 快捷菜单 List
     */
    private List<Shortcut> shortcutList;

    /**
     * 闪烁故障码 Task
     */
    private Runnable textBlinkTask;

    /**
     * 用于闪烁 TextView 的 Handler
     */
    private Handler blinkHandler = new Handler();

    /**
     * 当前是否有故障发生
     */
    private boolean isErrorStatus = false;

    private ExecutorService pool = Executors.newSingleThreadExecutor();

    /**
     * 同步间隔
     */
    private static final int SYNC_TIME = 1500;

    private Handler getHomeStatusValueHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    readHomeStatusValue();
                    break;
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Views.inject(this);
        readMonitorStateCode();
        syncTask = new Runnable() {
            @Override
            public void run() {
                if (running) {
                    if (BluetoothTool.getInstance(HomeActivity.this).isPrepared()) {
                        pool.execute(HomeActivity.this);
                        getHomeStatusValueHandler.sendEmptyMessage(0);
                        handler.postDelayed(this, SYNC_TIME);
                    }
                }
            }
        };
        textBlinkTask = new Runnable() {
            @Override
            public void run() {
                if (running) {
                    if (isErrorStatus) {
                        float alpha = HomeActivity.this.errorStatusTextView.getAlpha();
                        if (alpha == 0.0f) {
                            HomeActivity.this.errorStatusTextView.setAlpha(1.0f);
                        } else {
                            HomeActivity.this.errorStatusTextView.setAlpha(0.0f);
                        }
                    } else {
                        HomeActivity.this.errorStatusTextView.setAlpha(1.0f);
                    }
                    blinkHandler.postDelayed(textBlinkTask, 500);
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
                    return null;
                }
            };
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setListViewDataSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;
        BluetoothTool.getInstance(this).setTalkType(BluetoothTalk.NORMAL_TALK);
    }

    public void reSyncData() {
        blinkHandler.postDelayed(textBlinkTask, 500);
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
        if (BluetoothTool.getInstance(HomeActivity.this).isPrepared()) {
            BluetoothTool.getInstance(HomeActivity.this).setTalkType(BluetoothTalk.HOME_STATUS_TALK);
            BluetoothTool.getInstance(HomeActivity.this)
                    .setHandler(null)
                    .setCommunications(communications)
                    .send();
        }
    }

    /**
     * 更新快捷菜单数据
     */
    public void setListViewDataSource() {
        shortcutList = ShortcutDao.findAll(this);
        if (adapter == null) {
            adapter = new ShortcutListViewAdapter(HomeActivity.this, shortcutList);
            mListView.setAdapter(adapter);
        } else {
            adapter.setShortcutList(shortcutList);
        }
    }

    /**
     * 读取首页状态值
     */
    private void readHomeStatusValue() {
        Hashtable<String, byte[]> statusSet = BluetoothTool.getInstance(this).getHomeStatusValueSet();
        if (statusSet != null && statusSet.size() == communications.length) {
            for (RealTimeMonitor monitor : monitorLists) {
                monitor.setReceived(statusSet.get(monitor.getCode()));
                // 电梯运行速度
                if (monitor.getName().equalsIgnoreCase(ApplicationConfig.RUNNING_SPEED_NAME)) {
                    HomeActivity.this.runningSpeedTextView
                            .setText(ParseSerialsUtils.getValueTextFromRealTimeMonitor(monitor)
                                    + monitor.getUnit());
                }
                // 故障码
                if (monitor.getName().equalsIgnoreCase(ApplicationConfig.ERROR_CODE_NAME)) {
                    String errorCode = ParseSerialsUtils.getErrorCode(monitor.getReceived());
                    NavigationTabActivity tabActivity = (NavigationTabActivity) HomeActivity.this.getParent();
                    if (errorCode.equalsIgnoreCase("E00")) {
                        isErrorStatus = false;
                        HomeActivity.this.errorStatusTextView.setTextColor(0xff989898);
                        HomeActivity.this.errorStatusTextView.setText(R.string.home_no_error_text);
                        if (tabActivity != null && tabActivity.troubleAnalyzeIcon != null) {
                            tabActivity.troubleAnalyzeIcon.setImageResource(R.drawable.tab_trouble_analyze);
                        }
                    } else {
                        isErrorStatus = true;
                        HomeActivity.this.errorStatusTextView.setTextColor(0xffff594b);
                        HomeActivity.this.errorStatusTextView.setText(errorCode);
                        if (tabActivity != null && tabActivity.troubleAnalyzeIcon != null) {
                            tabActivity.troubleAnalyzeIcon.setImageResource(R.drawable.tab_trouble_analyze_error);
                        }
                    }
                }
                // 状态字功能(电梯开关门)
                if (monitor.getName().equalsIgnoreCase(ApplicationConfig.STATUS_WORD_NAME)) {
                    int controllerStatus = ParseSerialsUtils.getIntFromBytes(monitor.getReceived());
                    doorAnimationView.setCurrentDirection(controllerStatus);
                }
                // 当前楼层
                if (monitor.getName().equalsIgnoreCase(ApplicationConfig.CURRENT_FLOOR_NAME)) {
                    doorAnimationView.setCurrentFloor(ParseSerialsUtils.getIntFromBytes(monitor.getReceived()));
                }
                // 系统状态
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
                        } else {
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
    public void run() {
        HomeActivity.this.syncElevatorStatus();
    }

}
