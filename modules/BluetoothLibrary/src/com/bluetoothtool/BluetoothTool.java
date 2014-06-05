package com.bluetoothtool;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.kio.bluetooth.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
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
     * 读取电梯运行状态的通信内容
     */
    private BluetoothTalk[] getRunningStatusTalk;

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
     * 首页状态值内存区域
     */
    private Hashtable<String, byte[]> homeStatusValueSet = new Hashtable<String, byte[]>();

    /**
     * 当前故障值内存区域
     */
    private Hashtable<String, byte[]> currentTroubleValueSet = new Hashtable<String, byte[]>();

    /**
     * 当前通信内容类型
     */
    private int currentTalkType = BluetoothTalk.NORMAL_TALK;

    /**
     * 当前故障
     */
    private static final String getCurrentTroubleCode = "8000";

    /**
     * 运行速度
     * 故障信息
     * 状态字功能
     * 当前楼层
     * 系统状态
     */
    private static final String[] getHomeStatusCode = new String[]{"1010", "8000", "3000", "1018", "101D"};

    /**
     * 状态字命令
     */
    private static final String statusWordCode = "3000";

    /**
     * 是否已经读取到电梯运行状态
     */
    private boolean hasGetRunningStatus = false;

    /**
     * 设置当前的实例 Activity Context
     *
     * @param activity activity
     * @return BluetoothTool Instance
     */
    public static BluetoothTool getInstance(Activity activity) {
        if (null == instance.activity) {
            instance.activity = activity;
            instance.hasSelectDeviceType = false;
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
                activity.unregisterReceiver(broadcastReceiver);
                broadcastReceiver = null;
            } catch (Exception e) {
                Log.d(TAG, "activity failed unregistered...");
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
                    if (currentTalkType == BluetoothTalk.NORMAL_TALK) {
                        communication.setReceivedBuffer(readBuffer);
                        communication.afterReceive();
                        Message mg = new Message();
                        mg.what = BluetoothState.onTalkReceive;
                        mg.obj = communication.onParse();
                        if (handler != null) {
                            handler.sendMessage(mg);
                        }
                    }
                    if (currentTalkType == BluetoothTalk.HOME_STATUS_TALK) {
                        String sendHexString = SerialUtility.byte2HexStr(communication.getSendBuffer());
                        for (String code : getHomeStatusCode) {
                            if (sendHexString.contains(code)) {
                                homeStatusValueSet.put(code, readBuffer);
                            }
                        }
                    }
                    if (currentTalkType == BluetoothTalk.CURRENT_TROUBLE_TALK) {
                        String sendHexString = SerialUtility.byte2HexStr(communication.getSendBuffer());
                        if (sendHexString.contains(getCurrentTroubleCode)) {
                            currentTroubleValueSet.put(getCurrentTroubleCode, readBuffer);
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

    /**
     * 设置当前通信内容类型
     *
     * @param currentTalkType Talk Type
     */
    public void setTalkType(int currentTalkType) {
        this.currentTalkType = currentTalkType;
    }

    /**
     * 读取首页状态值
     *
     * @return HashTable
     */
    public Hashtable<String, byte[]> getHomeStatusValueSet() {
        return homeStatusValueSet;
    }

    /**
     * 读取当前故障值
     *
     * @return HashTable
     */
    public Hashtable<String, byte[]> getCurrentTroubleValueSet() {
        return currentTroubleValueSet;
    }

    /**
     * 连接蓝牙设备
     *
     * @param device BluetoothDevice
     */
    public void connectDevice(BluetoothDevice device) {
        stopDiscovery();
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
                errorMessage.obj = String.format(activity.getResources().getString(R.string.failed_bond),
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
        BluetoothSocket tempSocket = null;
        try {
            tempSocket = device.createRfcommSocketToServiceRecord(UUID_OTHER_DEVICE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (tempSocket != null) {
            bluetoothSocket = tempSocket;
            try {
                bluetoothSocket.connect();
            } catch (IOException e) {
                try {
                    bluetoothSocket.close();
                    currentState = BluetoothState.CONNECT_FAILED;
                    if (searchHandler != null)
                        searchHandler.sendEmptyMessage(BluetoothState.onConnectFailed);
                } catch (IOException e1) {
                    Log.v(TAG, "Connect Failed.");
                }
            }
            if (bluetoothSocket.isConnected()) {
                currentState = BluetoothState.CONNECTED;
                connectedDevice = device;
                if (searchHandler != null)
                    searchHandler.sendEmptyMessage(BluetoothState.onConnected);
            } else {
                currentState = BluetoothState.CONNECT_FAILED;
                if (searchHandler != null)
                    searchHandler.sendEmptyMessage(BluetoothState.onConnectFailed);
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
            activity.registerReceiver(broadcastReceiver, getIntentFilter());
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
                if (communications != null) {
                    for (BluetoothTalk comm : communications) {
                        if (abortTalking)
                            break;
                        talk(comm);
                    }
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

    public boolean hasSelectDeviceType() {
        return hasSelectDeviceType;
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
     * 读取电梯运行状态
     * 最多读取三次
     */
    public BluetoothTool getRunningStatus(BluetoothHandler handler) {
        setHandler(handler);
        setCommunications(getRunningStatusTalk);
        hasGetRunningStatus = false;
        if (getRunningStatusTalk == null) {
            getRunningStatusTalk = new BluetoothTalk[]{
                    new BluetoothTalk() {
                        @Override
                        public void beforeSend() {
                            this.setSendBuffer(SerialUtility.crc16(SerialUtility.hexStr2Ints("0103"
                                    + statusWordCode
                                    + "0001")));
                        }

                        @Override
                        public void afterSend() {

                        }

                        @Override
                        public void beforeReceive() {

                        }

                        @Override
                        public void afterReceive() {

                        }

                        @Override
                        public Object onParse() {
                            if (getReceivedBuffer().length == 8) {
                                int status = getReceivedBuffer()[4] << 8 & 0xFF00 | getReceivedBuffer()[5] & 0xFF;
                                hasGetRunningStatus = true;
                                return new Integer(status);
                            }
                            return null;
                        }
                    }
            };
        }
        new CountDownTimer(2400, 800) {
            @Override
            public void onTick(long l) {
                if (!hasGetRunningStatus) {
                    if (isPrepared()) {
                        send();
                    }
                } else {
                    this.cancel();
                    this.onFinish();
                }
            }

            @Override
            public void onFinish() {

            }
        }.start();
        return this;
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
