package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.os.Message;
import android.view.MenuItem;
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
import com.kio.ElevatorControl.views.viewpager.VerticalViewPager;
import org.holoeverywhere.app.Activity;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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

    private MoveInsideHandler mMoveInsideHandler;

    private FloorHandler floorHandler;

    private HCommunication[] communications;

    private boolean hasGetFloors = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.move_inside_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_move_inside);
        Views.inject(this);
        bindListViewItemClickEvent();
        viewPager.setAdapter(new MoveSidePagerAdapter(MoveInsideActivity.this, ApplicationConfig.DEFAULT_FLOORS));
        mMoveInsideHandler = new MoveInsideHandler(this);
        floorHandler = new FloorHandler(this);
        realTimeMonitors = RealTimeMonitorDao.findByType(this, codeType);
        createGetFloorsCommunication();
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!MoveInsideActivity.this.hasGetFloors) {
                    MoveInsideActivity.this.loadDataAndRenderView();
                } else {
                    timer.cancel();
                }
            }
        }, 0, 240);
        loadDataAndRenderView();
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
        List<ParameterSettings> settingsList = ParameterSettingsDao.findByNames(MoveInsideActivity.this, names);
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

    /**
     * 绑定ListView点击事件
     */
    private void bindListViewItemClickEvent() {
        /*
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // 0103 读取
                // 0106 写入
                final String[] condition = getCallCode(position);
                final RealTimeMonitor monitor = realTimeMonitors.get(Integer.parseInt(condition[1]));
                HCommunication[] communications = new HCommunication[]{
                        new HCommunication() {
                            @Override
                            public void beforeSend() {
                                this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0106"
                                        + condition[0]
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
                                    monitor.setReceived(received);
                                    return monitor;
                                }
                                return null;
                            }
                        }
                };
                if (HBluetooth.getInstance(MoveInsideActivity.this).isPrepared()) {
                    HBluetooth.getInstance(MoveInsideActivity.this)
                            .setHandler(floorHandler)
                            .setCommunications(communications)
                            .Start();
                }
            }
        });
        */
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
        }
    }

    /**
     * 取得需要发送的Code
     *
     * @param position GridView Item Index
     * @return String[]
     */
    private String[] getCallCode(int position) {
        int index = position + 1;
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
            if (index >= Integer.parseInt(parts[0]) && index <= Integer.parseInt(parts[1])) {
                searchCondition = condition + "层信息";
                section_location = index - Integer.parseInt(parts[0]);
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
            }
        }
    }

    // ================================= MoveInside handler ========================================== //

    /**
     * 蓝牙 Socket handler
     */
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
                    MoveSidePagerAdapter adapter = new MoveSidePagerAdapter(MoveInsideActivity.this,
                            new int[]{bottom, top});
                    MoveInsideActivity.this.viewPager.setAdapter(adapter);
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
            if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
                RealTimeMonitor monitor = (RealTimeMonitor) msg.obj;
            }
        }

    }

}