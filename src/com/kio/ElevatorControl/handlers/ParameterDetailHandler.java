package com.kio.ElevatorControl.handlers;

import android.app.Activity;
import android.os.Message;
import android.view.View;
import android.widget.Toast;
import com.hbluetooth.HHandler;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.activities.ParameterDetailActivity;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.models.ListHolder;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.mobsandgeeks.adapters.InstantAdapter;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

public class ParameterDetailHandler extends HHandler {

    private boolean talkingSuccess = false;

    private ListView listView;

    public List<ParameterSettings> parametersList;

    public InstantAdapter<ParameterSettings> parameterSettingsAdapter;

    private ProgressBar progressBar;

    public ParameterDetailHandler(Activity activity) {
        super(activity);
        TAG = ParameterDetailHandler.class.getSimpleName();
    }

    @Override
    public void onMultiTalkBegin(Message msg) {
        super.onMultiTalkBegin(msg);
        talkingSuccess = false;
        parametersList = new ArrayList<ParameterSettings>();
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
                        parametersList);
            }
            listView.setAdapter(parameterSettingsAdapter);
            progressBar.setVisibility(View.INVISIBLE);
            listView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onTalkReceive(Message msg) {
        talkingSuccess = true;
        if (msg.obj != null) {
            if (msg.obj instanceof ParameterSettings) {
                ParameterSettings item = (ParameterSettings) msg.obj;
                if (!item.getName().contains(ApplicationConfig.RETAIN_NAME)) {
                    parametersList.add(item);
                }
            }
            if (msg.obj instanceof ListHolder) {
                ListHolder holder = (ListHolder) msg.obj;
                for (ParameterSettings item : holder.getParameterSettingsList()) {
                    if (!item.getName().contains(ApplicationConfig.RETAIN_NAME)) {
                        parametersList.add(item);
                    }
                }
            }
        }
    }

}
