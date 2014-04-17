package com.kio.ElevatorControl.handlers;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Message;
import android.util.Log;
import com.hbluetooth.HHandler;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.activities.NavigationTabActivity;
import org.holoeverywhere.widget.Toast;

import java.util.Map;
import java.util.Set;

/**
 * Created by keith on 14-3-9.
 * User keith
 * Date 14-3-9
 * Time 下午9:41
 */
public class SearchBluetoothHandler extends HHandler {

    private static final String TAG = SearchBluetoothHandler.class.getSimpleName();

    private static final String[] filterArray = new String[]{"BC04-B"};

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
        mNavigationTabActivity.mRefreshActionItem.showProgress(true);
        assert ((Map<String, BluetoothDevice>) msg.obj) != null;
        Set<String> strings = ((Map<String, BluetoothDevice>) msg.obj).keySet();
        String[] devices = strings.toArray(new String[strings.size()]);
        mNavigationTabActivity.updateSpinnerDropdownItem(devices);
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
        // 进度条
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
        mNavigationTabActivity.mRefreshActionItem.showProgress(false);
    }

    /**
     * HANDLER CHANGED
     */
    @Override
    public void onHandlerChanged(Message msg) {
        // 进度条
        Log.v(TAG, "Handler changed");
    }

}
