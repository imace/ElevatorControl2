package com.kio.ElevatorControl.activities;

import org.holoeverywhere.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-6.
 * Time: 11:03.
 */
public class MoveInsideActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.move_inside_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_move_inside);
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

    private void loadDataAndRenderView() {
        HCommunication[] hCommunications = new HCommunication[1];
        HCommunication communication = new HCommunication() {
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
                if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                    // 校验数据
                    byte[] received = HSerial.trimEnd(getReceivedBuffer());
                    Log.v("parameterSettingDialog receive", HSerial.byte2HexStr(received));
                    return null;
                }
                return null;
            }
        };
        if (HBluetooth.getInstance(MoveInsideActivity.this).isPrepared()) {
            HBluetooth.getInstance(MoveInsideActivity.this)
                    .setCommunications(hCommunications)
                    .Start();
        }
    }

}