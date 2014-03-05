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
     * BEGINPREPARING
     *
     * @param msg
     */
    public void onBeginPreparing(Message msg) {
        Log.v(TAG, "onBeginPreparing");
    }

    /**
     * FOUNDDEVICE
     *
     * @param msg
     */
    @SuppressWarnings("unchecked")
    public void onFoundDevice(Message msg) {
        Log.v(TAG, "onFoundDevice : "
                + ((Map<String, BluetoothDevice>) msg.obj).keySet().toString());
    }

    /**
     * CHOOSEDEVICE
     *
     * @param msg
     */
    public void onChooseDevice(final Message msg) {
        Log.v(TAG, "onChooseDevice");
    }

    /**
     * RESETBLUETOOTH
     *
     * @param msg
     */
    public void onResetBluetooth(Message msg) {
        Log.v(TAG, "onResetBluetooth");
    }

    /**
     * KILLBLUETOOTH
     *
     * @param msg
     */
    public void onKillBluetooth(Message msg) {
        Log.v(TAG, "onKillBluetooth");
    }

    /**
     * PREPARESUCCESSFULL
     *
     * @param msg
     */
    public void onPrepared(Message msg) {
        Log.v(TAG, "onPrepared");
    }

    /**
     * PREPAREFAILED
     *
     * @param msg
     */
    public void onPrepError(Message msg) {
        Log.v(TAG, "onPrepError");
    }

    /**
     * TALKBEFORESEND
     *
     * @param msg
     */
    public void onBeforeTalkSend(Message msg) {
        Log.v(TAG, "onBeforeTalkSend");
    }

    /**
     * TALKAFTERSEND
     *
     * @param msg
     */
    public void onAfterTalkSend(Message msg) {
        Log.v(TAG, "onAfterTalkSend");
    }

    /**
     * TALKRECEIVE
     *
     * @param msg
     */
    public void onTalkReceive(Message msg) {
        Log.v(TAG, "onTalkReceive : " + msg.obj.toString());
    }

    /**
     * TALKERROR
     *
     * @param msg
     */
    public void onTalkError(Message msg) {
        Log.v(TAG,
                "onTalkError : "
                        + ((msg.obj == null) ? "..." : msg.obj.toString()));
    }

    /**
     * HANDLERCHANGED
     *
     * @param msg
     */
    public void onHandlerChanged(Message msg) { //the origin handler
        Log.v(TAG, "onHandlerChanged"
                + ((msg.obj == null) ? "UnKnownHHandler"
                : ((HHandler) msg.obj).TAG));
    }


    /**
     * MULTITALKBEGIN
     *
     * @param msg
     */
    public void onMultiTalkBegin(Message msg) {
        Log.v(TAG, "onMultiTalkBegin");
    }

    /**
     * MULTITALKEND
     *
     * @param msg
     */
    public void onMultiTalkEnd(Message msg) {
        Log.v(TAG, "onMultiTalkEnd");
    }
}
