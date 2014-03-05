package com.kio.ElevatorControl.handlers;

import android.app.Activity;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;
import com.hbluetooth.HHandler;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.activities.ParameterGroupActivity;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.views.dialogs.CustomDialog;
import com.mobsandgeeks.adapters.InstantAdapter;

import java.util.ArrayList;
import java.util.List;

public class ParameterHandler extends HHandler {

    private boolean talking_success = false;

    private List<ParameterSettings> parameterSettingses = new ArrayList<ParameterSettings>();

    public ParameterHandler(Activity activity) {
        super(activity);
        TAG = "ParameterHandler";
    }

    @Override
    public void onTalkReceive(Message msg) {
        talking_success = true;
        if (msg.obj instanceof ParameterSettings) {
            ParameterSettings p = (ParameterSettings) msg.obj;

            parameterSettingses.add((ParameterSettings) p);

            InstantAdapter<ParameterSettings> instantAdapter = new InstantAdapter<ParameterSettings>(
                    activity, R.layout.list_parameter_group_item,
                    ParameterSettings.class, parameterSettingses);
            ListView lv = ((ParameterGroupActivity) activity).parameterGroupSettingsList;
            lv.setAdapter(instantAdapter);
            lv.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View view,
                                        int pos, long i) {
                    CustomDialog.parameterSettingDialog(activity, parameterSettingses.get(pos)).show();
                }
            });


        }

    }

    @Override
    public void onMultiTalkBegin(Message msg) {
        super.onMultiTalkBegin(msg);
        talking_success = false;
        parameterSettingses.clear();
    }

    @Override
    public void onMultiTalkEnd(Message msg) {
        super.onMultiTalkEnd(msg);
        if (!talking_success) {
            Toast.makeText(
                    activity,
                    activity.getResources().getString(
                            R.string.error_no_data_received),
                    Toast.LENGTH_SHORT).show();
        }
    }

}
