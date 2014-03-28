package com.kio.ElevatorControl.handlers;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import com.hbluetooth.HHandler;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.models.HistoryError;
import com.kio.ElevatorControl.views.dialogs.CustomDialog;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.ProgressBar;
import org.holoeverywhere.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-21.
 * Time: 11:00.
 */
public class HistoryErrorHandler extends HHandler {

    private ListView listView;

    private List<HistoryError> errorList;

    private ProgressBar progressBar;

    private LinearLayout historyView;

    private HistoryAdapter adapter;

    public HistoryErrorHandler(Activity activity) {
        super(activity);
        TAG = FailureLogHandler.class.getSimpleName();
    }

    @Override
    public void onMultiTalkBegin(Message msg) {
        super.onMultiTalkBegin(msg);
        errorList = new ArrayList<HistoryError>();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        if (progressBar != null && historyView != null) {
            progressBar.setVisibility(View.VISIBLE);
            historyView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onMultiTalkEnd(Message msg) {
        super.onMultiTalkEnd(msg);
        if (progressBar == null) {
            progressBar = (ProgressBar) activity.findViewById(R.id.load_history_progress_bar);
        }
        if (historyView == null) {
            historyView = (LinearLayout) activity.findViewById(R.id.history_view);
        }
        if (listView == null) {
            listView = (ListView) activity.findViewById(R.id.history_list);
        }
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
        adapter = new HistoryAdapter();
        listView.setAdapter(adapter);
        progressBar.setVisibility(View.INVISIBLE);
        historyView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onTalkReceive(Message msg) {
        if (msg.obj != null && (msg.obj instanceof HistoryError)) {
            HistoryError historyError = (HistoryError) msg.obj;
            if (!historyError.isNoError()) {
                errorList.add((HistoryError) msg.obj);
            }
        }
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
            ViewHolder holder = null;
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
