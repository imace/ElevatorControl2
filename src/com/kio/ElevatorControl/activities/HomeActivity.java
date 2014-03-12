package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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

import java.util.Timer;
import java.util.TimerTask;

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

    private Handler handler;

    private Timer timer;

    private static final String TAG = HomeActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Views.inject(this);
        setListViewDataSource();
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        HomeActivity.this.syncElevatorStatus();
                        break;
                }
                super.handleMessage(msg);
            }
        };
        timer = new Timer();
        ViewTreeObserver vto = doorAnimationView.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                Log.e("ViewWidthHeight", "Height: " + doorAnimationView.getMeasuredHeight() + " Width: " + doorAnimationView.getMeasuredWidth());
                return true;
            }
        });
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
        timer = new Timer();
        loopSyncElevatorStatusTask();
    }

    @Override
    protected void onPause() {
        super.onPause();
        timer.cancel();
    }

    @Override
    protected void onStop() {
        super.onStop();
        timer.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
        timer.purge();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        doorAnimationView.getWidth();
        Log.v(TAG, doorAnimationView.getWidth() + ":" + doorAnimationView.getHeight());
    }

    /**
     * 开始同步电梯状态信息 Task
     */
    public void loopSyncElevatorStatusTask() {
        if (HBluetooth.getInstance(HomeActivity.this).isPrepared()) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);
                }
            }, 0, 1000);
        }
    }

    /**
     * 同步电梯状态信息
     */
    private void syncElevatorStatus() {
        if (communications == null) {
            SyncItem[] items = new SyncItem[]{
                    new SyncItem(runningSpeedTextView, "1010", "0.1"),
                    //new SyncItem(doorStatusTextView, ""),
                    //new SyncItem(lockStatusTextView, ""),
                    //new SyncItem(errorStatusTextView, ""),
                    //new SyncItem(errorStatusTextView, ""),
            };
            communications = new HCommunication[items.length];
            int commandSize = items.length;
            for (int index = 0; index < commandSize; index++) {
                final SyncItem item = items[index];
                communications[index] = new HCommunication() {
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
                                Float floatValue = value * Float.parseFloat(item.scale);
                                item.value = String.format("%.2f", floatValue);
                                return item;
                            }
                        }
                        return null;
                    }
                };
            }
        }
        if (mSyncStatusHandler == null) {
            mSyncStatusHandler = new SyncStatusHandler(HomeActivity.this);
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
                holder.mShortcutName = (TextView) convertView.findViewById(R.id.shortcut_name);
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
                item.displayTextView.setText(item.value);
            }
        }
    }

    // ========================================= Sync Status Object =============================================

    /**
     * 电梯状态条目
     */
    private class SyncItem {

        TextView displayTextView;

        String value;

        String code;

        String scale;

        public SyncItem(TextView textView, String code, String scale) {
            this.displayTextView = textView;
            this.code = code;
            this.scale = scale;
        }

    }

}
