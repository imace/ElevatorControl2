package com.kio.ElevatorControl.activities;

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
import com.kio.ElevatorControl.daos.RealTimeMonitorDao;
import com.kio.ElevatorControl.models.RealTimeMonitor;
import com.kio.ElevatorControl.views.TypefaceTextView;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.GridView;

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

    public Floors mFloors;

    private List<RealTimeMonitor> mStateCodes;

    @InjectView(R.id.grid_view)
    GridView mGridView;

    private MoveInsideHandler mMoveInsideHandler;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.move_inside_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_move_inside);
        Views.inject(this);
        // 取得召唤信息Code
        mStateCodes = RealTimeMonitorDao.findByType(this, codeType);
        mFloors = new Floors();
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
     * 同步电梯状态数据
     */
    private void syncElevatorStatus() {

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
    public void updateGridViewDataSource(Floors floors) {
        InsideFloorAdapter adapter = new InsideFloorAdapter(floors);
        mGridView.setAdapter(adapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // 0103 读取
                // 0106 写入
                String condition = getCondition(position);
                Log.v(TAG, condition);
                for (RealTimeMonitor stateCode : mStateCodes) {
                    if (condition.equalsIgnoreCase(stateCode.getName())) {
                        Log.v(TAG, stateCode.getCode());
                        final RealTimeMonitor codeObject = stateCode;
                        HCommunication[] communications = new HCommunication[]{
                                new HCommunication() {
                                    @Override
                                    public void beforeSend() {
                                        this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0106"
                                                + codeObject.getCode()
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
                                        byte[] received = HSerial.trimEnd(getReceivedBuffer());
                                        Log.v(TAG, HSerial.byte2HexStr(received));
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
                }
            }
        });
    }

    /**
     * 取得查询条件
     *
     * @param position index
     * @return String
     */
    private String getCondition(int position) {
        int index = position + 1;
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
            }
        }
        return searchCondition;
    }

    private void loadDataAndRenderView() {
        HCommunication[] communications = new HCommunication[]{
                // 读取最底层
                new HCommunication() {
                    @Override
                    public void beforeSend() {
                        // 0103 prefix
                        // 0001 suffix
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
                        if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                            // 校验数据
                            byte[] received = HSerial.trimEnd(getReceivedBuffer());
                            if (received.length == 8) {
                                int value = received[4];
                                value = value << 8;
                                value = value | received[5];
                                mFloors.setStartFloors(value);
                                return mFloors;
                            }
                        }
                        return null;
                    }
                },
                // 读取最高层
                new HCommunication() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103F6000001")));
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
                        byte[] received = HSerial.trimEnd(getReceivedBuffer());
                        if (received.length == 8) {
                            int value = received[4];
                            value = value << 8;
                            value = value | received[5];
                            mFloors.setEndFloors(value);
                            return mFloors;
                        }
                        return null;
                    }
                }
        };
        if (mMoveInsideHandler == null) {
            mMoveInsideHandler = new MoveInsideHandler(this);
        }
        if (HBluetooth.getInstance(MoveInsideActivity.this).isPrepared()) {
            HBluetooth.getInstance(MoveInsideActivity.this)
                    .setHandler(mMoveInsideHandler)
                    .setCommunications(communications)
                    .Start();
        }
    }

    // ================================= GridView adapter ========================================== //

    /**
     * 电梯层数 GridView adapter
     */
    private class InsideFloorAdapter extends BaseAdapter {

        private Floors mFloors;

        public InsideFloorAdapter(Floors floors) {
            mFloors = floors;
        }

        @Override
        public int getCount() {
            return (mFloors.endFloors - mFloors.startFloors + 1);
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
            holder.mFloorTextView.setText(String.valueOf(mFloors.startFloors + position));
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

        private Object receivedMessage;

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
            receivedMessage = msg.obj;
            if (receivedMessage != null && (receivedMessage instanceof Floors)) {
                Floors floors = (Floors) receivedMessage;
                if (floors.totalParams == 2) {
                    MoveInsideActivity.this.updateGridViewDataSource(floors);
                }
            }
        }

    }

    // ========================================== 最高层/最底层 =============================================//

    private class Floors {

        public int totalParams = 0;

        public int startFloors;

        public int endFloors;

        public void setStartFloors(int floors) {
            this.startFloors = floors;
            totalParams++;
        }

        public void setEndFloors(int floors) {
            this.endFloors = floors;
            totalParams++;
        }

    }
}