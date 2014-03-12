package com.kio.ElevatorControl.handlers;

import android.app.Activity;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import com.hbluetooth.HHandler;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.models.ErrorHelpLog;
import com.kio.ElevatorControl.views.dialogs.CustomDialog;
import com.mobsandgeeks.adapters.InstantAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 运行在CoreAcivity主线程,仅做一些ui操作
 *
 * @author jch
 */
public class FailureLogHandler extends HHandler {

    private List<ErrorHelpLog> logArrayList = new ArrayList<ErrorHelpLog>();

    public FailureLogHandler(Activity activity) {
        super(activity);
        TAG = FailureLogHandler.class.getSimpleName();
    }

    @Override
    public void onTalkReceive(Message msg) {
        if (msg.obj != null && (msg.obj instanceof ErrorHelpLog)) {
            ErrorHelpLog errorHelpLog = (ErrorHelpLog) msg.obj;
            logArrayList.add(errorHelpLog);
            // 我们要操作的列表控件
            ListView listView = (ListView) activity.findViewById(R.id.failure_history_list);
            InstantAdapter<ErrorHelpLog> instantAdapter = new InstantAdapter<ErrorHelpLog>(activity.getApplicationContext(), R.layout.list_trouble_history_item, ErrorHelpLog.class,
                    logArrayList);
            listView.setAdapter(instantAdapter);
            listView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                    CustomDialog.failureHistoryDialog(logArrayList.get(pos).getErrorHelp(), activity).show();
                }
            });


        }
    }
}
