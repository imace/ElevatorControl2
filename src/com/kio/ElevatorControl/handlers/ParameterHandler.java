package com.kio.ElevatorControl.handlers;

import android.app.Activity;
import android.os.Message;
import android.view.View;
import android.widget.Toast;
import com.hbluetooth.HHandler;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.activities.ParameterDetailActivity;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.mobsandgeeks.adapters.InstantAdapter;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

public class ParameterHandler extends HHandler {

    private boolean talkingSuccess = false;

    private ListView listView;

    public List<ParameterSettings> parameters = new ArrayList<ParameterSettings>();

    public InstantAdapter<ParameterSettings> parameterSettingsAdapter;

    private ProgressBar progressBar;

    public ParameterHandler(Activity activity) {
        super(activity);
        TAG = ParameterHandler.class.getSimpleName();
    }

    @Override
    public void onMultiTalkBegin(Message msg) {
        super.onMultiTalkBegin(msg);
        talkingSuccess = false;
        if (parameterSettingsAdapter != null) {
            parameterSettingsAdapter.clear();
        }
    }

    @Override
    public void onMultiTalkEnd(Message msg) {
        super.onMultiTalkEnd(msg);
        if (!talkingSuccess) {
            Toast.makeText(activity,
                    activity.getResources().getString(R.string.error_no_data_received),
                    Toast.LENGTH_SHORT).show();
        } else {
            if (progressBar == null) {
                progressBar = ((ParameterDetailActivity) activity).progressBar;
            }
            if (listView == null) {
                listView = ((ParameterDetailActivity) activity).parameterDetailListView;
            }
            if (parameterSettingsAdapter == null) {
                parameterSettingsAdapter = new InstantAdapter<ParameterSettings>(activity,
                        R.layout.list_parameter_group_item,
                        ParameterSettings.class,
                        parameters);
            }
            listView.setAdapter(parameterSettingsAdapter);
            progressBar.setVisibility(View.INVISIBLE);
            listView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onTalkReceive(Message msg) {
        talkingSuccess = true;
        if (msg.obj instanceof ParameterSettings) {
            ParameterSettings settings = (ParameterSettings) msg.obj;
            if (!settings.getName().contains(ApplicationConfig.RETAIN_NAME)) {
                parameters.add(settings);
            }
        }
    }

}
