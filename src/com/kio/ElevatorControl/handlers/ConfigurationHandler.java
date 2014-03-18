package com.kio.ElevatorControl.handlers;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import com.hbluetooth.HHandler;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.activities.ConfigurationActivity;
import com.kio.ElevatorControl.daos.MenuValuesDao;
import com.kio.ElevatorControl.models.RealTimeMonitor;
import com.kio.ElevatorControl.views.dialogs.CustomDialog;
import com.mobsandgeeks.adapters.InstantAdapter;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * 运行在主线程,仅做一些ui操作
 *
 * @author jch
 */
public class ConfigurationHandler extends HHandler {

    // 内容数据源
    private List<RealTimeMonitor> monitorList = new ArrayList<RealTimeMonitor>();

    private InstantAdapter<RealTimeMonitor> monitorListViewAdapter;

    private Object currentMessage;

    public ConfigurationHandler(Activity activity) {
        super(activity);
        TAG = ConfigurationHandler.class.getSimpleName();
    }

    @Override
    public void onMultiTalkBegin(Message msg) {
        super.onMultiTalkBegin(msg);
        monitorList.clear();
    }

    @Override
    public void onMultiTalkEnd(Message msg) {
        super.onMultiTalkEnd(msg);
    }

    @Override
    public void onTalkReceive(Message msg) {
        try {
            currentMessage = msg.obj;
            // 根据当前页反射执行 loadMonitorView() 方法
            this.getClass()
                    .getMethod(
                            MenuValuesDao.getConfigurationLoadMethodName(
                                    ((ConfigurationActivity) activity).pager
                                            .getCurrentItem(), activity))
                    .invoke(this);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, e.getMessage());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e(TAG, e.getMessage());
        } catch (InvocationTargetException e) {
            Log.e(TAG, e.getTargetException().getMessage());
        }

    }

    /**
     * 实时监控,加载内容
     */
    public void loadMonitorView() {
        if (null != this.currentMessage && currentMessage instanceof RealTimeMonitor) {
            monitorList.add((RealTimeMonitor) currentMessage);
        }
        if (monitorList.size() <= 1) {
            ListView monitorListView = (ListView) ((ConfigurationActivity) activity).pager
                    .findViewById(R.id.monitor_list);
            monitorListViewAdapter = new InstantAdapter<RealTimeMonitor>(
                    activity.getBaseContext(),
                    R.layout.list_configuration_monitor_item, RealTimeMonitor.class,
                    monitorList);
            monitorListView.setAdapter(monitorListViewAdapter);
            monitorListView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    Log.v(TAG, "position:" + position + ",id:" + id);
                    if (monitorList.get(position).isShowBit()) {
                        AlertDialog dialog = CustomDialog.switchersDialog(activity,
                                (byte) 0x11,
                                (byte) 0x22).create();
                        dialog.setInverseBackgroundForced(true);
                        dialog.show();
                    }
                }
            });
        } else {
            if (monitorListViewAdapter == null) {
                monitorListViewAdapter = new InstantAdapter<RealTimeMonitor>(
                        activity.getBaseContext(),
                        R.layout.list_configuration_monitor_item, RealTimeMonitor.class,
                        monitorList);
            }
            monitorListViewAdapter.notifyDataSetChanged();
        }
    }
}
