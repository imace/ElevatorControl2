package com.inovance.ElevatorControl.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.models.ParameterStatusItem;
import org.holoeverywhere.widget.Switch;
import org.holoeverywhere.widget.TextView;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-28.
 * Time: 13:28.
 */
public class DialogSwitchListViewAdapter extends BaseAdapter {

    private List<ParameterStatusItem> itemList;

    private Activity baseActivity;

    public DialogSwitchListViewAdapter(List<ParameterStatusItem> list, Activity activity) {
        this.itemList = list;
        this.baseActivity = activity;
    }

    public List<ParameterStatusItem> getItemList() {
        return itemList;
    }

    @Override
    public int getCount() {
        return itemList.size();
    }

    @Override
    public ParameterStatusItem getItem(int position) {
        return itemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ViewHolder holder;
        LayoutInflater mInflater = LayoutInflater.from(baseActivity);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.parameter_switch_item, null);
            holder = new ViewHolder();
            holder.statusName = (TextView) convertView.findViewById(R.id.switch_name);
            holder.statusSwitch = (Switch) convertView.findViewById(R.id.status_switch);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final ParameterStatusItem item = getItem(position);
        holder.statusName.setText(item.getName());
        holder.statusSwitch.setChecked(item.getStatus());
        holder.statusSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean status) {
                item.setStatus(status);
            }
        });
        if (!item.getEditStatus()) {
            holder.statusSwitch.setEnabled(false);
        } else {
            holder.statusSwitch.setEnabled(true);
        }
        return convertView;
    }

    /**
     * View Holder
     */
    private class ViewHolder {
        TextView statusName;
        Switch statusSwitch;
    }

}
