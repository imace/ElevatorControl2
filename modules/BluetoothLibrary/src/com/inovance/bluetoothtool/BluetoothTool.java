package com.inovance.bluetoothtool;

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

import com.google.common.primitives.Bytes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
     * CRC 校验值
     */
    public int crcValue = CRCValueNone;

    public static final int CRCValueNone = -1;

    /**
     * 蓝牙设备搜索 Handler
     */
    private BluetoothHandler eventHandler;

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
     * 读取超时时间
     */
    private static final int ReadTimeout = 50;

    public boolean isLocked() {
        return isLocked;
    }

    /**
     * 设备是否被锁定
     */
    private boolean isLocked;

    public void setUnlocking(boolean unlocking) {
        this.unlocking = unlocking;
    }

    public void setUnlocked(){
        this.unlocking = false;
        this.isLocked = false;
    }

    /**
     * 保持解锁状态
     */
    private boolean unlocking;

    /**
     * Init with ApplicationContext
     *
     * @param Context context
     */
    public void init(Context Context) {
        instance.context = Context;
        instance.hasSelectDeviceType = false;
        instance.isLocked = false;
        instance.unlocking = false;
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
                if (eventHandler != null) {
                    eventHandler.sendEmptyMessage(BluetoothState.onResetBluetooth);
                }
            }
        }
        if (null != broadcastReceiver) {
            try {
                context.unregisterReceiver(broadcastReceiver);
                broadcastReceiver = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        broadcastReceiver = null;
        currentState = BluetoothState.DISCONNECTED;
    }

    /**
     * 关闭现有连接
     */
    private void socketClose() {
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
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
    private void write(final BluetoothTalk communication) {
        if (!abortTalking) {
            Message msgError = new Message();
            msgError.what = BluetoothState.onTalkError;
            try {
                if (communication == null || bluetoothSocket == null) {
                    msgError.obj = "Communication failed";
                    if (handler != null) {
                        handler.sendMessage(msgError);
                    }
                }
                if (handler != null) {
                    handler.sendEmptyMessage(BluetoothState.onBeforeTalkSend);
                }
                communication.beforeSend();
                byte[] sendBuffer = communication.getSendBuffer();
                if (!(null != sendBuffer && sendBuffer.length > 0)) {
                    msgError.obj = "Null data!";
                    if (null != handler)
                        handler.sendMessage(msgError);
                }
                if (BuildConfig.DEBUG) {
                    Log.v(TAG, "SEND: " + SerialUtility.byte2HexStr(sendBuffer));
                }
                OutputStream outputStream = bluetoothSocket.getOutputStream();

                outputStream.write(sendBuffer);
                outputStream.flush();
                if (null != handler) {
                    Message mg = new Message();
                    mg.what = BluetoothState.onAfterTalkSend;
                    mg.obj = sendBuffer;
                    handler.sendMessage(mg);
                }
                communication.afterSend();
                String valueString = SerialUtility.byte2HexStr(sendBuffer);
                int expectLength;
                if (valueString.substring(0, 4).equalsIgnoreCase("0106")) {
                    expectLength = 8;
                } else {
                    expectLength = SerialUtility.getIntFromBytes(sendBuffer) * 2 + 6;
                }
                int timeout = 0;
                List<Byte> buffer = new ArrayList<Byte>();
                InputStream inputStream = bluetoothSocket.getInputStream();
                while (true) {
                    if (abortTalking) {
                        break;
                    }
                    byte[] data = new byte[64];
                    int length = inputStream.read(data);
                    if (length > 0) {
                        byte[] temp = Arrays.copyOfRange(data, 0, length);
                        buffer.addAll(Bytes.asList(temp));
                    }
                    byte[] result = Bytes.toArray(buffer);
                    String value = SerialUtility.byte2HexStr(result);
                    if (value.contains("8001") && result.length == 8) {
                        communication.setReceivedBuffer(result);
                        communication.afterReceive();
                        // 设备锁定
                        if (value.contains("80010007")) {
                            isLocked = true;
                            Message message = new Message();
                            message.what = BluetoothState.onDeviceLocked;
                            message.obj = communication.onParse();
                            if (handler != null) {
                                handler.sendMessage(message);
                                if (BuildConfig.DEBUG) {
                                    Log.v(TAG, "RECEIVE: " + SerialUtility.byte2HexStr(result));
                                }
                            }
                        } else {
                            Message message = new Message();
                            message.what = BluetoothState.onTalkReceive;
                            message.obj = communication.onParse();
                            if (handler != null) {
                                handler.sendMessage(message);
                                if (BuildConfig.DEBUG) {
                                    Log.v(TAG, "RECEIVE: " + SerialUtility.byte2HexStr(result));
                                }
                            }
                        }
                        break;
                    }
                    if (result.length == expectLength) {
                        communication.setReceivedBuffer(result);
                        communication.afterReceive();
                        Message mg = new Message();
                        mg.what = BluetoothState.onTalkReceive;
                        mg.obj = communication.onParse();
                        if (handler != null) {
                            handler.sendMessage(mg);
                            if (BuildConfig.DEBUG) {
                                Log.v(TAG, "RECEIVE: " + SerialUtility.byte2HexStr(result));
                            }
                        }
                        break;
                    }
                    timeout++;
                    if (timeout >= ReadTimeout) {
                        // 蓝牙连接异常
                        if (currentState != BluetoothState.Exception) {
                            currentState = BluetoothState.Exception;
                            if (eventHandler != null) {
                                eventHandler.sendEmptyMessage(BluetoothState.onBluetoothConnectException);
                            }
                            if (handler != null) {
                                handler.sendEmptyMessage(BluetoothState.onBluetoothConnectException);
                            }
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
            if (eventHandler != null) {
                eventHandler.sendEmptyMessage(BluetoothState.onDeviceChanged);
            }
        }
        if (eventHandler != null) {
            eventHandler.sendEmptyMessage(BluetoothState.onWillConnect);
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
                if (eventHandler != null) {
                    eventHandler.sendEmptyMessage(BluetoothState.onConnectFailed);
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
        stopDiscovery();
        currentState = BluetoothState.CONNECTING;
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_OTHER_DEVICE);
            // Lenovo a850 connect bluetooth device async.
            bluetoothSocket.connect();
            if (bluetoothSocket.isConnected()) {
                connectedDevice = device;
                currentState = BluetoothState.CONNECTED;
                if (eventHandler != null) {
                    eventHandler.sendEmptyMessage(BluetoothState.onConnected);
                }
            }
        } catch (IOException e) {
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                try {
                    bluetoothSocket.close();
                    bluetoothSocket = null;
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
            currentState = BluetoothState.CONNECT_FAILED;
            if (eventHandler != null) {
                eventHandler.sendEmptyMessage(BluetoothState.onConnectFailed);
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
        if (eventHandler != null) {
            eventHandler.sendEmptyMessage(BluetoothState.onBeginDiscovering);
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
            if (eventHandler != null) {
                eventHandler.sendEmptyMessage(BluetoothState.onDiscoveryFinished);
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
                        if (eventHandler != null) {
                            Message msg = new Message();
                            msg.what = BluetoothState.onFoundDevice;
                            DevicesHolder devices = new DevicesHolder();
                            devices.setDevices(foundedDeviceList);
                            msg.obj = devices;
                            eventHandler.sendMessage(msg);
                        }
                    }
                } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        buildConnection(device);
                    }
                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) {
                    currentState = BluetoothState.DISCONNECTED;
                    if (eventHandler != null) {
                        eventHandler.sendEmptyMessage(BluetoothState.onDisconnected);
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                    if (eventHandler != null) {
                        eventHandler.sendEmptyMessage(BluetoothState.onDiscoveryFinished);
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
     * @return bluetoothtool Instance
     */
    public BluetoothTool search() {
        currentState = BluetoothState.WILL_DISCOVERING;
        pool.execute(this);
        return this;
    }

    /**
     * 蓝牙搜索 Handler
     *
     * @param handler BluetoothHandler
     * @return bluetoothtool
     */
    public BluetoothTool setEventHandler(BluetoothHandler handler) {
        this.abortTalking = true;
        this.eventHandler = handler;
        interrupt();
        return this;
    }

    /**
     * 当转换handler的时候应该终止串口通信
     *
     * @param handler BluetoothHandler
     * @return bluetoothtool
     */
    public BluetoothTool setHandler(BluetoothHandler handler) {
        if (!unlocking && !isLocked) {
            if (BuildConfig.DEBUG){
                Log.v(TAG, "Set handler");
            }
            Message msg = new Message();
            msg.what = BluetoothState.onHandlerChanged;
            msg.obj = this.handler;
            if (null != this.handler) {
                this.handler.sendMessage(msg);
            }
            this.abortTalking = true;
            this.handler = handler;
            interrupt();
        }
        return this;
    }

    /**
     * 当设置communication的时候应该允许串口通信
     *
     * @param talk BluetoothTalk[]
     * @return BluetoothTool
     */
    public BluetoothTool setCommunications(final BluetoothTalk[] talk) {
        if (!unlocking && !isLocked) {
            this.abortTalking = false;
            this.communications = talk;
        }
        return this;
    }

    /**
     * 发送蓝牙指令
     *
     * @return BluetoothTool Instance
     */
    public BluetoothTool startTask() {
        if (!unlocking && !isLocked) {
            abortTalking = false;
            pool.execute(this);
        }
        return this;
    }

    /**
     * 解锁设备
     *
     * @param handler BluetoothHandler
     * @param talk    BluetoothTalk[]
     * @return BluetoothTool
     */
    public BluetoothTool unlock(BluetoothHandler handler, BluetoothTalk[] talk) {
        this.unlocking = true;

        this.abortTalking = true;
        this.handler = handler;
        interrupt();

        this.abortTalking = false;
        this.communications = talk;

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
                if (communications == null) {
                    return;
                }
                if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                    return;
                }
                if (handler != null) {
                    handler.sendEmptyMessage(BluetoothState.onMultiTalkBegin);
                }
                if (communications != null) {
                    // =========================== Lenovo a850 not work =================== //
                    try {
                        InputStream inputStream = bluetoothSocket.getInputStream();
                        while (true) {
                            int available = inputStream.available();
                            if (available > 0) {
                                byte[] temp = new byte[1024];
                                inputStream.read(temp);
                            } else {
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        abortTalking = true;
                        if (eventHandler != null) {
                            eventHandler.sendEmptyMessage(BluetoothState.onBluetoothConnectException);
                        }
                        if (handler != null) {
                            handler.sendEmptyMessage(BluetoothState.onBluetoothConnectException);
                        }
                    }
                    // =========================== Lenovo a850 not work =================== //
                    for (BluetoothTalk content : communications) {
                        if (abortTalking) {
                            break;
                        }
                        write(content);
                    }
                }
                this.abortTalking = true;
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
     * 暂停线程等待上一次通信结束
     *
     * @return bluetoothtool Instance
     */
    private BluetoothTool interrupt() {
        try {
            abortTalking = true;
            pool.awaitTermination(200, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            abortTalking = false;
        }
        return this;
    }

}
