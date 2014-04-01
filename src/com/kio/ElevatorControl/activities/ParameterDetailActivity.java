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
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.ProgressBar;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ParameterDetailActivity extends Activity {

    private static final String TAG = ParameterDetailActivity.class.getSimpleName();

    private List<ParameterSettings> parameterSettings;

    private ParameterDetailHandler parameterDetailHandler;

    private UpdateHandler updateHandler;

    private AlertDialog detailDialog;

    /**
     * 加载指示器
     */
    @InjectView(R.id.progress_bar)
    public ProgressBar progressBar;

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
                final int index = position;
                final ParameterSettings settings = parameterDetailHandler.parametersList.get(position);
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
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        int SelectedId = this.getIntent().getIntExtra("SelectedId", 0);
        ParameterGroupSettings parameterGroupSettings = ParameterGroupSettingsDao.findById(
                this, SelectedId);
        this.setTitle(parameterGroupSettings.getGroupText());
        parameterSettings = parameterGroupSettings.getParametersettings().getList();
        //startCommunications();
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
        final ParameterSettings settings = parameterDetailHandler.parametersList.get(position);
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
     * Start communications
     */
    private void startCommunications() {
        HCommunication[] communications = new HCommunication[parameterSettings.size()];
        int size = parameterSettings.size();
        for (int i = 0; i < size; i++) {
            communications[i] = new HCommunication(parameterSettings.get(i)) {
                @Override
                public void beforeSend() {
                    if (this.getItem() instanceof ParameterSettings) {
                        ParameterSettings settings = (ParameterSettings) this
                                .getItem();
                        settings.getCode();
                        String code = ParseSerialsUtils.getCalculatedCode(settings);
                        this.setSendBuffer(HSerial.crc16(HSerial
                                .hexStr2Ints("0103" + code + "0001")));
                    }
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
                        ParameterSettings settings = (ParameterSettings) this.getItem();
                        settings.setReceived(received);
                        return settings;
                    }
                    return null;
                }

            };
        }
        if (HBluetooth.getInstance(ParameterDetailActivity.this).isPrepared()) {
            HBluetooth.getInstance(ParameterDetailActivity.this)
                    .setHandler(parameterDetailHandler)
                    .setCommunications(communications)
                    .Start();
        }
    }

    /**
     * Start Combination Communications
     * 组合发送指令
     */
    private void startCombinationCommunications() {
        final int size = parameterSettings.size();
        final int count = size <= 10 ? 1 : ((size - size % 10) / 10 + (size % 10 == 0 ? 0 : 1));
        HCommunication[] communications = new HCommunication[count];
        for (int i = 0; i < count; i++) {
            final int position = i;
            final ParameterSettings firstItem = parameterSettings.get(position * 10);
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
                                ParameterSettings item = parameterSettings.get(position * 10 + j);
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
        if (HBluetooth.getInstance(ParameterDetailActivity.this).isPrepared()) {
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
                parameterDetailHandler.parametersList.set(index, settings);
                parameterDetailHandler.parameterSettingsAdapter.notifyDataSetChanged();
            }
        }

    }

}
