package com.inovance.ElevatorControl.handlers;

import android.app.Activity;
import android.os.Message;
import android.util.Log;
import com.bluetoothtool.BluetoothHandler;
import com.inovance.ElevatorControl.activities.ParameterDetailActivity;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.models.ObjectListHolder;
import com.inovance.ElevatorControl.models.ParameterSettings;

import java.util.ArrayList;
import java.util.List;

public class ParameterDetailHandler extends BluetoothHandler {

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
        if (receiveCount == sendCount) {
            ParameterDetailActivity parentActivity = ((ParameterDetailActivity) activity);
            if (parentActivity.mRefreshActionItem != null) {
                parentActivity.mRefreshActionItem.showProgress(false);
            }
            parentActivity.listViewDataSource.clear();
            parentActivity.listViewDataSource.addAll(tempList);
            parentActivity.instantAdapter.notifyDataSetChanged();
            parentActivity.syncingParameter = false;
        } else {
            ((ParameterDetailActivity) activity).startCombinationCommunications();
        }
    }

    @Override
    public void onTalkReceive(Message msg) {
        if (msg.obj instanceof ObjectListHolder) {
            ObjectListHolder holder = (ObjectListHolder) msg.obj;
            for (ParameterSettings item : holder.getParameterSettingsList()) {
                if (!item.getName().contains(ApplicationConfig.RETAIN_NAME)) {
                    if (tempList == null) {
                        tempList = new ArrayList<ParameterSettings>();
                    }
                    tempList.add(item);
                }
            }
            receiveCount++;
        }
    }

    @Override
    public void onTalkError(Message msg) {
        super.onTalkError(msg);
    }

}
