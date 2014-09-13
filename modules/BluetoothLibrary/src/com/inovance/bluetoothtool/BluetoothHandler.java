package com.inovance.bluetoothtool;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Map;

public class BluetoothHandler extends Handler {

    public String TAG = BluetoothHandler.class.getSimpleName();

    protected Activity activity;

    private boolean DEBUG = false;

    public BluetoothHandler(Activity activity) {
        super();
        this.activity = activity;
    }

    /**
     * 消息处理
     */
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case BluetoothState.onBeginDiscovering:
                onBeginDiscovering(msg);
                break;
            case BluetoothState.onFoundDevice:
                onFoundDevice(msg);
                break;
            case BluetoothState.onResetBluetooth:
                onResetBluetooth(msg);
                break;
            case BluetoothState.onKillBluetooth:
                onKillBluetooth(msg);
                break;
            case BluetoothState.onChooseDevice:
                onChooseDevice(msg);
                break;
            case BluetoothState.onConnected:
                onConnected(msg);
                break;
            case BluetoothState.onConnectFailed:
                onConnectFailed(msg);
                break;
            case BluetoothState.onMultiTalkBegin:
                onMultiTalkBegin(msg);
                break;
            case BluetoothState.onMultiTalkEnd:
                onMultiTalkEnd(msg);
                break;
            case BluetoothState.onBeforeTalkSend:
                onBeforeTalkSend(msg);
                break;
            case BluetoothState.onAfterTalkSend:
                onAfterTalkSend(msg);
                break;
            case BluetoothState.onTalkError:
                onTalkError(msg);
                break;
            case BluetoothState.onTalkReceive:
                onTalkReceive(msg);
                break;
            case BluetoothState.onHandlerChanged:
                onHandlerChanged(msg);
                break;
            case BluetoothState.onDiscoveryFinished:
                onDiscoveryFinished(msg);
                break;
            case BluetoothState.onDisconnected:
                onDisconnected(msg);
                break;
            case BluetoothState.onWillConnect:
                onWillConnect(msg);
                break;
            case BluetoothState.onDeviceChanged:
                onDeviceChanged(msg);
                break;
            case BluetoothState.onBluetoothConnectException:
                onBluetoothConnectException(msg);
                break;
        }
    }

    /**
     * BEGIN PREPARING
     *
     * @param msg message
     */
    public void onBeginDiscovering(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onBeginDiscovering");
        }
    }

    /**
     * FOUND DEVICE
     *
     * @param msg message
     */
    @SuppressWarnings("unchecked")
    public void onFoundDevice(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onFoundDevice : "
                    + ((Map<String, BluetoothDevice>) msg.obj).keySet().toString());
        }
    }

    /**
     * CHOOSE DEVICE
     *
     * @param msg message
     */
    public void onChooseDevice(final Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onChooseDevice");
        }
    }

    /**
     * RESET BLUETOOTH
     *
     * @param msg message
     */
    public void onResetBluetooth(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onResetBluetooth");
        }
    }

    /**
     * KILL BLUETOOTH
     *
     * @param msg message
     */
    public void onKillBluetooth(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onKillBluetooth");
        }
    }

    /**
     * PREPARE SUCCESSFUL
     *
     * @param msg message
     */
    public void onConnected(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onConnected");
        }
    }

    /**
     * PREPARE FAILED
     *
     * @param msg message
     */
    public void onConnectFailed(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onConnectFailed");
        }
    }

    /**
     * TALK BEFORE SEND
     *
     * @param msg message
     */
    public void onBeforeTalkSend(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onBeforeTalkSend");
        }
    }

    /**
     * TALK AFTER SEND
     *
     * @param msg message
     */
    public void onAfterTalkSend(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onAfterTalkSend");
        }
    }

    /**
     * TALK RECEIVE
     *
     * @param msg message
     */
    public void onTalkReceive(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onTalkReceive : " + msg.obj.toString());
        }
    }

    /**
     * TALK ERROR
     *
     * @param msg message
     */
    public void onTalkError(Message msg) {
        if (DEBUG) {
            Log.v(TAG,
                    "onTalkError : "
                            + ((msg.obj == null) ? "..." : msg.obj.toString()));
        }
    }

    /**
     * HANDLER CHANGED
     *
     * @param msg message
     */
    public void onHandlerChanged(Message msg) { //the origin handler
        if (DEBUG) {
            Log.v(TAG, "onHandlerChanged"
                    + ((msg.obj == null) ? "UnKnownHHandler"
                    : ((BluetoothHandler) msg.obj).TAG));
        }
    }


    /**
     * MULTI TALK BEGIN
     *
     * @param msg message
     */
    public void onMultiTalkBegin(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onMultiTalkBegin");
        }
    }

    /**
     * MULTI TALK END
     *
     * @param msg message
     */
    public void onMultiTalkEnd(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onMultiTalkEnd");
        }
    }

    /**
     * 蓝牙设备搜索结束
     *
     * @param message message
     */
    public void onDiscoveryFinished(Message message) {
        if (DEBUG) {
            Log.v(TAG, "onDiscoveryFinished");
        }
    }

    /**
     * 蓝牙连接断开
     *
     * @param message message
     */
    public void onDisconnected(Message message) {
        if (DEBUG) {
            Log.v(TAG, "onDisconnected");
        }
    }

    /**
     * 将要进行设备连接
     *
     * @param message message
     */
    public void onWillConnect(Message message) {
        if (DEBUG) {
            Log.v(TAG, "onWillConnect");
        }
    }

    /**
     * 设备切换
     *
     * @param message message
     */
    public void onDeviceChanged(Message message) {
        if (DEBUG) {
            Log.v(TAG, "onWillConnect");
        }
    }

    /**
     * 蓝牙设备连接异常
     *
     * @param message message
     */
    public void onBluetoothConnectException(Message message) {
        if (DEBUG) {
            Log.v(TAG, "onBluetoothConnectException");
        }
    }
}
