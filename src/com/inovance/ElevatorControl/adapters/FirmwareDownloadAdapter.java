package com.inovance.ElevatorControl.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.models.Firmware;
import org.holoeverywhere.widget.TextView;

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
        void onClick(int position, Firmware firmware);
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
            holder.approveDate = (TextView) convertView.findViewById(R.id.approve_date);
            holder.downloadButton = convertView.findViewById(R.id.download_button);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final Firmware firmware = getItem(position);
        final int index = position;
        holder.approveDate.setText(firmware.getApproveDate());
        holder.downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (clickListener != null) {
                    clickListener.onClick(index, firmware);
                }
            }
        });
        return convertView;
    }

    // ====================== View Holder ==================================

    private class ViewHolder {
        TextView approveDate;
        View downloadButton;
    }

}
