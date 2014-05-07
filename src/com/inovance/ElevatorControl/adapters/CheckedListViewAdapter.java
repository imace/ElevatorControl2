package com.inovance.ElevatorControl.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import com.inovance.ElevatorControl.R;
import org.holoeverywhere.widget.TextView;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-5-6.
 * Time: 16:21.
 */
public class CheckedListViewAdapter extends BaseAdapter {

    private String[] checkList;

    private Activity activity;

    private int checkedIndex;

    public CheckedListViewAdapter(Activity activity, String[] checkList, int checkedIndex) {
        this.activity = activity;
        this.checkList = checkList;
        this.checkedIndex = checkedIndex;
    }

    public void setCheckedIndex(int checkedIndex) {
        this.checkedIndex = checkedIndex;
        this.notifyDataSetChanged();
    }

    public int getCheckedIndex() {
        return checkedIndex;
    }

    @Override
    public int getCount() {
        return checkList == null ? 0 : checkList.length;
    }

    @Override
    public String getItem(int i) {
        return checkList[i];
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ViewHolder holder;
        LayoutInflater mInflater = LayoutInflater.from(activity);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.checked_listview_item, null);
            holder = new ViewHolder();
            holder.checkedTextView = (TextView) convertView.findViewById(R.id.checked_text_view);
            holder.checkedMarkView = (ImageView) convertView.findViewById(R.id.checked_mark);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.checkedTextView.setText(getItem(position));
        if (checkedIndex == position){
            holder.checkedMarkView.setImageResource(R.drawable.btn_radio_on_holo_light);
        }
        else {
            holder.checkedMarkView.setImageResource(R.drawable.btn_radio_off_holo_light);
        }
        return convertView;
    }

    private class ViewHolder {
        TextView checkedTextView;
        ImageView checkedMarkView;
    }
}
