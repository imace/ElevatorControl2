package com.inovance.elevatorcontrol.window;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;

import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.views.TypefaceTextView;

public class CallInsideFragment extends Fragment {

    private GridViewAdapter mGridViewAdapter;

    public static final int SELECT_NONE = -1;

    /**
     * 电梯最高层和最低层
     */
    private int[] mFloors;

    /**
     * 已召唤的楼层
     */
    private int[] mCalledFloors;

    /**
     * 选中的楼层
     */
    private int selectedIndex = SELECT_NONE;

    /**
     * 选中某个楼层是的时间戳
     */
    private long selectFloorTimeMillis;

    public static interface OnCallFloorListener {
        void onCallFloor(int[] floors, int index);
    }

    private OnCallFloorListener mOnCallFloorListener;

    public void setOnCallFloorListener(OnCallFloorListener listener) {
        mOnCallFloorListener = listener;
    }

    public void removeListener() {
        mOnCallFloorListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        View view = getLayoutInflater(savedInstanceState).inflate(R.layout.call_inside_window_fragment, container, false);
        GridView gridView = (GridView) view.findViewById(R.id.grid_view);
        mGridViewAdapter = new GridViewAdapter();
        gridView.setAdapter(mGridViewAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mOnCallFloorListener != null) {
                    mOnCallFloorListener.onCallFloor(mFloors, position);
                }
            }
        });
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        removeListener();
    }

    /**
     * 更新最高层和最低层信息
     *
     * @param floors 最高层和最低层
     */
    public void updateFloors(int[] floors) {
        mFloors = floors;
        selectedIndex = SELECT_NONE;
        mGridViewAdapter.notifyDataSetChanged();
    }

    public void updateFloorCallStatus(int[] calledFloors) {
        mCalledFloors = calledFloors;
        // 至少保持楼层选中状态 1 秒左右
        if (System.currentTimeMillis() - selectFloorTimeMillis > 1000) {
            selectedIndex = SELECT_NONE;
        }
        mGridViewAdapter.notifyDataSetChanged();
    }

    public void updateSelectedIndex(int index) {
        selectedIndex = index;
        selectFloorTimeMillis = System.currentTimeMillis();
        mGridViewAdapter.notifyDataSetChanged();
    }

    private class GridViewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (mFloors != null && mFloors.length == 2) {
                return Math.abs(mFloors[0] - mFloors[1]) + 1;
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            LayoutInflater mInflater = LayoutInflater.from(getActivity());
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.call_inside_window_item, null);
                holder = new ViewHolder();
                holder.floorTextView = (TypefaceTextView) convertView.findViewById(R.id.floor_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            int floor = Math.min(mFloors[0], mFloors[1]) + position;
            holder.floorTextView.setText(String.valueOf(floor));
            // Check floor is selected
            if (selectedIndex == position) {
                convertView.setBackgroundResource(R.drawable.elevator_button_highlighted);
                holder.floorTextView.setSelected(true);
            } else {
                convertView.setBackgroundResource(R.drawable.elevator_button_background);
                holder.floorTextView.setSelected(false);
            }
            // Check floor is called
            if (mCalledFloors != null) {
                for (int calledFloor : mCalledFloors) {
                    if (calledFloor == floor) {
                        convertView.setBackgroundResource(R.drawable.elevator_button_highlighted);
                        holder.floorTextView.setSelected(true);
                        break;
                    }
                }
            }
            return convertView;
        }

        private class ViewHolder {
            TypefaceTextView floorTextView;
        }
    }
}
