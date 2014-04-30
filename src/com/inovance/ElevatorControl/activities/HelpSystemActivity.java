package com.inovance.ElevatorControl.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import butterknife.InjectView;
import butterknife.Views;
import com.inovance.ElevatorControl.R;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.TextView;

/**
 * 帮助
 */
public class HelpSystemActivity extends Activity {

    @InjectView(R.id.system_list)
    ListView systemSettingListView;

    @InjectView(R.id.other_list)
    ListView otherSettingListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_system);
        Views.inject(this);
        String[] systemArray = getResources().getStringArray(R.array.system_setting_array);
        String[] otherArray = getResources().getStringArray(R.array.other_setting_array);
        systemSettingListView.setAdapter(new SettingListAdapter(systemArray));
        systemSettingListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        startActivity(new Intent(HelpSystemActivity.this, ShortcutSettingActivity.class));
                        break;
                    case 1:
                        break;
                    case 2:
                        startActivity(new Intent(HelpSystemActivity.this, RemoteHelpActivity.class));
                        break;
                    case 3:
                        startActivity(new Intent(HelpSystemActivity.this, BluetoothAddressActivity.class));
                        break;
                }
            }
        });
        otherSettingListView.setAdapter(new SettingListAdapter(otherArray));
        otherSettingListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        startActivity(new Intent(HelpSystemActivity.this, BarcodeCaptureActivity.class));
                        break;
                    case 1:
                        break;
                    case 2:
                        startActivity(new Intent(HelpSystemActivity.this, AboutActivity.class));
                        break;
                }
            }
        });

    }

    private class SettingListAdapter extends BaseAdapter {

        private String[] settingArray;

        public SettingListAdapter(String[] stringArray) {
            settingArray = stringArray;
        }

        @Override
        public int getCount() {
            return settingArray.length;
        }

        @Override
        public String getItem(int position) {
            return settingArray[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            LayoutInflater mInflater = LayoutInflater.from(HelpSystemActivity.this);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.setting_list_item, null);
                holder = new ViewHolder();
                holder.settingTitle = (TextView) convertView.findViewById(R.id.setting_title);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.settingTitle.setText(getItem(position));
            return convertView;
        }

        private class ViewHolder {
            TextView settingTitle;
        }
    }
}
