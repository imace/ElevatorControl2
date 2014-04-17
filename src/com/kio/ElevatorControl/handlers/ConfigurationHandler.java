package com.kio.ElevatorControl.handlers;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import com.hbluetooth.HHandler;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.activities.ConfigurationActivity;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.models.RealTimeMonitor;
import com.kio.ElevatorControl.views.dialogs.CustomDialog;
import com.mobsandgeeks.adapters.InstantAdapter;
import org.holoeverywhere.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
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
            final List<RealTimeMonitor> finalList = new ArrayList<RealTimeMonitor>();
            List<RealTimeMonitor> inputMonitor = new ArrayList<RealTimeMonitor>();
            List<RealTimeMonitor> outputMonitor = new ArrayList<RealTimeMonitor>();
            for (String normal : ApplicationConfig.normalFilters){
                for (RealTimeMonitor monitor : monitorList) {
                    if (monitor.getName().equalsIgnoreCase(normal)){
                        finalList.add(monitor);
                    }
                }
            }
            for (String input : ApplicationConfig.inputFilters){
                for (RealTimeMonitor monitor : monitorList) {
                    if (monitor.getName().equalsIgnoreCase(input)){
                        inputMonitor.add(monitor);
                    }
                }
            }
            for (String output : ApplicationConfig.outputFilters){
                for (RealTimeMonitor monitor : monitorList) {
                    if (monitor.getName().equalsIgnoreCase(output)){
                        outputMonitor.add(monitor);
                    }
                }
            }
            if (inputMonitor.size() == 4) {
                RealTimeMonitor monitor = inputMonitor.get(0);
                monitor.dataArray = new ArrayList<byte[]>();
                monitor.dataArray.add(inputMonitor.get(0).getReceived());
                monitor.dataArray.add(inputMonitor.get(1).getReceived());
                monitor.dataArray.add(inputMonitor.get(2).getReceived());
                monitor.dataArray.add(inputMonitor.get(3).getReceived());
                monitor.setCode(inputMonitor.get(0).getCode()
                        + "+" + inputMonitor.get(1).getCode()
                        + "+" + inputMonitor.get(2).getCode()
                        + "+" + inputMonitor.get(3).getCode());
                monitor.setDescriptionType(ApplicationConfig.specialTypeInput);
                monitor.setName("输入端子");
                finalList.add(monitor);
            }
            if (outputMonitor.size() == 2) {
                RealTimeMonitor monitor = outputMonitor.get(0);
                monitor.dataArray = new ArrayList<byte[]>();
                monitor.dataArray.add(inputMonitor.get(0).getReceived());
                monitor.dataArray.add(inputMonitor.get(1).getReceived());
                monitor.setCode(outputMonitor.get(0).getCode() + "+" + outputMonitor.get(0).getCode());
                monitor.setDescriptionType(ApplicationConfig.specialTypeOutput);
                monitor.setName("输出端子");
                finalList.add(monitor);
            }
            ListView listView = (ListView) ((ConfigurationActivity) activity).pager.findViewById(R.id.monitor_list);
            InstantAdapter<RealTimeMonitor> monitorInstantAdapter = new InstantAdapter<RealTimeMonitor>(
                    activity.getBaseContext(),
                    R.layout.list_configuration_monitor_item,
                    RealTimeMonitor.class,
                    finalList);
            listView.setAdapter(monitorInstantAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    RealTimeMonitor monitor = finalList.get(position);
                    if (monitor.getDescriptionType() != ApplicationConfig.DESCRIPTION_TYPE[0]) {
                        AlertDialog dialog = CustomDialog.terminalDetailDialog(activity, monitor).create();
                        dialog.setInverseBackgroundForced(true);
                        dialog.show();
                    }
                }
            });
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
