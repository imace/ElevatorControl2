package com.kio.ElevatorControl.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import butterknife.InjectView;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.daos.ParameterGroupSettingsDao;
import com.kio.ElevatorControl.handlers.ParameterHandler;
import com.kio.ElevatorControl.models.ParameterGroupSettings;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;

import java.util.List;

public class ParameterGroupActivity extends Activity {

    private List<ParameterSettings> parameterSettings;

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 组
        int SelectedId = this.getIntent().getIntExtra("SelectedId", 0);
        ParameterGroupSettings parameterGroupSettings = ParameterGroupSettingsDao.findById(
                this, SelectedId);

        this.setTitle(parameterGroupSettings.getGroupText());
        // 组成员
        parameterSettings = parameterGroupSettings.getParametersettings().getList();
        initBluetooth();
    }

    private void initBluetooth() {
        HCommunication[] hCommunications = new HCommunication[parameterSettings.size()];
        for (int i = 0; i < parameterSettings.size(); i++) {
            hCommunications[i] = new HCommunication(parameterSettings.get(i)) {
                @Override
                public void beforeSend() {
                    if (this.getItem() instanceof ParameterSettings) {
                        ParameterSettings settings = (ParameterSettings) this
                                .getItem();
                        settings.getCode();
                        String code = ParseSerialsUtils.getCalculatedCode(settings);
                        this.setSendBuffer(HSerial.crc16(HSerial
                                .hexStr2Ints("0103" + code + "0001")));
                        Log.v("ParameterGroupActivity read : ",
                                "0103" + code + "0001");
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
        HBluetooth.getInstance(this).setHandler(new ParameterHandler(this)).setCommunications(hCommunications).HStart();
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
        getMenuInflater().inflate(R.menu.parameter_group, menu);
        return true;
    }

}
