package com.inovance.ElevatorControl.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
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
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import com.inovance.ElevatorControl.views.TypefaceTextView;
import com.inovance.ElevatorControl.views.viewpager.VerticalViewPager;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.ImageButton;
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

    @InjectView(R.id.up_button)
    ImageButton upButton;

    @InjectView(R.id.down_button)
    ImageButton downButton;

    private static final String codeType = "64";

    private List<RealTimeMonitor> realTimeMonitors;

    private MoveOutsideHandler mMoveOutsideHandler;

    private BluetoothTalk[] getFloorsCommunications;

    private boolean hasGetFloors = false;

    private int selectedFloor;

    private SyncMoveOutsideInfoHandler mSyncMoveOutsideInfoHandler;

    private Runnable syncTask;

    private boolean running = false;

    private Handler syncHandler = new Handler();

    private FloorHandler floorHandler;

    private boolean isWritingData = false;

    private boolean isWriteSuccessful = false;

    private boolean isSyncing = false;

    private static final String[] CallCode = new String[]{
            "0001:0002", // 召唤一楼
            "0004:0008", // 召唤二楼
            "0010:0020", // 召唤三楼
            "0040:0080" // 召唤四楼
    };

    private MoveSidePagerAdapter moveSidePagerAdapter;

    /**
     * 同步间隔
     */
    private static final int SYNC_TIME = 1000;

    private BluetoothTalk[] getMoveOutsideInfoCommunications;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setTitle(R.string.move_outside_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_move_outside);
        Views.inject(this);
        moveSidePagerAdapter = new MoveSidePagerAdapter(this, ApplicationConfig.DEFAULT_FLOORS);
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
        realTimeMonitors = RealTimeMonitorDao.findByType(this, codeType);
        createGetFloorsCommunication();
        syncTask = new Runnable() {
            @Override
            public void run() {
                if (running) {
                    if (hasGetFloors) {
                        if (!isWritingData) {
                            MoveOutsideActivity.this.syncMoveOutsideInfoStatus();
                        }
                    } else {
                        MoveOutsideActivity.this.loadDataAndRenderView();
                    }
                    syncHandler.postDelayed(this, SYNC_TIME);
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BluetoothTool.getInstance(this).isConnected()) {
            running = true;
            syncHandler.postDelayed(syncTask, SYNC_TIME);
        } else {
            Toast.makeText(this,
                    R.string.not_connect_device_error,
                    android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void createGetMoveOutsideInfoCommunications() {
        if (getMoveOutsideInfoCommunications == null) {
            final List<RealTimeMonitor> monitorLists = RealTimeMonitorDao
                    .findByNames(this, ApplicationConfig.moveOutsideInfoName);
            if (monitorLists.size() == ApplicationConfig.moveOutsideInfoName.length) {
                getMoveOutsideInfoCommunications = new BluetoothTalk[3];
                final int index01 = 0;
                final int index02 = 1;
                final int index03 = 11;
                final int length01 = 1;
                final int length02 = 10;
                final int length03 = 2;
                final RealTimeMonitor monitor01 = monitorLists.get(index01);
                final RealTimeMonitor monitor02 = monitorLists.get(index02);
                final RealTimeMonitor monitor03 = monitorLists.get(index03);
                for (int i = 0; i < 3; i++) {
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
                    if (i == 2) {
                        hexString = "0103"
                                + monitor03.getCode()
                                + String.format("%04x", length03)
                                + "0001";
                    }
                    final String sendCode = hexString;
                    final int index = i;
                    getMoveOutsideInfoCommunications[i] = new BluetoothTalk() {
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
                                        for (int m = 0; m < length02; m++) {
                                            byte[] tempData = SerialUtility.crc16(SerialUtility.hexStr2Ints("01030002"
                                                    + SerialUtility.byte2HexStr(new byte[]{data[4
                                                    + m * 2], data[5 + m * 2]})));
                                            try {
                                                RealTimeMonitor monitor = (RealTimeMonitor)
                                                        monitorLists.get(m + index02).clone();
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
                                if (index == 2) {
                                    if (length03 * 2 == bytesLength) {
                                        List<RealTimeMonitor> tempList = new ArrayList<RealTimeMonitor>();
                                        for (int n = 0; n < length03; n++) {
                                            byte[] tempData = SerialUtility.crc16(SerialUtility.hexStr2Ints("01030002"
                                                    + SerialUtility.byte2HexStr(new byte[]{data[4
                                                    + n * 2], data[5 + n * 2]})));
                                            try {
                                                RealTimeMonitor monitor = (RealTimeMonitor)
                                                        monitorLists.get(n + index03).clone();
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
     * 同步外召信息
     */
    private void syncMoveOutsideInfoStatus() {
        if (!isSyncing) {
            if (BluetoothTool.getInstance(MoveOutsideActivity.this).isConnected()) {
                if (mSyncMoveOutsideInfoHandler != null && getMoveOutsideInfoCommunications != null) {
                    MoveOutsideActivity.this.isSyncing = true;
                    mSyncMoveOutsideInfoHandler.sendCount = getMoveOutsideInfoCommunications.length;
                    BluetoothTool.getInstance(MoveOutsideActivity.this)
                            .setHandler(mSyncMoveOutsideInfoHandler)
                            .setCommunications(getMoveOutsideInfoCommunications)
                            .send();
                } else {
                    errorHandler.sendEmptyMessage(0);
                }
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
        MoveOutsideActivity.this.isSyncing = false;
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
                MoveOutsideActivity.this.isWritingData = false;
            }
        }.start();
    }

    @OnClick(R.id.down_button)
    void OnDownButtonClick(View view) {
        MoveOutsideActivity.this.isSyncing = false;
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
                MoveOutsideActivity.this.isWritingData = false;
            }
        }.start();
    }

    /**
     * Create Get Floors Communication
     */
    private void createGetFloorsCommunication() {
        ArrayList<String> names = new ArrayList<String>();
        names.add(ApplicationConfig.GET_FLOOR_NAME);
        List<ParameterSettings> settingsList = ParameterSettingsDao.findByNames(MoveOutsideActivity.this,
                names.toArray(new String[names.size()]));
        final String code = settingsList.get(0).getCode() + "0002";
        getFloorsCommunications = new BluetoothTalk[]{
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
                            return SerialUtility.trimEnd(getReceivedBuffer());
                        }
                        return null;
                    }
                }
        };
    }

    private void loadDataAndRenderView() {
        if (getFloorsCommunications != null) {
            if (BluetoothTool.getInstance(MoveOutsideActivity.this).isConnected()) {
                BluetoothTool.getInstance(MoveOutsideActivity.this)
                        .setHandler(mMoveOutsideHandler)
                        .setCommunications(getFloorsCommunications)
                        .send();
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
                            byte[] received = SerialUtility.trimEnd(getReceivedBuffer());
                            return SerialUtility.byte2HexStr(received);
                        }
                        return null;
                    }
                }
        };
        if (BluetoothTool.getInstance(MoveOutsideActivity.this).isConnected()) {
            floorHandler.writeCode = codeArray[0];
            BluetoothTool.getInstance(MoveOutsideActivity.this)
                    .setHandler(floorHandler)
                    .setCommunications(communications)
                    .send();
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
    private class FloorHandler extends BluetoothHandler {

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
                    MoveOutsideActivity.this.isWritingData = false;
                }
            }
        }

    }

    // =================================== MoveOutside Handler ==============================

    /**
     * 蓝牙 Socket handler
     */
    private class MoveOutsideHandler extends BluetoothHandler {

        private int topFloor;

        private int bottomFloor;

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
                    topFloor = ByteBuffer.wrap(new byte[]{data[4], data[5]}).getShort();
                    bottomFloor = ByteBuffer.wrap(new byte[]{data[6], data[7]}).getShort();
                    moveSidePagerAdapter = new MoveSidePagerAdapter(MoveOutsideActivity.this,
                            new int[]{bottomFloor, topFloor});
                    moveSidePagerAdapter.setOnSelectFloorListener(new MoveSidePagerAdapter.onSelectFloorListener() {
                        @Override
                        public void onSelect(int floor) {
                            if (floor == Math.min(topFloor, bottomFloor)) {
                                MoveOutsideActivity.this.upButton.setEnabled(true);
                                MoveOutsideActivity.this.downButton.setEnabled(false);
                            } else if (floor == Math.max(topFloor, bottomFloor)) {
                                MoveOutsideActivity.this.upButton.setEnabled(false);
                                MoveOutsideActivity.this.downButton.setEnabled(true);
                            } else {
                                MoveOutsideActivity.this.upButton.setEnabled(true);
                                MoveOutsideActivity.this.downButton.setEnabled(true);
                            }
                            MoveOutsideActivity.this.selectedFloor = floor;
                        }
                    });
                    MoveOutsideActivity.this.viewPager.setAdapter(moveSidePagerAdapter);
                    MoveOutsideActivity.this.createGetMoveOutsideInfoCommunications();
                    MoveOutsideActivity.this.hasGetFloors = true;
                }
            }
        }

    }

    // ============================== 同步外召信息 ================================================ //
    private class SyncMoveOutsideInfoHandler extends BluetoothHandler {

        public int sendCount;

        private int receiveCount;

        private List<ObjectListHolder> holderList;

        public SyncMoveOutsideInfoHandler(Activity activity) {
            super(activity);
            TAG = SyncMoveOutsideInfoHandler.class.getSimpleName();
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
                        MoveOutsideActivity.this.currentFloorTextView
                                .setText(String.valueOf(ParseSerialsUtils.getIntFromBytes(monitor.getReceived())));
                    } else {
                        String callFloor = SerialUtility.byte2HexStr(new byte[]{monitor.getReceived()[4],
                                monitor.getReceived()[5]});
                        int length01 = CallCode.length;
                        for (int m = 0; m < length01; m++) {
                            if (CallCode[m].contains(callFloor)) {
                                int length02 = ApplicationConfig.moveOutsideName.length;
                                for (int n = 0; n < length02; n++) {
                                    if (monitor.getName().equalsIgnoreCase(ApplicationConfig.moveOutsideName[n])) {
                                        // 召唤的楼层
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
            if (msg.obj != null && msg.obj instanceof ObjectListHolder) {
                holderList.add((ObjectListHolder) msg.obj);
                receiveCount++;
            }
        }
    }

}