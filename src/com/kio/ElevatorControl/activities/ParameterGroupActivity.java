package com.kio.ElevatorControl.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
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
        Views.inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 组
        int SelectedId = this.getIntent().getIntExtra("SelectedId", 0);
        ParameterGroupSettings pgroup = ParameterGroupSettingsDao.findById(
                this, SelectedId);

        this.setTitle(pgroup.getGroupText());
        // 组成员
        parameterSettings = pgroup.getParametersettings().getList();
        initBluetooth();
    }

    private void initBluetooth() {
        HCommunication[] cumms = new HCommunication[parameterSettings.size()];
        for (int i = 0; i < parameterSettings.size(); i++) {
            cumms[i] = new HCommunication(parameterSettings.get(i)) {
                @Override
                public void beforeSend() {
                    if (this.getItem() instanceof ParameterSettings) {
                        ParameterSettings psobj = (ParameterSettings) this
                                .getItem();
                        psobj.getCode();
                        String acode = ParseSerialsUtils.getCalculatedCode(psobj);
                        this.setSendbuffer(HSerial.crc16(HSerial
                                .hexStr2Ints("0103" + acode + "0001")));
                        Log.v("ParameterGroupActivity read : ",
                                "0103" + acode + "0001");
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
                    if (HSerial.isCRC16Valid(getReceivebuffer())) {
                        byte[] received = HSerial
                                .trimEnd(getReceivebuffer());
                        ParameterSettings psobj = (ParameterSettings) this
                                .getItem();
                        psobj.setReceived(received);
                        return psobj;
                    }
                    return null;
                }

            };
        }

        HBluetooth.getInstance(this).setHandler(new ParameterHandler(this)).setCommunications(cumms).HStart();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.parameter_group, menu);
        return true;
    }

}
