package com.hbluetooth;

import android.annotation.SuppressLint;
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SuppressLint("NewApi")
public class HBluetooth implements Runnable {

    private final String TAG = HBluetooth.class.getSimpleName();

    private final int SOCKET_TIMEOUT = 100;

    private final int MAX_READ_BUFFER = 1024;

    private int[] arrPorts = {// 可尝试的用来打开蓝牙socket的端口
            1
    };
    private HHandler handler = null; // 由主线程生成并传入
    private Activity activity = null;// 用来注册广播的activity

    private BluetoothAdapter mAdapter = null;
    private BroadcastReceiver broadcastReceiver = null;

    // 创建一个可单线程的线程池
    private ExecutorService pool = Executors.newSingleThreadExecutor();

    // 扫描到的所有设备
    private Map<String, BluetoothDevice> stringBluetoothDeviceMap = null;

    // 准备完毕,它的set接口是开放的,只要将其设为false,就重做准备工作
    // 代替btSocket.isConnected()这个接口有的设备出现明明连接上但值是false的情况
    private boolean prepared;

    // true表示不要自动匹配开发板,而是尽量多的发现蓝牙设备
    // false表示直接根据judgment算法判断自动匹配开发板
    private boolean discoveryMode = true;

    // 中止发送多条指令状态
    private volatile boolean abortTalking = false;

    // 重写的judge抽象方法来判断设备，默认为null
    private HJudgeListener judgement = null;

    // 交互内容,包括send和receive
    private HCommunication[] communications = null;

    // socket
    public BluetoothSocket btSocket;

    private static HBluetooth instance = new HBluetooth();

    /**
     * 不管多少次getInstance(activity) activity始终是第一次调用的时候的那个
     *
     * @param activity activity
     * @return bluetooth
     */
    public static HBluetooth getInstance(Activity activity) {
        if (null == instance.activity) {
            instance.activity = activity;
        }
        instance.arrPorts = instance
                .activity
                .getResources()
                .getIntArray(R.array.array_ports);
        return instance;
    }

    /**
     * 关闭连接,释放对象
     */
    public void kill() {
        // 强行关闭
        HInterrupt();
        reset(true);
        if (null != handler) {
            // 代表kill的消息
            handler.sendEmptyMessage(R.string.kill_bluetooth);
        }
    }

    /**
     * 停止搜索 clearPair : true 停止搜索,关闭连接,清除配对 false 停止搜索
     */
    public void reset(boolean clearPair) {

        prepared = abortTalking = false;

        mAdapter = BluetoothAdapter.getDefaultAdapter();

        judgement = null;

        // 如果在检测蓝牙设备,则中止
        stopDiscovery();

        //
        if (clearPair) {
            if (null != stringBluetoothDeviceMap) {
                try {
                    // 删除之前配对,以后必须重新配对,重新输入pin
                    stringBluetoothDeviceMap.clear();
                    if (null != handler) {
                        // 代表重置的消息
                        handler.sendEmptyMessage(R.string.rest_bluetooth);
                    }
                    Log.d(TAG, "removing cached devices...");
                } catch (UnsupportedOperationException e) {
                    Log.d(TAG, e.getMessage());
                }
            }
        }
        if (null != broadcastReceiver) {
            try {
                activity.unregisterReceiver(broadcastReceiver);
                broadcastReceiver = null;
                Log.d(TAG, "activity unregistered...");
            } catch (Exception e) {
                Log.d(TAG, "activity failed unregistered...");
            }
        }
        stringBluetoothDeviceMap = new ConcurrentHashMap<String, BluetoothDevice>();

        if (clearPair) {
            socketClose();
        }
        broadcastReceiver = null;
    }

    /**
     * 关闭现有连接
     */
    private void socketClose() {
        if (null != btSocket) {
            if (btSocket.isConnected()) {
                // 关闭
                try {
                    btSocket.getInputStream().close();
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage());
                }
                try {
                    btSocket.getOutputStream().close();
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage());
                }
                try {
                    btSocket.close();
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage());
                } finally {
                    btSocket = null;
                }
            }
            btSocket = null;
            Log.v(TAG, "bluetooth socket closed.");
        }
    }

    /**
     * 与蓝牙串口设备交互 实现基本串口IO并排除通信端错误
     *
     * @return
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("unused")
    private String talk(HCommunication communication) {
        Message msgError = new Message(); // 错误消息
        msgError.what = R.string.talk_error;
        try {
            // 没配置好,不能发送
            if (null == communication || null == btSocket || !btSocket.isConnected()) {
                final String errorMessage = "cannot send or receive! " + ((communication == null) ? "communication==null" : "") + ((btSocket == null) ? "btSocket" : "")
                        + ((btSocket.isConnected()) ? "connected" : "unconnected");
                msgError.obj = errorMessage;// 消息体
                if (null != handler)
                    handler.sendMessage(msgError);
                Log.d(TAG, errorMessage);
                return null;
            }
            if (null != handler)
                handler.sendEmptyMessage(R.string.talk_before_send);
            // 发送之前
            communication.beforeSend();
            byte[] sendBuffer = communication.getSendBuffer();// 指令
            if (!(null != sendBuffer && sendBuffer.length > 0)) {// 出错
                final String errorMessage = "no CMD or DATA!";
                msgError.obj = errorMessage;// 消息体
                if (null != handler)
                    handler.sendMessage(msgError);
                Log.d(TAG, errorMessage);
                return null;
            }
            // 发送
            btSocket.getOutputStream().write(sendBuffer);
            btSocket.getOutputStream().flush();
            if (null != handler) {// 发送之后
                Message mg = new Message();
                mg.what = R.string.talk_after_send;
                mg.obj = sendBuffer;
                handler.sendMessage(mg);
            }
            communication.afterSend();

            // 休眠适当时间等待接收完全
            Thread.sleep(SOCKET_TIMEOUT);

            communication.beforeReceive();
            byte[] readBuffer = new byte[MAX_READ_BUFFER];// 预留足够大空间
            BufferedInputStream inStream = new BufferedInputStream(btSocket.getInputStream());
            int ret = -1;
            while (inStream.available() > 0) {
                try {
                    ret = inStream.read(readBuffer);
                } catch (IOException e) {// 出错
                    msgError.obj = e.getMessage();
                    if (null != handler)
                        handler.sendMessage(msgError);
                    return null;// 接收出错直接退出
                }
            }
            communication.setReceivedBuffer(readBuffer);
            communication.afterReceive();
            // 能走到此处说明一次交互完成,则发出消息
            Message mg = new Message();
            mg.what = R.string.talk_receive;
            mg.obj = communication.onParse();
            if (null != handler)
                handler.sendMessage(mg);
            return HSerial.byte2HexStr(readBuffer);
        } catch (IOException e) {// 出错
            msgError.obj = e.getMessage();
            if (null != handler)
                handler.sendMessage(msgError);
            Log.d(TAG, e.getMessage());
            return null;
        } catch (InterruptedException e1) {// 出错
            msgError.obj = e1.getMessage();
            if (null != handler)
                handler.sendMessage(msgError);
            Thread.currentThread().interrupt();
            return null;
        } catch (NullPointerException e2) {
            msgError.obj = e2.getMessage();
            if (null != handler)
                handler.sendMessage(msgError);
            Log.d(TAG, "null pointer");
            return null;
        }
    }

    /**
     * 准备工作: 打开蓝牙->找到开发板->配对->建立连接
     */
    public void prepare() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        // 未打开蓝牙,则弹出对话框提示用户是否打开蓝牙
        if (!mAdapter.isEnabled()) {
            activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), activity.getResources().getInteger(R.integer.request_enable_bt));
        }

        if (null == judgement) {
            if (discoveryMode)
                reset(false);
            restartSearch();

        } else {
            // 系统存了已配对设备
            if (mAdapter.getBondedDevices().size() > 0) {
                boolean ped = false;// 已配对设备中存在我想要的设备
                for (BluetoothDevice dev : mAdapter.getBondedDevices()) {
                    if (isWantedDevice(dev)) { // 发现了我想要的设备
                        ped = true;
                        stopDiscovery(); // 停止搜索
                        if (buildConnection(dev)) { // 将得到btSocket
                            prepared = true;
                            if (null != handler)
                                handler.sendEmptyMessage(R.string.prepare_successful);
                        } else {
                            prepared = false;
                            if (null != handler)
                                handler.sendEmptyMessage(R.string.prepare_failed);
                        }
                        break;
                    }
                }
                if (!ped) {// 已配对设备中不存在我想要的设备
                    restartSearch();
                }
            } else {// 系统没有已配对设备,重新找
                restartSearch();
            }
        }
    }

    private void restartSearch() {
        startDiscovery();
        if (null != broadcastReceiver) {
            try {
                activity.unregisterReceiver(broadcastReceiver);
                broadcastReceiver = null;
                Log.d(TAG, "activity unregistered...");
            } catch (Exception e) {
                Log.d(TAG, "activity failed unregistered...");
            }
        }
        // 注册一个Receiver
        broadcastReceiver = getPrepareReceiver();
        activity.registerReceiver(broadcastReceiver, getPrepareFilter());
        // 开始准备
        if (null != handler)
            handler.sendEmptyMessage(R.string.begin_preparing);
    }

    /**
     * 开始搜索可用设备
     */
    private void startDiscovery() {
        Log.d(TAG, "start discovery...");
        mAdapter.startDiscovery();
    }

    /**
     * 结束搜索
     */
    private void stopDiscovery() {
        Log.d(TAG, "cancel discovery...");
        mAdapter.cancelDiscovery();
    }

    /**
     * 是否为需要的设备
     *
     * @param dev
     * @return
     */
    private boolean isWantedDevice(BluetoothDevice dev) {
        if (discoveryMode)
            return false;
        if (null == judgement)
            return false;
        return judgement.judge(dev);
    }

    /**
     * 尝试建立连接
     *
     * @param dev
     * @return 是否建立了连接
     */
    @SuppressLint("NewApi")
    private boolean buildConnection(BluetoothDevice dev) {
        socketClose();
        boolean buildSuccessful = false;
        try {
            if (!buildSuccessful) {
                try {
                    btSocket = dev.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                    btSocket.connect();
                    if (btSocket.isConnected()) {
                        buildSuccessful = true;// 只有connect()不发生错误才连接成功
                    }
                } catch (IOException e) {
                    buildSuccessful = false;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "error on tring to connet through UUID : " + e.getMessage());
        } finally {
            int len = arrPorts.length;
            while ((!buildSuccessful) && len > 0) {// 以上不成功,改用非公开的api
                len--;
                try {
                    Log.v(TAG, "try open port : " + arrPorts[len]);
                    btSocket = (BluetoothSocket) dev.getClass().getMethod("createRfcommSocket", new Class[]{
                            int.class
                    }).invoke(dev, Integer.valueOf(arrPorts[len]));
                    btSocket.connect();
                    buildSuccessful = true;// 只有connect()不发生错误才连接成功
                    break;
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "error on tring to connet through socket : " + e.getMessage());
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "error on tring to connet through socket : " + e.getMessage());
                } catch (InvocationTargetException e) {
                    Log.e(TAG, "error on tring to connet through socket : " + e.getMessage());
                } catch (NoSuchMethodException e) {
                    Log.e(TAG, "error on tring to connet through socket : " + e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "error on tring to connet through socket : " + e.getMessage());
                }
            }

        }

        if (null != broadcastReceiver) {
            try {
                activity.unregisterReceiver(broadcastReceiver);
                Log.d(TAG, "activity unregistered...");
            } catch (Exception e) {
                Log.d(TAG, "activity failed unregistered...");
            }
        }

        return buildSuccessful;
    }

    /**
     * new一个Receiver用来注册Receiver
     *
     * @return
     */
    private BroadcastReceiver getPrepareReceiver() {
        return new BroadcastReceiver() {

            /**
             * 已配对的设备,判断是否是我想要的设备, 是则停止搜索,并获取与这个设备通信的socket 只有这一个方法能建立socket连接
             * prepare才为true
             *
             * @param dev
             */
            private void onReceivePairedDevice(BluetoothDevice dev) {
                if (isWantedDevice(dev)) { // 发现了我想要的设备
                    stopDiscovery(); // 停止搜索
                    if (buildConnection(dev)) { // 将得到btSocket
                        prepared = true;
                        if (null != handler)
                            handler.sendEmptyMessage(R.string.prepare_successful);
                    } else {
                        prepared = false;
                        if (null != handler)
                            handler.sendEmptyMessage(R.string.prepare_failed);
                    }
                }
            }

            /**
             * 未配对的设备
             *
             * @param dev
             */
            private void onReceiveUnPairedDevice(BluetoothDevice dev) {
                if (isWantedDevice(dev)) { // 发现了我想要的设备
                    stopDiscovery(); // 停止搜索
                    // 只是建立配对,只弹出框来输入pin码,并未真正建立配对prepared仍为false
                    // 仍不能建立socket
                    buildPair(dev);
                }
            }

            /**
             * 调用配对
             *
             * @return 配对, 注意并不是配对是否建立
             */
            private void buildPair(BluetoothDevice dev) {
                try {
                    Boolean returnValue = (Boolean) Class.forName("android.bluetooth.BluetoothDevice").getMethod("createBond").invoke(dev);
                    Log.v(TAG, "auto bond : " + returnValue);
                } catch (Exception e) {// any exception occurs
                    Log.e(TAG, e.getMessage());
                    Message msgBond = new Message();
                    msgBond.what = R.string.prepare_failed;
                    msgBond.obj = String.format(activity.getResources().getString(R.string.failed_bond), dev.getName() + "(" + dev.getAddress() + ")");
                    if (null != handler)
                        handler.sendMessage(msgBond);
                }
            }

            /**
             * 取消配对
             *
             * @param dev
             */
            private void cancelPair(BluetoothDevice dev) {
                try {
                    Boolean returnValue = (Boolean) Class.forName("android.bluetooth.BluetoothDevice").getMethod("cancelBondProcess").invoke(dev);
                    Log.v(TAG, "cancel bond : " + returnValue);
                } catch (Exception e) {// any exception occurs
                    Log.e(TAG, e.getMessage());
                    Message msgBond = new Message();
                    msgBond.what = R.string.prepare_failed;
                    msgBond.obj = String.format(activity.getResources().getString(R.string.cancel_bond), dev.getName() + "(" + dev.getAddress() + ")");
                    if (null != handler)
                        handler.sendMessage(msgBond);
                }
            }

            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {// 搜到任意蓝牙设备
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    // 如果不是discoveryMode则不要发消息,那么这个过程就不会影响ui而是自动判断并弹出配对框
                    if (discoveryMode) {
                        String deviceLogName = device.getName() + "(" + device.getAddress() + ")";
                        stringBluetoothDeviceMap.put(deviceLogName, device);
                        if (null != handler) {
                            Message msg = new Message();
                            msg.what = R.string.found_device;
                            msg.obj = stringBluetoothDeviceMap;
                            handler.sendMessage(msg);
                        }
                        Log.v(TAG, "find device : " + deviceLogName);
                        Log.v(TAG, "map added " + deviceLogName + "(" + stringBluetoothDeviceMap.size() + ")");
                    }

                    // 该设备已配对
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        onReceivePairedDevice(device);
                    } else {// 该设备没配对
                        onReceiveUnPairedDevice(device);
                    }
                } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {// 改变中的状态
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    switch (device.getBondState()) {
                        case BluetoothDevice.BOND_BONDING:
                            Log.v(TAG, "BOND BONDING : ");
                            break;
                        case BluetoothDevice.BOND_BONDED:
                            Log.v(TAG, "BOND BONDED : ");
                            onReceivePairedDevice(device);// 连接设备
                            break;
                        case BluetoothDevice.BOND_NONE:
                            cancelPair(device);
                        default:
                            break;
                    }
                }
            }
        };
    }

    // Filter用来注册Receiver
    private IntentFilter getPrepareFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);// 用BroadcastReceiver来取得搜索结果
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return filter;
    }

    @Override
    public void run() {
        if (!isPrepared()) {
            prepare();
        } else {// 已经连接,发送指令
            if (null == communications)
                return;
            if (null != mAdapter && mAdapter.isDiscovering())
                return;
            // 多条指令开始发送
            if (null != handler)
                handler.sendEmptyMessage(R.string.multi_talk_begin);
            for (HCommunication comm : communications) {
                if (abortTalking)
                    break;
                talk(comm);
            }
            if (null != handler)
                handler.sendEmptyMessage(R.string.multi_talk_end);
            // 多条指令发送终止
            communications = null;
        }
    }

    public boolean isPrepared() {
        return prepared;
    }

    public HBluetooth setPrepared(boolean prepared) {
        this.prepared = prepared;
        return this;
    }

    public HJudgeListener getJudgement() {
        return judgement;
    }

    public HBluetooth setJudgement(HJudgeListener judgement) {
        this.judgement = judgement;
        return this;
    }

    public boolean isDiscoveryMode() {
        return discoveryMode;
    }

    /**
     * 如果discoveryMode: 则 setJudgement(null)
     *
     * @param discoveryMode
     * @return
     */
    public HBluetooth setDiscoveryMode(boolean discoveryMode) {
        this.discoveryMode = discoveryMode;
        if (discoveryMode) {
            setJudgement(null);
        }
        return this;
    }

    public Handler getHandler() {
        return handler;
    }

    /**
     * 当转换handler的时候应该终止串口通信
     *
     * @param handler
     * @return
     */
    public HBluetooth setHandler(HHandler handler) {
        Message msg = new Message();
        msg.what = R.string.handler_changed;
        msg.obj = this.handler;
        if (null != this.handler)
            this.handler.sendMessage(msg);
        this.abortTalking = true;
        this.handler = handler;

        HInterrupt();
        return this;
    }

    public final HCommunication[] getCommunications() {
        return communications;
    }

    /**
     * 当设置communication的时候应该允许串口通信
     *
     * @param c
     * @return
     */
    public HBluetooth setCommunications(final HCommunication[] c) {
        this.abortTalking = false;
        this.communications = c;
        return this;
    }

    private HBluetooth HInterrupt() {
        try {
            abortTalking = true;
            pool.awaitTermination(100, TimeUnit.MILLISECONDS);
//			Thread.currentThread().interrupt();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            abortTalking = false;
        }
        return this;
    }

    public HBluetooth Start() {
        HInterrupt();
        pool.execute(this);
        return this;
    }

    public HBluetooth HRun() {
        run();
        return this;
    }

}