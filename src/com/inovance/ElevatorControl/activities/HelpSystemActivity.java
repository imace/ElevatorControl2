package com.inovance.elevatorcontrol.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.inovance.bluetoothtool.BluetoothTool;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.adapters.PreferenceAdapter;
import com.inovance.elevatorcontrol.utils.UpdateApplication;
import com.inovance.elevatorcontrol.window.UnlockWindow;

import butterknife.InjectView;
import butterknife.Views;

/**
 * 帮助标签
 */
public class HelpSystemActivity extends Activity {

    @InjectView(R.id.preference_list)
    ListView preferenceListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setContentView(R.layout.activity_help_system);
        Views.inject(this);

        String[] preferenceArray = getResources().getStringArray(R.array.preference_array);
        PreferenceAdapter adapter = new PreferenceAdapter(this);
        adapter.setItems(preferenceArray);
        adapter.setSectionIndex(new int[]{0, 3, 7});
        preferenceListView.setAdapter(adapter);
        preferenceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1:
                        startActivity(new Intent(HelpSystemActivity.this, ShortcutSettingActivity.class));
                        break;
                    case 2:
                        checkApplicationUpdate();
                        break;
                    case 4:
                        startActivity(new Intent(HelpSystemActivity.this, ZxingScannerActivity.class));
                        break;
                    case 5:
                        startActivity(new Intent(HelpSystemActivity.this, RemoteHelpActivity.class));
                        break;
                    case 6:
                        unlockDevice();
                        break;
                    case 8:
                        startActivity(new Intent(HelpSystemActivity.this, SystemLogActivity.class));
                        break;
                    case 9:
                        startActivity(new Intent(HelpSystemActivity.this, AboutActivity.class));
                        break;
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    /**
     * Unlock device
     */
    private void unlockDevice() {
        if (BluetoothTool.getInstance().isConnected()) {
            startActivity(new Intent(HelpSystemActivity.this, UnlockWindow.class));
        } else {
            Toast.makeText(this, R.string.connect_device_to_unlock, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 检查软件更新
     */
    private void checkApplicationUpdate() {
        Toast.makeText(this, R.string.check_application_update_text, Toast.LENGTH_SHORT).show();
        UpdateApplication.getInstance().init(this);
        UpdateApplication.getInstance().setOnNoUpdateFoundListener(new UpdateApplication.OnNoUpdateFoundListener() {
            @Override
            public void onNoUpdate() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), R.string.installed_last_application_text, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        UpdateApplication.getInstance().checkUpdate();
    }
}
