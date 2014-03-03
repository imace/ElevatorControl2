package com.kio.ElevatorControl.handlers;

import android.app.Activity;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import com.hbluetooth.HHandler;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.activities.ConfigurationActivity;
import com.kio.ElevatorControl.daos.ValuesDao;
import com.kio.ElevatorControl.models.RealtimeMonitor;
import com.kio.ElevatorControl.views.dialogs.CustomDialoger;
import com.mobsandgeeks.adapters.InstantAdapter;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * 运行在主线程,仅做一些ui操作
 *
 * @author jch
 */
public class TransactionHandler extends HHandler {

    // 内容数据源
    private List<RealtimeMonitor> monitorlist = new ArrayList<RealtimeMonitor>();

    private Object currentMessage;

    public TransactionHandler(Activity activity) {
        super(activity);
        HTAG = "TransactionHandler";
    }

    @Override
    public void onMultiTalkBegin(Message msg) {
        super.onMultiTalkBegin(msg);
        monitorlist.clear();
    }

    @Override
    public void onMultiTalkEnd(Message msg) {
        super.onMultiTalkEnd(msg);
    }

    @Override
    public void onTalkReceive(Message msg) {
        try {
            currentMessage = msg.obj;
            // 根据当前页反射执行
            this.getClass()
                    .getMethod(
                            ValuesDao.getTransactionTabsLoadMethodName(
                                    ((ConfigurationActivity) activity).pager
                                            .getCurrentItem(), activity))
                    .invoke(this);
        } catch (NoSuchMethodException e) {
            Log.e(HTAG, e.getMessage());
        } catch (IllegalArgumentException e) {
            Log.e(HTAG, e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e(HTAG, e.getMessage());
        } catch (InvocationTargetException e) {
            Log.e(HTAG, e.getTargetException().getMessage());
        }

    }

    /**
     * 实时监控,加载内容
     */
    public void loadMonitorLV() {
        if (null != this.currentMessage) {
            monitorlist.add((RealtimeMonitor) currentMessage);
        }
        ListView lstv = (ListView) ((ConfigurationActivity) activity).pager
                .findViewById(R.id.monitor_list);

        InstantAdapter<RealtimeMonitor> itadp = new InstantAdapter<RealtimeMonitor>(
                activity.getBaseContext(),
                R.layout.list_transaction_monitor_item, RealtimeMonitor.class,
                monitorlist);

        lstv.setAdapter(itadp);
        lstv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Log.v(HTAG, "position:" + position + ",id:" + id);
                if (monitorlist.get(position).isShowBit()) {
                    CustomDialoger.switchersDialog(activity, (byte) 0x11,
                            (byte) 0x22).show();
                }

            }
        });
    }
}
