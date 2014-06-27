package com.inovance.ElevatorControl.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import com.bluetoothtool.BluetoothTool;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.activities.FirmwareManageActivity;
import com.inovance.ElevatorControl.models.Firmware;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-13.
 * Time: 14:02.
 */
public class FirmwareBurnAdapter extends BaseAdapter {

    private FirmwareManageActivity baseActivity;

    private List<Firmware> firmwareLists;

    public FirmwareBurnAdapter(FirmwareManageActivity activity, List<Firmware> lists) {
        baseActivity = activity;
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
        LayoutInflater mInflater = LayoutInflater.from(baseActivity);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.firmware_burn_item, null);
            holder = new ViewHolder();
            holder.firmwareName = (TextView) convertView.findViewById(R.id.firmware_name);
            holder.createDate = (TextView) convertView.findViewById(R.id.create_date);
            holder.expireDate = (TextView) convertView.findViewById(R.id.expire_date);
            holder.residueTime = (TextView) convertView.findViewById(R.id.residue_time);
            holder.moreOption = (ImageButton) convertView.findViewById(R.id.more_option);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final int index = position;
        final Firmware firmware = getItem(position);
        holder.firmwareName.setText(firmware.getFileURL());
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        holder.createDate.setText(firmware.getDownloadDate());
        String timeString = dateFormat.format(new Date(Long.parseLong(firmware.getExpireDate()) * 1000));
        holder.expireDate.setText(timeString);
        int residueBurnTime = firmware.getTotalBurnTimes() - firmware.getBurnTimes();
        holder.residueTime.setText(String.valueOf(residueBurnTime));
        holder.moreOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BluetoothTool.getInstance().isPrepared()) {
                    baseActivity.onClickFirmwareBurnItemMoreOption(view, index, firmware);
                }
            }
        });
        return convertView;
    }

    // ====================== View Holder ==================================

    private class ViewHolder {
        TextView firmwareName;
        TextView createDate;
        TextView expireDate;
        TextView residueTime;
        ImageButton moreOption;
    }
}
