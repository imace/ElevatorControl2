package com.inovance.elevatorcontrol.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Views;
import com.inovance.bluetoothtool.BluetoothHandler;
import com.inovance.bluetoothtool.BluetoothTalk;
import com.inovance.bluetoothtool.BluetoothTool;
import com.inovance.bluetoothtool.SerialUtility;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.adapters.MoveSidePagerAdapter;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.daos.ParameterSettingsDao;
import com.inovance.elevatorcontrol.daos.RealTimeMonitorDao;
import com.inovance.elevatorcontrol.models.ParameterSettings;
import com.inovance.elevatorcontrol.models.RealTimeMonitor;
import com.inovance.elevatorcontrol.utils.LogUtils;
import com.inovance.elevatorcontrol.utils.ParseSerialsUtils;
import com.inovance.elevatorcontrol.views.TypefaceTextView;
import com.inovance.elevatorcontrol.views.viewpager.VerticalViewPager;

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
public class MoveOutsideActivity extends Activity implements Runnable, MoveSidePagerAdapter.OnSelectFloorListener {

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
     * 用于召唤楼层的指令列表
     */
    private List<RealTimeMonitor> moveOutsideMonitorList;

    /**
     * 用于获取电梯最高层和最底层的 Handler
     */
    private FloorInformationHandler floorInformationHandler;

    /**
     * 获取最高层和最底层的通信内容
     */
    private BluetoothTalk[] getFloorsCommunications;

    /**
     * 读取电梯最高层和最底层
     */
    private static final int GET_FLOOR_INFORMATION = 1;

    /**
     * 读取当前楼层和楼层召唤信息
     */
    private static final int GET_CURRENT_FLOOR_AND_CALL_STATE = 2;

    /**
     * 召唤楼层
     */
    private static final int CALL_FLOOR = 3;

    /**
     * 当前执行的任务
     */
    private int currentTask;

    /**
     * 当前召唤楼层
     */
    private int currentCallFloor = -1;

    private static int TOP_FLOOR = 1;

    private static int MIDDLE_FLOOR = 2;

    private static int BOTTOM_FLOOR = 3;

    /**
     * 最高层还是最底层还是中间层
     */
    private int floorLocation = MIDDLE_FLOOR;

    /**
     * 上召
     */
    private static final int CALL_UP = 1;

    /**
     * 下召
     */
    private static final int CALL_DOWN = 2;

    /**
     * 当前召唤方向
     */
    private int currentCallDirection;

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
     * 最高层
     */
    private int topFloor;

    /**
     * 最底层
     */
    private int bottomFloor;

    /**
     * 用于召唤楼层的 Handler
     */
    private CallCurrentFloorHandler callCurrentFloorHandler;

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
            "0040:0080" //  召唤四楼
    };

    /**
     * Vertical View Pager Adapter
     */
    private MoveSidePagerAdapter moveSidePagerAdapter;

    private ExecutorService pool = Executors.newSingleThreadExecutor();

    /**
     * 读取当前楼层和楼层召唤信息
     */
    private BluetoothTalk[] getCurrentFloorAndCallStateCommunications;

    private GetCurrentFloorAndCallStateHandler getCurrentFloorAndCallStateHandler;

    /**
     * 同步间隔
     */
    private static final int SYNC_TIME = 800;

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
        floorInformationHandler = new FloorInformationHandler(this);
        callCurrentFloorHandler = new CallCurrentFloorHandler(this);
        getCurrentFloorAndCallStateHandler = new GetCurrentFloorAndCallStateHandler(this);
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
            currentTask = GET_FLOOR_INFORMATION;
            syncHandler.postDelayed(syncTask, SYNC_TIME);
        }
    }

    private void createGetCurrentFloorAndCallStateCommunications() {
        List<RealTimeMonitor> monitorList = new ArrayList<RealTimeMonitor>();
        int index = 0;
        for (final RealTimeMonitor monitor : moveOutsideMonitorList) {
            if (currentCallFloor >= (index * 4 + 1) && currentCallFloor <= (index + 1) * 4) {
                monitorList.add(monitor);
            }
            index++;
        }
        monitorList.add(RealTimeMonitorDao.findByStateID(this, ApplicationConfig.CurrentFloorType));
        getCurrentFloorAndCallStateCommunications = new BluetoothTalk[monitorList.size()];
        index = 0;
        for (final RealTimeMonitor monitor : monitorList) {
            getCurrentFloorAndCallStateCommunications[index] = new BluetoothTalk() {
                @Override
                public void beforeSend() {
                    this.setSendBuffer(SerialUtility.crc16("0103"
                            + monitor.getCode()
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
                        monitor.setReceived(getReceivedBuffer());
                        return monitor;
                    }
                    return null;
                }
            };
            index++;
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
        Log.v(TAG, "CallUp");
        BluetoothTool.getInstance().setHandler(null);
        // 发送上召指令
        currentCallDirection = CALL_UP;
        currentTask = CALL_FLOOR;
        isSyncing = false;
        upButton.setEnabled(false);
        upButton.setImageResource(R.drawable.elevator_up_highlighted);
    }

    /**
     * 电梯下召
     *
     * @param view Button View
     */
    @OnClick(R.id.down_button)
    void OnDownButtonClick(View view) {
        Log.v(TAG, "CallDown");
        BluetoothTool.getInstance().setHandler(null);
        // 发送下召指令
        currentCallDirection = CALL_DOWN;
        currentTask = CALL_FLOOR;
        isSyncing = false;
        // 高亮下召按钮并清空选中的楼层
        downButton.setEnabled(false);
        downButton.setImageResource(R.drawable.elevator_down_highlighted);
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
    private void getFloorInformation() {
        isSyncing = true;
        if (BluetoothTool.getInstance().isPrepared()) {
            floorInformationHandler.sendCount = getFloorsCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(floorInformationHandler)
                    .setCommunications(getFloorsCommunications)
                    .startTask();
        }
    }

    /**
     * 读取当前楼层和楼层召唤信息
     */
    private void getCurrentFloorAndCallState() {
        isSyncing = true;
        if (BluetoothTool.getInstance().isPrepared()) {
            createGetCurrentFloorAndCallStateCommunications();
            getCurrentFloorAndCallStateHandler.sendCount = getCurrentFloorAndCallStateCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(getCurrentFloorAndCallStateHandler)
                    .setCommunications(getCurrentFloorAndCallStateCommunications)
                    .startTask();
        }
    }

    /**
     * 外召楼层
     */
    private void moveOutsideCallFloor() {
        int index = 0;
        for (final RealTimeMonitor monitor : moveOutsideMonitorList) {
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
                                    monitor.setReceived(getReceivedBuffer());
                                    return monitor;
                                }
                                return null;
                            }
                        }
                };
                if (BluetoothTool.getInstance().isPrepared()) {
                    isSyncing = true;
                    callCurrentFloorHandler.writeCode = callCode;
                    callCurrentFloorHandler.floor = currentCallFloor;
                    callCurrentFloorHandler.isUp = currentCallDirection == CALL_UP;
                    BluetoothTool.getInstance()
                            .setHandler(callCurrentFloorHandler)
                            .setCommunications(communications)
                            .startTask();
                }
                break;
            }
            index++;
        }
    }

    @Override
    public void run() {
        switch (currentTask) {
            case GET_FLOOR_INFORMATION:
                // 读取楼层最高层最底层信息
                getFloorInformation();
                break;
            case GET_CURRENT_FLOOR_AND_CALL_STATE:
                // 读取当前楼层和楼层召唤信息
                getCurrentFloorAndCallState();
                break;
            case CALL_FLOOR:
                // 召唤楼层
                moveOutsideCallFloor();
                break;
        }
    }

    @Override
    public void onSelect(int floor) {
        MoveOutsideActivity.this.upButton.setEnabled(false);
        MoveOutsideActivity.this.upButton.setImageResource(R.drawable.elevator_up_button);
        MoveOutsideActivity.this.downButton.setEnabled(false);
        MoveOutsideActivity.this.downButton.setImageResource(R.drawable.elevator_down_button);
        if (floor == Math.min(topFloor, bottomFloor)) {
            MoveOutsideActivity.this.floorLocation = BOTTOM_FLOOR;
        } else if (floor == Math.max(topFloor, bottomFloor)) {
            MoveOutsideActivity.this.floorLocation = TOP_FLOOR;
        } else {
            MoveOutsideActivity.this.floorLocation = MIDDLE_FLOOR;
        }
        MoveOutsideActivity.this.isSyncing = false;
        MoveOutsideActivity.this.currentCallFloor = floor;
        MoveOutsideActivity.this.currentTask = GET_CURRENT_FLOOR_AND_CALL_STATE;
    }

    // ============================================== 召唤楼层 ==================================================== //

    private class CallCurrentFloorHandler extends BluetoothHandler {

        private RealTimeMonitor monitor;

        public String writeCode;

        public int floor;

        public boolean isUp;

        public CallCurrentFloorHandler(Activity activity) {
            super(activity);
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            monitor = null;
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (monitor != null) {
                String receive = SerialUtility.byte2HexStr(monitor.getReceived());
                String checkResult = ParseSerialsUtils.isWriteSuccess(receive);
                if (checkResult != null) {
                    Toast.makeText(MoveOutsideActivity.this,
                            checkResult,
                            Toast.LENGTH_SHORT)
                            .show();
                } else {
                    if (receive.contains(writeCode)) {
                        // 写入外召日志
                        LogUtils.getInstance().write(ApplicationConfig.LogMoveOutside,
                                writeCode,
                                receive,
                                floor, isUp ? 1 : 2);
                    } else {
                        Toast.makeText(MoveOutsideActivity.this,
                                R.string.move_outside_failed_text,
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                }
                MoveOutsideActivity.this.currentTask = GET_CURRENT_FLOOR_AND_CALL_STATE;
            }
            MoveOutsideActivity.this.isSyncing = false;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
                monitor = (RealTimeMonitor) msg.obj;
            }
        }

    }

    // ========================================== 读取电梯最高层和最底层 ============================================= //

    private class FloorInformationHandler extends BluetoothHandler {

        public int sendCount;

        public int receiveCount;

        private List<ParameterSettings> settingsList;

        public FloorInformationHandler(Activity activity) {
            super(activity);
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
                MoveOutsideActivity.this.topFloor = ByteBuffer.wrap(new byte[]{data1[4], data1[5]}).getShort();
                MoveOutsideActivity.this.bottomFloor = ByteBuffer.wrap(new byte[]{data2[4], data2[5]}).getShort();
                moveSidePagerAdapter = new MoveSidePagerAdapter(MoveOutsideActivity.this,
                        new int[]{MoveOutsideActivity.this.topFloor, MoveOutsideActivity.this.bottomFloor});
                moveSidePagerAdapter.setOnSelectFloorListener(MoveOutsideActivity.this);
                MoveOutsideActivity.this.viewPager.setAdapter(moveSidePagerAdapter);
                MoveOutsideActivity.this.loadView.setVisibility(View.GONE);
                MoveOutsideActivity.this.viewPager.setVisibility(View.VISIBLE);
                MoveOutsideActivity.this.isSyncing = false;
                MoveOutsideActivity.this.currentTask = GET_CURRENT_FLOOR_AND_CALL_STATE;
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

    // ================================= Get current floor and call state handler ============================== //

    private class GetCurrentFloorAndCallStateHandler extends BluetoothHandler {

        public int sendCount;

        private int receiveCount;

        private List<RealTimeMonitor> monitorList;

        public GetCurrentFloorAndCallStateHandler(Activity activity) {
            super(activity);
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
            monitorList = new ArrayList<RealTimeMonitor>();
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
                monitorList.add((RealTimeMonitor) msg.obj);
                receiveCount++;
            }
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (sendCount == receiveCount) {
                for (RealTimeMonitor monitor : monitorList) {
                    if (monitor.getStateID() == ApplicationConfig.CurrentFloorType) {
                        int currentFloor = ParseSerialsUtils.getIntFromBytes(monitor.getReceived());
                        MoveOutsideActivity.this.currentFloorTextView.setText(String.valueOf(currentFloor));
                    }
                    if (monitor.getStateID() == ApplicationConfig.MoveOutsideInformationType) {
                        // Restore button state
                        MoveOutsideActivity.this.upButton.setEnabled(true);
                        MoveOutsideActivity.this.upButton.setImageResource(R.drawable.elevator_up_button);
                        MoveOutsideActivity.this.downButton.setEnabled(true);
                        MoveOutsideActivity.this.downButton.setImageResource(R.drawable.elevator_down_button);

                        boolean[] result = ParseSerialsUtils.getBooleanValueArray(new byte[]{
                                monitor.getReceived()[5]
                        });
                        if (result.length == 8) {
                            boolean[][] state = new boolean[][]{
                                    new boolean[]{result[0], result[1]},
                                    new boolean[]{result[2], result[3]},
                                    new boolean[]{result[4], result[5]},
                                    new boolean[]{result[6], result[7]}
                            };
                            String[] scopes = monitor.getScope().split("~");
                            int min = Integer.parseInt(scopes[0]);
                            int index = MoveOutsideActivity.this.currentCallFloor - min;
                            if (index >= 0 && index < 4) {
                                boolean hasCallUp = state[index][0];
                                boolean hasCallDown = state[index][1];
                                // 已经被上召
                                if (hasCallUp) {
                                    MoveOutsideActivity.this.upButton.setEnabled(false);
                                    MoveOutsideActivity.this.upButton.setImageResource(R.drawable.elevator_up_highlighted);
                                    MoveOutsideActivity.this.downButton.setEnabled(true);
                                    MoveOutsideActivity.this.downButton.setImageResource(R.drawable.elevator_down_button);
                                }
                                // 已经被下召
                                if (hasCallDown) {
                                    MoveOutsideActivity.this.upButton.setEnabled(true);
                                    MoveOutsideActivity.this.upButton.setImageResource(R.drawable.elevator_up_button);
                                    MoveOutsideActivity.this.downButton.setEnabled(false);
                                    MoveOutsideActivity.this.downButton.setImageResource(R.drawable.elevator_down_highlighted);
                                }
                            }
                        }
                        // 最高层不能上召
                        if (MoveOutsideActivity.this.floorLocation == TOP_FLOOR) {
                            MoveOutsideActivity.this.upButton.setEnabled(false);
                            MoveOutsideActivity.this.upButton.setImageResource(R.drawable.elevator_up_button);
                        }
                        //  最底层不能上召
                        if (MoveOutsideActivity.this.floorLocation == BOTTOM_FLOOR) {
                            MoveOutsideActivity.this.downButton.setEnabled(false);
                            MoveOutsideActivity.this.downButton.setImageResource(R.drawable.elevator_down_button);
                        }
                    }
                }
            }
            MoveOutsideActivity.this.isSyncing = false;
            MoveOutsideActivity.this.currentTask = GET_CURRENT_FLOOR_AND_CALL_STATE;
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