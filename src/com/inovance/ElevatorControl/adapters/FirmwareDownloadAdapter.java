package com.inovance.elevatorcontrol.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.models.Firmware;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-13.
 * Time: 14:02.
 */
public class FirmwareDownloadAdapter extends BaseAdapter {

    private List<Firmware> firmwareLists;

    private Context mContext;

    private onDownloadButtonClickListener clickListener;

    public static interface onDownloadButtonClickListener {
        void onClick(View view, int position, Firmware firmware);
    }

    public void setOnDownloadButtonClickListener(onDownloadButtonClickListener listener) {
        this.clickListener = listener;
    }

    public FirmwareDownloadAdapter(Context context, List<Firmware> lists) {
        mContext = context;
        firmwareLists = lists;
    }

    @Override
    public int getCount() {
        return firmwareLists.size();
    }

    @Override
    public Firmware getItem(int i) {
        return firmwareLists.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ViewHolder holder;
        LayoutInflater mInflater = LayoutInflater.from(mContext);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.firmware_download_item, null);
            holder = new ViewHolder();
            holder.firmwareName = (TextView) convertView.findViewById(R.id.firmware_name);
            holder.moreOptionButton = (ImageButton) convertView.findViewById(R.id.more_option);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final Firmware firmware = getItem(position);
        final int index = position;
        holder.firmwareName.setText(firmware.getFileURL());
        holder.moreOptionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (clickListener != null) {
                    clickListener.onClick(view, index, firmware);
                }
            }
        });
        return convertView;
    }

    // ====================== View Holder ==================================

    private class ViewHolder {
        TextView firmwareName;
        ImageButton moreOptionButton;
    }

}
