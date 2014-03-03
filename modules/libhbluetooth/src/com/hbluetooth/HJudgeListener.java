package com.hbluetooth;

import android.bluetooth.BluetoothDevice;

/**
 * @author jch
 */
public abstract class HJudgeListener {
    public abstract boolean judge(BluetoothDevice dev);
}
