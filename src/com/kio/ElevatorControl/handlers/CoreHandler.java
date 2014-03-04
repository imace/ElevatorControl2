package com.kio.ElevatorControl.handlers;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HHandler;
import com.hbluetooth.HJudgeListener;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.views.customspinner.HCustomSpinner;

import java.util.Map;

/**
 * 运行在CoreAcivity主线程,仅做一些ui操作
 *
 * @author jch
 */
public class CoreHandler extends HHandler {

    private HBluetooth hbt;

    public CoreHandler(Activity activity) {
        super(activity);
        this.HTAG = "CoreHandler";
        this.hbt = HBluetooth.getInstance(activity);
    }

    /**
     * BEGINPREPARING
     *
     * @param msg
     */
    @Override
    public void onBeginPreparing(Message msg) {
        Log.v(HTAG, "onBeginPreparing");
        activity.getActionBar().getCustomView().findViewById(R.id.title_process_bar).setVisibility(View.VISIBLE);
    }

    /**
     * FOUNDDEVICE
     *
     * @param msg
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onFoundDevice(Message msg) {

        activity.getActionBar().getCustomView().findViewById(R.id.title_process_bar).setVisibility(View.VISIBLE);

        HCustomSpinner spinn = null;
        spinn = ((HCustomSpinner) activity.getActionBar().getCustomView().findViewById(R.id.custom_spinner));
        spinn.setAdapter(new ArrayAdapter<String>(activity, android.R.layout.select_dialog_item, ((Map<String, BluetoothDevice>) msg.obj).keySet().toArray(new String[]{})));
        spinn.setOnItemSelectedListener(new HCustomSpinner.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                activity.getActionBar().getCustomView().findViewById(R.id.title_process_bar).setVisibility(View.VISIBLE);

                // 设置选中
                final String devName = parent.getItemAtPosition(position).toString();
                ((HCustomSpinner) activity.getActionBar().getCustomView().findViewById(R.id.custom_spinner)).setText(devName);
                //
                hbt.setPrepared(false).setDiscoveryMode(false).setJudgement(new HJudgeListener() {

                    @Override
                    public boolean judge(BluetoothDevice dev) {
                        String deviceLogName = dev.getName() + "(" + dev.getAddress() + ")";
                        return deviceLogName.trim().equalsIgnoreCase(devName);
                    }
                }).HStart();
            }
        });
        Log.v(HTAG, "onFoundDevice : " + ((Map<String, BluetoothDevice>) msg.obj).keySet().toString());
    }

    /**
     * KILLBLUETOOTH
     *
     * @param msg
     */
    @Override
    public void onKillBluethooth(Message msg) {
        super.onKillBluethooth(msg);
    }

    /**
     * PREPARESUCCESSFULL
     *
     * @param msg
     */
    @Override
    public void onPrepared(Message msg) {
        // 进度条
        activity.getActionBar().getCustomView().findViewById(R.id.title_process_bar).setVisibility(View.INVISIBLE);
        Toast.makeText(activity, activity.getResources().getString(R.string.prepbthconn), Toast.LENGTH_SHORT).show();
    }

    /**
     * PREPAREFAILED
     *
     * @param msg
     */
    @Override
    public void onPrepError(Message msg) {
        // 进度条
        activity.getActionBar().getCustomView().findViewById(R.id.title_process_bar).setVisibility(View.INVISIBLE);
        String errmsg = (null == msg.obj) ? activity.getResources().getString(R.string.failbthconn) : msg.obj.toString();
        Toast.makeText(activity, errmsg, Toast.LENGTH_SHORT).show();
    }

    /**
     * TALKRECEIVE
     *
     * @param msg
     */
    @Override
    public void onTalkReceive(Message msg) {
    }

    /**
     * HANDLERCHANGED
     */
    @Override
    public void onHandlerChanged(Message msg) {
        // 进度条
        activity.getActionBar().getCustomView().findViewById(R.id.title_process_bar).setVisibility(View.INVISIBLE);
    }

}
