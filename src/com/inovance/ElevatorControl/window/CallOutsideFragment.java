package com.inovance.elevatorcontrol.window;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;

import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.views.TypefaceTextView;

import java.util.List;

public class CallOutsideFragment extends Fragment {

    private GridViewAdapter mGridViewAdapter;

    /**
     * 最高层和最低层
     */
    private int[] mFloors;

    public static interface OnCallUpListener {
        void onCallUp(int[] floors, int index);
    }

    public static interface OnCallDownListener {
        void onCallDown(int[] floors, int index);
    }

    private OnCallUpListener mOnCallUpListener;

    private OnCallDownListener mOnCallDownListener;

    private List<Integer[]> mFloorCallStatus;

    public void setOnCallUpListener(OnCallUpListener listener) {
        mOnCallUpListener = listener;
    }

    public void setOnCallDownListener(OnCallDownListener listener) {
        mOnCallDownListener = listener;
    }

    public void removeListener() {
        mOnCallUpListener = null;
        mOnCallDownListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        View view = getLayoutInflater(savedInstanceState).inflate(R.layout.call_outside_window_fragment, container, false);
        GridView gridView = (GridView) view.findViewById(R.id.grid_view);
        mGridViewAdapter = new GridViewAdapter();
        gridView.setAdapter(mGridViewAdapter);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        removeListener();
    }

    public void updateFloors(int[] floors) {
        mFloors = floors;
        mGridViewAdapter.notifyDataSetChanged();
    }

    public void updateFloorCallStatus(List<Integer[]> statusList) {
        mFloorCallStatus = statusList;
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
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            LayoutInflater mInflater = LayoutInflater.from(getActivity());
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.call_outside_window_item, null);
                holder = new ViewHolder();
                holder.floorTextView = (TypefaceTextView) convertView.findViewById(R.id.floor_text);
                holder.callUpButton = (ImageButton) convertView.findViewById(R.id.call_up);
                holder.callDownButton = (ImageButton) convertView.findViewById(R.id.call_down);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            int floor = Math.min(mFloors[0], mFloors[1]) + position;
            holder.floorTextView.setText(String.valueOf(floor));
            holder.callUpButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mOnCallUpListener != null) {
                        mOnCallUpListener.onCallUp(mFloors, position);
                    }
                }
            });
            holder.callDownButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mOnCallDownListener != null) {
                        mOnCallDownListener.onCallDown(mFloors, position);
                    }
                }
            });

            holder.callUpButton.setEnabled(true);
            holder.callUpButton.setImageResource(R.drawable.call_up_button);

            holder.callDownButton.setEnabled(true);
            holder.callDownButton.setImageResource(R.drawable.call_down_button);

            if (mFloorCallStatus != null) {
                for (Integer[] status : mFloorCallStatus) {
                    if (status.length == 3) {
                        if (status[0] == floor) {
                            if (status[1] == 0) {
                                // 未上召
                                holder.callUpButton.setEnabled(true);
                                holder.callUpButton.setImageResource(R.drawable.icon_call_up);
                            } else {
                                // 已上召
                                holder.callUpButton.setEnabled(false);
                                holder.callUpButton.setImageResource(R.drawable.icon_call_up_highlighted);
                            }
                            if (status[2] == 0) {
                                // 未下召
                                holder.callDownButton.setEnabled(true);
                                holder.callDownButton.setImageResource(R.drawable.icon_call_down);
                            } else {
                                // 已下召
                                holder.callDownButton.setEnabled(false);
                                holder.callDownButton.setImageResource(R.drawable.icon_call_down_highlighted);
                            }
                            break;
                        }
                    }
                }
            }

            if (position == 0) {
                holder.callUpButton.setVisibility(View.VISIBLE);
                holder.callDownButton.setVisibility(View.GONE);
            } else if (position == getCount() - 1) {
                holder.callUpButton.setVisibility(View.GONE);
                holder.callDownButton.setVisibility(View.VISIBLE);
            } else {
                holder.callUpButton.setVisibility(View.VISIBLE);
                holder.callDownButton.setVisibility(View.VISIBLE);
            }

            return convertView;
        }

        private class ViewHolder {
            TypefaceTextView floorTextView;
            ImageButton callUpButton;
            ImageButton callDownButton;
        }
    }
}
