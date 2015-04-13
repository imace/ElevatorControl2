package com.inovance.bluetoothtool;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-28.
 * Time: 16:22.
 */
public class BluetoothState {

    /**
     * 搜索模式
     */
    public static final int DISCOVERING = 1;

    /**
     * 连接已建立
     */
    public static final int CONNECTED = 2;

    /**
     * 建立连接失败
     */
    public static final int CONNECT_FAILED = 3;

    /**
     * 准备断开连接
     */
    public static final int WILL_DISCONNECT = 4;

    /**
     * 未建立链接
     */
    public static final int DISCONNECTED = 5;

    /**
     * 正在连接蓝牙设备
     */
    public static final int CONNECTING = 6;

    /**
     * 将要进行搜索
     */
    public static final int WILL_DISCOVERING = 7;

    /**
     * 正在配对
     */
    public static final int PAIRING = 8;

    /**
     * 连接异常
     */
    public static final int Exception = 9;

    /**
     * 开始搜索
     */
    public static final int onBeginDiscovering = 0;

    /**
     * 搜索到蓝牙设备
     */
    public static final int onFoundDevice = 1;

    /**
     * 重置蓝牙
     */
    public static final int onResetBluetooth = 2;

    /**
     * 结束蓝牙 Socket
     */
    public static final int onKillBluetooth = 3;

    /**
     * 选择蓝牙设备
     */
    public static final int onChooseDevice = 4;

    /**
     * 建立连接成功
     */
    public static final int onConnected = 5;

    /**
     * 建立连接失败
     */
    public static final int onConnectFailed = 6;

    /**
     * 通信开始
     */
    public static final int onMultiTalkBegin = 7;

    /**
     * 通信结束
     */
    public static final int onMultiTalkEnd = 8;

    /**
     * 单个指令发送前
     */
    public static final int onBeforeTalkSend = 9;

    /**
     * 单个指令发送后
     */
    public static final int onAfterTalkSend = 10;

    /**
     * 通信错误
     */
    public static final int onTalkError = 11;

    /**
     * 接收到指令
     */
    public static final int onTalkReceive = 12;

    /**
     * Handler 改变
     */
    public static final int onHandlerChanged = 13;

    /**
     * 蓝牙搜索结束
     */
    public static final int onDiscoveryFinished = 14;

    /**
     * 蓝牙连接断开
     */
    public static final int onDisconnected = 15;

    /**
     * 将要进行连接
     */
    public static final int onWillConnect = 16;

    /**
     * 设备切换
     */
    public static final int onDeviceChanged = 17;

    /**
     * 蓝牙设备连接异常
     */
    public static final int onBluetoothConnectException = 18;

    /**
     * 设备锁定
     */
    public static final int onDeviceLocked = 19;

}
