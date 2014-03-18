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
public class FirmwareBurnAdapter extends BaseAdapter {

    private List<Firmware> firmwareLists;

    private Context mContext;

    public FirmwareBurnAdapter(Context context, List<Firmware> lists) {
        mContext = context;
        firmwareLists = lists;
    }

    @Override
    public int getCount() {
        return 0;
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
            convertView = mInflater.inflate(R.layout.firmware_burn_item, null);
            holder = new ViewHolder();
            holder.firmwareVersion = (TextView) convertView.findViewById(R.id.firmware_version);
            holder.updateDate = (TextView) convertView.findViewById(R.id.update_date);
            holder.expireDate = (TextView) convertView.findViewById(R.id.expire_date);
            holder.residueTime = (TextView) convertView.findViewById(R.id.residue_time);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Firmware firmware = getItem(position);
        holder.firmwareVersion.setText(firmware.getVersion());
        holder.updateDate.setText(firmware.getUpdateDate());
        holder.expireDate.setText(firmware.getExpireDate());
        holder.residueTime.setText(firmware.getResidueTime());
        return convertView;
    }

    // ====================== View Holder ==================================

    private class ViewHolder {
        TextView firmwareVersion;
        TextView updateDate;
        TextView expireDate;
        TextView residueTime;
    }
}
