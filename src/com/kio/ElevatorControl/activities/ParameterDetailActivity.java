package com.kio.ElevatorControl.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import butterknife.InjectView;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HHandler;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.daos.ParameterGroupSettingsDao;
import com.kio.ElevatorControl.handlers.ParameterHandler;
import com.kio.ElevatorControl.models.ParameterGroupSettings;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;
import com.kio.ElevatorControl.views.dialogs.CustomDialog;
import org.holoeverywhere.app.Activity;

import java.util.List;

public class ParameterDetailActivity extends Activity {

    private static final String TAG = ParameterDetailActivity.class.getSimpleName();

    private List<ParameterSettings> parameterSettings;

    private ParameterHandler parameterHandler;

    private UpdateHandler updateHandler;

    private int currentUpdateIndex = -1;

    /**
     * 页面元素注入
     */
    @InjectView(R.id.parameter_group_settings_list)
    public ListView parameterGroupSettingsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parameter_group);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        Views.inject(this);
        parameterHandler = new ParameterHandler(this);
        updateHandler = new UpdateHandler(this);
        bindListViewItemClickListener();
    }

    private void bindListViewItemClickListener() {
        parameterGroupSettingsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                final ParameterSettings setting = parameterHandler.parameters.get(position);
                AlertDialog.Builder dialog = CustomDialog.parameterSettingDialog(ParameterDetailActivity.this,
                        setting);
                dialog.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int position) {
                        currentUpdateIndex = position;
                        final Dialog dialogView = Dialog.class.cast(dialogInterface);
                        HCommunication[] communications = new HCommunication[1];
                        communications[0] = new HCommunication() {
                            @Override
                            public void beforeSend() {
                                String inputData = ((EditText) dialogView.findViewById(R.id.parameter_setting_value))
                                        .getText()
                                        .toString();
                                String parseData = ParseSerialsUtils
                                        .getHexStringFromUserInputParameterSetting(inputData, setting);
                                setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0106"
                                        + setting.getCode()
                                        + parseData
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
                                    byte[] received = HSerial
                                            .trimEnd(getReceivedBuffer());
                                    setting.setReceived(received);
                                    Log.v(TAG, HSerial.byte2HexStr(received));
                                    return setting;
                                }
                                return null;
                            }
                        };
                        if (HBluetooth.getInstance(ParameterDetailActivity.this).isPrepared()) {
                            HBluetooth.getInstance(ParameterDetailActivity.this)
                                    .setHandler(updateHandler)
                                    .setCommunications(communications)
                                    .Start();
                        }
                    }
                });
                dialog.show();
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
                        byte[] received = HSerial
                                .trimEnd(getReceivedBuffer());
                        ParameterSettings settings = (ParameterSettings) this
                                .getItem();
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
                parameterHandler.parameters.set(currentUpdateIndex, settings);
                parameterHandler.parameterSettingsAdapter.notifyDataSetChanged();
                ParameterDetailActivity.this.currentUpdateIndex = -1;
            }
        }

    }

}
