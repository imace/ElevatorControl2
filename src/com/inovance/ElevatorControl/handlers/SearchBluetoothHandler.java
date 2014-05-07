package com.inovance.ElevatorControl.handlers;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Message;
import com.bluetoothtool.BluetoothHandler;
import com.bluetoothtool.DevicesHolder;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.activities.NavigationTabActivity;
import org.holoeverywhere.widget.Toast;

import java.util.ArrayList;

/**
 * Created by keith on 14-3-9.
 * User keith
 * Date 14-3-9
 * Time 下午9:41
 */
public class SearchBluetoothHandler extends BluetoothHandler {

    private static final String TAG = SearchBluetoothHandler.class.getSimpleName();

    private NavigationTabActivity mNavigationTabActivity;

    public SearchBluetoothHandler(Activity activity) {
        super(activity);
        mNavigationTabActivity = (NavigationTabActivity) activity;
    }

    /**
     * BEGIN PREPARING
     *
     * @param msg message
     */
    @Override
    public void onBeginDiscovering(Message msg) {
        super.onBeginDiscovering(msg);
        mNavigationTabActivity.showRefreshButtonProgress(true);
        mNavigationTabActivity.updateSpinnerDropdownItem(new ArrayList<BluetoothDevice>());
    }

    /**
     * FOUND DEVICE
     *
     * @param msg message
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onFoundDevice(Message msg) {
        super.onFoundDevice(msg);
        if (mNavigationTabActivity.researchDevicesButton != null) {
            mNavigationTabActivity.showRefreshButtonProgress(true);
        }
        if (msg.obj != null && msg.obj instanceof DevicesHolder) {
            DevicesHolder devicesHolder = ((DevicesHolder) msg.obj);
            if (devicesHolder.getDevices() != null) {
                if (mNavigationTabActivity != null) {
                    mNavigationTabActivity.updateSpinnerDropdownItem(devicesHolder.getDevices());
                }
            }
        }
    }

    /**
     * KILL BLUETOOTH
     *
     * @param msg message
     */
    @Override
    public void onKillBluetooth(Message msg) {
        super.onKillBluetooth(msg);
    }

    /**
     * PREPARE SUCCESSFUL
     *
     * @param msg message
     */
    @Override
    public void onConnected(Message msg) {
        super.onConnected(msg);
        mNavigationTabActivity.showRefreshButtonProgress(false);
        mNavigationTabActivity.startGetDeviceTypeAndNumberTask();
        Toast.makeText(activity, activity.getResources().getString(R.string.success_connect),
                Toast.LENGTH_SHORT).show();
    }

    /**
     * PREPARE FAILED
     *
     * @param msg message
     */
    @Override
    public void onConnectFailed(Message msg) {
        super.onConnectFailed(msg);
        mNavigationTabActivity.showRefreshButtonProgress(false);
        String errorMessage = (null == msg.obj) ? activity.getResources().getString(R.string.failed_connect)
                : msg.obj.toString();
        Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show();
    }

    public void onDiscoveryFinished(Message message) {
        super.onDiscoveryFinished(message);
        mNavigationTabActivity.showRefreshButtonProgress(false);
    }
}
