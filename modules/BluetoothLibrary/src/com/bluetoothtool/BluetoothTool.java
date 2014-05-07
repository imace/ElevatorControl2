package com.bluetoothtool;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.kio.bluetooth.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BluetoothTool implements Runnable {

    private final String TAG = BluetoothTool.class.getSimpleName();

    /**
     * 通信 Handler
     */
    private BluetoothHandler handler;

    /**
     * 蓝牙设备搜索 Handler
     */
    private BluetoothHandler searchHandler;

    /**
     * 用来注册广播的activity
     */
    private Activity activity;

    private BluetoothAdapter bluetoothAdapter;

    private BroadcastReceiver broadcastReceiver;

    private ExecutorService pool = Executors.newFixedThreadPool(3);

    private List<BluetoothDevice> foundedDeviceList;

    /**
     * 是否终止发送
     */
    private volatile boolean abortTalking = false;

    private BluetoothTalk[] communications;

    /**
     * 蓝牙 Socket
     */
    public BluetoothSocket bluetoothSocket;

    private static BluetoothTool instance = new BluetoothTool();

    public BluetoothDevice connectedDevice;

    private InputStream bluetoothInputStream;

    /**
     * 当前蓝牙状态
     */
    public int currentState;

    /**
     * 设置当前的实例 Activity Context
     *
     * @param activity activity
     * @return BluetoothTool Instance
     */
    public static BluetoothTool getInstance(Activity activity) {
        if (null == instance.activity) {
            instance.activity = activity;
        }
        return instance;
    }

    /**
     * 关闭连接,释放对象
     */
    public void kill() {
        interrupt();
        reset(true);
        if (null != handler) {
            handler.sendEmptyMessage(BluetoothState.onKillBluetooth);
        }
    }

    /**
     * 重置蓝牙
     *
     * @param closeConnected Close Connect Device
     */
    public void reset(boolean closeConnected) {
        abortTalking = false;
        currentState = BluetoothState.NOT_CONNECT;
        stopDiscovery();
        if (closeConnected) {
            socketClose();
            if (null != foundedDeviceList) {
                foundedDeviceList.clear();
                if (searchHandler != null) {
                    searchHandler.sendEmptyMessage(BluetoothState.onResetBluetooth);
                }
            }
        }
        if (null != broadcastReceiver) {
            try {
                activity.unregisterReceiver(broadcastReceiver);
                broadcastReceiver = null;
            } catch (Exception e) {
                Log.d(TAG, "activity failed unregistered...");
            }
        }
        broadcastReceiver = null;
        currentState = BluetoothState.NOT_CONNECT;
    }

    /**
     * 关闭现有连接
     */
    private void socketClose() {
        if (null != bluetoothSocket) {
            if (bluetoothSocket.isConnected()) {
                try {
                    bluetoothSocket.getInputStream().close();
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage());
                }
                try {
                    bluetoothSocket.getOutputStream().close();
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage());
                }
                try {
                    bluetoothSocket.close();
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage());
                } finally {
                    bluetoothSocket = null;
                }
            }
            bluetoothInputStream = null;
            bluetoothSocket = null;
        }
        currentState = BluetoothState.NOT_CONNECT;
    }

    /**
     * 发送指令
     *
     * @param communication BluetoothTalk[]
     */
    private void talk(final BluetoothTalk communication) {
        Message msgError = new Message();
        msgError.what = BluetoothState.onTalkError;
        try {
            if (null == communication || null == bluetoothSocket || !bluetoothSocket.isConnected()) {
                final String errorMessage = "cannot send or receive! "
                        + ((communication == null) ? "communication==null" : "")
                        + ((bluetoothSocket == null) ? "bluetoothSocket" : "")
                        + ((bluetoothSocket.isConnected()) ? "connected" : "unconnected");
                msgError.obj = errorMessage;
                if (null != handler)
                    handler.sendMessage(msgError);
                Log.d(TAG, errorMessage);
            }
            if (null != handler) {
                handler.sendEmptyMessage(BluetoothState.onBeforeTalkSend);
            }
            communication.beforeSend();
            byte[] sendBuffer = communication.getSendBuffer();
            if (!(null != sendBuffer && sendBuffer.length > 0)) {
                msgError.obj = "Null data!";
                if (null != handler)
                    handler.sendMessage(msgError);
            }
            bluetoothSocket.getOutputStream().write(sendBuffer);
            bluetoothSocket.getOutputStream().flush();
            if (null != handler) {
                Message mg = new Message();
                mg.what = BluetoothState.onAfterTalkSend;
                mg.obj = sendBuffer;
                handler.sendMessage(mg);
            }
            communication.afterSend();
            while (true) {
                if (abortTalking) {
                    break;
                }
                if (bluetoothInputStream == null) {
                    break;
                }
                int length = bluetoothInputStream.available();
                if (length >= 8) {
                    byte[] readBuffer = new byte[length];
                    bluetoothInputStream.read(readBuffer);
                    communication.setReceivedBuffer(readBuffer);
                    communication.afterReceive();
                    Message mg = new Message();
                    mg.what = BluetoothState.onTalkReceive;
                    mg.obj = communication.onParse();
                    if (handler != null) {
                        handler.sendMessage(mg);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            msgError.obj = e.getMessage();
            if (null != handler) {
                handler.sendMessage(msgError);
            }
        }
    }

    /**
     * 连接蓝牙设备
     *
     * @param device BluetoothDevice
     */
    public void connectDevice(BluetoothDevice device) {
        stopDiscovery();
        boolean exist = false;
        if (bluetoothAdapter == null){
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        for (BluetoothDevice item : bluetoothAdapter.getBondedDevices()) {
            if (item.equals(device)) {
                exist = true;
                if (buildConnection(device)) {
                    currentState = BluetoothState.CONNECTED;
                    if (searchHandler != null)
                        searchHandler.sendEmptyMessage(BluetoothState.onConnected);
                } else {
                    currentState = BluetoothState.CONNECT_FAILED;
                    if (searchHandler != null)
                        searchHandler.sendEmptyMessage(BluetoothState.onConnectFailed);
                }
            }
        }
        if (!exist) {
            currentState = BluetoothState.PAIRING;
            try {
                BluetoothDevice.class.getMethod("createBond").invoke(device);
            } catch (Exception e) {
                Message msgBond = new Message();
                msgBond.what = BluetoothState.onConnectFailed;
                msgBond.obj = String.format(activity.getResources().getString(R.string.failed_bond),
                        device.getName() + "(" + device.getAddress() + ")");
                if (searchHandler != null) {
                    searchHandler.sendMessage(msgBond);
                }
            }
        }
    }

    /**
     * 搜索蓝牙设备
     */
    private void restartSearch() {
        if (bluetoothAdapter == null){
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (!bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.startDiscovery();
            currentState = BluetoothState.DISCOVERING;
        }
        if (null != broadcastReceiver) {
            try {
                activity.unregisterReceiver(broadcastReceiver);
                broadcastReceiver = null;
            } catch (Exception e) {
                Log.d(TAG, "activity failed unregistered...");
            }
        }
        broadcastReceiver = getBroadcastReceiver();
        activity.registerReceiver(broadcastReceiver, getIntentFilter());
        if (searchHandler != null) {
            searchHandler.sendEmptyMessage(BluetoothState.onBeginDiscovering);
        }
    }

    /**
     * 结束搜索
     */
    private void stopDiscovery() {
        if (bluetoothAdapter == null){
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            if (searchHandler != null) {
                searchHandler.sendEmptyMessage(BluetoothState.onDiscoveryFinished);
            }
        }
    }

    /**
     * 尝试建立连接
     *
     * @param device BluetoothDevice
     * @return 是否建立了连接
     */
    private boolean buildConnection(BluetoothDevice device) {
        socketClose();
        currentState = BluetoothState.CONNECTING;
        boolean buildSuccessful = false;
        try {
            if (!buildSuccessful) {
                try {
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID
                            .fromString("00001101-0000-1000-8000-00805F9B34FB"));
                    bluetoothSocket.connect();
                    if (bluetoothSocket.isConnected()) {
                        connectedDevice = device;
                        buildSuccessful = true;
                    }
                } catch (IOException e) {
                    connectedDevice = null;
                    buildSuccessful = false;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error create connect : " + e.getMessage());
        }
        if (null != broadcastReceiver) {
            try {
                activity.unregisterReceiver(broadcastReceiver);
            } catch (Exception e) {
                Log.d(TAG, "Activity failed unregistered...");
            }
        }
        if (buildSuccessful) {
            try {
                bluetoothInputStream = bluetoothSocket.getInputStream();
            } catch (IOException e) {
                Log.v(TAG, "Create input stream failed.");
            }
        }
        return buildSuccessful;
    }

    /**
     * 注册BroadcastReceiver
     *
     * @return BroadcastReceiver
     */
    private BroadcastReceiver getBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (currentState == BluetoothState.DISCOVERING) {
                        if (foundedDeviceList == null) {
                            foundedDeviceList = new ArrayList<BluetoothDevice>();
                        }
                        foundedDeviceList.add(device);
                        if (searchHandler != null) {
                            Message msg = new Message();
                            msg.what = BluetoothState.onFoundDevice;
                            DevicesHolder devices = new DevicesHolder();
                            devices.setDevices(foundedDeviceList);
                            msg.obj = devices;
                            searchHandler.sendMessage(msg);
                        }
                    }
                } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {// 改变中的状态
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    switch (device.getBondState()) {
                        case BluetoothDevice.BOND_BONDED: {
                            stopDiscovery();
                            if (buildConnection(device)) {
                                currentState = BluetoothState.CONNECTED;
                                if (searchHandler != null)
                                    searchHandler.sendEmptyMessage(BluetoothState.onConnected);
                            } else {
                                currentState = BluetoothState.CONNECT_FAILED;
                                if (searchHandler != null)
                                    searchHandler.sendEmptyMessage(BluetoothState.onConnectFailed);
                            }
                        }
                        break;
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                    if (searchHandler != null) {
                        searchHandler.sendEmptyMessage(BluetoothState.onDiscoveryFinished);
                    }
                }
            }
        };
    }

    private IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        return filter;
    }


    /**
     * 搜索蓝牙设备
     *
     * @return BluetoothTool Instance
     */
    public BluetoothTool search() {
        currentState = BluetoothState.WILL_DISCOVERING;
        interrupt();
        pool.execute(this);
        return this;
    }

    /**
     * 发送蓝牙指令
     *
     * @return BluetoothTool Instance
     */
    public BluetoothTool send() {
        interrupt();
        pool.execute(this);
        return this;
    }

    @Override
    public void run() {
        switch (currentState) {
            case BluetoothState.WILL_DISCOVERING:
                restartSearch();
                break;
            case BluetoothState.CONNECTED: {
                if (null == communications)
                    return;
                if (null != bluetoothAdapter && bluetoothAdapter.isDiscovering())
                    return;
                if (null != handler)
                    handler.sendEmptyMessage(BluetoothState.onMultiTalkBegin);
                for (BluetoothTalk comm : communications) {
                    if (abortTalking)
                        break;
                    talk(comm);
                }
                if (null != handler)
                    handler.sendEmptyMessage(BluetoothState.onMultiTalkEnd);
                communications = null;
            }
            break;
        }
    }

    /**
     * 蓝牙连接是否已建立
     *
     * @return Connect State
     */
    public boolean isConnected() {
        return currentState == BluetoothState.CONNECTED;
    }

    public Handler getHandler() {
        return handler;
    }

    /**
     * 当转换handler的时候应该终止串口通信
     *
     * @param handler BluetoothHandler
     * @return BluetoothTool
     */
    public BluetoothTool setHandler(BluetoothHandler handler) {
        Message msg = new Message();
        msg.what = BluetoothState.onHandlerChanged;
        msg.obj = this.handler;
        if (null != this.handler)
            this.handler.sendMessage(msg);
        this.abortTalking = true;
        this.handler = handler;
        interrupt();
        return this;
    }

    /**
     * 蓝牙搜索 Handler
     *
     * @param handler BluetoothHandler
     * @return BluetoothTool
     */
    public BluetoothTool setSearchHandler(BluetoothHandler handler) {
        this.abortTalking = true;
        this.searchHandler = handler;
        interrupt();
        return this;
    }

    public final BluetoothTalk[] getCommunications() {
        return communications;
    }

    /**
     * 当设置communication的时候应该允许串口通信
     *
     * @param talk BluetoothTalk[]
     * @return BluetoothTool
     */
    public BluetoothTool setCommunications(final BluetoothTalk[] talk) {
        this.abortTalking = false;
        this.communications = talk;
        return this;
    }

    private BluetoothTool interrupt() {
        try {
            abortTalking = true;
            pool.awaitTermination(120, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            abortTalking = false;
        }
        return this;
    }

}
