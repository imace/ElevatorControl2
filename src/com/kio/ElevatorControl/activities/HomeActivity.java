package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HHandler;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.daos.RealTimeMonitorDao;
import com.kio.ElevatorControl.models.RealTimeMonitor;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;
import com.kio.ElevatorControl.views.DoorAnimationView;
import com.kio.ElevatorControl.views.TypefaceTextView;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.TextView;

import java.util.List;

public class HomeActivity extends Activity {

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

    private Thread syncThread;

    private boolean threadPause;

    private static final String TAG = HomeActivity.class.getSimpleName();

    private List<RealTimeMonitor> monitorLists;

    /**
     * 同步间隔
     */
    private static final int SYNC_TIME = 600;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Views.inject(this);
        threadPause = false;
        mSyncStatusHandler = new SyncStatusHandler(HomeActivity.this);
        setListViewDataSource();
        readMonitorStateCode();
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
                        Log.v(TAG, HSerial.byte2HexStr(received));
                        RealTimeMonitor monitor = (RealTimeMonitor) monitorLists.get(i).clone();
                        monitor.setReceived(received);
                        return monitor;
                    }
                    return null;
                }
            };
        }
    }

    @OnClick(R.id.door_button)
    void openDoor() {
        doorAnimationView.openDoor();
    }

    @OnClick(R.id.close_door_button)
    void closeDoor() {
        doorAnimationView.closeDoor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        threadPause = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        threadPause = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        threadPause = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        threadPause = true;
        syncThread.interrupt();
    }

    /**
     * 开始同步电梯状态信息 Task
     */
    public void loopSyncElevatorStatusTask() {
        if (HBluetooth.getInstance(HomeActivity.this).isPrepared()) {
            syncThread = new Thread() {
                public void run() {
                    while (true) {
                        if (!threadPause) {
                            HomeActivity.this.syncElevatorStatus();
                        }
                        try {
                            Thread.sleep(SYNC_TIME);
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Bluetooth Thread Error", e);
                        }
                    }
                }
            };
            syncThread.start();
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
        }
    }

    public void setListViewDataSource() {
        ShortcutListViewAdapter adapter = new ShortcutListViewAdapter();
        mListView.setAdapter(adapter);
    }

    // ==================================== Shortcut ListView Adapter =====================================

    /**
     * 快捷菜单 Adapter
     */
    private class ShortcutListViewAdapter extends BaseAdapter {

        public ShortcutListViewAdapter() {

        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            LayoutInflater mInflater = LayoutInflater.from(HomeActivity.this);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.home_list_view_item, null);
                holder = new ViewHolder();
                holder.mShortcutName = (TextView) convertView.findViewById(R.id.shortcut);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mShortcutName.setText("快捷菜单");
            return convertView;
        }

        private class ViewHolder {
            TextView mShortcutName;
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
                    HomeActivity.this.errorStatusTextView
                            .setText(ParseSerialsUtils.getErrorCode(monitor.getReceived()));
                } else if (monitor.getName().equalsIgnoreCase(ApplicationConfig.CURRENT_FLOOR_NAME)) {
                    HomeActivity.this.currentFloorTextView
                            .setText(ParseSerialsUtils.getIntString(monitor));
                } else if (monitor.getName().equalsIgnoreCase(ApplicationConfig.SYSTEM_STATUS_NAME)) {
                    switch (ParseSerialsUtils.getSystemStatusCode(monitor)){
                        case 1:
                            HomeActivity.this.lockStatusTextView
                                    .setText("开门");
                            HomeActivity.this.doorAnimationView.openDoor();
                            break;
                        case 3:
                            HomeActivity.this.lockStatusTextView
                                    .setText("关门");
                            HomeActivity.this.doorAnimationView.closeDoor();
                            break;
                    }
                }
            }
        }
    }

}
