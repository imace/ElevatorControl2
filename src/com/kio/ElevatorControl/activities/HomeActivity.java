package com.kio.ElevatorControl.activities;

import android.content.Intent;
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
import com.kio.ElevatorControl.views.DoorAnimationView;
import com.kio.ElevatorControl.views.TypefaceTextView;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends Activity {

    @InjectView(R.id.list_view)
    ListView mListView;

    @InjectView(R.id.door_animation_view)
    DoorAnimationView doorAnimationView;

    @InjectView(R.id.running_speed)
    TextView runningSpeedTextView;

    @InjectView(R.id.door_status)
    TextView doorStatusTextView;

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

    private static final int RUNNING_SPEED = 0;

    private static final int DOOR_STATUS = 1;

    private static final int LOCK_STATUS = 2;

    private static final int ERROR_CODE = 3;

    private static final int CURRENT_FLOOR = 4;

    /**
     * 同步间隔
     */
    private static final int SYNC_TIME = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Views.inject(this);
        threadPause = false;
        mSyncStatusHandler = new SyncStatusHandler(HomeActivity.this);
        setListViewDataSource();
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
        if (communications == null) {
            List<SyncItem> items = new ArrayList<SyncItem>();
            items.add(new SyncItem(RUNNING_SPEED, "1010"));
            items.add(new SyncItem(ERROR_CODE, "8000"));
            items.add(new SyncItem(CURRENT_FLOOR, "1018"));
            communications = new HCommunication[items.size()];
            int index = 0;
            for (SyncItem syncItem : items) {
                final int i = index;
                final SyncItem item = syncItem;
                communications[i] = new HCommunication() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103" + item.code + "0001")));
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
                            // 通过验证
                            byte[] received = HSerial.trimEnd(getReceivedBuffer());
                            if (received.length == 8) {
                                int value = received[4];
                                value = value << 8;
                                value = value | received[5];
                                switch (item.type) {
                                    // 运行速度
                                    case RUNNING_SPEED: {
                                        Float floatValue = value * Float.parseFloat("0.001");
                                        item.value = String.format("%.2f", floatValue) + "m/s";
                                        return item;
                                    }
                                    // 错误码
                                    case ERROR_CODE: {
                                        item.value = String.format("E%02d", value);
                                        return item;
                                    }
                                    // 当前楼层
                                    case CURRENT_FLOOR: {
                                        item.value = String.valueOf(value);
                                        return item;
                                    }
                                }
                            }
                        }
                        return null;
                    }
                };
                index++;
            }
        }
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
            if (msg.obj != null && (msg.obj instanceof SyncItem)) {
                SyncItem item = (SyncItem) msg.obj;
                switch (item.type) {
                    // 运行速度
                    case RUNNING_SPEED: {
                        HomeActivity.this.runningSpeedTextView.setText(item.value);
                    }
                    break;
                    // 错误码
                    case ERROR_CODE: {
                        HomeActivity.this.errorStatusTextView.setText(item.value);
                    }
                    break;
                    // 当前楼层
                    case CURRENT_FLOOR: {
                        HomeActivity.this.currentFloorTextView.setText(item.value);
                    }
                    break;
                }
            }
        }
    }

    // ========================================= Sync Status Object =============================================

    /**
     * 电梯同步状态条目
     */
    private class SyncItem {

        int type;

        String value;

        String code;

        public SyncItem(int type, String code) {
            this.type = type;
            this.code = code;
        }

    }

}
