package com.inovance.ElevatorControl.activities;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import butterknife.InjectView;
import butterknife.Views;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.models.ParameterGroupSettings;
import com.inovance.ElevatorControl.models.ParameterSettings;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.BaseExpandableListAdapter;
import org.holoeverywhere.widget.ExpandableListView;
import org.holoeverywhere.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * 查看已保存的参数内容
 * User: keith.
 * Date: 14-4-30.
 * Time: 14:25.
 */
public class ParameterViewerActivity extends Activity {

    private final static String DIRECTORY_NAME = "Profile";

    /**
     * Expandable ListView
     */
    @InjectView(R.id.expandable_list_view)
    ExpandableListView expandableListView;

    private List<ParameterGroupSettings> groupList;

    private Handler handler;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String profileName = getIntent().getStringExtra("profileName");
        setTitle(profileName);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setContentView(R.layout.activity_parameter_viewer_layout);
        Views.inject(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        readFileAndParseJSONString(profileName);
        handler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                if (msg.obj != null && msg.obj instanceof ListHolder) {
                    groupList = ((ListHolder) msg.obj).getSettingsList();
                    ExpandableAdapter adapter = new ExpandableAdapter();
                    expandableListView.setGroupIndicator(null);
                    expandableListView.setAdapter(adapter);
                }
            }

        };
    }

    /**
     * 读取参数配置文件并生成List
     *
     * @param fileName 文件名
     */
    private void readFileAndParseJSONString(final String fileName) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    File directory = new File(getApplicationContext().getExternalCacheDir().getPath()
                            + "/"
                            + DIRECTORY_NAME);
                    File file = new File(directory, fileName);
                    try {
                        InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file));
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                        String receiveString = "";
                        StringBuilder stringBuilder = new StringBuilder();
                        while ((receiveString = bufferedReader.readLine()) != null) {
                            stringBuilder.append(receiveString);
                        }
                        bufferedReader.close();
                        inputStreamReader.close();
                        JSONArray groups = new JSONArray(stringBuilder.toString());
                        int size = groups.length();
                        List<ParameterGroupSettings> groupList = new ArrayList<ParameterGroupSettings>();
                        for (int m = 0; m < size; m++) {
                            JSONObject groupObject = groups.getJSONObject(m);
                            ParameterGroupSettings groupItem = new ParameterGroupSettings(groupObject);
                            JSONArray settingArray = groupObject.getJSONArray("parameterSettings");
                            int length = settingArray.length();
                            List<ParameterSettings> settingsList = new ArrayList<ParameterSettings>();
                            for (int n = 0; n < length; n++) {
                                JSONObject settingObject = settingArray.getJSONObject(n);
                                ParameterSettings settings = new ParameterSettings(settingObject);
                                settingsList.add(settings);
                            }
                            groupItem.setSettingsList(settingsList);
                            groupList.add(groupItem);
                        }
                        Message message = new Message();
                        ListHolder holder = new ListHolder();
                        holder.setSettingsList(groupList);
                        message.obj = holder;
                        handler.sendMessage(message);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * ExpandableAdapter
     */
    private class ExpandableAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return ParameterViewerActivity.this.groupList.size();
        }

        @Override
        public int getChildrenCount(int groupID) {
            return ParameterViewerActivity.this.groupList.get(groupID).getSettingsList().size();
        }

        @Override
        public ParameterGroupSettings getGroup(int groupID) {
            return ParameterViewerActivity.this.groupList.get(groupID);
        }

        @Override
        public ParameterSettings getChild(int groupID, int childID) {
            return ParameterViewerActivity.this.groupList.get(groupID).getSettingsList().get(childID);
        }

        @Override
        public long getGroupId(int groupID) {
            return groupID;
        }

        @Override
        public long getChildId(int groupID, int childID) {
            return childID;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupID, boolean isExpanded, View convertView, ViewGroup viewGroup) {
            ParameterGroupSettings item = ParameterViewerActivity.this.groupList.get(groupID);
            GroupViewHolder holder;
            LayoutInflater mInflater = LayoutInflater.from(ParameterViewerActivity.this);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.parameter_viewer_group_item, null);
                holder = new GroupViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.group_title);
                holder.indicator = (ImageView) convertView.findViewById(R.id.indicator);
                convertView.setTag(holder);
            } else {
                holder = (GroupViewHolder) convertView.getTag();
            }
            holder.title.setText(item.getGroupText());
            if (isExpanded) {
                holder.indicator.setImageResource(R.drawable.ic_expand);
            } else {
                holder.indicator.setImageResource(R.drawable.ic_collapse);
            }
            return convertView;
        }

        @Override
        public View getChildView(int groupID, int childID, boolean b, View convertView, ViewGroup viewGroup) {
            ParameterSettings item = ParameterViewerActivity.this.groupList.get(groupID)
                    .getSettingsList().get(childID);
            ChildViewHolder holder;
            LayoutInflater mInflater = LayoutInflater.from(ParameterViewerActivity.this);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.parameter_viewer_setting_item, null);
                holder = new ChildViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.setting_title);
                holder.value = (TextView) convertView.findViewById(R.id.setting_value);
                convertView.setTag(holder);
            } else {
                holder = (ChildViewHolder) convertView.getTag();
            }
            holder.title.setText(item.getCodeText() + " " + item.getName());
            try {
                holder.value.setText(Integer.parseInt(item.getUserValue())
                        * Integer.parseInt(item.getScale()) + item.getUnit());
            } catch (Exception e) {
                double doubleValue = Double.parseDouble(item.getUserValue()) * Double.parseDouble(item.getScale());
                holder.value.setText(String.format("%." + (item.getScale().length() - 2) + "f", doubleValue)
                        + item.getUnit());
            }
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int i, int i2) {
            return false;
        }

        /**
         * Group item view holder
         */
        private class GroupViewHolder {
            TextView title;
            ImageView indicator;
        }

        /**
         * Child item view holder
         */
        private class ChildViewHolder {
            TextView title;
            TextView value;
        }
    }

    private class ListHolder {

        private List<ParameterGroupSettings> settingsList;

        public List<ParameterGroupSettings> getSettingsList() {
            return settingsList;
        }

        public void setSettingsList(List<ParameterGroupSettings> settingsList) {
            this.settingsList = settingsList;
        }
    }

}