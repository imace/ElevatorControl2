package com.kio.ElevatorControl.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import com.kio.ElevatorControl.handlers.ParameterHandler;
import com.kio.ElevatorControl.models.ParameterGroupSettings;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.models.ParameterStatusItem;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;
import com.kio.ElevatorControl.views.dialogs.CustomDialog;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.ProgressBar;

import java.util.List;

public class ParameterDetailActivity extends Activity {

    private static final String TAG = ParameterDetailActivity.class.getSimpleName();

    private List<ParameterSettings> parameterSettings;

    private ParameterHandler parameterHandler;

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
        parameterHandler = new ParameterHandler(this);
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
                final ParameterSettings settings = parameterHandler.parameters.get(position);
                AlertDialog.Builder builder = CustomDialog.parameterDetailDialog(ParameterDetailActivity.this,
                        settings);
                builder.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final AlertDialog dialogView = AlertDialog.class.cast(dialogInterface);
                        if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[0]) {
                            final String inputValue = ((EditText) dialogView.findViewById(R.id.setting_value))
                                    .getText()
                                    .toString();
                            if (ParseSerialsUtils.validateUserInputValue(ParameterDetailActivity.this,
                                    settings,
                                    inputValue)) {
                                startSetNewValueCommunications(index,
                                        ParseSerialsUtils
                                                .getHexStringFromUserInputParameterSetting(inputValue, settings));
                            }
                        }
                        if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[1]) {
                            int checkedIndex = dialogView.getListView().getCheckedItemPosition();
                            if (checkedIndex != ParseSerialsUtils.getIntFromBytes(settings.getReceived())) {
                                startSetNewValueCommunications(index, String.format("%04x ", checkedIndex));
                            }
                        }
                        if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[2]) {
                            ListView listView = (ListView) dialogView.findViewById(R.id.switch_list);
                            DialogSwitchListViewAdapter adapter = (DialogSwitchListViewAdapter) listView.getAdapterSource();
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
                        }
                        if (detailDialog != null) {
                            detailDialog.dismiss();
                        }
                    }
                });
                detailDialog = builder.create();
                detailDialog.show();
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
        initBluetooth();
    }

    /**
     * Start Set New Value Communications
     *
     * @param position ListView Item Position
     * @param value    New Setting value (Hex String)
     */
    private void startSetNewValueCommunications(int position, String value) {
        final String newSettingValue = value;
        final ParameterSettings settings = parameterHandler.parameters.get(position);
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

    private void initBluetooth() {
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
                    .setHandler(parameterHandler)
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

    // ===================================== Update ListView Data Handler =============================================

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
                parameterHandler.parameters.set(index, settings);
                parameterHandler.parameterSettingsAdapter.notifyDataSetChanged();
            }
        }

    }

}
