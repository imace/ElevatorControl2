package com.kio.ElevatorControl.activities;

import android.nfc.Tag;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HHandler;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.daos.ParameterSettingsDao;
import com.kio.ElevatorControl.daos.RealTimeMonitorDao;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.models.RealTimeMonitor;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;
import com.kio.ElevatorControl.views.TypefaceTextView;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.GridView;

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

    @InjectView(R.id.grid_view)
    GridView mGridView;

    private MoveInsideHandler mMoveInsideHandler;

    private FloorHandler floorHandler;

    private List<Integer> floors;

    private HCommunication[] getFloorsCommunications;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.move_inside_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_move_inside);
        Views.inject(this);
        bindListViewItemClickEvent();
        mMoveInsideHandler = new MoveInsideHandler(this);
        floorHandler = new FloorHandler(this);
        floors = new ArrayList<Integer>();
        realTimeMonitors = RealTimeMonitorDao.findByType(this, codeType);
        createGetFloorsCommunication();
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
        names.add(ApplicationConfig.BOTTOM_FLOOR_NAME);
        names.add(ApplicationConfig.TOP_FLOOR_NAME);
        List<ParameterSettings> settingsList = ParameterSettingsDao.findByNames(MoveInsideActivity.this, names);
        int size = settingsList.size();
        getFloorsCommunications = new HCommunication[size];
        for (int i = 0; i < size; i++) {
            final ParameterSettings setting = settingsList.get(i);
            getFloorsCommunications[i] = new HCommunication() {
                @Override
                public void beforeSend() {
                    this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103"
                            + setting.getCode()
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
                        MoveInsideActivity.this.floors.add(ParseSerialsUtils.getIntFromBytes(received));
                        Log.v(TAG, HSerial.byte2HexStr(received));
                        setting.setReceived(received);
                        return setting;
                    }
                    return null;
                }
            };
        }
    }

    /**
     * 绑定ListView点击事件
     */
    private void bindListViewItemClickEvent() {
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // 0103 读取
                // 0106 写入
                final String[] condition = getCallCode(position);
                Log.v(TAG, condition[0]);
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
     * 更新 GridView 数据源
     */
    public void updateGridViewDataSource(List<Integer> floors) {
        InsideFloorAdapter adapter = new InsideFloorAdapter(floors);
        mGridView.setAdapter(adapter);
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
        if (getFloorsCommunications != null) {
            if (HBluetooth.getInstance(MoveInsideActivity.this).isPrepared()) {
                HBluetooth.getInstance(MoveInsideActivity.this)
                        .setHandler(mMoveInsideHandler)
                        .setCommunications(getFloorsCommunications)
                        .Start();
            }
        }
    }

    // ================================= GridView adapter ========================================== //

    /**
     * 电梯层数 GridView adapter
     */
    private class InsideFloorAdapter extends BaseAdapter {

        private List<Integer> mFloors;

        public InsideFloorAdapter(List<Integer> floors) {
            mFloors = floors;
        }

        @Override
        public int getCount() {
            return Math.abs(mFloors.get(0) - mFloors.get(1)) + 1;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            ViewHolder holder = null;
            LayoutInflater mInflater = LayoutInflater.from(MoveInsideActivity.this);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.move_inside_row_item, null);
                holder = new ViewHolder();
                holder.mFloorTextView = (TypefaceTextView) convertView.findViewById(R.id.floor_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mFloorTextView.setText(String.valueOf(Math.min(mFloors.get(0), mFloors.get(1)) + position));
            return convertView;
        }

        private class ViewHolder {
            TypefaceTextView mFloorTextView;
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
            if (MoveInsideActivity.this.floors.size() == 2) {
                MoveInsideActivity.this.updateGridViewDataSource(MoveInsideActivity.this.floors);
            } else {
                MoveInsideActivity.this.floors = new ArrayList<Integer>();
                MoveInsideActivity.this.loadDataAndRenderView();
            }
        }

        @Override
        public void onTalkReceive(Message msg) {

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