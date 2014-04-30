package com.hbluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Map;

public class HHandler extends Handler {

    public String TAG = HHandler.class.getSimpleName();

    protected Activity activity;

    private boolean DEBUG = false;

    public HHandler(Activity activity) {
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
            case Config.onBeginPreparing:
                onBeginPreparing(msg);
                break;
            case Config.onFoundDevice:
                onFoundDevice(msg);
                break;
            case Config.onResetBluetooth:
                onResetBluetooth(msg);
                break;
            case Config.onKillBluetooth:
                onKillBluetooth(msg);
                break;
            case Config.onChooseDevice:
                onChooseDevice(msg);
                break;
            case Config.onPrepared:
                onPrepared(msg);
                break;
            case Config.onPrepError:
                onPrepError(msg);
                break;
            case Config.onMultiTalkBegin:
                onMultiTalkBegin(msg);
                break;
            case Config.onMultiTalkEnd:
                onMultiTalkEnd(msg);
                break;
            case Config.onBeforeTalkSend:
                onBeforeTalkSend(msg);
                break;
            case Config.onAfterTalkSend:
                onAfterTalkSend(msg);
                break;
            case Config.onTalkError:
                onTalkError(msg);
                break;
            case Config.onTalkReceive:
                onTalkReceive(msg);
                break;
            case Config.onHandlerChanged:
                onHandlerChanged(msg);
                break;
            case Config.onDiscoveryFinished:
                onDiscoveryFinished(msg);
                break;
        }
    }

    /**
     * BEGIN PREPARING
     *
     * @param msg message
     */
    public void onBeginPreparing(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onBeginPreparing");
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
    public void onPrepared(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onPrepared");
        }
    }

    /**
     * PREPARE FAILED
     *
     * @param msg message
     */
    public void onPrepError(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onPrepError");
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
                    : ((HHandler) msg.obj).TAG));
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
}
