package com.kio.ElevatorControl.handlers;

import android.app.Activity;
import android.os.Message;
import com.hbluetooth.HHandler;
import com.kio.ElevatorControl.models.ErrorHelpLog;

import java.util.ArrayList;
import java.util.List;

/**
 * 运行在CoreAcivity主线程,仅做一些ui操作
 *
 * @author jch
 */
public class FailureLogHandler extends HHandler {

    private List<ErrorHelpLog> logArrayList = new ArrayList<ErrorHelpLog>();

    public FailureLogHandler(Activity activity) {
        super(activity);
        TAG = FailureLogHandler.class.getSimpleName();
    }

    @Override
    public void onTalkReceive(Message msg) {
        if (msg.obj != null && (msg.obj instanceof ErrorHelpLog)) {
            ErrorHelpLog errorHelpLog = (ErrorHelpLog) msg.obj;
            logArrayList.add(errorHelpLog);
        }
    }
}
