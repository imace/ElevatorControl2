package com.inovance.ElevatorControl.activities;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import butterknife.InjectView;
import butterknife.Views;
import com.bluetoothtool.BluetoothTool;
import com.inovance.ElevatorControl.R;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.TextView;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-4.
 * Time: 10:40.
 */

public class BluetoothAddressActivity extends Activity {

    @InjectView(R.id.device_name)
    TextView deviceName;

    @InjectView(R.id.device_address)
    TextView deviceAddress;

    @InjectView(R.id.connected_device_view)
    LinearLayout connectedDeviceView;

    @InjectView(R.id.no_connected_device)
    TextView noConnectedDevice;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setTitle(R.string.bluetooth_address_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_bluttooth_address);
        Views.inject(this);
        updateConnectedDeviceInformation();
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

    /**
     * 更新连接蓝牙设备信息
     */
    private void updateConnectedDeviceInformation() {
        if (BluetoothTool.getInstance(BluetoothAddressActivity.this).isConnected()) {
            BluetoothDevice connectedDevice = BluetoothTool.getInstance(BluetoothAddressActivity.this).connectedDevice;
            deviceName.setText(connectedDevice.getName());
            deviceAddress.setText(connectedDevice.getAddress());
        } else {
            connectedDeviceView.setVisibility(View.GONE);
            noConnectedDevice.setVisibility(View.VISIBLE);
        }
    }

}