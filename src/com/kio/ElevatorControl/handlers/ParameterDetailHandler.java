package com.kio.ElevatorControl.handlers;

import android.app.Activity;
import android.os.Message;
import com.hbluetooth.HHandler;
import com.kio.ElevatorControl.activities.ParameterDetailActivity;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.models.ListHolder;
import com.kio.ElevatorControl.models.ParameterSettings;

import java.util.ArrayList;
import java.util.List;

public class ParameterDetailHandler extends HHandler {

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
            ((ParameterDetailActivity) activity).settingsList.clear();
            ((ParameterDetailActivity) activity).settingsList.addAll(tempList);
            ((ParameterDetailActivity) activity).instantAdapter.notifyDataSetChanged();
            ((ParameterDetailActivity) activity).isSynced = true;
        } else {
            ((ParameterDetailActivity) activity).startCombinationCommunications();
        }
    }

    @Override
    public void onTalkReceive(Message msg) {
        if (msg.obj instanceof ListHolder) {
            ListHolder holder = (ListHolder) msg.obj;
            for (ParameterSettings item : holder.getParameterSettingsList()) {
                if (!item.getName().contains(ApplicationConfig.RETAIN_NAME)) {
                    tempList.add(item);
                }
            }
            receiveCount++;
        }
    }

    @Override
    public void onTalkError(Message msg) {
        ((ParameterDetailActivity) activity).startCombinationCommunications();
    }

}
