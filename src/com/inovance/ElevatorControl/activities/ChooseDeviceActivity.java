package com.inovance.elevatorcontrol.activities;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.inovance.bluetoothtool.BluetoothTool;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.handlers.UnlockHandler;

import java.util.Map;
import java.util.Set;

import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Views;

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

    private SharedPreferences preferences;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setContentView(R.layout.activity_choose_device);
        Views.inject(this);
        researchDevices.setEnabled(false);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String devName = adapter.getItem(position);
                BluetoothTool bluetoothSocket = BluetoothTool.getInstance();
            }
        });
        searchHandler = new SearchHandler(ChooseDeviceActivity.this);
        researchDevices();
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    /**
     * 搜索蓝牙设备
     */
    private void researchDevices() {
        if (!BluetoothTool.getInstance().isConnected()) {
            BluetoothTool.getInstance()
                    .setEventHandler(searchHandler)
                    .search();
        }
    }

    @OnClick(R.id.research_devices)
    void researchButtonClick() {
        ChooseDeviceActivity.this.listView.setVisibility(View.INVISIBLE);
        ChooseDeviceActivity.this.progressBar.setVisibility(View.VISIBLE);
        researchDevices();
    }

    // ================================================== SpecialDevice Adapter ==============================================

    /**
     * SpecialDevice Adapter
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

    // =========================================== Search Bluetooth SpecialDevice Handler ====================================

    /**
     * Search Bluetooth SpecialDevice Handler
     */
    private class SearchHandler extends UnlockHandler {

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
        public void onBeginDiscovering(Message msg) {

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
        public void onConnected(Message msg) {
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
        public void onConnectFailed(Message msg) {
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