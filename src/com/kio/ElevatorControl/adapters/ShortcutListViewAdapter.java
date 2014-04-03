package com.kio.ElevatorControl.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.models.Shortcut;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.TextView;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-3.
 * Time: 11:32.
 */
public class ShortcutListViewAdapter extends BaseAdapter {

    private List<Shortcut> shortcutList;

    private static interface sss {

    }

    private Activity baseActivity;

    public ShortcutListViewAdapter(Activity activity, List<Shortcut> list) {
        this.baseActivity = activity;
        this.shortcutList = list;
    }

    public void setShortcutList(List<Shortcut> list) {
        this.shortcutList = list;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return shortcutList.size();
    }

    @Override
    public Shortcut getItem(int position) {
        return shortcutList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        LayoutInflater mInflater = LayoutInflater.from(baseActivity);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_shortcut_item, null);
            holder = new ViewHolder();
            holder.mShortcutName = (TextView) convertView.findViewById(R.id.shortcut);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Shortcut item = getItem(position);
        holder.mShortcutName.setText(item.getName());
        return convertView;
    }

    private class ViewHolder {
        TextView mShortcutName;
    }
}