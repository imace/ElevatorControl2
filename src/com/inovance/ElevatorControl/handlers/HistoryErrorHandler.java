package com.inovance.ElevatorControl.handlers;

import android.app.Activity;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.bluetoothtool.BluetoothHandler;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.activities.TroubleAnalyzeActivity;
import com.inovance.ElevatorControl.models.HistoryError;
import com.inovance.ElevatorControl.models.ObjectListHolder;
import com.inovance.ElevatorControl.models.ParameterSettings;
import com.mobsandgeeks.adapters.InstantAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-21.
 * Time: 11:00.
 */
public class HistoryErrorHandler extends BluetoothHandler {

    private List<HistoryError> errorList;

    private List<ParameterSettings> parameterSettingsList;

    public int sendCount;

    public int receiveCount;

    private InstantAdapter instantAdapter;

    public HistoryErrorHandler(Activity activity) {
        super(activity);
        TAG = HistoryErrorHandler.class.getSimpleName();
    }

    @Override
    public void onMultiTalkBegin(Message msg) {
        super.onMultiTalkBegin(msg);
        receiveCount = 0;
        errorList = new ArrayList<HistoryError>();
        parameterSettingsList = new ArrayList<ParameterSettings>();
    }

    @Override
    public void onMultiTalkEnd(Message msg) {
        super.onMultiTalkEnd(msg);
        if (sendCount == receiveCount) {
            ViewPager pager = ((TroubleAnalyzeActivity) activity).pager;
            View loadView = pager.findViewById(R.id.history_load_view);
            View errorView = pager.findViewById(R.id.history_error_view);
            View noErrorView = pager.findViewById(R.id.history_no_error_view);
            View noDeviceView = pager.findViewById(R.id.history_no_device_view);
            ListView listView = (ListView) pager.findViewById(R.id.history_error_list);
            if (loadView != null && errorView != null && noErrorView != null
                    && listView != null && noDeviceView != null) {
                if (instantAdapter == null) {
                    instantAdapter = new InstantAdapter<ParameterSettings>(activity,
                            R.layout.list_parameter_group_item,
                            ParameterSettings.class,
                            parameterSettingsList);
                    listView.setAdapter(instantAdapter);
                } else {
                    instantAdapter.notifyDataSetChanged();
                }
                loadView.setVisibility(View.GONE);
                noErrorView.setVisibility(View.GONE);
                noDeviceView.setVisibility(View.GONE);
                errorView.setVisibility(View.VISIBLE);
            }
            /*
            int size = parameterSettingsList.size();
            if (size % 4 == 0) {
                for (int index = 0; index < size / 4; index++) {
                    byte[] data00 = parameterSettingsList.get(index * 4).getReceived();
                    byte[] data01 = parameterSettingsList.get(index * 4 + 1).getReceived();
                    byte[] data02 = parameterSettingsList.get(index * 4 + 2).getReceived();
                    byte[] data03 = parameterSettingsList.get(index * 4 + 3).getReceived();
                    byte[] errorData = new byte[]{
                            data00[4], data00[5],
                            data01[4], data01[5],
                            data02[4], data02[5],
                            data03[4], data03[5]
                    };
                    HistoryError historyError = new HistoryError();
                    historyError.setData(errorData);
                    if (!historyError.isNoError()) {
                        errorList.add(historyError);
                    }
                }
                ViewPager pager = ((TroubleAnalyzeActivity) activity).pager;
                View loadView = pager.findViewById(R.id.history_load_view);
                View errorView = pager.findViewById(R.id.history_error_view);
                View noErrorView = pager.findViewById(R.id.history_no_error_view);
                View noDeviceView = pager.findViewById(R.id.history_no_device_view);
                ListView listView = (ListView) pager.findViewById(R.id.history_error_list);
                if (loadView != null && errorView != null && noErrorView != null
                        && listView != null && noDeviceView != null) {
                    if (errorList.size() > 0) {
                        if (listView.getOnItemClickListener() == null) {
                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                    final HistoryError historyError = errorList.get(i);
                                    AlertDialog.Builder builder = CustomDialog.historyErrorDialog(historyError,
                                            HistoryErrorHandler.this.activity);
                                    AlertDialog dialog = builder.create();
                                    dialog.show();
                                }
                            });
                        }
                        HistoryAdapter adapter = new HistoryAdapter();
                        listView.setAdapter(adapter);
                        loadView.setVisibility(View.GONE);
                        noErrorView.setVisibility(View.GONE);
                        noDeviceView.setVisibility(View.GONE);
                        errorView.setVisibility(View.VISIBLE);
                    } else {
                        loadView.setVisibility(View.GONE);
                        noDeviceView.setVisibility(View.GONE);
                        errorView.setVisibility(View.GONE);
                        noErrorView.setVisibility(View.VISIBLE);
                    }
                }
            }
            */
        }
        ((TroubleAnalyzeActivity) activity).isSyncing = false;
    }

    @Override
    public void onTalkReceive(Message msg) {
        super.onTalkReceive(msg);
        if (msg.obj != null && (msg.obj instanceof ObjectListHolder)) {
            parameterSettingsList.addAll(((ObjectListHolder) msg.obj).getParameterSettingsList());
            receiveCount++;
        }
    }

    @Override
    public void onTalkError(Message msg) {
        super.onTalkError(msg);
    }

    // ================================ History List View Adapter =========================================

    /**
     * History List View Adapter
     */
    private class HistoryAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return HistoryErrorHandler.this.errorList.size();
        }

        @Override
        public HistoryError getItem(int i) {
            return HistoryErrorHandler.this.errorList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            ViewHolder holder;
            LayoutInflater mInflater = LayoutInflater.from(HistoryErrorHandler.this.activity);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.history_error_item, null);
                holder = new ViewHolder();
                holder.errorCode = (TextView) convertView.findViewById(R.id.history_error_code);
                holder.errorFloor = (TextView) convertView.findViewById(R.id.history_error_floor);
                holder.errorDate = (TextView) convertView.findViewById(R.id.history_error_date);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            HistoryError historyError = getItem(i);
            holder.errorCode.setText(historyError.getErrorCode());
            holder.errorFloor.setText(historyError.getErrorFloor());
            holder.errorDate.setText(historyError.getErrorDateTime());
            return convertView;
        }

        private class ViewHolder {
            TextView errorCode;
            TextView errorFloor;
            TextView errorDate;
        }

    }

}
