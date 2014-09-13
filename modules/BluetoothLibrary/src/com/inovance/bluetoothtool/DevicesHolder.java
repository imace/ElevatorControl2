package com.inovance.bluetoothtool;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-28.
 * Time: 17:41.
 */
public class DevicesHolder {

    private List<BluetoothDevice> devices;

    public List<BluetoothDevice> getDevices() {
        if (devices == null) {
            return new ArrayList<BluetoothDevice>();
        }
        return devices;
    }

    public void setDevices(List<BluetoothDevice> devices) {
        this.devices = devices;
    }
}
