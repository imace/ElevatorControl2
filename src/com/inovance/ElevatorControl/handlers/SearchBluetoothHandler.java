package com.inovance.ElevatorControl.handlers;

import android.app.Activity;
import android.os.Message;
import com.hbluetooth.Devices;
import com.hbluetooth.HHandler;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.activities.NavigationTabActivity;
import org.holoeverywhere.widget.Toast;

/**
 * Created by keith on 14-3-9.
 * User keith
 * Date 14-3-9
 * Time 下午9:41
 */
public class SearchBluetoothHandler extends HHandler {

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
    public void onBeginPreparing(Message msg) {
        super.onBeginPreparing(msg);
        mNavigationTabActivity.mRefreshActionItem.showProgress(true);
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
        mNavigationTabActivity.mRefreshActionItem.showProgress(true);
        if (msg.obj != null && msg.obj instanceof Devices) {
            mNavigationTabActivity.updateSpinnerDropdownItem(((Devices) msg.obj).getDevices());
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
    public void onPrepared(Message msg) {
        super.onPrepared(msg);
        mNavigationTabActivity.mRefreshActionItem.showProgress(false);
        mNavigationTabActivity.startHomeActivityStatusSyncTask();
        Toast.makeText(activity, activity.getResources().getString(R.string.success_connect),
                Toast.LENGTH_SHORT).show();
    }

    /**
     * PREPARE FAILED
     *
     * @param msg message
     */
    @Override
    public void onPrepError(Message msg) {
        super.onPrepError(msg);
        mNavigationTabActivity.mRefreshActionItem.showProgress(false);
        String errorMessage = (null == msg.obj) ? activity.getResources().getString(R.string.failed_connect) : msg.obj.toString();
        Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show();
    }

    /**
     * TALK RECEIVE
     *
     * @param msg message
     */
    @Override
    public void onTalkReceive(Message msg) {
        super.onTalkReceive(msg);
        mNavigationTabActivity.mRefreshActionItem.showProgress(false);
    }

    /**
     * HANDLER CHANGED
     */
    @Override
    public void onHandlerChanged(Message msg) {
        super.onHandlerChanged(msg);
    }

    public void onDiscoveryFinished(Message message) {
        super.onDiscoveryFinished(message);
        mNavigationTabActivity.mRefreshActionItem.showProgress(false);
    }
}
