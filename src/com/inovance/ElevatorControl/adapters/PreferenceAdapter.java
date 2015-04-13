package com.inovance.elevatorcontrol.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.inovance.elevatorcontrol.R;

import java.util.TreeSet;

/**
 * Created by keith on 14-6-7.
 * User keith
 * Date 14-6-7
 * Time 下午2:21
 */
public class PreferenceAdapter extends BaseAdapter {

    private static final int TYPE_ITEM = 0;

    private static final int TYPE_SEPARATOR = 1;

    private LayoutInflater mInflater;

    public PreferenceAdapter(Context context) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    private String[] mData = new String[]{};

    private TreeSet<Integer> sectionHeader = new TreeSet<Integer>();

    public void setItems(String[] items) {
        mData = items;
        notifyDataSetChanged();
    }

    public void setSectionIndex(int[] indexSet) {
        sectionHeader.clear();
        for (int index : indexSet) {
            sectionHeader.add(index);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mData.length;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public String getItem(int position) {
        return mData[position];
    }

    @Override
    public int getItemViewType(int position) {
        return sectionHeader.contains(position) ? TYPE_SEPARATOR : TYPE_ITEM;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        int rowType = getItemViewType(position);
        if (convertView == null) {
            holder = new ViewHolder();
            switch (rowType) {
                case TYPE_ITEM:
                    convertView = mInflater.inflate(R.layout.preference_item, null);
                    holder.textView = (TextView) convertView.findViewById(R.id.title);
                    break;
                case TYPE_SEPARATOR:
                    convertView = mInflater.inflate(R.layout.preference_section_item, null);
                    holder.textView = (TextView) convertView.findViewById(R.id.section_title);
                    break;
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.textView.setText(getItem(position));
        return convertView;
    }

    private static class ViewHolder {
        public TextView textView;
    }
}
