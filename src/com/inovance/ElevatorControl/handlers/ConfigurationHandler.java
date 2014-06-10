package com.inovance.ElevatorControl.handlers;

import android.app.Activity;
import android.os.Message;
import com.bluetoothtool.BluetoothHandler;
import com.inovance.ElevatorControl.activities.ConfigurationActivity;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.models.RealTimeMonitor;
import com.inovance.ElevatorControl.views.fragments.ConfigurationFragment;

import java.util.ArrayList;
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
            final List<RealTimeMonitor> finalList = new ArrayList<RealTimeMonitor>();
            List<RealTimeMonitor> inputMonitor = new ArrayList<RealTimeMonitor>();
            List<RealTimeMonitor> outputMonitor = new ArrayList<RealTimeMonitor>();
            RealTimeMonitor hvMonitor = null;
            for (String normal : ApplicationConfig.normalFilters) {
                for (RealTimeMonitor monitor : monitorList) {
                    if (monitor.getName().equalsIgnoreCase(normal)) {
                        finalList.add(monitor);
                    }
                    if (monitor.getName().equalsIgnoreCase(ApplicationConfig.HVInputTerminalStatusName)) {
                        hvMonitor = monitor;
                    }
                }
            }
            for (String input : ApplicationConfig.inputFilters) {
                for (RealTimeMonitor monitor : monitorList) {
                    if (monitor.getName().equalsIgnoreCase(input)) {
                        inputMonitor.add(monitor);
                        break;
                    }
                }
            }
            for (String output : ApplicationConfig.outputFilters) {
                for (RealTimeMonitor monitor : monitorList) {
                    if (monitor.getName().equalsIgnoreCase(output)) {
                        outputMonitor.add(monitor);
                        break;
                    }
                }
            }
            if (inputMonitor.size() == 4) {
                try {
                    RealTimeMonitor monitor = (RealTimeMonitor) inputMonitor.get(0).clone();
                    monitor.setName("主控板输入端子");
                    byte[] combineBytes = new byte[]{
                            inputMonitor.get(3).getReceived()[4],
                            inputMonitor.get(3).getReceived()[5],

                            inputMonitor.get(2).getReceived()[4],
                            inputMonitor.get(2).getReceived()[5],

                            inputMonitor.get(1).getReceived()[4],
                            inputMonitor.get(1).getReceived()[5],

                            inputMonitor.get(0).getReceived()[4],
                            inputMonitor.get(0).getReceived()[5],
                    };
                    monitor.setCombineBytes(combineBytes);
                    if (hvMonitor != null) {
                        monitor.setHVInputTerminalBytes(new byte[]{hvMonitor.getReceived()[4],
                                hvMonitor.getReceived()[5]});
                    }
                    monitor.setCode(inputMonitor.get(0).getCode()
                            + "+" + inputMonitor.get(1).getCode()
                            + "+" + inputMonitor.get(2).getCode()
                            + "+" + inputMonitor.get(3).getCode());
                    monitor.setDescriptionType(ApplicationConfig.specialTypeInput);
                    finalList.add(monitor);
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
            if (outputMonitor.size() == 1) {
                try {
                    RealTimeMonitor monitor = (RealTimeMonitor) inputMonitor.get(0).clone();
                    monitor.setName("主控板输出端子");
                    byte[] combineBytes = outputMonitor.get(0).getReceived();
                    monitor.setCombineBytes(combineBytes);
                    monitor.setCode(outputMonitor.get(0).getCode() + "+" + outputMonitor.get(0).getCode());
                    monitor.setDescriptionType(ApplicationConfig.specialTypeOutput);
                    finalList.add(monitor);
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
            ConfigurationActivity parentActivity = ((ConfigurationActivity) activity);
            ConfigurationFragment fragment = parentActivity.mConfigurationAdapter.getItem(parentActivity.pageIndex);
            fragment.syncMonitorViewData(finalList);
            parentActivity.isSyncing = false;
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
        super.onTalkError(msg);
    }
}
