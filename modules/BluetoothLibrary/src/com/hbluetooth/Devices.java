package com.hbluetooth;

import android.bluetooth.BluetoothDevice;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-28.
 * Time: 17:41.
 */
public class Devices {

    private List<BluetoothDevice> devices;

    public List<BluetoothDevice> getDevices() {
        return devices;
    }

    public void setDevices(List<BluetoothDevice> devices) {
        this.devices = devices;
    }
}
