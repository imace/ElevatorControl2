package com.inovance.ElevatorControl.activities;

import android.os.Bundle;
import android.os.Message;
import android.view.MenuItem;
import butterknife.InjectView;
import butterknife.Views;
import com.bluetoothtool.BluetoothHandler;
import com.bluetoothtool.BluetoothTalk;
import com.bluetoothtool.BluetoothTool;
import com.bluetoothtool.SerialUtility;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.daos.ParameterSettingsDao;
import com.inovance.ElevatorControl.models.ObjectListHolder;
import com.inovance.ElevatorControl.models.ParameterSettings;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import com.mobsandgeeks.adapters.InstantAdapter;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.ListView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-17.
 * Time: 10:48.
 */
public class ViewErrorStatusActivity extends Activity {

    @InjectView(R.id.list_view)
    ListView listView;

    private static final String[] filters = new String[]{
            "最后一次故障",
            "最后一次子码",
            "最后一次月日",
            "最后一次时间",
            "最后一次逻辑信息",
            "最后一次曲线信息",
            "最后一次设定速度",
            "最后一次反馈速度",
            "最后一次母线电压",
            "最后一次当前位置",
            "最后一次输出电流",
            "最后一次输出频率",
            "最后一次转矩电流"
    };

    private ErrorStatusHandler errorStatusHandler;

    private BluetoothTalk[] communications;

    public InstantAdapter<ParameterSettings> instantAdapter;

    private List<ParameterSettings> settingsList;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setTitle(R.string.error_status_title);
        setContentView(R.layout.activity_view_error_status);
        Views.inject(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        errorStatusHandler = new ErrorStatusHandler(this);
        createCommunication();
        startCommunication();
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

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    private void createCommunication() {
        if (communications == null) {
            settingsList = ParameterSettingsDao.findByNames(this, filters);
            instantAdapter = new InstantAdapter<ParameterSettings>(this,
                    R.layout.list_parameter_group_item,
                    ParameterSettings.class,
                    settingsList);
            listView.setAdapter(instantAdapter);
            final int size = settingsList.size();
            final int count = size <= 10 ? 1 : ((size - size % 10) / 10 + (size % 10 == 0 ? 0 : 1));
            communications = new BluetoothTalk[count];
            for (int i = 0; i < count; i++) {
                final int position = i;
                final ParameterSettings firstItem = settingsList.get(position * 10);
                final int length = size <= 10 ? size : (size % 10 == 0 ? 10 : ((position == count - 1) ? size % 10 : 10));
                communications[i] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16(SerialUtility
                                .hexStr2Ints("0103"
                                        + ParseSerialsUtils.getCalculatedCode(firstItem)
                                        + String.format("%04x", length)
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
                            byte[] data = SerialUtility.trimEnd(getReceivedBuffer());
                            short bytesLength = ByteBuffer.wrap(new byte[]{data[2], data[3]}).getShort();
                            if (length * 2 == bytesLength) {
                                List<ParameterSettings> tempList = new ArrayList<ParameterSettings>();
                                for (int j = 0; j < length; j++) {
                                    if (position * 10 + j < settingsList.size()) {
                                        ParameterSettings item = settingsList.get(position * 10 + j);
                                        byte[] tempData = SerialUtility.crc16(SerialUtility.hexStr2Ints("01030002"
                                                + SerialUtility.byte2HexStr(new byte[]{data[4 + j * 2], data[5 + j * 2]})));
                                        item.setReceived(tempData);
                                        tempList.add(item);
                                    }
                                }
                                ObjectListHolder holder = new ObjectListHolder();
                                holder.setParameterSettingsList(tempList);
                                return holder;
                            }
                        }
                        return null;
                    }
                };
            }
        }
    }

    public void startCommunication() {
        if (communications != null) {
            if (BluetoothTool.getInstance(ViewErrorStatusActivity.this).isPrepared()) {
                errorStatusHandler.sendCount = communications.length;
                BluetoothTool.getInstance(ViewErrorStatusActivity.this)
                        .setHandler(errorStatusHandler)
                        .setCommunications(communications)
                        .send();
            }
        }
    }

    // ================================================== Handler =========================================== //
    public class ErrorStatusHandler extends BluetoothHandler {

        public int sendCount = 0;

        public int receiveCount = 0;

        public List<ParameterSettings> tempList;

        public ErrorStatusHandler(android.app.Activity activity) {
            super(activity);
            TAG = ErrorStatusHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
            tempList = new ArrayList<ParameterSettings>();
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (receiveCount == sendCount) {
                for (ParameterSettings item : tempList) {
                    if (item.getName().equalsIgnoreCase(filters[2])) {
                        int intValue = ParseSerialsUtils.getIntFromBytes(item.getReceived());
                        item.setFinalValue(intValue / 100 + "月" + intValue % 100 + "日");
                    }
                    if (item.getName().equalsIgnoreCase(filters[3])) {
                        int intValue = ParseSerialsUtils.getIntFromBytes(item.getReceived());
                        item.setFinalValue(intValue / 100 + ":" + intValue % 100);
                    }
                }
                ((ViewErrorStatusActivity) activity).settingsList.clear();
                ((ViewErrorStatusActivity) activity).settingsList.addAll(tempList);
                ((ViewErrorStatusActivity) activity).instantAdapter.notifyDataSetChanged();
            } else {
                ((ViewErrorStatusActivity) activity).startCommunication();
            }
        }

        @Override
        public void onTalkReceive(Message msg) {
            if (msg.obj instanceof ObjectListHolder) {
                ObjectListHolder holder = (ObjectListHolder) msg.obj;
                for (ParameterSettings item : holder.getParameterSettingsList()) {
                    if (!item.getName().contains(ApplicationConfig.RETAIN_NAME)) {
                        tempList.add(item);
                    }
                }
                receiveCount++;
            }
        }

        @Override
        public void onTalkError(Message msg) {
            ((ViewErrorStatusActivity) activity).startCommunication();
        }

    }

}