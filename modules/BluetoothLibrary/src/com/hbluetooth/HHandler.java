package com.hbluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.kio.bluetooth.R;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
     * 根据hhandler.xml配置进行消息处理
     */
    @Override
    public void handleMessage(Message msg) {
        for (Field f : R.string.class.getFields()) {
            try {
                if (msg.what == f.getInt(null)) {
                    this.getClass().getMethod(
                            activity.getResources().getString(msg.what),
                            Message.class).invoke(this, msg);
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "IllegalArgumentException");
            } catch (IllegalAccessException e) {
                Log.e(TAG, "IllegalAccessException");
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "NoSuchMethodException");
            } catch (InvocationTargetException e) {
                Log.e(TAG, "InvocationTargetException");
            } catch (NullPointerException e) {
                Log.e(TAG, "NullPointerException");
            }
        }
        super.handleMessage(msg);
    }

    /**
     * BEGIN PREPARING
     *
     * @param msg
     */
    public void onBeginPreparing(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onBeginPreparing");
        }
    }

    /**
     * FOUND DEVICE
     *
     * @param msg
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
     * @param msg
     */
    public void onChooseDevice(final Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onChooseDevice");
        }
    }

    /**
     * RESET BLUETOOTH
     *
     * @param msg
     */
    public void onResetBluetooth(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onResetBluetooth");
        }
    }

    /**
     * KILL BLUETOOTH
     *
     * @param msg
     */
    public void onKillBluetooth(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onKillBluetooth");
        }
    }

    /**
     * PREPARE SUCCESSFUL
     *
     * @param msg
     */
    public void onPrepared(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onPrepared");
        }
    }

    /**
     * PREPARE FAILED
     *
     * @param msg
     */
    public void onPrepError(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onPrepError");
        }
    }

    /**
     * TALK BEFORE SEND
     *
     * @param msg
     */
    public void onBeforeTalkSend(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onBeforeTalkSend");
        }
    }

    /**
     * TALK AFTER SEND
     *
     * @param msg
     */
    public void onAfterTalkSend(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onAfterTalkSend");
        }
    }

    /**
     * TALK RECEIVE
     *
     * @param msg
     */
    public void onTalkReceive(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onTalkReceive : " + msg.obj.toString());
        }
    }

    /**
     * TALK ERROR
     *
     * @param msg
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
     * @param msg
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
     * @param msg
     */
    public void onMultiTalkBegin(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onMultiTalkBegin");
        }
    }

    /**
     * MULTI TALK END
     *
     * @param msg
     */
    public void onMultiTalkEnd(Message msg) {
        if (DEBUG) {
            Log.v(TAG, "onMultiTalkEnd");
        }
    }
}
