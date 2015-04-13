package com.inovance.elevatorcontrol.handlers;

import android.app.Activity;
import android.os.Message;

import com.inovance.elevatorcontrol.activities.ParameterDetailActivity;
import com.inovance.elevatorcontrol.models.ObjectListHolder;
import com.inovance.elevatorcontrol.models.ParameterSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ParameterDetailHandler extends UnlockHandler {

    public int sendCount = 0;

    public int receiveCount = 0;

    public List<ParameterSettings> tempList;

    public ParameterDetailHandler(Activity activity) {
        super(activity);
        TAG = ParameterDetailHandler.class.getSimpleName();
    }

    @Override
    public void onMultiTalkBegin(Message msg) {
        super.onMultiTalkBegin(msg);
        receiveCount = 0;
        tempList = new ArrayList<ParameterSettings>();
    }

    @Override
    public void onMultiTalkEnd(Message msg) {
        super.onMultiTalkEnd(msg);
        final ParameterDetailActivity parentActivity = ((ParameterDetailActivity) activity);
        if (receiveCount == sendCount) {
            if (parentActivity.mRefreshActionItem != null) {
                parentActivity.mRefreshActionItem.showProgress(false);
            }
            parentActivity.listViewDataSource.clear();
            parentActivity.listViewDataSource.addAll(tempList);
            parentActivity.instantAdapter.notifyDataSetChanged();
            parentActivity.syncingParameter = false;
        } else {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    parentActivity.startCombinationCommunications();
                }
            }, 500);
        }
    }

    @Override
    public void onTalkReceive(Message msg) {
        if (msg.obj instanceof ObjectListHolder) {
            ObjectListHolder holder = (ObjectListHolder) msg.obj;
            for (ParameterSettings item : holder.getParameterSettingsList()) {
                if (tempList == null) {
                    tempList = new ArrayList<ParameterSettings>();
                }
                tempList.add(item);
            }
            receiveCount++;
        }
    }

}
