package com.inovance.ElevatorControl.handlers;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Message;
import android.widget.Toast;
import com.bluetoothtool.BluetoothHandler;
import com.bluetoothtool.BluetoothTool;
import com.bluetoothtool.DevicesHolder;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.activities.NavigationTabActivity;

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

    private boolean isChangingDevice = false;

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
        isChangingDevice = false;
        mNavigationTabActivity.showRefreshButtonProgress(false);
        mNavigationTabActivity.startGetNormalDeviceTypeTask();
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
        mNavigationTabActivity.setSpinnerDataSource();
        Toast.makeText(activity, R.string.failed_connect, Toast.LENGTH_SHORT).show();
    }

    /**
     * 搜索结束
     *
     * @param message message
     */
    @Override
    public void onDiscoveryFinished(Message message) {
        super.onDiscoveryFinished(message);
        mNavigationTabActivity.showRefreshButtonProgress(false);
        mNavigationTabActivity.updateSearchResult();
    }

    /**
     * 将要连接
     *
     * @param message message
     */
    @Override
    public void onWillConnect(Message message) {
        super.onWillConnect(message);
        mNavigationTabActivity.showRefreshButtonProgress(false);
    }

    /**
     * 设备断开连接
     *
     * @param message message
     */
    @Override
    public void onDisconnected(Message message) {
        super.onDisconnected(message);
        if (!isChangingDevice) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                    .setTitle(R.string.connect_lost_title)
                    .setMessage(R.string.connect_lost_message)
                    .setNegativeButton(R.string.dialog_btn_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mNavigationTabActivity.setSpinnerDataSource();
                        }
                    })
                    .setPositiveButton(R.string.retry_connect_device, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            final BluetoothDevice currentDevice = BluetoothTool.getInstance().connectedDevice;
                            if (currentDevice != null) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        BluetoothTool.getInstance().connectDevice(currentDevice);
                                    }
                                }).start();
                            }
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * 设备切换
     *
     * @param message message
     */
    @Override
    public void onDeviceChanged(Message message) {
        super.onDeviceChanged(message);
        isChangingDevice = true;
    }
}
