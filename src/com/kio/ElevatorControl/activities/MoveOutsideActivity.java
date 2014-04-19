package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HHandler;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.adapters.MoveSidePagerAdapter;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.daos.ParameterSettingsDao;
import com.kio.ElevatorControl.daos.RealTimeMonitorDao;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.models.RealTimeMonitor;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;
import com.kio.ElevatorControl.views.TypefaceTextView;
import com.kio.ElevatorControl.views.viewpager.VerticalViewPager;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-6.
 * Time: 11:04.
 */
public class MoveOutsideActivity extends Activity {

    private static final String TAG = MoveOutsideActivity.class.getSimpleName();

    @InjectView(R.id.vertical_view_pager)
    VerticalViewPager viewPager;

    @InjectView(R.id.current_floor)
    TypefaceTextView currentFloorTextView;

    private static final String codeType = "64";

    private List<RealTimeMonitor> realTimeMonitors;

    private MoveOutsideHandler mMoveOutsideHandler;

    private HCommunication[] getFloorsCommunications;

    private boolean hasGetFloors = false;

    private int selectedFloor;

    private SyncStatusHandler mSyncStatusHandler;

    private Runnable syncTask;

    private boolean running = false;

    private Handler syncHandler = new Handler();

    private FloorHandler floorHandler;

    private boolean isWritingData = false;

    private boolean isWriteSuccessful = false;

    /**
     * 同步间隔
     */
    private static final int SYNC_TIME = 1000;

    private HCommunication[] getCurrentFloorCommunications;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setTitle(R.string.move_outside_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_move_outside);
        Views.inject(this);
        final MoveSidePagerAdapter adapter = new MoveSidePagerAdapter(this, ApplicationConfig.DEFAULT_FLOORS);
        viewPager.setAdapter(adapter);
        viewPager.setOnPageChangeListener(new VerticalViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                adapter.currentPager = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mMoveOutsideHandler = new MoveOutsideHandler(this);
        floorHandler = new FloorHandler(this);
        realTimeMonitors = RealTimeMonitorDao.findByType(this, codeType);
        createGetFloorsCommunication();
        syncTask = new Runnable() {
            @Override
            public void run() {
                if (running) {
                    if (hasGetFloors) {
                        if (!isWritingData) {
                            MoveOutsideActivity.this.syncElevatorCurrentFloorStatus();
                        }
                    } else {
                        MoveOutsideActivity.this.loadDataAndRenderView();
                    }
                    errorHandler.postDelayed(this, SYNC_TIME);
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        running = true;
        syncHandler.postDelayed(syncTask, SYNC_TIME);
    }

    private void createGetCurrentFloorCommunications() {
        if (getCurrentFloorCommunications == null) {
            final List<RealTimeMonitor> monitorLists = RealTimeMonitorDao
                    .findByNames(this, new String[]{
                            ApplicationConfig.CURRENT_FLOOR_NAME
                    });
            if (monitorLists.size() == 1) {
                getCurrentFloorCommunications = new HCommunication[]{
                        new HCommunication() {
                            @Override
                            public void beforeSend() {
                                this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103"
                                        + monitorLists.get(0).getCode()
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
                                        monitor = (RealTimeMonitor) monitorLists.get(0).clone();
                                        monitor.setReceived(received);
                                        return monitor;
                                    } catch (CloneNotSupportedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                return null;
                            }
                        }
                };
            }
        }
        mSyncStatusHandler = new SyncStatusHandler(this);
    }

    /**
     * 同步当前楼层
     */
    private void syncElevatorCurrentFloorStatus() {
        if (HBluetooth.getInstance(MoveOutsideActivity.this).isPrepared()) {
            if (mSyncStatusHandler != null && getCurrentFloorCommunications != null) {
                HBluetooth.getInstance(MoveOutsideActivity.this)
                        .setHandler(mSyncStatusHandler)
                        .setCommunications(getCurrentFloorCommunications)
                        .Start();
            } else {
                errorHandler.sendEmptyMessage(0);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.up_button)
    void OnUpButtonClick(View view) {
        MoveOutsideActivity.this.isWritingData = true;
        MoveOutsideActivity.this.isWriteSuccessful = false;
        new CountDownTimer(1500, 500) {
            public void onTick(long millisUntilFinished) {
                if (!MoveOutsideActivity.this.isWriteSuccessful) {
                    moveOutsideCallFloor(selectedFloor, true);
                } else {
                    MoveOutsideActivity.this.isWritingData = false;
                }
            }

            public void onFinish() {
                if (!MoveOutsideActivity.this.isWriteSuccessful) {
                    Toast.makeText(MoveOutsideActivity.this,
                            R.string.write_failed_text,
                            android.widget.Toast.LENGTH_SHORT).show();
                }
                MoveOutsideActivity.this.isWritingData = true;
            }
        }.start();
    }

    @OnClick(R.id.down_button)
    void OnDownButtonClick(View view) {
        MoveOutsideActivity.this.isWritingData = true;
        MoveOutsideActivity.this.isWriteSuccessful = false;
        new CountDownTimer(1500, 500) {
            public void onTick(long millisUntilFinished) {
                if (!MoveOutsideActivity.this.isWriteSuccessful) {
                    moveOutsideCallFloor(selectedFloor, false);
                } else {
                    MoveOutsideActivity.this.isWritingData = false;
                }
            }

            public void onFinish() {
                if (!MoveOutsideActivity.this.isWriteSuccessful) {
                    Toast.makeText(MoveOutsideActivity.this,
                            R.string.write_failed_text,
                            android.widget.Toast.LENGTH_SHORT).show();
                }
                MoveOutsideActivity.this.isWritingData = true;
            }
        }.start();
    }

    /**
     * Create Get Floors Communication
     */
    private void createGetFloorsCommunication() {
        ArrayList<String> names = new ArrayList<String>();
        names.add(ApplicationConfig.GET_FLOOR_NAME);
        List<ParameterSettings> settingsList = ParameterSettingsDao.findByNames(MoveOutsideActivity.this, names);
        final String code = settingsList.get(0).getCode() + "0002";
        getFloorsCommunications = new HCommunication[]{
                new HCommunication() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103"
                                + code
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
                            return HSerial.trimEnd(getReceivedBuffer());
                        }
                        return null;
                    }
                }
        };
    }

    private void loadDataAndRenderView() {
        if (getFloorsCommunications != null) {
            if (HBluetooth.getInstance(MoveOutsideActivity.this).isPrepared()) {
                HBluetooth.getInstance(MoveOutsideActivity.this)
                        .setHandler(mMoveOutsideHandler)
                        .setCommunications(getFloorsCommunications)
                        .Start();
            } else {
                errorHandler.sendEmptyMessage(0);
            }
        }
    }

    /**
     * 外召
     *
     * @param floor Floor
     * @param isUp  Is up
     */
    private void moveOutsideCallFloor(int floor, boolean isUp) {
        final String[] codeArray = getCallCode(floor, isUp);
        HCommunication[] communications = new HCommunication[]{
                new HCommunication() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0106"
                                + codeArray[0]
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
                }
        };
        if (HBluetooth.getInstance(MoveOutsideActivity.this).isPrepared()) {
            floorHandler.writeCode = codeArray[0];
            HBluetooth.getInstance(MoveOutsideActivity.this)
                    .setHandler(floorHandler)
                    .setCommunications(communications)
                    .Start();
        } else {
            Toast.makeText(this,
                    R.string.not_connect_device_error,
                    android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * 取得需要发送的Code
     *
     * @param position GridView Item Index
     * @return String[]
     */
    private String[] getCallCode(int floor, boolean isUp) {
        int section_location = 0;
        String searchCondition = "";
        String[] conditions = new String[]{"1~4",
                "5~8",
                "9~12",
                "13~16",
                "17~20",
                "21~24",
                "25~28",
                "29~32",
                "33~36",
                "37~40",
                "41~44",
                "45~48"};
        for (String condition : conditions) {
            String[] parts = condition.split("~");
            if (floor >= Integer.parseInt(parts[0]) && floor <= Integer.parseInt(parts[1])) {
                searchCondition = condition + "层召唤信息";
                int minus = floor - Integer.parseInt(parts[0]);
                section_location = isUp ? minus * 2 : (2 * minus + 1);
            }
        }
        String code = "";
        int offset = 0;
        int location = 0;
        for (RealTimeMonitor stateCode : realTimeMonitors) {
            if (searchCondition.equalsIgnoreCase(stateCode.getName())) {
                code = stateCode.getCode() + ApplicationConfig.MOVE_SIDE_CODE[section_location];
                location = offset;
            }
            offset++;
        }
        return new String[]{code, String.valueOf(location)};
    }

    private Handler errorHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0: {
                    Toast.makeText(MoveOutsideActivity.this,
                            R.string.not_connect_device_error,
                            android.widget.Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            }
            super.handleMessage(msg);
        }

    };

    // ==================================== 召唤楼层 =================================================//

    /**
     * 召唤楼层
     */
    private class FloorHandler extends HHandler {

        public String writeCode;

        public FloorHandler(Activity activity) {
            super(activity);
            TAG = FloorHandler.class.getSimpleName();
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
            if (msg.obj != null && msg.obj instanceof String) {
                if (((String) msg.obj).contains(writeCode)) {
                    MoveOutsideActivity.this.isWriteSuccessful = true;
                }
            }
        }

    }

    // =================================== MoveOutside Handler ==============================

    /**
     * 蓝牙 Socket handler
     */
    private class MoveOutsideHandler extends HHandler {

        public MoveOutsideHandler(Activity activity) {
            super(activity);
            TAG = MoveInsideActivity.class.getSimpleName();
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
            if (msg.obj != null && msg.obj instanceof byte[]) {
                byte[] data = (byte[]) msg.obj;
                int length = ByteBuffer.wrap(new byte[]{data[2], data[3]}).getShort();
                if (length == 4) {
                    int top = ByteBuffer.wrap(new byte[]{data[4], data[5]}).getShort();
                    int bottom = ByteBuffer.wrap(new byte[]{data[6], data[7]}).getShort();
                    MoveSidePagerAdapter adapter = new MoveSidePagerAdapter(MoveOutsideActivity.this,
                            new int[]{bottom, top});
                    adapter.setOnSelectFloorListener(new MoveSidePagerAdapter.onSelectFloorListener() {
                        @Override
                        public void onSelect(int floor) {
                            MoveOutsideActivity.this.selectedFloor = floor;
                        }
                    });
                    MoveOutsideActivity.this.viewPager.setAdapter(adapter);
                    MoveOutsideActivity.this.createGetCurrentFloorCommunications();
                    MoveOutsideActivity.this.hasGetFloors = true;
                }
            }
        }

    }

    // ============================== 同步当前楼层 ================================================ //
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
            if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
                RealTimeMonitor monitor = (RealTimeMonitor) msg.obj;
                MoveOutsideActivity.this.currentFloorTextView
                        .setText(String.valueOf(ParseSerialsUtils.getIntFromBytes(monitor.getReceived())));
            }
        }
    }

}