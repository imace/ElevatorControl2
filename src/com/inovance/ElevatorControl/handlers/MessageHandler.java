package com.inovance.elevatorcontrol.handlers;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.inovance.elevatorcontrol.R;

import java.lang.ref.WeakReference;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-5-8.
 * Time: 15:05.
 */
public class MessageHandler {

    private static MessageHandler instance = new MessageHandler();

    private DealHandler handler;

    private Activity activity;

    public static final int NOT_CONNECTED = 1;

    public static final int WRITE_DATA_FAILED = 2;

    public static final int WRITE_DATA_SUCCESSFUL = 3;

    public static final int NO_DATA_RECEIVE = 4;

    public static final int CODE_DATA_ERROR = 5;

    public static MessageHandler getInstance(Activity activity) {
        if (instance.activity == null) {
            instance.activity = activity;
        }
        instance.handler = new DealHandler(instance);
        return instance;
    }

    private static class DealHandler extends Handler {

        private final WeakReference<MessageHandler> mHandler;

        public DealHandler(MessageHandler handler) {
            mHandler = new WeakReference<MessageHandler>(handler);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MessageHandler handler = mHandler.get();
            if (handler != null) {
                switch (msg.what) {
                    case NOT_CONNECTED:
                        handler.onNotConnect();
                        break;
                    case WRITE_DATA_FAILED:
                        handler.onWriteDataFailed();
                        break;
                    case WRITE_DATA_SUCCESSFUL:
                        handler.onWriteDataSuccessful();
                        break;
                    case NO_DATA_RECEIVE:
                        handler.onNoDataReceived();
                        break;
                    case CODE_DATA_ERROR:
                        handler.onParseCodeError();
                        break;
                }
            }
        }
    }

    public void sendMessage(int messageWhat) {
        if (handler != null) {
            handler.sendEmptyMessage(messageWhat);
        }
    }

    private void onNotConnect() {
        if (activity != null) {
            Toast.makeText(activity,
                    R.string.not_connect_device_error,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void onWriteDataFailed() {
        if (activity != null) {
            Toast.makeText(activity,
                    R.string.write_failed_text,
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void onWriteDataSuccessful() {
        if (activity != null) {
            Toast.makeText(activity,
                    R.string.write_parameter_successful,
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void onNoDataReceived() {
        if (activity != null) {
            Toast.makeText(activity,
                    R.string.error_no_data_received,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void onParseCodeError() {
        if (activity != null) {
            Toast.makeText(activity,
                    R.string.code_parse_error_tips,
                    Toast.LENGTH_SHORT).show();
        }
    }

}
