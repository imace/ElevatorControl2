package com.kio.ElevatorControl.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.models.Firmware;
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
        ViewHolder holder = null;
        LayoutInflater mInflater = LayoutInflater.from(mContext);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.firmware_download_item, null);
            holder = new ViewHolder();
            holder.firmwareName = (TextView) convertView.findViewById(R.id.firmware_name);
            holder.firmwareVersion = (TextView) convertView.findViewById(R.id.firmware_version);
            holder.firmwareDownloadStatus = (TextView) convertView.findViewById(R.id.firmware_download_status);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Firmware firmware = getItem(position);
        holder.firmwareName.setText(firmware.getName());
        holder.firmwareVersion.setText(firmware.getVersion());
        holder.firmwareDownloadStatus.setText(firmware.getDownloadStatusText());
        return convertView;
    }

    // ====================== View Holder ==================================

    private class ViewHolder {
        TextView firmwareName;
        TextView firmwareVersion;
        TextView firmwareDownloadStatus;
    }

}
