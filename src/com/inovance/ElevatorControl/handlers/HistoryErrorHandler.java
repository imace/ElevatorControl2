package com.inovance.elevatorcontrol.handlers;

import android.app.Activity;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import com.inovance.bluetoothtool.BluetoothHandler;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.activities.TroubleAnalyzeActivity;
import com.inovance.elevatorcontrol.factory.ParameterFactory;
import com.inovance.elevatorcontrol.models.ObjectListHolder;
import com.inovance.elevatorcontrol.models.ParameterSettings;
import com.inovance.elevatorcontrol.models.TroubleGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-21.
 * Time: 11:00.
 */
public class HistoryErrorHandler extends BluetoothHandler {

    private List<ParameterSettings> parameterSettingsList;

    private List<TroubleGroup> troubleGroupList;

    public int sendCount;

    public int receiveCount;

    private ExpandableAdapter adapter;

    public HistoryErrorHandler(Activity activity) {
        super(activity);
        TAG = HistoryErrorHandler.class.getSimpleName();
    }

    @Override
    public void onMultiTalkBegin(Message msg) {
        super.onMultiTalkBegin(msg);
        receiveCount = 0;
        troubleGroupList = new ArrayList<TroubleGroup>();
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
            troubleGroupList = ParameterFactory.getParameter().getTroubleGroupList(activity, parameterSettingsList);
            ExpandableListView listView = (ExpandableListView) pager.findViewById(R.id.history_error_list);
            if (loadView != null && errorView != null && noErrorView != null
                    && listView != null && noDeviceView != null) {
                if (adapter == null) {
                    adapter = new ExpandableAdapter();
                    listView.setAdapter(adapter);
                } else {
                    adapter.notifyDataSetChanged();
                }
                loadView.setVisibility(View.GONE);
                noErrorView.setVisibility(View.GONE);
                noDeviceView.setVisibility(View.GONE);
                errorView.setVisibility(View.VISIBLE);
            }
        }
        ((TroubleAnalyzeActivity) activity).isSyncing = false;
        ((TroubleAnalyzeActivity) activity).hasGetHistoryTrouble = true;
    }

    @Override
    public void onTalkReceive(Message msg) {
        super.onTalkReceive(msg);
        if (msg.obj != null && (msg.obj instanceof ObjectListHolder)) {
            parameterSettingsList.addAll(((ObjectListHolder) msg.obj).getParameterSettingsList());
            receiveCount++;
        }
    }

    // ================================ History List View Expand Adapter ========================================= //

    private class ExpandableAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return troubleGroupList.size();
        }

        @Override
        public int getChildrenCount(int groupID) {
            return troubleGroupList.get(groupID).getTroubleChildList().size();
        }

        @Override
        public TroubleGroup getGroup(int groupID) {
            return troubleGroupList.get(groupID);
        }

        @Override
        public ParameterSettings getChild(int groupID, int childID) {
            return getGroup(groupID).getTroubleChildList().get(childID);
        }

        @Override
        public long getGroupId(int groupID) {
            return groupID;
        }

        @Override
        public long getChildId(int groupID, int childID) {
            return childID;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupID, boolean isExpanded, View convertView, ViewGroup viewGroup) {
            TroubleGroup group = getGroup(groupID);
            GroupViewHolder holder;
            LayoutInflater mInflater = LayoutInflater.from(activity);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.history_error_group_item, viewGroup, false);
                holder = new GroupViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.group_title);
                holder.indicator = (ImageView) convertView.findViewById(R.id.indicator);
                convertView.setTag(holder);
            } else {
                holder = (GroupViewHolder) convertView.getTag();
            }
            holder.title.setText(group.getName());
            if (isExpanded) {
                holder.indicator.setImageResource(R.drawable.ic_expand);
            } else {
                holder.indicator.setImageResource(R.drawable.ic_collapse);
            }
            return convertView;
        }

        @Override
        public View getChildView(int groupID, int childID, boolean b, View convertView, ViewGroup viewGroup) {
            ParameterSettings child = getChild(groupID, childID);
            ChildViewHolder holder;
            LayoutInflater mInflater = LayoutInflater.from(activity);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.history_error_child_item, viewGroup, false);
                holder = new ChildViewHolder();
                holder.codeText = (TextView) convertView.findViewById(R.id.code_text);
                holder.nameText = (TextView) convertView.findViewById(R.id.name_text);
                holder.valueText = (TextView) convertView.findViewById(R.id.value_text);
                convertView.setTag(holder);
            } else {
                holder = (ChildViewHolder) convertView.getTag();
            }
            holder.codeText.setText(child.getCodeText());
            holder.nameText.setText(child.getName());
            holder.valueText.setText(child.getFinalValue());
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int i, int i2) {
            return false;
        }

        /**
         * Group item view holder
         */
        private class GroupViewHolder {
            TextView title;
            ImageView indicator;
        }

        /**
         * Child item view holder
         */
        private class ChildViewHolder {
            TextView codeText;
            TextView nameText;
            TextView valueText;
        }
    }

}
