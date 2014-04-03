package com.kio.ElevatorControl.handlers;

import android.app.Activity;
import android.os.Message;
import com.hbluetooth.HHandler;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.activities.ConfigurationActivity;
import com.kio.ElevatorControl.models.RealTimeMonitor;
import com.mobsandgeeks.adapters.InstantAdapter;
import org.holoeverywhere.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * 运行在主线程,仅做一些ui操作
 *
 * @author jch
 */
public class ConfigurationHandler extends HHandler {

    public int sendCount;

    public int receiveCount;

    public List<RealTimeMonitor> monitorList;

    public ConfigurationHandler(Activity activity) {
        super(activity);
        TAG = ConfigurationHandler.class.getSimpleName();
    }

    @Override
    public void onMultiTalkBegin(Message msg) {
        super.onMultiTalkBegin(msg);
        receiveCount = 0;
        monitorList = new ArrayList<RealTimeMonitor>();
    }

    @Override
    public void onMultiTalkEnd(Message msg) {
        super.onMultiTalkEnd(msg);
        if (receiveCount == sendCount) {
            ListView listView = (ListView) ((ConfigurationActivity) activity).pager.findViewById(R.id.monitor_list);
            InstantAdapter<RealTimeMonitor> monitorInstantAdapter = new InstantAdapter<RealTimeMonitor>(
                    activity.getBaseContext(),
                    R.layout.list_configuration_monitor_item,
                    RealTimeMonitor.class,
                    monitorList);
            listView.setAdapter(monitorInstantAdapter);
        } else {
            ((ConfigurationActivity) activity).loadMonitorView();
        }
    }

    @Override
    public void onTalkReceive(Message msg) {
        if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
            monitorList.add((RealTimeMonitor) msg.obj);
            receiveCount++;
        }
    }

    @Override
    public void onTalkError(Message msg) {
        ((ConfigurationActivity) activity).loadMonitorView();
    }
}
