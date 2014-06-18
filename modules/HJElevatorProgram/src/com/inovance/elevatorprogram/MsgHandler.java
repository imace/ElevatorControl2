package com.inovance.elevatorprogram;

import android.os.Handler;
import android.os.Message;

public class MsgHandler extends Handler {

	@Override
	public void handleMessage(Message msg) {
		// TODO Auto-generated method stub
		switch (msg.what) {
		case BaseProgram.MESSAGE_PROGRESSINFO:
			ProgramInfo(msg);
			break;
		}

		super.handleMessage(msg);
	}

	/**
	 * 烧录信息
	 */
	public void ProgramInfo(Message msg) {
		switch (msg.arg1) {
		case IProgram.ERROR_CODE:
			//故障码
			break;
		case IProgram.PROGRAM_PROGRESS:
			//烧录进度
			//int percent = msg.arg2;//百分比
			break;
		case IProgram.PROGRAM_TEXT_INFO:
			//文字信息
			//String str  = (String)msg.obj;
			break;
		case IProgram.PROGRAM_TIME:
			//烧录总时间
			// millisecond = msg.arg2;
			break;
		}
	}
}
