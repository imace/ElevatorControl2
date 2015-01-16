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
import com.inovance.elevatorcontrol.models.ObjectListHolder;
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
 * 电梯内召
 * User: keith.
 * Date: 14-3-6.
 * Time: 11:03.
 */
public class MoveInsideActivity extends Activity implements Runnable {

    private static final String TAG = MoveInsideActivity.class.getSimpleName();

    /**
     * 用于召唤楼层的指令列表
     */
    private List<RealTimeMonitor> moveInsideMonitorList;

    /**
     * 当前楼层
     */
    private RealTimeMonitor currentFloorMonitor;

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
    private CallFloorHandler callFloorHandler;

    /**
     * 同步电梯召唤状态 Handler
     */
    private SyncMoveInsideInfoHandler mSyncMoveInsideInfoHandler;

    /**
     * 取得电梯最高层、最底层通信内容
     */
    private BluetoothTalk[] getFloorsCommunications;

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

    private static final int GET_FLOOR = 1;

    private static final int GET_CALL_STATUS = 2;

    private static final int CALL_FLOOR = 3;

    private int currentTask;

    /**
     * 是否正在同步电梯召唤信息
     */
    private boolean isSyncing = false;

    /**
     * 当前召唤楼层
     */
    private int currentCallFloor;

    /**
     * Vertical View Pager Adapter
     */
    private MoveSidePagerAdapter moveSidePagerAdapter;

    private ExecutorService pool = Executors.newSingleThreadExecutor();

    /**
     * 同步间隔
     */
    private static final int SYNC_TIME = 800;

    /**
     * 获取电梯召唤状态的通信内容
     */
    private BluetoothTalk[] getMoveInsideInfoCommunications;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setTitle(R.string.move_inside_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_move_inside);
        Views.inject(this);
        moveSidePagerAdapter = new MoveSidePagerAdapter(this, ApplicationConfig.DefaultFloors);
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
        callFloorHandler = new CallFloorHandler(this);
        currentFloorMonitor = RealTimeMonitorDao.findByStateID(this, ApplicationConfig.CurrentFloorType);
        moveInsideMonitorList = RealTimeMonitorDao.findAllByStateID(this, ApplicationConfig.MoveInsideInformationType);
        Collections.sort(moveInsideMonitorList, new SortComparator());
        createGetFloorsCommunication();
        syncTask = new Runnable() {
            @Override
            public void run() {
                if (running) {
                    if (BluetoothTool.getInstance().isPrepared()) {
                        if (!isSyncing) {
                            pool.execute(MoveInsideActivity.this);
                        }
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
            int size = moveInsideMonitorList.size();
            final int count = size <= 10 ? 1 : ((size - size % 10) / 10 + (size % 10 == 0 ? 0 : 1));
            getMoveInsideInfoCommunications = new BluetoothTalk[count + 1];
            for (int i = 0; i < count; i++) {
                final int position = i;
                final RealTimeMonitor firstItem = moveInsideMonitorList.get(position * 10);
                final int length = size <= 10 ? size : (size % 10 == 0 ? 10 : ((position == count - 1) ? size % 10 : 10));
                getMoveInsideInfoCommunications[i] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16("0103"
                                + firstItem.getCode()
                                + String.format("%04x", length)));
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
                                    if (position * 10 + j < moveInsideMonitorList.size()) {
                                        RealTimeMonitor item = moveInsideMonitorList.get(position * 10 + j);
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
            getMoveInsideInfoCommunications[count] = new BluetoothTalk() {
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
     * 同步当前电梯召唤信息
     */
    private void syncMoveInsideInfoStatus() {
        if (BluetoothTool.getInstance().isPrepared()) {
            isSyncing = true;
            mSyncMoveInsideInfoHandler.sendCount = getMoveInsideInfoCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(mSyncMoveInsideInfoHandler)
                    .setCommunications(getMoveInsideInfoCommunications)
                    .startTask();
        }
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
     * 生成用于取得电梯最高层和最底层的通信内容
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
     * 召唤楼层
     */
    private void moveInsideCallFloor() {
        int index = 0;
        for (final RealTimeMonitor monitor : moveInsideMonitorList) {
            if (currentCallFloor >= (index * 8 + 1) && currentCallFloor <= (index + 1) * 8) {
                int callIndex = currentCallFloor - (index * 8 + 1);
                final String callCode = monitor.getCode() + ApplicationConfig.MoveSideCallCode[callIndex];
                BluetoothTalk[] communications = new BluetoothTalk[]{
                        new BluetoothTalk() {
                            @Override
                            public void beforeSend() {
                                this.setSendBuffer(SerialUtility.crc16("0106"
                                        + callCode));
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
                    MoveInsideActivity.this.isSyncing = true;
                    MoveInsideActivity.this.currentTask = CALL_FLOOR;
                    MoveInsideActivity.this.callFloorHandler.writeCode = callCode;
                    MoveInsideActivity.this.callFloorHandler.floor = currentCallFloor;
                    BluetoothTool.getInstance()
                            .setHandler(callFloorHandler)
                            .setCommunications(communications)
                            .startTask();
                }
                break;
            }
            index++;
        }
    }

    /**
     * 取得电梯层数
     */
    private void loadDataAndRenderView() {
        isSyncing = true;
        if (BluetoothTool.getInstance().isPrepared()) {
            mMoveInsideHandler.sendCount = getFloorsCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(mMoveInsideHandler)
                    .setCommunications(getFloorsCommunications)
                    .startTask();
        }
    }

    @Override
    public void run() {
        switch (currentTask) {
            case GET_FLOOR:
                loadDataAndRenderView();
                break;
            case GET_CALL_STATUS:
                syncMoveInsideInfoStatus();
                break;
            case CALL_FLOOR:
                moveInsideCallFloor();
                break;
        }
    }

    // ================================= 获取电梯最高层和最底层 ========================================== //

    private class MoveInsideHandler extends BluetoothHandler {

        public int sendCount;

        public int receiveCount;

        private List<ParameterSettings> settingsList;

        public MoveInsideHandler(Activity activity) {
            super(activity);
            TAG = MoveInsideHandler.class.getSimpleName();
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
                int top = ByteBuffer.wrap(new byte[]{data1[4], data1[5]}).getShort();
                int bottom = ByteBuffer.wrap(new byte[]{data2[4], data2[5]}).getShort();
                moveSidePagerAdapter = new MoveSidePagerAdapter(MoveInsideActivity.this,
                        new int[]{bottom, top});
                moveSidePagerAdapter.setOnSelectFloorListener(new MoveSidePagerAdapter.OnSelectFloorListener() {
                    @Override
                    public void onSelect(int floor) {
                        BluetoothTool.getInstance().setHandler(null);
                        MoveInsideActivity.this.currentCallFloor = floor;
                        MoveInsideActivity.this.isSyncing = false;
                        MoveInsideActivity.this.currentTask = CALL_FLOOR;
                    }
                });
                MoveInsideActivity.this.viewPager.setAdapter(moveSidePagerAdapter);
                MoveInsideActivity.this.createGetMoveInsideInfoCommunications();
                MoveInsideActivity.this.loadView.setVisibility(View.GONE);
                MoveInsideActivity.this.viewPager.setVisibility(View.VISIBLE);
                MoveInsideActivity.this.isSyncing = false;
                MoveInsideActivity.this.currentTask = GET_CALL_STATUS;
            }
            MoveInsideActivity.this.isSyncing = false;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof ParameterSettings) {
                settingsList.add((ParameterSettings) msg.obj);
                receiveCount++;
            }
        }

    }

    // ==================================== 召唤楼层 ================================================= //

    /**
     * 召唤楼层
     */
    private class CallFloorHandler extends BluetoothHandler {

        private RealTimeMonitor monitor;

        public String writeCode;

        public int floor;

        public CallFloorHandler(Activity activity) {
            super(activity);
            TAG = CallFloorHandler.class.getSimpleName();
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
                    Toast.makeText(MoveInsideActivity.this,
                            checkResult,
                            Toast.LENGTH_SHORT)
                            .show();
                } else {
                    if (receive.contains(writeCode)) {
                        // 写入内召日志
                        LogUtils.getInstance().write(ApplicationConfig.LogMoveInside, writeCode, receive, floor);
                    } else {
                        Toast.makeText(MoveInsideActivity.this,
                                R.string.move_inside_failed_text,
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                }
                moveSidePagerAdapter.clearSelectIndex();
                MoveInsideActivity.this.currentTask = GET_CALL_STATUS;
            }
            MoveInsideActivity.this.isSyncing = false;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
                monitor = (RealTimeMonitor) msg.obj;
            }
        }

    }

    // ============================== 同步内召信息 ================================================ //

    private class SyncMoveInsideInfoHandler extends BluetoothHandler {

        public int sendCount;

        private int receiveCount;

        private List<RealTimeMonitor> monitorList;

        public SyncMoveInsideInfoHandler(Activity activity) {
            super(activity);
            TAG = SyncMoveInsideInfoHandler.class.getSimpleName();
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
                        MoveInsideActivity.this.currentFloorTextView
                                .setText(String.valueOf(ParseSerialsUtils.getIntFromBytes(monitor.getReceived())));
                    } else {
                        Log.v("SyncCallStatus", monitor.getName() + SerialUtility.byte2HexStr(monitor.getReceived()));
                        boolean[] booleanArray = ParseSerialsUtils.getBooleanValueArray(new byte[]{
                                monitor.getReceived()[5]
                        });
                        String[] scopes = monitor.getScope().split("~");
                        int min = Integer.parseInt(scopes[0]);
                        for (int i = 0; i < booleanArray.length; i++) {
                            if (booleanArray[i]) {
                                calledFloorList.add(min + i);
                            }
                        }
                    }
                }
                moveSidePagerAdapter.updateCurrentCalledFloor(calledFloorList);
            }
            MoveInsideActivity.this.currentTask = GET_CALL_STATUS;
            MoveInsideActivity.this.isSyncing = false;
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