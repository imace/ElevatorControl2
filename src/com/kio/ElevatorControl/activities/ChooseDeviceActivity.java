package com.kio.ElevatorControl.activities;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HHandler;
import com.hbluetooth.HJudgeListener;
import com.kio.ElevatorControl.R;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.ProgressBar;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;

import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-18.
 * Time: 16:50.
 */
public class ChooseDeviceActivity extends Activity {

    @InjectView(R.id.list_view)
    ListView listView;

    @InjectView(R.id.research_devices)
    View researchDevices;

    @InjectView(R.id.progress_bar)
    ProgressBar progressBar;

    private SearchHandler searchHandler;

    private DeviceAdapter adapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_device);
        Views.inject(this);
        researchDevices.setEnabled(false);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String devName = adapter.getItem(position);
                HBluetooth bluetoothSocket = HBluetooth.getInstance(ChooseDeviceActivity.this);
                bluetoothSocket.setPrepared(false)
                        .setDiscoveryMode(false)
                        .setJudgement(new HJudgeListener() {
                            @Override
                            public boolean judge(BluetoothDevice dev) {
                                String deviceLogName = dev.getName() + "(" + dev.getAddress() + ")";
                                return deviceLogName.trim().equalsIgnoreCase(devName);
                            }
                        }).Start();
            }
        });
        searchHandler = new SearchHandler(ChooseDeviceActivity.this);
        researchDevices();
    }

    /**
     * 搜索蓝牙设备
     */
    private void researchDevices() {
        if (!HBluetooth.getInstance(ChooseDeviceActivity.this).isPrepared()) {
            HBluetooth.getInstance(ChooseDeviceActivity.this)
                    .setPrepared(false)
                    .setDiscoveryMode(true)
                    .setHandler(searchHandler)
                    .Start();
        }
    }

    @OnClick(R.id.research_devices)
    void researchButtonClick(){
        ChooseDeviceActivity.this.listView.setVisibility(View.INVISIBLE);
        ChooseDeviceActivity.this.progressBar.setVisibility(View.VISIBLE);
        researchDevices();
    }

    // ================================================== Device Adapter ==============================================

    /**
     * Device Adapter
     */
    private class DeviceAdapter extends BaseAdapter {

        private String[] deviceLists;

        public DeviceAdapter(String[] devices) {
            this.deviceLists = devices;
        }

        @Override
        public int getCount() {
            return deviceLists.length;
        }

        @Override
        public String getItem(int position) {
            return deviceLists[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            LayoutInflater mInflater = LayoutInflater.from(ChooseDeviceActivity.this);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.bluetooth_device_item, null);
                holder = new ViewHolder();
                holder.deviceInfo = (TextView) convertView.findViewById(R.id.device_info);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.deviceInfo.setText(getItem(position));
            return convertView;
        }

        private class ViewHolder {
            TextView deviceInfo;
        }
    }

    // =========================================== Search Bluetooth Device Handler ====================================

    /**
     * Search Bluetooth Device Handler
     */
    private class SearchHandler extends HHandler {

        public SearchHandler(Activity activity) {
            super(activity);
            TAG = SearchHandler.class.getSimpleName();
        }

        /**
         * BEGIN PREPARING
         *
         * @param msg message
         */
        @Override
        public void onBeginPreparing(Message msg) {

        }

        /**
         * FOUND DEVICE
         *
         * @param msg message
         */
        @Override
        @SuppressWarnings("unchecked")
        public void onFoundDevice(Message msg) {
            assert ((Map<String, BluetoothDevice>) msg.obj) != null;
            Set<String> strings = ((Map<String, BluetoothDevice>) msg.obj).keySet();
            String[] devices = strings.toArray(new String[strings.size()]);
            adapter = new DeviceAdapter(devices);
            ChooseDeviceActivity.this.listView.setAdapter(adapter);
            ChooseDeviceActivity.this.listView.setVisibility(View.VISIBLE);
            ChooseDeviceActivity.this.progressBar.setVisibility(View.INVISIBLE);
            ChooseDeviceActivity.this.researchDevices.setEnabled(true);
        }

        /**
         * KILL BLUETOOTH
         *
         * @param msg message
         */
        @Override
        public void onKillBluetooth(Message msg) {
            super.onKillBluetooth(msg);
            researchDevices.setEnabled(true);
        }

        /**
         * PREPARE SUCCESSFUL
         *
         * @param msg message
         */
        @Override
        public void onPrepared(Message msg) {
            Toast.makeText(activity, activity.getResources()
                    .getString(R.string.success_connect), Toast.LENGTH_SHORT)
                    .show();
            ChooseDeviceActivity.this.startActivity(new Intent(ChooseDeviceActivity.this, NavigationTabActivity.class));
        }

        /**
         * PREPARE FAILED
         *
         * @param msg message
         */
        @Override
        public void onPrepError(Message msg) {
            String errorMessage = (null == msg.obj) ?
                    activity.getResources().getString(R.string.failed_connect) :
                    msg.obj.toString();
            Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show();
            researchDevices.setEnabled(true);
        }

        /**
         * TALK RECEIVE
         *
         * @param msg message
         */
        @Override
        public void onTalkReceive(Message msg) {

        }

        /**
         * HANDLER CHANGED
         */
        @Override
        public void onHandlerChanged(Message msg) {
            researchDevices.setEnabled(true);
        }
    }

}