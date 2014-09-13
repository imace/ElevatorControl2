package com.inovance.elevatorcontrol.handlers;

import android.app.Activity;
import android.os.Message;
import com.inovance.bluetoothtool.BluetoothHandler;
import com.inovance.elevatorcontrol.activities.ConfigurationActivity;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.models.RealTimeMonitor;
import com.inovance.elevatorcontrol.views.fragments.ConfigurationFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 运行在主线程,仅做一些ui操作
 *
 * @author jch
 */
public class ConfigurationHandler extends BluetoothHandler {

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
            ConfigurationActivity parentActivity = ((ConfigurationActivity) activity);
            List<RealTimeMonitor> showMonitorList = new ArrayList<RealTimeMonitor>();
            // 输入端子 ID
            int inputStateID = ApplicationConfig.MonitorStateCode[5];
            // 输出端子 ID
            int outputStateID = ApplicationConfig.MonitorStateCode[6];
            List<RealTimeMonitor> tempInputMonitor = new ArrayList<RealTimeMonitor>();
            List<RealTimeMonitor> tempOutputMonitor = new ArrayList<RealTimeMonitor>();
            for (RealTimeMonitor item1 : monitorList) {
                if (item1.getStateID() == inputStateID) {
                    tempInputMonitor.add(item1);
                } else if (item1.getStateID() == outputStateID) {
                    tempOutputMonitor.add(item1);
                } else {
                    showMonitorList.add(item1);
                }
            }
            // 根据 Sort 排序
            Collections.sort(tempInputMonitor, new SortComparator());
            Collections.sort(tempOutputMonitor, new SortComparator());
            // 取得输入、输出端子位置索引
            RealTimeMonitor monitor;
            if (tempInputMonitor.size() > 0) {
                monitor = tempInputMonitor.get(tempInputMonitor.size() - 1);
                monitor.setCombineBytes(ConfigurationHandler.getCombineBytes(tempInputMonitor));
                showMonitorList.add(monitor);
            }
            if (tempOutputMonitor.size() > 0) {
                monitor = tempOutputMonitor.get(tempOutputMonitor.size() - 1);
                monitor.setCombineBytes(ConfigurationHandler.getCombineBytes(tempOutputMonitor));
                showMonitorList.add(monitor);
            }
            ConfigurationFragment fragment = parentActivity.mConfigurationAdapter.getItem(0);
            fragment.reloadDataSource(showMonitorList);
            parentActivity.isSyncing = false;
        }
    }

    @Override
    public void onTalkReceive(Message msg) {
        super.onTalkReceive(msg);
        if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
            monitorList.add((RealTimeMonitor) msg.obj);
            receiveCount++;
        }
    }

    @Override
    public void onTalkError(Message msg) {
        super.onTalkError(msg);
    }

    private class SortComparator implements Comparator<RealTimeMonitor> {

        @Override
        public int compare(RealTimeMonitor object1, RealTimeMonitor object2) {
            if (object1.getSort() < object2.getSort()) {
                return 1;
            } else if (object1.getSort() > object2.getSort()) {
                return -1;
            } else {
                return 0;
            }
        }

    }

    public static byte[] getCombineBytes(List<RealTimeMonitor> monitorList) {
        List<Byte> byteList = new ArrayList<Byte>();
        for (RealTimeMonitor monitor : monitorList) {
            byteList.add(monitor.getReceived()[4]);
            byteList.add(monitor.getReceived()[5]);
        }
        byte[] combineBytes = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            combineBytes[i] = byteList.get(i);
        }
        return combineBytes;
    }

}
