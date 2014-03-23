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
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.daos.ParameterSettingsDao;
import com.kio.ElevatorControl.daos.RealTimeMonitorDao;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.models.RealTimeMonitor;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;
import com.kio.ElevatorControl.views.TintedImageButton;
import com.kio.ElevatorControl.views.TypefaceTextView;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.GridView;

import java.util.ArrayList;
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

    private static final String codeType = "64";

    private List<RealTimeMonitor> realTimeMonitors;

    private MoveOutsideHandler mMoveOutsideHandler;

    private static final String TAG = MoveOutsideActivity.class.getSimpleName();

    private List<Integer> floors;

    private HCommunication[] getFloorsCommunications;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.move_outside_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_move_outside);
        Views.inject(this);
        mMoveOutsideHandler = new MoveOutsideHandler(this);
        realTimeMonitors = RealTimeMonitorDao.findByType(this, codeType);
        floors = new ArrayList<Integer>();
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
        List<ParameterSettings> settingsList = ParameterSettingsDao.findByNames(MoveOutsideActivity.this, names);
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
                        MoveOutsideActivity.this.floors.add(ParseSerialsUtils.getIntFromBytes(received));
                        setting.setReceived(received);
                        return setting;
                    }
                    return null;
                }
            };
        }
    }

    /**
     * 更新 GridView 数据源
     */
    public void updateGridViewDataSource(List<Integer> floors) {
        OutsideFloorAdapter adapter = new OutsideFloorAdapter(floors);
        mGridView.setAdapter(adapter);
    }

    private void loadDataAndRenderView() {
        if (getFloorsCommunications != null) {
            if (HBluetooth.getInstance(MoveOutsideActivity.this).isPrepared()) {
                HBluetooth.getInstance(MoveOutsideActivity.this)
                        .setHandler(mMoveOutsideHandler)
                        .setCommunications(getFloorsCommunications)
                        .Start();
            }
        }
    }

    /**
     * 取得需要发送的Code
     *
     * @param position GridView Item Index
     * @return String[]
     */
    private String[] getCallCode(int position, boolean isUp) {
        int index = position + 1;
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
            if (index >= Integer.parseInt(parts[0]) && index <= Integer.parseInt(parts[1])) {
                searchCondition = condition + "层召唤信息";
                int minus = index - Integer.parseInt(parts[0]);
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

    // ================================= GridView adapter ========================================== //

    /**
     * 电梯层数 GridView adapter
     */
    private class OutsideFloorAdapter extends BaseAdapter {

        private List<Integer> mFloors;

        public OutsideFloorAdapter(List<Integer> floors) {
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
            holder.mFloorTextView.setText(String.valueOf(Math.min(mFloors.get(0), mFloors.get(1)) + position));
            holder.mUpButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final String[] condition = MoveOutsideActivity.this.getCallCode(index, true);
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
                    if (HBluetooth.getInstance(MoveOutsideActivity.this).isPrepared()) {
                        HBluetooth.getInstance(MoveOutsideActivity.this)
                                .setCommunications(communications)
                                .Start();
                    }
                }
            });
            holder.mDownButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final String[] condition = MoveOutsideActivity.this.getCallCode(index, false);
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
                    if (HBluetooth.getInstance(MoveOutsideActivity.this).isPrepared()) {
                        HBluetooth.getInstance(MoveOutsideActivity.this)
                                .setCommunications(communications)
                                .Start();
                    }
                }
            });
            if (position == 0) {
                holder.mUpButton.setVisibility(View.VISIBLE);
                holder.mDownButton.setVisibility(View.INVISIBLE);
            } else if (position == getCount() - 1) {
                holder.mUpButton.setVisibility(View.INVISIBLE);
                holder.mDownButton.setVisibility(View.VISIBLE);
            } else {
                holder.mUpButton.setVisibility(View.VISIBLE);
                holder.mDownButton.setVisibility(View.VISIBLE);
            }
            return convertView;
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
            if (MoveOutsideActivity.this.floors.size() == 2) {
                MoveOutsideActivity.this.updateGridViewDataSource(MoveOutsideActivity.this.floors);
            } else {
                MoveOutsideActivity.this.floors = new ArrayList<Integer>();
                MoveOutsideActivity.this.loadDataAndRenderView();
            }
        }

        @Override
        public void onTalkReceive(Message msg) {

        }

    }

}