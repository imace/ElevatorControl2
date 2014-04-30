package com.inovance.ElevatorControl.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HHandler;
import com.hbluetooth.HSerial;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.adapters.MoveSidePagerAdapter;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.daos.ParameterSettingsDao;
import com.inovance.ElevatorControl.daos.RealTimeMonitorDao;
import com.inovance.ElevatorControl.models.ListHolder;
import com.inovance.ElevatorControl.models.ParameterSettings;
import com.inovance.ElevatorControl.models.RealTimeMonitor;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import com.inovance.ElevatorControl.views.TypefaceTextView;
import com.inovance.ElevatorControl.views.viewpager.VerticalViewPager;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-6.
 * Time: 11:03.
 */
public class MoveInsideActivity extends Activity {

    private static final String TAG = MoveInsideActivity.class.getSimpleName();

    private static final String codeType = "62";

    private List<RealTimeMonitor> realTimeMonitors;

    @InjectView(R.id.vertical_view_pager)
    VerticalViewPager viewPager;

    @InjectView(R.id.current_floor)
    TypefaceTextView currentFloorTextView;

    private MoveInsideHandler mMoveInsideHandler;

    private FloorHandler floorHandler;

    private SyncMoveInsideInfoHandler mSyncMoveInsideInfoHandler;

    private HCommunication[] communications;

    private Runnable syncTask;

    private boolean running = false;

    private Handler syncHandler = new Handler();

    private boolean isWritingData = false;

    private boolean isWriteSuccessful = false;

    private boolean isSyncing = false;

    private MoveSidePagerAdapter moveSidePagerAdapter;

    /**
     * 同步间隔
     */
    private static final int SYNC_TIME = 1000;

    private HCommunication[] getMoveInsideInfoCommunications;

    private boolean hasGetFloors = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setTitle(R.string.move_inside_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_move_inside);
        Views.inject(this);
        moveSidePagerAdapter = new MoveSidePagerAdapter(this, ApplicationConfig.DEFAULT_FLOORS);
        viewPager.setAdapter(moveSidePagerAdapter);
        viewPager.setOnPageChangeListener(new VerticalViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (moveSidePagerAdapter != null) {
                    moveSidePagerAdapter.currentPager = position;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mSyncMoveInsideInfoHandler = new SyncMoveInsideInfoHandler(this);
        mMoveInsideHandler = new MoveInsideHandler(this);
        floorHandler = new FloorHandler(this);
        realTimeMonitors = RealTimeMonitorDao.findByType(this, codeType);
        createGetFloorsCommunication();
        syncTask = new Runnable() {
            @Override
            public void run() {
                if (running) {
                    if (hasGetFloors) {
                        if (!isWritingData) {
                            MoveInsideActivity.this.syncMoveInsideInfoStatus();
                        }
                    } else {
                        MoveInsideActivity.this.loadDataAndRenderView();
                    }
                    syncHandler.postDelayed(this, SYNC_TIME);
                }
            }
        };
    }

    private void createGetMoveInsideInfoCommunications() {
        if (getMoveInsideInfoCommunications == null) {
            final List<RealTimeMonitor> monitorLists = RealTimeMonitorDao
                    .findByNames(this, ApplicationConfig.moveInsideInfoName);
            if (monitorLists.size() == ApplicationConfig.moveInsideInfoName.length) {
                getMoveInsideInfoCommunications = new HCommunication[2];
                final int index01 = 0;
                final int index02 = 1;
                final int length01 = 1;
                final int length02 = 6;
                final RealTimeMonitor monitor01 = monitorLists.get(index01);
                final RealTimeMonitor monitor02 = monitorLists.get(index02);
                for (int i = 0; i < 2; i++) {
                    String hexString = "";
                    if (i == 0) {
                        hexString = "0103"
                                + monitor01.getCode()
                                + String.format("%04x", length01)
                                + "0001";
                    }
                    if (i == 1) {
                        hexString = "0103"
                                + monitor02.getCode()
                                + String.format("%04x", length02)
                                + "0001";
                    }
                    final String sendCode = hexString;
                    final int index = i;
                    getMoveInsideInfoCommunications[i] = new HCommunication() {
                        @Override
                        public void beforeSend() {
                            this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints(sendCode)));
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
                                byte[] data = HSerial.trimEnd(getReceivedBuffer());
                                short bytesLength = ByteBuffer.wrap(new byte[]{data[2], data[3]}).getShort();
                                if (index == 0) {
                                    if (length01 * 2 == bytesLength) {
                                        List<RealTimeMonitor> tempList = new ArrayList<RealTimeMonitor>();
                                        byte[] tempData = HSerial.crc16(HSerial.hexStr2Ints("01030002"
                                                + HSerial.byte2HexStr(new byte[]{data[4], data[5]})));
                                        try {
                                            RealTimeMonitor monitor = (RealTimeMonitor) monitor01.clone();
                                            monitor.setReceived(tempData);
                                            tempList.add(monitor);
                                            ListHolder holder = new ListHolder();
                                            holder.setRealTimeMonitorList(tempList);
                                            return holder;
                                        } catch (CloneNotSupportedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                if (index == 1) {
                                    if (length02 * 2 == bytesLength) {
                                        List<RealTimeMonitor> tempList = new ArrayList<RealTimeMonitor>();
                                        for (int j = 0; j < length02; j++) {
                                            byte[] tempData = HSerial.crc16(HSerial.hexStr2Ints("01030002"
                                                    + HSerial.byte2HexStr(new byte[]{data[4
                                                    + j * 2], data[5 + j * 2]})));
                                            try {
                                                RealTimeMonitor monitor = (RealTimeMonitor)
                                                        monitorLists.get(j + index02).clone();
                                                monitor.setReceived(tempData);
                                                tempList.add(monitor);
                                            } catch (CloneNotSupportedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        ListHolder holder = new ListHolder();
                                        holder.setRealTimeMonitorList(tempList);
                                        return holder;
                                    }
                                }
                            }
                            return null;
                        }
                    };
                }
            }
        }
    }

    /**
     * 同步当前电梯召唤信息
     */
    private void syncMoveInsideInfoStatus() {
        if (!isSyncing) {
            if (HBluetooth.getInstance(MoveInsideActivity.this).isPrepared()) {
                if (mSyncMoveInsideInfoHandler != null && getMoveInsideInfoCommunications != null) {
                    MoveInsideActivity.this.isSyncing = true;
                    mSyncMoveInsideInfoHandler.sendCount = getMoveInsideInfoCommunications.length;
                    HBluetooth.getInstance(MoveInsideActivity.this)
                            .setHandler(mSyncMoveInsideInfoHandler)
                            .setCommunications(getMoveInsideInfoCommunications)
                            .Start();
                } else {
                    errorHandler.sendEmptyMessage(0);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        running = true;
        syncHandler.postDelayed(syncTask, SYNC_TIME);
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

    /**
     * Create Get Floors Communication
     */
    private void createGetFloorsCommunication() {
        ArrayList<String> names = new ArrayList<String>();
        names.add(ApplicationConfig.GET_FLOOR_NAME);
        List<ParameterSettings> settingsList = ParameterSettingsDao.findByNames(MoveInsideActivity.this,
                names.toArray(new String[names.size()]));
        final String code = settingsList.get(0).getCode() + "0002";
        communications = new HCommunication[]{
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

    // 开门
    @OnClick(R.id.open_door_button)
    void openDoorButtonClick() {
        HCommunication[] communications = new HCommunication[]{
                new HCommunication() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103F6010001")));
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
        if (HBluetooth.getInstance(MoveInsideActivity.this).isPrepared()) {
            HBluetooth.getInstance(MoveInsideActivity.this)
                    .setCommunications(communications)
                    .Start();
        } else {
            Toast.makeText(this,
                    R.string.not_connect_device_error,
                    android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
    }

    // 关门
    @OnClick(R.id.close_door_button)
    void closeDoorButtonClick() {
        HCommunication[] communications = new HCommunication[]{
                new HCommunication() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103F6010001")));
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
        if (HBluetooth.getInstance(MoveInsideActivity.this).isPrepared()) {
            HBluetooth.getInstance(MoveInsideActivity.this)
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
     * 召唤楼层
     *
     * @param floor Floor
     */
    private void moveInsideCallFloor(int floor) {
        final String[] codeArray = getCallCode(floor);
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
                        if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                            byte[] received = HSerial.trimEnd(getReceivedBuffer());
                            return HSerial.byte2HexStr(received);
                        }
                        return null;
                    }
                }
        };
        if (HBluetooth.getInstance(MoveInsideActivity.this).isPrepared()) {
            floorHandler.writeCode = codeArray[0];
            HBluetooth.getInstance(MoveInsideActivity.this)
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
    private String[] getCallCode(int floor) {
        int section_location = 0;
        String searchCondition = "";
        String[] conditions = new String[]{"1-8",
                "9-16",
                "17-24",
                "25-32",
                "33-40",
                "41-48"};
        for (String condition : conditions) {
            String[] parts = condition.split("-");
            if (floor >= Integer.parseInt(parts[0]) && floor <= Integer.parseInt(parts[1])) {
                searchCondition = condition + "层信息";
                section_location = floor - Integer.parseInt(parts[0]);
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

    /**
     * 取得电梯层数
     */
    private void loadDataAndRenderView() {
        if (communications != null) {
            if (HBluetooth.getInstance(MoveInsideActivity.this).isPrepared()) {
                HBluetooth.getInstance(MoveInsideActivity.this)
                        .setHandler(mMoveInsideHandler)
                        .setCommunications(communications)
                        .Start();
            } else {
                errorHandler.sendEmptyMessage(0);
            }
        }
    }

    private Handler errorHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0: {
                    Toast.makeText(MoveInsideActivity.this,
                            R.string.not_connect_device_error,
                            android.widget.Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            }
            super.handleMessage(msg);
        }

    };

    // ================================= MoveInside handler ========================================== //
    private class MoveInsideHandler extends HHandler {

        public MoveInsideHandler(Activity activity) {
            super(activity);
            TAG = MoveInsideHandler.class.getSimpleName();
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
                    moveSidePagerAdapter = new MoveSidePagerAdapter(MoveInsideActivity.this,
                            new int[]{bottom, top});
                    moveSidePagerAdapter.setOnSelectFloorListener(new MoveSidePagerAdapter.onSelectFloorListener() {
                        @Override
                        public void onSelect(int floor) {
                            final int calledFloor = floor;
                            MoveInsideActivity.this.isSyncing = false;
                            MoveInsideActivity.this.isWritingData = true;
                            MoveInsideActivity.this.isWriteSuccessful = false;
                            new CountDownTimer(1500, 500) {
                                public void onTick(long millisUntilFinished) {
                                    if (!MoveInsideActivity.this.isWriteSuccessful) {
                                        MoveInsideActivity.this.moveInsideCallFloor(calledFloor);
                                    } else {
                                        MoveInsideActivity.this.isWritingData = false;
                                    }
                                }

                                public void onFinish() {
                                    if (!MoveInsideActivity.this.isWriteSuccessful) {
                                        Toast.makeText(MoveInsideActivity.this,
                                                R.string.write_failed_text,
                                                android.widget.Toast.LENGTH_SHORT).show();
                                    }
                                    MoveInsideActivity.this.isWritingData = false;
                                }
                            }.start();
                        }
                    });
                    MoveInsideActivity.this.viewPager.setAdapter(moveSidePagerAdapter);
                    MoveInsideActivity.this.createGetMoveInsideInfoCommunications();
                    MoveInsideActivity.this.hasGetFloors = true;
                }
            }
        }

    }

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
                    MoveInsideActivity.this.isWriteSuccessful = true;
                    MoveInsideActivity.this.isWritingData = false;
                }
            }
        }

    }

    // ============================== 同步内召信息 ================================================ //
    private class SyncMoveInsideInfoHandler extends HHandler {

        public int sendCount;

        private int receiveCount;

        private List<ListHolder> holderList;

        public SyncMoveInsideInfoHandler(Activity activity) {
            super(activity);
            TAG = SyncMoveInsideInfoHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
            holderList = new ArrayList<ListHolder>();
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (sendCount == receiveCount) {
                List<RealTimeMonitor> monitorList = new ArrayList<RealTimeMonitor>();
                for (ListHolder holder : holderList) {
                    monitorList.addAll(holder.getRealTimeMonitorList());
                }
                List<Integer> calledFloorList = new ArrayList<Integer>();
                for (RealTimeMonitor monitor : monitorList) {
                    if (monitor.getName().equalsIgnoreCase(ApplicationConfig.moveInsideInfoName[0])) {
                        MoveInsideActivity.this.currentFloorTextView
                                .setText(String.valueOf(ParseSerialsUtils.getIntFromBytes(monitor.getReceived())));
                    } else {
                        String callFloor = HSerial.byte2HexStr(new byte[]{monitor.getReceived()[4],
                                monitor.getReceived()[5]});
                        int length01 = ApplicationConfig.MOVE_SIDE_CODE.length;
                        for (int m = 0; m < length01; m++) {
                            if (callFloor.equalsIgnoreCase(ApplicationConfig.MOVE_SIDE_CODE[m])) {
                                int length02 = ApplicationConfig.moveInsideName.length;
                                for (int n = 0; n < length02; n++) {
                                    if (monitor.getName().equalsIgnoreCase(ApplicationConfig.moveInsideName[n])) {
                                        // 召唤的楼层
                                        calledFloorList.add(n * 8 + m + 1);
                                    }
                                }
                            }
                        }
                    }
                }
                if (calledFloorList.size() > 0) {
                    if (moveSidePagerAdapter != null) {
                        moveSidePagerAdapter.updateCurrentCalledFloor(calledFloorList);
                    }
                }
            }
            MoveInsideActivity.this.isSyncing = false;
        }

        @Override
        public void onTalkReceive(Message msg) {
            if (msg.obj != null && msg.obj instanceof ListHolder) {
                holderList.add((ListHolder) msg.obj);
                receiveCount++;
            }
        }

        @Override
        public void onTalkError(Message msg) {
            super.onTalkError(msg);
        }
    }
}