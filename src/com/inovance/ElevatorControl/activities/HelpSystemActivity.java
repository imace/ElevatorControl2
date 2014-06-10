package com.inovance.ElevatorControl.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import butterknife.InjectView;
import butterknife.Views;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.adapters.PreferenceAdapter;

/**
 * 帮助标签
 */
public class HelpSystemActivity extends Activity {

    @InjectView(R.id.preference_list)
    ListView preferenceListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_system);
        Views.inject(this);
        String[] preferenceArray = getResources().getStringArray(R.array.preference_array);
        PreferenceAdapter adapter = new PreferenceAdapter(this);
        adapter.setItems(preferenceArray);
        adapter.setSectionIndex(new int[]{0, 3, 8});
        preferenceListView.setAdapter(adapter);
        preferenceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1:
                        startActivity(new Intent(HelpSystemActivity.this, ShortcutSettingActivity.class));
                        break;
                    case 2:
                        break;
                    case 4:
                        startActivity(new Intent(HelpSystemActivity.this, ApplyPermissionActivity.class));
                        break;
                    case 5:
                        startActivity(new Intent(HelpSystemActivity.this, BarcodeCaptureActivity.class));
                        break;
                    case 6:
                        startActivity(new Intent(HelpSystemActivity.this, RemoteHelpActivity.class));
                        break;
                    case 7:
                        startActivity(new Intent(HelpSystemActivity.this, BluetoothAddressActivity.class));
                        break;
                    case 9:
                        startActivity(new Intent(HelpSystemActivity.this, SystemLogActivity.class));
                        break;
                    case 10:
                        startActivity(new Intent(HelpSystemActivity.this, AboutActivity.class));
                        break;
                }
            }
        });

    }
}
