package com.inovance.ElevatorControl.activities;

import android.app.Activity;
import android.os.Bundle;
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
import com.inovance.ElevatorControl.models.ObjectListHolder;
import com.inovance.ElevatorControl.models.ParameterSettings;
import com.inovance.ElevatorControl.models.RealTimeMonitor;
import com.inovance.ElevatorControl.utils.LogUtils;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import com.inovance.ElevatorControl.views.TypefaceTextView;
import com.inovance.ElevatorControl.views.viewpager.VerticalViewPager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by IntelliJ IDEA.
 * 电梯外召
 * User: keith.
 * Date: 14-3-6.
 * Time: 11:04.
 */
public class MoveOutsideActivity extends Activity implements Runnable {

    private static final String TAG = MoveOutsideActivity.class.getSimpleName();

    /**
     * Vertical View Pager
     */
    @InjectView(R.id.vertical_view_pager)
    VerticalViewPager viewPager;

    /**
     * 当前楼层
     */
    @InjectView(R.id.current_floor)
    TypefaceTextView currentFloorTextView;

    /**
     * 上召按钮
     */
    @InjectView(R.id.up_button)
    ImageButton upButton;

    /**
     * 下召按钮
     */
    @InjectView(R.id.down_button)
    ImageButton downButton;

    /**
     * 获取电梯最高层和最底层进度指示
     */
    @InjectView(R.id.load_view)
    LinearLayout loadView;

    /**
     * 当前楼层
     */
    private RealTimeMonitor currentFloorMonitor;

    /**
     * 用于召唤楼层的指令列表
     */
    private List<RealTimeMonitor> moveOutsideMonitorList;

    /**
     * 用于获取电梯最高层和最底层的 Handler
     */
    private MoveOutsideHandler mMoveOutsideHandler;

    /**
     * 获取最高层和最底层的通信内容
     */
    private BluetoothTalk[] getFloorsCommunications;

    private static final int GET_FLOOR = 1;

    private static final int GET_CALL_STATUS = 2;

    private static final int CALL_FLOOR = 3;

    private int currentTask;

    /**
     * 当前召唤楼层
     */
    private int currentCallFloor;

    private static final int CALL_UP = 1;

    private static final int CALL_DOWN = 2;

    private int currentCallDirection;

    /**
     * 用于同步电梯召唤状态信息的 Handler
     */
    private SyncMoveOutsideInfoHandler mSyncMoveOutsideInfoHandler;

    /**
     * 用于同步状态的 Task
     */
    private Runnable syncTask;

    /**
     * 是否终止同步状态 Task
     */
    private boolean running = false;

    /**
     * 用于同步状态的 Handler
     */
    private Handler syncHandler = new Handler();

    /**
     * 用于召唤楼层的 Handler
     */
    private FloorHandler floorHandler;

    /**
     * 是否正在同步电梯召唤状态信息
     */
    private boolean isSyncing = false;

    /**
     * 用于读取电梯外召信息的指令
     */
    private static final String[] CallCode = new String[]{
            "0001:0002", // 召唤一楼
            "0004:0008", // 召唤二楼
            "0010:0020", // 召唤三楼
            "0040:0080" // 召唤四楼
    };

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
     * 用于读取电梯外召信息的通信内容
     */
    private BluetoothTalk[] getMoveOutsideInfoCommunications;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setTitle(R.string.move_outside_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_move_outside);
        Views.inject(this);
        moveSidePagerAdapter = new MoveSidePagerAdapter(this, ApplicationConfig.DefaultFloors);
        viewPager.setAdapter(moveSidePagerAdapter);
        viewPager.setOnPageChangeListener(new VerticalViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                moveSidePagerAdapter.currentPager = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mMoveOutsideHandler = new MoveOutsideHandler(this);
        mSyncMoveOutsideInfoHandler = new SyncMoveOutsideInfoHandler(this);
        floorHandler = new FloorHandler(this);
        currentFloorMonitor = RealTimeMonitorDao.findByStateID(this, ApplicationConfig.CurrentFloorType);
        moveOutsideMonitorList = RealTimeMonitorDao.findAllByStateID(this, ApplicationConfig.MoveOutsideInformationType);
        Collections.sort(moveOutsideMonitorList, new SortComparator());
        createGetFloorsCommunication();
        syncTask = new Runnable() {
            @Override
            public void run() {
                if (running) {
                    if (BluetoothTool.getInstance().isPrepared()) {
                        if (!isSyncing) {
                            pool.execute(MoveOutsideActivity.this);
                        }
                        syncHandler.postDelayed(this, SYNC_TIME);
                    }
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BluetoothTool.getInstance().isPrepared()) {
            running = true;
            currentTask = GET_FLOOR;
            syncHandler.postDelayed(syncTask, SYNC_TIME);
        }
    }

    /**
     * 生成获取电梯召唤信息的通信内容
     */
    private void createGetMoveOutsideInfoCommunications() {
        if (getMoveOutsideInfoCommunications == null) {
            int size = moveOutsideMonitorList.size();
            final int count = size <= 10 ? 1 : ((size - size % 10) / 10 + (size % 10 == 0 ? 0 : 1));
            getMoveOutsideInfoCommunications = new BluetoothTalk[count + 1];
            for (int i = 0; i < count; i++) {
                final int position = i;
                final RealTimeMonitor firstItem = moveOutsideMonitorList.get(position * 10);
                final int length = size <= 10 ? size : (size % 10 == 0 ? 10 : ((position == count - 1) ? size % 10 : 10));
                getMoveOutsideInfoCommunications[i] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16("0103"
                                        + firstItem.getCode()
                                        + String.format("%04x", length)
                                        + "0001"));
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
                            if (length * 2 == bytesLength) {
                                List<RealTimeMonitor> tempList = new ArrayList<RealTimeMonitor>();
                                for (int j = 0; j < length; j++) {
                                    if (position * 10 + j < moveOutsideMonitorList.size()) {
                                        RealTimeMonitor item = moveOutsideMonitorList.get(position * 10 + j);
                                        byte[] tempData = SerialUtility.crc16("01030002"
                                                + SerialUtility.byte2HexStr(new byte[]{data[4 + j * 2], data[5 + j * 2]}));
                                        item.setReceived(tempData);
                                        tempList.add(item);
                                    }
                                }
                                ObjectListHolder holder = new ObjectListHolder();
                                holder.setRealTimeMonitorList(tempList);
                                return holder;
                            }
                        }
                        return null;
                    }
                };
            }
            getMoveOutsideInfoCommunications[count] = new BluetoothTalk() {
                @Override
                public void beforeSend() {
                    this.setSendBuffer(SerialUtility.crc16("0103"
                            + currentFloorMonitor.getCode()
                            + "0001"));
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
                        ObjectListHolder holder = new ObjectListHolder();
                        currentFloorMonitor.setReceived(getReceivedBuffer());
                        List<RealTimeMonitor> tempList = new ArrayList<RealTimeMonitor>();
                        tempList.add(currentFloorMonitor);
                        holder.setRealTimeMonitorList(tempList);
                        return holder;
                    }
                    return null;
                }
            };
        }
    }

    /**
     * 同步外召信息
     */
    private void syncMoveOutsideInfoStatus() {
        if (BluetoothTool.getInstance().isPrepared()) {
            isSyncing = true;
            mSyncMoveOutsideInfoHandler.sendCount = getMoveOutsideInfoCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(mSyncMoveOutsideInfoHandler)
                    .setCommunications(getMoveOutsideInfoCommunications)
                    .send();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                BluetoothTool.getInstance()
                        .setHandler(null);
                setResult(RESULT_OK);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 电梯上召
     *
     * @param view Button View
     */
    @OnClick(R.id.up_button)
    void OnUpButtonClick(View view) {
        isSyncing = false;
        currentCallDirection = CALL_UP;
        currentTask = CALL_FLOOR;
    }

    /**
     * 电梯下召
     *
     * @param view Button View
     */
    @OnClick(R.id.down_button)
    void OnDownButtonClick(View view) {
        isSyncing = false;
        currentCallDirection = CALL_DOWN;
        currentTask = CALL_FLOOR;
    }

    /**
     * 生成取得电梯最高层和最底层的通信内容
     */
    private void createGetFloorsCommunication() {
        List<ParameterSettings> settingsList = ParameterSettingsDao.findAllByCodes(this, new String[]{"F600", "F601"});
        getFloorsCommunications = new BluetoothTalk[settingsList.size()];
        int index = 0;
        for (final ParameterSettings settings : settingsList) {
            getFloorsCommunications[index] = new BluetoothTalk() {
                @Override
                public void beforeSend() {
                    this.setSendBuffer(SerialUtility.crc16("0103"
                            + settings.getCode()
                            + "0001"));
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
                        settings.setReceived(getReceivedBuffer());
                        return settings;
                    }
                    return null;
                }
            };
            index++;
        }
    }

    /**
     * 读取电梯最高层和最底层
     */
    private void loadDataAndRenderView() {
        isSyncing = true;
        if (BluetoothTool.getInstance().isPrepared()) {
            mMoveOutsideHandler.sendCount = getFloorsCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(mMoveOutsideHandler)
                    .setCommunications(getFloorsCommunications)
                    .send();
        }
    }

    /**
     * 外召楼层
     */
    private void moveOutsideCallFloor() {
        int index = 0;
        for (RealTimeMonitor monitor : moveOutsideMonitorList) {
            if (currentCallFloor >= (index * 4 + 1) && currentCallFloor <= ((index + 1) * 4)) {
                int minus = currentCallFloor - (index * 4 + 1);
                int callIndex = 0;
                switch (currentCallDirection) {
                    case CALL_UP:
                        callIndex = minus * 2;
                        break;
                    case CALL_DOWN:
                        callIndex = 2 * minus + 1;
                        break;
                }
                final String callCode = monitor.getCode() + ApplicationConfig.MoveSideCallCode[callIndex];
                BluetoothTalk[] communications = new BluetoothTalk[]{
                        new BluetoothTalk() {
                            @Override
                            public void beforeSend() {
                                this.setSendBuffer(SerialUtility.crc16("0106"
                                        + callCode
                                        + "0001"));
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
                                    return SerialUtility.byte2HexStr(received);
                                }
                                return null;
                            }
                        }
                };
                if (BluetoothTool.getInstance().isPrepared()) {
                    isSyncing = true;
                    floorHandler.writeCode = callCode;
                    floorHandler.floor = currentCallFloor;
                    floorHandler.isUp = currentCallDirection == CALL_UP;
                    BluetoothTool.getInstance()
                            .setHandler(floorHandler)
                            .setCommunications(communications)
                            .send();
                }
                break;
            }
            index++;
        }
    }

    @Override
    public void run() {
        switch (currentTask) {
            case GET_FLOOR:
                loadDataAndRenderView();
                break;
            case GET_CALL_STATUS:
                syncMoveOutsideInfoStatus();
                break;
            case CALL_FLOOR:
                moveOutsideCallFloor();
                break;
        }
    }

    // ==================================== 召唤楼层 =================================================//

    /**
     * 召唤楼层
     */
    private class FloorHandler extends BluetoothHandler {

        public String writeCode;

        public int floor;

        public boolean isUp;

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
            MoveOutsideActivity.this.isSyncing = false;
        }

        @Override
        public void onTalkReceive(Message msg) {
            if (msg.obj != null && msg.obj instanceof String) {
                String receive = (String) msg.obj;
                if (receive.contains(writeCode)) {
                    MoveOutsideActivity.this.isSyncing = false;
                    MoveOutsideActivity.this.currentTask = GET_CALL_STATUS;
                    // 写入外召日志
                    LogUtils.getInstance().write(ApplicationConfig.LogMoveOutside,
                            writeCode,
                            receive,
                            floor, isUp ? 1 : 2);
                }
            }
        }

    }

    // =================================== 读取电梯最高层和最底层 ==============================
    private class MoveOutsideHandler extends BluetoothHandler {

        public int sendCount;

        public int receiveCount;

        private List<ParameterSettings> settingsList;

        public MoveOutsideHandler(Activity activity) {
            super(activity);
            TAG = MoveInsideActivity.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
            settingsList = new ArrayList<ParameterSettings>();
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (sendCount == receiveCount && settingsList.size() == 2) {
                byte[] data1 = settingsList.get(0).getReceived();
                byte[] data2 = settingsList.get(1).getReceived();
                final int top = ByteBuffer.wrap(new byte[]{data1[4], data1[5]}).getShort();
                final int bottom = ByteBuffer.wrap(new byte[]{data2[4], data2[5]}).getShort();
                moveSidePagerAdapter = new MoveSidePagerAdapter(MoveOutsideActivity.this,
                        new int[]{bottom, top});
                moveSidePagerAdapter.setOnSelectFloorListener(new MoveSidePagerAdapter.OnSelectFloorListener() {
                    @Override
                    public void onSelect(int floor) {
                        if (floor == Math.min(top, bottom)) {
                            MoveOutsideActivity.this.upButton.setEnabled(true);
                            MoveOutsideActivity.this.downButton.setEnabled(false);
                        } else if (floor == Math.max(top, bottom)) {
                            MoveOutsideActivity.this.upButton.setEnabled(false);
                            MoveOutsideActivity.this.downButton.setEnabled(true);
                        } else {
                            MoveOutsideActivity.this.upButton.setEnabled(true);
                            MoveOutsideActivity.this.downButton.setEnabled(true);
                        }
                        MoveOutsideActivity.this.currentCallFloor = floor;
                    }
                });
                MoveOutsideActivity.this.viewPager.setAdapter(moveSidePagerAdapter);
                MoveOutsideActivity.this.createGetMoveOutsideInfoCommunications();
                MoveOutsideActivity.this.loadView.setVisibility(View.GONE);
                MoveOutsideActivity.this.viewPager.setVisibility(View.VISIBLE);
                MoveOutsideActivity.this.isSyncing = false;
                MoveOutsideActivity.this.currentTask = GET_CALL_STATUS;
            }
            MoveOutsideActivity.this.isSyncing = false;
        }

        @Override
        public void onTalkReceive(Message msg) {
            if (msg.obj != null && msg.obj instanceof ParameterSettings) {
                settingsList.add((ParameterSettings) msg.obj);
                receiveCount++;
            }
        }

    }

    // ============================== 同步外召信息 ================================================ //
    private class SyncMoveOutsideInfoHandler extends BluetoothHandler {

        public int sendCount;

        private int receiveCount;

        private List<RealTimeMonitor> monitorList;

        public SyncMoveOutsideInfoHandler(Activity activity) {
            super(activity);
            TAG = SyncMoveOutsideInfoHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
            monitorList = new ArrayList<RealTimeMonitor>();
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (sendCount == receiveCount) {
                List<Integer> calledFloorList = new ArrayList<Integer>();
                for (RealTimeMonitor monitor : monitorList) {
                    if (monitor.getStateID() == ApplicationConfig.CurrentFloorType) {
                        MoveOutsideActivity.this.currentFloorTextView
                                .setText(String.valueOf(ParseSerialsUtils.getIntFromBytes(monitor.getReceived())));
                    } else {
                        String callFloor = SerialUtility.byte2HexStr(new byte[]{monitor.getReceived()[4],
                                monitor.getReceived()[5]});
                        int length01 = CallCode.length;
                        for (int m = 0; m < length01; m++) {
                            if (CallCode[m].contains(callFloor)) {
                                int length02 = moveOutsideMonitorList.size();
                                for (int n = 0; n < length02; n++) {
                                    if (monitor.getName().equalsIgnoreCase(moveOutsideMonitorList.get(n).getName())) {
                                        /**
                                         * 当前所有被召唤的楼层
                                         */
                                        calledFloorList.add(n * 4 + m + 1);
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
            MoveOutsideActivity.this.isSyncing = false;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof ObjectListHolder) {
                monitorList.addAll(((ObjectListHolder) msg.obj).getRealTimeMonitorList());
                receiveCount++;
            }
        }
    }

    private class SortComparator implements Comparator<RealTimeMonitor> {

        @Override
        public int compare(RealTimeMonitor object1, RealTimeMonitor object2) {
            if (object1.getSort() < object2.getSort()) {
                return -1;
            } else if (object1.getSort() > object2.getSort()) {
                return 1;
            } else {
                return 0;
            }
        }

    }

}