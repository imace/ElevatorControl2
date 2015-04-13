package com.inovance.elevatorcontrol.handlers;

import android.app.Activity;
import android.os.Message;

import com.inovance.elevatorcontrol.activities.TroubleAnalyzeActivity;
import com.inovance.elevatorcontrol.models.ErrorHelp;

public class CurrentErrorHandler extends UnlockHandler {

    private ErrorHelp errorHelp;

    public int sendCount;

    private int receiveCount;

    public CurrentErrorHandler(Activity activity) {
        super(activity);
        TAG = CurrentErrorHandler.class.getSimpleName();
    }

    @Override
    public void onMultiTalkBegin(Message msg) {
        super.onMultiTalkBegin(msg);
        errorHelp = null;
        receiveCount = 0;
    }

    @Override
    public void onMultiTalkEnd(Message msg) {
        super.onMultiTalkEnd(msg);
        TroubleAnalyzeActivity parentActivity = (TroubleAnalyzeActivity) activity;
        if (receiveCount == sendCount) {
            parentActivity.displayErrorInformation(errorHelp);
        }
        parentActivity.isSyncing = false;
    }

    @Override
    public void onTalkReceive(Message msg) {
        if (msg.obj != null && (msg.obj instanceof ErrorHelp)) {
            errorHelp = (ErrorHelp) msg.obj;
        }
        receiveCount++;
    }

    @Override
    public void onTalkError(Message msg) {
        super.onTalkError(msg);
    }
}
