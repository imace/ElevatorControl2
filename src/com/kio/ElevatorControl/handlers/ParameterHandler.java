package com.kio.ElevatorControl.handlers;

import android.app.Activity;
import android.os.Message;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;
import com.hbluetooth.HHandler;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.activities.ParameterDetailActivity;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.mobsandgeeks.adapters.InstantAdapter;

import java.util.ArrayList;
import java.util.List;

public class ParameterHandler extends HHandler {

    private boolean talkingSuccess = false;

    public List<ParameterSettings> parameters = new ArrayList<ParameterSettings>();

    public InstantAdapter<ParameterSettings> parameterSettingsAdapter;

    public ParameterHandler(Activity activity) {
        super(activity);
        TAG = ParameterHandler.class.getSimpleName();
    }

    @Override
    public void onTalkReceive(Message msg) {
        talkingSuccess = true;
        Log.v(TAG, String.valueOf(msg.obj));
        if (msg.obj instanceof ParameterSettings) {
            ParameterSettings settings = (ParameterSettings) msg.obj;
            parameters.add(settings);
            if (parameters.size() <= 1) {
                ListView listView = ((ParameterDetailActivity) activity).parameterGroupSettingsList;
                parameterSettingsAdapter = new InstantAdapter<ParameterSettings>(activity,
                        R.layout.list_parameter_group_item,
                        ParameterSettings.class,
                        parameters);
                listView.setAdapter(parameterSettingsAdapter);
            } else {
                if (parameterSettingsAdapter == null) {
                    parameterSettingsAdapter = new InstantAdapter<ParameterSettings>(activity,
                            R.layout.list_parameter_group_item,
                            ParameterSettings.class,
                            parameters);
                }
                parameterSettingsAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onMultiTalkBegin(Message msg) {
        super.onMultiTalkBegin(msg);
        talkingSuccess = false;
        parameterSettingsAdapter.clear();
    }

    @Override
    public void onMultiTalkEnd(Message msg) {
        super.onMultiTalkEnd(msg);
        if (!talkingSuccess) {
            Toast.makeText(
                    activity,
                    activity.getResources().getString(
                            R.string.error_no_data_received),
                    Toast.LENGTH_SHORT).show();
        }
    }

}
