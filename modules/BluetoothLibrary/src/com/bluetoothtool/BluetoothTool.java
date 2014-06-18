package com.bluetoothtool;

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

    public int crcValue = -1;

    /**
     * 蓝牙设备搜索 Handler
     */
    private BluetoothHandler searchHandler;

    /**
     * 用来注册广播的activity
     */
    private Context context;

    private BluetoothAdapter bluetoothAdapter;

    /**
     * 注册蓝牙事件广播
     */
    private BroadcastReceiver broadcastReceiver;

    private ExecutorService pool = Executors.newFixedThreadPool(4);

    /**
     * 已搜索到的蓝牙设备列表
     */
    private List<BluetoothDevice> foundedDeviceList;

    /**
     * 是否终止发送
     */
    private volatile boolean abortTalking = false;

    /**
     * 通信内容
     */
    private BluetoothTalk[] communications;

    /**
     * 蓝牙 Socket
     */
    public BluetoothSocket bluetoothSocket;

    private static BluetoothTool instance = new BluetoothTool();

    /**
     * 已连接的蓝牙设备
     */
    public BluetoothDevice connectedDevice;

    /**
     * RfcommSocket UUID
     */
    private static final UUID UUID_OTHER_DEVICE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * 是否已选择设备类型
     */
    private boolean hasSelectDeviceType = false;

    /**
     * 当前蓝牙状态
     */
    public int currentState;

    /**
     * 设置当前的实例 Activity Context
     *
     * @param activity context
     */

    public void init(Context Context) {
        instance.context = Context;
        instance.hasSelectDeviceType = false;
    }

    public static BluetoothTool getInstance() {
        return instance;
    }

    /**
     * 关闭连接,释放对象
     */
    public void kill() {
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
        abortTalking = true;
        currentState = BluetoothState.DISCONNECTED;
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
                context.unregisterReceiver(broadcastReceiver);
                broadcastReceiver = null;
            } catch (Exception e) {
                Log.d(TAG, "context failed unregistered...");
            }
        }
        broadcastReceiver = null;
        currentState = BluetoothState.DISCONNECTED;
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
            bluetoothSocket = null;
        }
        currentState = BluetoothState.DISCONNECTED;
    }

    /**
     * 发送指令
     *
     * @param communication BluetoothTalk[]
     */
    private void talk(final BluetoothTalk communication) {
        if (!abortTalking) {
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
                    if (bluetoothSocket.getInputStream() == null) {
                        break;
                    }
                    int length = bluetoothSocket.getInputStream().available();
                    if (length >= 8) {
                        byte[] readBuffer = new byte[length];
                        bluetoothSocket.getInputStream().read(readBuffer);
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
    }

    /**
     * 连接蓝牙设备
     *
     * @param device BluetoothDevice
     */
    public void connectDevice(BluetoothDevice device) {
        stopDiscovery();
        if (connectedDevice != null && connectedDevice != device) {
            if (searchHandler != null) {
                searchHandler.sendEmptyMessage(BluetoothState.onDeviceChanged);
            }
        }
        if (searchHandler != null) {
            searchHandler.sendEmptyMessage(BluetoothState.onWillConnect);
        }
        boolean exist = false;
        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        for (BluetoothDevice item : bluetoothAdapter.getBondedDevices()) {
            if (item.equals(device)) {
                exist = true;
                buildConnection(device);
            }
        }
        if (!exist) {
            currentState = BluetoothState.PAIRING;
            try {
                BluetoothDevice.class.getMethod("createBond").invoke(device);
            } catch (Exception e) {
                Message errorMessage = new Message();
                errorMessage.what = BluetoothState.onConnectFailed;
                errorMessage.obj = String.format(context.getResources().getString(R.string.failed_bond),
                        device.getName() + "(" + device.getAddress() + ")");
                if (searchHandler != null) {
                    searchHandler.sendMessage(errorMessage);
                }
            }
        }
    }

    /**
     * 尝试建立连接
     *
     * @param device BluetoothDevice
     */
    private void buildConnection(BluetoothDevice device) {
        socketClose();
        currentState = BluetoothState.CONNECTING;
        BluetoothSocket tempSocket;
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            try {
                bluetoothSocket.close();
                tempSocket = device.createRfcommSocketToServiceRecord(UUID_OTHER_DEVICE);
                tempSocket.connect();
                if (tempSocket.isConnected()) {
                    bluetoothSocket = tempSocket;
                    connectedDevice = device;
                    currentState = BluetoothState.CONNECTED;
                    if (searchHandler != null) {
                        searchHandler.sendEmptyMessage(BluetoothState.onConnected);
                    } else {
                        currentState = BluetoothState.CONNECT_FAILED;
                        if (searchHandler != null) {
                            searchHandler.sendEmptyMessage(BluetoothState.onConnectFailed);
                        }
                    }
                }
            } catch (IOException e) {
                currentState = BluetoothState.CONNECT_FAILED;
                if (searchHandler != null) {
                    searchHandler.sendEmptyMessage(BluetoothState.onConnectFailed);
                }
            }
        } else {
            try {
                tempSocket = device.createRfcommSocketToServiceRecord(UUID_OTHER_DEVICE);
                tempSocket.connect();
                if (tempSocket.isConnected()) {
                    bluetoothSocket = tempSocket;
                    connectedDevice = device;
                    currentState = BluetoothState.CONNECTED;
                    if (searchHandler != null) {
                        searchHandler.sendEmptyMessage(BluetoothState.onConnected);
                    } else {
                        currentState = BluetoothState.CONNECT_FAILED;
                        if (searchHandler != null) {
                            searchHandler.sendEmptyMessage(BluetoothState.onConnectFailed);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 搜索蓝牙设备
     */
    private void restartSearch() {
        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (!bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.startDiscovery();
            currentState = BluetoothState.DISCOVERING;
        }
        if (broadcastReceiver == null) {
            broadcastReceiver = getBroadcastReceiver();
            context.registerReceiver(broadcastReceiver, getIntentFilter());
        }
        if (searchHandler != null) {
            searchHandler.sendEmptyMessage(BluetoothState.onBeginDiscovering);
        }
    }

    /**
     * 停止搜索
     */
    private void stopDiscovery() {
        if (bluetoothAdapter == null) {
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
                } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    switch (device.getBondState()) {
                        case BluetoothDevice.BOND_BONDED: {
                            buildConnection(device);
                        }
                        break;
                    }
                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) {
                    currentState = BluetoothState.DISCONNECTED;
                    if (searchHandler != null) {
                        searchHandler.sendEmptyMessage(BluetoothState.onDisconnected);
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
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
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
        pool.execute(this);
        return this;
    }

    /**
     * 发送蓝牙指令
     *
     * @return BluetoothTool Instance
     */
    public BluetoothTool send() {
        while (true) {
            try {
                if (bluetoothSocket != null) {
                    int length = bluetoothSocket.getInputStream().available();
                    Log.v(TAG, String.valueOf(length));
                    if (length <= 0) {
                        abortTalking = false;
                        pool.execute(this);
                        break;
                    } else {
                        byte[] temp = new byte[1024];
                        bluetoothSocket.getInputStream().read(temp);
                    }
                } else {
                    break;
                }
            } catch (IOException e) {
                Log.v(TAG, e.getMessage());
                e.printStackTrace();
                break;
            }
        }
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
                if (communications != null) {
                    for (BluetoothTalk content : communications) {
                        if (abortTalking)
                            break;
                        talk(content);
                    }
                }
                if (null != handler) {
                    handler.sendEmptyMessage(BluetoothState.onMultiTalkEnd);
                }
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

    /**
     * 蓝牙连接和设备选择是否已就绪
     *
     * @return boolean
     */
    public boolean isPrepared() {
        return currentState == BluetoothState.CONNECTED && hasSelectDeviceType;
    }

    public void setHasSelectDeviceType(boolean hasSelectDeviceType) {
        this.hasSelectDeviceType = hasSelectDeviceType;
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
        if (null != this.handler) {
            this.handler.sendMessage(msg);
        }
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

    /**
     * 暂停线程等待上一次通信结束
     *
     * @return BluetoothTool Instance
     */
    private BluetoothTool interrupt() {
        try {
            abortTalking = true;
            pool.awaitTermination(100, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            abortTalking = false;
        }
        return this;
    }

}
