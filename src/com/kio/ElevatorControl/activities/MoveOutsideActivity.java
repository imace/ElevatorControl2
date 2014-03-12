package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import butterknife.InjectView;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HHandler;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.daos.RealTimeMonitorDao;
import com.kio.ElevatorControl.models.RealTimeMonitor;
import com.kio.ElevatorControl.views.TintedImageButton;
import com.kio.ElevatorControl.views.TypefaceTextView;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.GridView;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-6.
 * Time: 11:04.
 */
public class MoveOutsideActivity extends Activity {

    @InjectView(R.id.grid_view)
    GridView mGridView;

    public Floors mFloors;

    private static final String codeType = "64";

    private List<RealTimeMonitor> mStateCodes;

    private MoveOutsideHandler mMoveOutsideHandler;

    private static final String TAG = MoveOutsideActivity.class.getSimpleName();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.move_outside_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_move_outside);
        Views.inject(this);
        // 取得外召信息Code
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
     * 更新 GridView 数据源
     */
    public void updateGridViewDataSource(Floors floors) {
        OutsideFloorAdapter adapter = new OutsideFloorAdapter(floors);
        mGridView.setAdapter(adapter);
    }

    private void loadDataAndRenderView() {
        HCommunication[] communications = new HCommunication[]{
                // 读取最底层
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
                        if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                            // 校验数据
                            byte[] received = HSerial.trimEnd(getReceivedBuffer());
                            Log.v(TAG, String.valueOf(received.length));
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
        if (mMoveOutsideHandler == null) {
            mMoveOutsideHandler = new MoveOutsideHandler(MoveOutsideActivity.this);
        }
        if (HBluetooth.getInstance(MoveOutsideActivity.this).isPrepared()) {
            HBluetooth.getInstance(MoveOutsideActivity.this)
                    .setHandler(mMoveOutsideHandler)
                    .setCommunications(communications)
                    .Start();
        }
    }

    // ================================= GridView adapter ========================================== //

    /**
     * 电梯层数 GridView adapter
     */
    private class OutsideFloorAdapter extends BaseAdapter {

        private Floors mFloors;

        public OutsideFloorAdapter(Floors floors) {
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
            LayoutInflater mInflater = LayoutInflater.from(MoveOutsideActivity.this);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.move_outside_row_item, null);
                holder = new ViewHolder();
                holder.mFloorTextView = (TypefaceTextView) convertView.findViewById(R.id.floor_text);
                holder.mUpButton = (TintedImageButton) convertView.findViewById(R.id.up_button);
                holder.mDownButton = (TintedImageButton) convertView.findViewById(R.id.down_button);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final int index = position;
            holder.mFloorTextView.setText(String.valueOf(mFloors.startFloors + position));
            holder.mUpButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String condition = getCondition(index);
                    for (RealTimeMonitor stateCode : mStateCodes) {
                        if (condition.equalsIgnoreCase(stateCode.getName())) {
                            final RealTimeMonitor codeObject = stateCode;
                            Log.v(TAG, codeObject.getCode());
                            HCommunication[] communications = new HCommunication[]{
                                    new HCommunication() {
                                        @Override
                                        public void beforeSend() {

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
                                HBluetooth.getInstance(MoveOutsideActivity.this)
                                        .setCommunications(communications)
                                        .Start();
                            }
                        }
                    }
                }
            });
            holder.mDownButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String condition = getCondition(index);
                    for (RealTimeMonitor stateCode : mStateCodes) {
                        if (condition.equalsIgnoreCase(stateCode.getName())) {
                            final RealTimeMonitor codeObject = stateCode;
                            Log.v(TAG, codeObject.getCode());
                            HCommunication[] communications = new HCommunication[]{
                                    new HCommunication() {
                                        @Override
                                        public void beforeSend() {

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
                                HBluetooth.getInstance(MoveOutsideActivity.this)
                                        .setCommunications(communications)
                                        .Start();
                            }
                        }
                    }
                }
            });
            if (position == 0) {
                holder.mUpButton.setVisibility(View.VISIBLE);
                holder.mDownButton.setVisibility(View.INVISIBLE);
            } else if (position == (mFloors.endFloors - mFloors.startFloors)) {
                holder.mUpButton.setVisibility(View.INVISIBLE);
                holder.mDownButton.setVisibility(View.VISIBLE);
            } else {
                holder.mUpButton.setVisibility(View.VISIBLE);
                holder.mDownButton.setVisibility(View.VISIBLE);
            }
            return convertView;
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
                if (index >= Integer.parseInt(parts[0]) && index <= Integer.parseInt(parts[1])) {
                    searchCondition = condition + "层召唤信息";
                }
            }
            return searchCondition;
        }

        // View holder
        private class ViewHolder {
            TypefaceTextView mFloorTextView;
            TintedImageButton mUpButton;
            TintedImageButton mDownButton;
        }
    }

    // =================================== MoveOutside Handler ==============================

    /**
     * 蓝牙 Socket handler
     */
    private class MoveOutsideHandler extends HHandler {

        private Object receivedMessage;

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
            receivedMessage = msg.obj;
            if (receivedMessage != null && (receivedMessage instanceof Floors)) {
                Floors floors = (Floors) receivedMessage;
                if (floors.totalParams == 2) {
                    MoveOutsideActivity.this.updateGridViewDataSource(floors);
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