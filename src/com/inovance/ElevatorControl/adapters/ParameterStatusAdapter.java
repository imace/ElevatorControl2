package com.inovance.elevatorcontrol.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.models.ParameterStatusItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by keith on 14-4-24.
 * User keith
 * Date 14-4-24
 * Time 下午7:35
 */
public class ParameterStatusAdapter extends BaseAdapter {

    private Activity activity;

    private List<ParameterStatusItem> statusItemList = new ArrayList<ParameterStatusItem>();

    private String[] statusTextArray;

    public ParameterStatusAdapter(Activity activity, List<ParameterStatusItem> list) {
        this.activity = activity;
        this.statusItemList = list;
        this.statusTextArray = activity.getResources().getStringArray(R.array.status_text_array);
    }

    public void setStatusList(List<ParameterStatusItem> list) {
        statusItemList.clear();
        statusItemList.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return statusItemList.size();
    }

    @Override
    public ParameterStatusItem getItem(int position) {
        return statusItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        LayoutInflater mInflater = LayoutInflater.from(activity);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.terminal_status_item, null);
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.status_name);
            holder.status = (TextView) convertView.findViewById(R.id.status_value);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        ParameterStatusItem item = getItem(position);
        holder.name.setText(item.getName());
        if (item.getStatusString() != null && item.getStatusString().length() > 0) {
            holder.status.setText(item.getStatusString());
            holder.status.setTextColor(0xff515151);
        } else {
            if (item.isParseFailed()) {
                holder.status.setText("--");
                holder.status.setTextColor(0xffaf0000);
            } else {
                String statusString = item.getStatus() ? statusTextArray[0] : statusTextArray[1];
                holder.status.setText(statusString);
                if (item.getStatus()) {
                    holder.status.setTextColor(0xff4dc11b);
                } else {
                    holder.status.setTextColor(0xff515151);
                }
            }
        }
        return convertView;
    }

    private class ViewHolder {
        TextView name;
        TextView status;
    }
}
