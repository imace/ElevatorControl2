package com.inovance.ElevatorControl.activities;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.InjectView;
import butterknife.Views;
import com.bluetoothtool.BluetoothTool;
import com.inovance.ElevatorControl.R;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-4.
 * Time: 10:40.
 */

public class BluetoothAddressActivity extends Activity {

    /**
     * 设备名称
     */
    @InjectView(R.id.device_name)
    TextView deviceName;

    /**
     * 设备地址
     */
    @InjectView(R.id.device_address)
    TextView deviceAddress;

    /**
     * 已连接设备信息Container
     */
    @InjectView(R.id.connected_device_view)
    LinearLayout connectedDeviceView;

    /**
     * 未连接设备文字提示
     */
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
        if (BluetoothTool.getInstance().isConnected()) {
            BluetoothDevice connectedDevice = BluetoothTool.getInstance().connectedDevice;
            deviceName.setText(connectedDevice.getName());
            deviceAddress.setText(connectedDevice.getAddress());
        } else {
            connectedDeviceView.setVisibility(View.GONE);
            noConnectedDevice.setVisibility(View.VISIBLE);
        }
    }

}