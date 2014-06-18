package com.inovance.ElevatorControl.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.models.ParameterStatusItem;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-28.
 * Time: 13:28.
 */
public class DialogSwitchListViewAdapter extends BaseAdapter {

    private List<ParameterStatusItem> itemList;

    private List<ParameterStatusItem> statusItemList;

    private Context context;

    public DialogSwitchListViewAdapter(List<ParameterStatusItem> list, Context context) {
        this.itemList = list;
        this.statusItemList = list;
        this.context = context;
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
        LayoutInflater mInflater = LayoutInflater.from(context);
        convertView = mInflater.inflate(R.layout.parameter_switch_item, null);
        TextView switchName = (TextView) convertView.findViewById(R.id.switch_name);
        Switch switchView = (Switch) convertView.findViewById(R.id.status_switch);
        final ParameterStatusItem item = getItem(position);
        switchName.setText(item.getName());
        switchView.setChecked(item.getStatus());
        if (item.isSpecial()) {
            switchView.setTextOn(context.getResources().getString(R.string.always_open_text));
            switchView.setTextOff(context.getResources().getString(R.string.always_close_text));
        }
        final int index = position;
        switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean status) {
                statusItemList.get(index).setStatus(status);
            }
        });
        return convertView;
    }

}
