package com.kio.ElevatorControl.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import butterknife.InjectView;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HHandler;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.adapters.DialogSwitchListViewAdapter;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.daos.ParameterGroupSettingsDao;
import com.kio.ElevatorControl.handlers.ParameterDetailHandler;
import com.kio.ElevatorControl.models.ListHolder;
import com.kio.ElevatorControl.models.ParameterGroupSettings;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.models.ParameterStatusItem;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;
import com.kio.ElevatorControl.views.dialogs.CustomDialog;
import com.mobsandgeeks.adapters.InstantAdapter;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.ListView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ParameterDetailActivity extends Activity {

    private static final String TAG = ParameterDetailActivity.class.getSimpleName();

    private UpdateHandler updateHandler;

    private AlertDialog detailDialog;

    public List<ParameterSettings> settingsList;

    private ParameterDetailHandler parameterDetailHandler;

    public InstantAdapter<ParameterSettings> instantAdapter;

    private HCommunication[] communications;

    public boolean isSynced;

    /**
     * 功能参数详细列表
     */
    @InjectView(R.id.parameter_detail_list_view)
    public ListView parameterDetailListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parameter_detail);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        Views.inject(this);
        isSynced = false;
        parameterDetailHandler = new ParameterDetailHandler(this);
        updateHandler = new UpdateHandler(this);
        bindListViewItemClickListener();
    }

    /**
     * 绑定ListView Item点击时间
     */
    private void bindListViewItemClickListener() {
        parameterDetailListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (isSynced) {
                    final int index = position;
                    final ParameterSettings settings = settingsList.get(position);
                    AlertDialog.Builder builder = CustomDialog.parameterDetailDialog(ParameterDetailActivity.this,
                            settings);
                    builder.setPositiveButton(R.string.dialog_btn_ok, null);
                    detailDialog = builder.create();
                    detailDialog.show();
                    detailDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[0]) {
                                final String inputValue = ((EditText) detailDialog.findViewById(R.id.setting_value))
                                        .getText()
                                        .toString();
                                if (ParseSerialsUtils.validateUserInputValue(ParameterDetailActivity.this,
                                        settings,
                                        inputValue)) {
                                    startSetNewValueCommunications(index,
                                            String.format("%04x ", Integer.parseInt(inputValue)));
                                    detailDialog.dismiss();
                                }
                            }
                            if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[1]) {
                                int checkedIndex = detailDialog.getListView().getCheckedItemPosition();
                                if (checkedIndex != ParseSerialsUtils.getIntFromBytes(settings.getReceived())) {
                                    startSetNewValueCommunications(index, String.format("%04x ", checkedIndex));
                                }
                                detailDialog.dismiss();
                            }
                            if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[2]) {
                                ListView listView = (ListView) detailDialog.findViewById(R.id.switch_list);
                                DialogSwitchListViewAdapter adapter = (DialogSwitchListViewAdapter) listView
                                        .getAdapterSource();
                                List<ParameterStatusItem> list = adapter.getItemList();
                                byte[] data = settings.getReceived();
                                boolean[] booleans = HSerial.byte2BoolArr(data[4], data[5]);
                                int size = booleans.length;
                                String binaryString = "";
                                for (int j = 0; j < size; j++) {
                                    boolean hasValue = false;
                                    boolean settingValue = false;
                                    for (ParameterStatusItem item : list) {
                                        if (Integer.parseInt(item.id) == j) {
                                            hasValue = true;
                                            settingValue = item.status;
                                        }
                                    }
                                    if (hasValue) {
                                        binaryString += settingValue ? 1 : 0;
                                    } else {
                                        binaryString += booleans[j] ? 1 : 0;
                                    }
                                }
                                startSetNewValueCommunications(index,
                                        String.format("%04x ", Integer.parseInt(binaryString, 2)));
                                detailDialog.dismiss();
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        int SelectedId = this.getIntent().getIntExtra("SelectedId", 0);
        ParameterGroupSettings parameterGroupSettings = ParameterGroupSettingsDao.findById(
                this, SelectedId);
        this.setTitle(parameterGroupSettings.getGroupText());
        settingsList = parameterGroupSettings.getParametersettings().getList();
        List<ParameterSettings> tempList = new ArrayList<ParameterSettings>();
        for (ParameterSettings item : settingsList) {
            if (!item.getName().contains(ApplicationConfig.RETAIN_NAME)) {
                tempList.add(item);
            }
        }
        instantAdapter = new InstantAdapter<ParameterSettings>(this,
                R.layout.list_parameter_group_item,
                ParameterSettings.class,
                tempList);
        parameterDetailListView.setAdapter(instantAdapter);
        startCombinationCommunications();
    }

    /**
     * Start Set New Value Communications
     *
     * @param position ListView Item Position
     * @param value    New Setting value (Hex String)
     */
    private void startSetNewValueCommunications(int position, String value) {
        final String newSettingValue = value;
        final ParameterSettings settings = settingsList.get(position);
        HCommunication[] communications = new HCommunication[]{
                new HCommunication() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0106"
                                + settings.getCode()
                                + newSettingValue
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
                            settings.setReceived(received);
                            return settings;
                        }
                        return null;
                    }
                }
        };
        if (HBluetooth.getInstance(ParameterDetailActivity.this).isPrepared()) {
            updateHandler.index = position;
            HBluetooth.getInstance(ParameterDetailActivity.this)
                    .setHandler(updateHandler)
                    .setCommunications(communications)
                    .Start();
        }
    }

    /**
     * Start Combination Communications
     * 组合发送指令
     */
    public void startCombinationCommunications() {
        if (communications == null) {
            final int size = settingsList.size();
            final int count = size <= 10 ? 1 : ((size - size % 10) / 10 + (size % 10 == 0 ? 0 : 1));
            communications = new HCommunication[count];
            for (int i = 0; i < count; i++) {
                final int position = i;
                final ParameterSettings firstItem = settingsList.get(position * 10);
                final int length = size <= 10 ? size : (size % 10 == 0 ? 10 : ((position == count - 1) ? size % 10 : 10));
                communications[i] = new HCommunication() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(HSerial.crc16(HSerial
                                .hexStr2Ints("0103"
                                        + ParseSerialsUtils.getCalculatedCode(firstItem)
                                        + String.format("%04x ", length)
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
                            byte[] data = HSerial.trimEnd(getReceivedBuffer());
                            short bytesLength = ByteBuffer.wrap(new byte[]{data[2], data[3]}).getShort();
                            if (length * 2 == bytesLength) {
                                List<ParameterSettings> tempList = new ArrayList<ParameterSettings>();
                                for (int j = 0; j < length; j++) {
                                    ParameterSettings item = settingsList.get(position * 10 + j);
                                    byte[] tempData = HSerial.crc16(HSerial.hexStr2Ints("01030002"
                                            + HSerial.byte2HexStr(new byte[]{data[4 + j * 2], data[5 + j * 2]})));
                                    item.setReceived(tempData);
                                    tempList.add(item);
                                }
                                ListHolder holder = new ListHolder();
                                holder.setParameterSettingsList(tempList);
                                return holder;
                            }
                        }
                        return null;
                    }
                };
            }
        }
        if (HBluetooth.getInstance(ParameterDetailActivity.this).isPrepared()) {
            parameterDetailHandler.sendCount = communications.length;
            HBluetooth.getInstance(ParameterDetailActivity.this)
                    .setHandler(parameterDetailHandler)
                    .setCommunications(communications)
                    .Start();
        }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.parameter_group, menu);
        return true;
    }

    // ===================================== Update ListView Data Handler ======================================== //

    /**
     * Update Handler
     */
    private class UpdateHandler extends HHandler {

        private int index;

        public UpdateHandler(android.app.Activity activity) {
            super(activity);
            TAG = UpdateHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {

        }

        @Override
        public void onMultiTalkEnd(Message msg) {

        }

        @Override
        public void onTalkReceive(Message msg) {
            if (msg.obj instanceof ParameterSettings) {
                ParameterSettings settings = (ParameterSettings) msg.obj;
                ParameterDetailActivity.this.settingsList.set(index, settings);
                ParameterDetailActivity.this.instantAdapter.notifyDataSetChanged();
            }
        }

    }

}
