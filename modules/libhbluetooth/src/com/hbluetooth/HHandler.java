package com.hbluetooth;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.sz.siiit.libhbt.R;

public class HHandler extends Handler {

	public String HTAG = "UnKnownHHandler";

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
		for (Field f : R.msgwhat.class.getFields()) {
			try {
				if (msg.what == f.getInt(null)) {
					this.getClass().getMethod(
							activity.getResources().getString(msg.what),
							Message.class).invoke(this, msg);
				}
			} catch (IllegalArgumentException e) {
				Log.e(HTAG, "IllegalArgumentException");
			} catch (IllegalAccessException e) {
				Log.e(HTAG, "IllegalAccessException");
			} catch (NoSuchMethodException e) {
				Log.e(HTAG, "NoSuchMethodException");
			} catch (InvocationTargetException e) {
				Log.e(HTAG,"InvocationTargetException");
			} catch (NullPointerException e){
				Log.e(HTAG, "NullPointerException");
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
		Log.v(HTAG, "onBeginPreparing");
	}

	/**
	 * FOUNDDEVICE
	 * 
	 * @param msg
	 */
	@SuppressWarnings("unchecked")
	public void onFoundDevice(Message msg) {
		Log.v(HTAG, "onFoundDevice : "
				+ ((Map<String, BluetoothDevice>) msg.obj).keySet().toString());
	}

	/**
	 * CHOOSEDEVICE
	 * 
	 * @param msg
	 */
	public void onChooseDevice(final Message msg) {
		Log.v(HTAG, "onChooseDevice");
	}

	/**
	 * RESETBLUETOOTH
	 * 
	 * @param msg
	 */
	public void onResetBluethooth(Message msg) {
		Log.v(HTAG, "onResetBluethooth");
	}

	/**
	 * KILLBLUETOOTH
	 * 
	 * @param msg
	 */
	public void onKillBluethooth(Message msg) {
		Log.v(HTAG, "onKillBluethooth");
	}

	/**
	 * PREPARESUCCESSFULL
	 * 
	 * @param msg
	 */
	public void onPrepared(Message msg) {
		Log.v(HTAG, "onPrepared");
	}

	/**
	 * PREPAREFAILED
	 * 
	 * @param msg
	 */
	public void onPrepError(Message msg) {
		Log.v(HTAG, "onPrepError");
	}

	/**
	 * TALKBEFORESEND
	 * 
	 * @param msg
	 */
	public void onBeforeTalkSend(Message msg) {
		Log.v(HTAG, "onBeforeTalkSend");
	}

	/**
	 * TALKAFTERSEND
	 * 
	 * @param msg
	 */
	public void onAfterTalkSend(Message msg) {
		Log.v(HTAG, "onAfterTalkSend");
	}

	/**
	 * TALKRECEIVE
	 * 
	 * @param msg
	 */
	public void onTalkReceive(Message msg) {
		Log.v(HTAG, "onTalkReceive : " +  msg.obj.toString());
	}

	/**
	 * TALKERROR
	 * 
	 * @param msg
	 */
	public void onTalkError(Message msg) {
		Log.v(HTAG,
				"onTalkError : "
						+ ((msg.obj == null) ? "..." : msg.obj.toString()));
	}

	/**
	 * HANDLERCHANGED
	 * @param msg
	 */
	public void onHandlerChanged(Message msg) { //the origin handler
		Log.v(HTAG, "onHandlerChanged"
				+ ((msg.obj == null) ? "UnKnownHHandler"
						: ((HHandler) msg.obj).HTAG));
	}

	
	/**
	 * MULTITALKBEGIN
	 * @param msg
	 */
	public void onMultiTalkBegin(Message msg) {
		Log.v(HTAG, "onMultiTalkBegin");
	}
	
	/**
	 * MULTITALKEND
	 * @param msg
	 */
	public void onMultiTalkEnd(Message msg) {
		Log.v(HTAG, "onMultiTalkEnd");
	}
}
