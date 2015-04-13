package com.inovance.elevatorcontrol.handlers;

import android.app.Activity;
import android.content.Intent;
import android.os.Message;

import com.inovance.bluetoothtool.BluetoothHandler;
import com.inovance.elevatorcontrol.window.UnlockWindow;

public class UnlockHandler extends BluetoothHandler {

    private static final String DEBUG_TAG = UnlockHandler.class.getSimpleName();

    private Activity mActivity;

    public UnlockHandler(Activity activity) {
        super(activity);
        mActivity = activity;
    }

    @Override
    public void onDeviceLocked(Message message) {
        super.onDeviceLocked(message);
        mActivity.startActivity(new Intent(mActivity, UnlockWindow.class));
    }
}
