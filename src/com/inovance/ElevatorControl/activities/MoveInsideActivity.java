package com.inovance.ElevatorControl.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Views;
import com.bluetoothtool.BluetoothHandler;
import com.bluetoothtool.BluetoothTalk;
import com.bluetoothtool.BluetoothTool;
import com.bluetoothtool.SerialUtility;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.adapters.MoveSidePagerAdapter;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.daos.ParameterSettingsDao;
import com.inovance.ElevatorControl.daos.RealTimeMonitorDao;
import com.inovance.ElevatorControl.handlers.GlobalHandler;
import com.inovance.ElevatorControl.models.ObjectListHolder;
import com.inovance.ElevatorControl.models.ParameterSettings;
import com.inovance.ElevatorControl.models.RealTimeMonitor;
import com.inovance.ElevatorControl.utils.LogUtils;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import com.inovance.ElevatorControl.views.TypefaceTextView;
import com.inovance.ElevatorControl.views.viewpager.VerticalViewPager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by IntelliJ IDEA.
 * 电梯内召
 * User: keith.
 * Date: 14-3-6.
 * Time: 11:03.
 */
public class MoveInsideActivity extends Activity implements Runnable {

    private static final String TAG = MoveInsideActivity.class.getSimpleName();

    /**
     * 内召指令
     */
    private static final String codeType = "62";

    /**
     * 用于召唤楼层的指令列表
     */
    private List<RealTimeMonitor> realTimeMonitors;

    /**
     * View Pager
     */
    @InjectView(R.id.vertical_view_pager)
    VerticalViewPager viewPager;

    /**
     * 当前楼层
     */
    @InjectView(R.id.current_floor)
    TypefaceTextView currentFloorTextView;

    /**
     * 开门按钮
     */
    @InjectView(R.id.open_door_button)
    ImageButton openDoorButton;

    /**
     * 关门按钮
     */
    @InjectView(R.id.close_door_button)
    ImageButton closeDoorButton;

    /**
     * 获取电梯最高层、最底层进度指示
     */
    @InjectView(R.id.load_view)
    LinearLayout loadView;

    /**
     * 获取电梯最高层、最底层 Handler
     */
    private MoveInsideHandler mMoveInsideHandler;

    /**
     * 召唤楼层 Handler
     */
    private FloorHandler floorHandler;

    /**
     * 同步电梯召唤状态 Handler
     */
    private SyncMoveInsideInfoHandler mSyncMoveInsideInfoHandler;

    /**
     * 取得电梯最高层、最底层通信内容
     */
    private BluetoothTalk[] communications;

    /**
     * 同步电梯召唤状态 Task
     */
    private Runnable syncTask;

    /**
     * 是否暂停同步 Task
     */
    private boolean running = false;

    /**
     * 用于同步的 Handler
     */
    private Handler syncHandler = new Handler();

    /**
     * 是否正在召唤楼层
     */
    private boolean isWritingData = false;

    /**
     * 是否召唤成功
     */
    private boolean isWriteSuccessful = false;

    /**
     * 是否正在同步电梯召唤信息
     */
    private boolean isSyncing = false;

    /**
     * Vertical View Pager Adapter
     */
    private MoveSidePagerAdapter moveSidePagerAdapter;

    private ExecutorService pool = Executors.newSingleThreadExecutor();

    /**
     * 同步间隔
     */
    private static final int SYNC_TIME = 1000;

    /**
     * 获取电梯召唤状态的通信内容
     */
    private BluetoothTalk[] getMoveInsideInfoCommunications;

    /**
     * 是否已经获取到电梯最高层、最底层
     */
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
                    if (BluetoothTool.getInstance(MoveInsideActivity.this).isPrepared()) {
                        pool.execute(MoveInsideActivity.this);
                        syncHandler.postDelayed(this, SYNC_TIME);
                    }
                }
            }
        };
        openDoorButton.setEnabled(false);
        closeDoorButton.setEnabled(false);
    }

    /**
     * 生成用于读取电梯召唤信息的通信内容
     */
    private void createGetMoveInsideInfoCommunications() {
        if (getMoveInsideInfoCommunications == null) {
            final List<RealTimeMonitor> monitorLists = RealTimeMonitorDao
                    .findByNames(this, ApplicationConfig.moveInsideInfoName);
            if (monitorLists.size() == ApplicationConfig.moveInsideInfoName.length) {
                getMoveInsideInfoCommunications = new BluetoothTalk[2];
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
                    getMoveInsideInfoCommunications[i] = new BluetoothTalk() {
                        @Override
                        public void beforeSend() {
                            this.setSendBuffer(SerialUtility.crc16(SerialUtility.hexStr2Ints(sendCode)));
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
                                if (index == 0) {
                                    if (length01 * 2 == bytesLength) {
                                        List<RealTimeMonitor> tempList = new ArrayList<RealTimeMonitor>();
                                        byte[] tempData = SerialUtility.crc16(SerialUtility.hexStr2Ints("01030002"
                                                + SerialUtility.byte2HexStr(new byte[]{data[4], data[5]})));
                                        try {
                                            RealTimeMonitor monitor = (RealTimeMonitor) monitor01.clone();
                                            monitor.setReceived(tempData);
                                            tempList.add(monitor);
                                            ObjectListHolder holder = new ObjectListHolder();
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
                                            byte[] tempData = SerialUtility.crc16(SerialUtility.hexStr2Ints("01030002"
                                                    + SerialUtility.byte2HexStr(new byte[]{data[4
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
                                        ObjectListHolder holder = new ObjectListHolder();
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
            if (BluetoothTool.getInstance(MoveInsideActivity.this).isPrepared()) {
                if (mSyncMoveInsideInfoHandler != null && getMoveInsideInfoCommunications != null) {
                    MoveInsideActivity.this.isSyncing = true;
                    mSyncMoveInsideInfoHandler.sendCount = getMoveInsideInfoCommunications.length;
                    BluetoothTool.getInstance(MoveInsideActivity.this)
                            .setHandler(mSyncMoveInsideInfoHandler)
                            .setCommunications(getMoveInsideInfoCommunications)
                            .send();
                } else {
                    GlobalHandler.getInstance(MoveInsideActivity.this)
                            .sendMessage(GlobalHandler.NOT_CONNECTED);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BluetoothTool.getInstance(this).isPrepared()) {
            running = true;
            syncHandler.postDelayed(syncTask, SYNC_TIME);
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
                BluetoothTool.getInstance(MoveInsideActivity.this)
                        .setHandler(null);
                setResult(RESULT_OK);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 生成用于取得电梯最高层和最底层的通信内容
     */
    private void createGetFloorsCommunication() {
        ArrayList<String> names = new ArrayList<String>();
        names.add(ApplicationConfig.GET_FLOOR_NAME);
        List<ParameterSettings> settingsList = ParameterSettingsDao.findByNames(MoveInsideActivity.this,
                names.toArray(new String[names.size()]));
        final String code = settingsList.get(0).getCode() + "0002";
        communications = new BluetoothTalk[]{
                new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16(SerialUtility.hexStr2Ints("0103"
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
                        if (SerialUtility.isCRC16Valid(getReceivedBuffer())) {
                            return getReceivedBuffer();
                        }
                        return null;
                    }
                }
        };
    }

    /**
     * 读取到电梯最高层和最底层数据
     *
     * @param data byte[]
     */
    private void onGetFloors(byte[] data) {
        if (data.length == 10) {
            int length = ByteBuffer.wrap(new byte[]{data[2], data[3]}).getShort();
            if (length == 4) {
                int top = ByteBuffer.wrap(new byte[]{data[4], data[5]}).getShort();
                int bottom = ByteBuffer.wrap(new byte[]{data[6], data[7]}).getShort();
                moveSidePagerAdapter = new MoveSidePagerAdapter(MoveInsideActivity.this,
                        new int[]{bottom, top});
                moveSidePagerAdapter.setOnSelectFloorListener(new MoveSidePagerAdapter.OnSelectFloorListener() {
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
                                    this.cancel();
                                    this.onFinish();
                                }
                            }

                            public void onFinish() {
                                if (!MoveInsideActivity.this.isWriteSuccessful) {
                                    GlobalHandler.getInstance(MoveInsideActivity.this)
                                            .sendMessage(GlobalHandler.WRITE_DATA_FAILED);
                                }
                                MoveInsideActivity.this.isWritingData = false;
                            }
                        }.start();
                    }
                });
                MoveInsideActivity.this.viewPager.setAdapter(moveSidePagerAdapter);
                MoveInsideActivity.this.createGetMoveInsideInfoCommunications();
                MoveInsideActivity.this.hasGetFloors = true;
                MoveInsideActivity.this.loadView.setVisibility(View.GONE);
                MoveInsideActivity.this.viewPager.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 开门
     */
    @OnClick(R.id.open_door_button)
    void openDoorButtonClick() {
        BluetoothTalk[] communications = new BluetoothTalk[]{
                new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16(SerialUtility.hexStr2Ints("0103F6010001")));
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
        if (BluetoothTool.getInstance(MoveInsideActivity.this).isPrepared()) {
            BluetoothTool.getInstance(MoveInsideActivity.this)
                    .setCommunications(communications)
                    .send();
        } else {
            GlobalHandler.getInstance(MoveInsideActivity.this)
                    .sendMessage(GlobalHandler.NOT_CONNECTED);
        }
    }

    /**
     * 关门
     */
    @OnClick(R.id.close_door_button)
    void closeDoorButtonClick() {
        BluetoothTalk[] communications = new BluetoothTalk[]{
                new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16(SerialUtility.hexStr2Ints("0103F6010001")));
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
        if (BluetoothTool.getInstance(MoveInsideActivity.this).isPrepared()) {
            BluetoothTool.getInstance(MoveInsideActivity.this)
                    .setCommunications(communications)
                    .send();
        }
    }

    /**
     * 召唤楼层
     *
     * @param floor Floor
     */
    private void moveInsideCallFloor(int floor) {
        final String[] codeArray = getCallCode(floor);
        BluetoothTalk[] communications = new BluetoothTalk[]{
                new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16(SerialUtility.hexStr2Ints("0106"
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
                        if (SerialUtility.isCRC16Valid(getReceivedBuffer())) {
                            return getReceivedBuffer();
                        }
                        return null;
                    }
                }
        };
        if (BluetoothTool.getInstance(MoveInsideActivity.this).isPrepared()) {
            floorHandler.writeCode = codeArray[0];
            floorHandler.floor = floor;
            BluetoothTool.getInstance(MoveInsideActivity.this)
                    .setHandler(floorHandler)
                    .setCommunications(communications)
                    .send();
        }
    }

    /**
     * 取得需要发送的用于召唤楼层的指令
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
            if (BluetoothTool.getInstance(MoveInsideActivity.this).isPrepared()) {
                BluetoothTool.getInstance(MoveInsideActivity.this)
                        .setHandler(mMoveInsideHandler)
                        .setCommunications(communications)
                        .send();
            }
        }
    }

    @Override
    public void run() {
        if (hasGetFloors) {
            if (!isWritingData) {
                MoveInsideActivity.this.syncMoveInsideInfoStatus();
            }
        } else {
            MoveInsideActivity.this.loadDataAndRenderView();
        }
    }

    // ================================= 获取电梯最高层和最底层 ========================================== //
    private class MoveInsideHandler extends BluetoothHandler {

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
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof byte[]) {
                MoveInsideActivity.this.onGetFloors((byte[]) msg.obj);
            }
        }

    }

    // ==================================== 召唤楼层 =================================================//

    /**
     * 召唤楼层
     */
    private class FloorHandler extends BluetoothHandler {

        public String writeCode;

        public int floor;

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
                String receive = (String) msg.obj;
                if (receive.contains(writeCode)) {
                    MoveInsideActivity.this.isWriteSuccessful = true;
                    MoveInsideActivity.this.isWritingData = false;
                    // 写入内召日志
                    LogUtils.getInstance().write(ApplicationConfig.LogMoveInside, writeCode, receive, floor);
                }
            }
        }

    }

    // ============================== 同步内召信息 ================================================ //
    private class SyncMoveInsideInfoHandler extends BluetoothHandler {

        public int sendCount;

        private int receiveCount;

        private List<ObjectListHolder> holderList;

        public SyncMoveInsideInfoHandler(Activity activity) {
            super(activity);
            TAG = SyncMoveInsideInfoHandler.class.getSimpleName();
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
                List<RealTimeMonitor> monitorList = new ArrayList<RealTimeMonitor>();
                for (ObjectListHolder holder : holderList) {
                    monitorList.addAll(holder.getRealTimeMonitorList());
                }
                List<Integer> calledFloorList = new ArrayList<Integer>();
                for (RealTimeMonitor monitor : monitorList) {
                    if (monitor.getName().equalsIgnoreCase(ApplicationConfig.moveInsideInfoName[0])) {
                        MoveInsideActivity.this.currentFloorTextView
                                .setText(String.valueOf(ParseSerialsUtils.getIntFromBytes(monitor.getReceived())));
                    } else {
                        String callFloor = SerialUtility.byte2HexStr(new byte[]{monitor.getReceived()[4],
                                monitor.getReceived()[5]});
                        int length01 = ApplicationConfig.MOVE_SIDE_CODE.length;
                        for (int m = 0; m < length01; m++) {
                            if (callFloor.equalsIgnoreCase(ApplicationConfig.MOVE_SIDE_CODE[m])) {
                                int length02 = ApplicationConfig.moveInsideName.length;
                                for (int n = 0; n < length02; n++) {
                                    if (monitor.getName().equalsIgnoreCase(ApplicationConfig.moveInsideName[n])) {
                                        /**
                                         * 当前所有召唤的楼层
                                         */
                                        calledFloorList.add(n * 8 + m + 1);
                                    }
                                }
                            }
                        }
                    }
                }
                if (calledFloorList.size() > 0) {
                    /**
                     * 更新已召唤的楼层UI
                     */
                    if (moveSidePagerAdapter != null) {
                        moveSidePagerAdapter.updateCurrentCalledFloor(calledFloorList);
                    }
                }
            }
            MoveInsideActivity.this.isSyncing = false;
        }

        @Override
        public void onTalkReceive(Message msg) {
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