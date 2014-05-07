package com.inovance.ElevatorControl.adapters;

import android.content.Context;
import android.graphics.Point;
import android.util.TypedValue;
import android.view.*;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.activities.MoveInsideActivity;
import com.inovance.ElevatorControl.activities.MoveOutsideActivity;
import com.inovance.ElevatorControl.views.TypefaceTextView;
import com.inovance.ElevatorControl.views.viewpager.PagerAdapter;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.GridView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-2.
 * Time: 16:02.
 */
public class MoveSidePagerAdapter extends PagerAdapter {

    private int PER_PAGER_SIZE = 12;

    private int[] floorsArray;

    private Activity baseActivity;

    private int contentViewHeight;

    private int selectIndex;

    private boolean firstRender;

    public int currentPager;

    private List<MoveSideGridViewAdapter> gridViewAdapterList;

    public interface onSelectFloorListener {
        void onSelect(int floor);
    }

    private onSelectFloorListener selectListener;

    public void setOnSelectFloorListener(onSelectFloorListener listener) {
        selectListener = listener;
    }

    /**
     * MoveInsideActivity Construct
     *
     * @param activity MoveInsideActivity
     * @param floors   floors array
     */
    public MoveSidePagerAdapter(MoveInsideActivity activity, int[] floors) {
        this.baseActivity = activity;
        this.floorsArray = floors;
        gridViewAdapterList = new ArrayList<MoveSideGridViewAdapter>();
        getContentViewHeight();
    }

    /**
     * MoveOutsideActivity Construct
     *
     * @param activity MoveOutsideActivity
     * @param floors   floors array
     */
    public MoveSidePagerAdapter(MoveOutsideActivity activity, int[] floors) {
        this.baseActivity = activity;
        this.floorsArray = floors;
        gridViewAdapterList = new ArrayList<MoveSideGridViewAdapter>();
        getContentViewHeight();
    }

    /**
     * Update Current Called Floor
     *
     * @param floor Called Floor Array
     */
    public void updateCurrentCalledFloor(List<Integer> floors) {
        int[] intFloors = new int[floors.size()];
        for (int i = 0; i < intFloors.length; i++) {
            intFloors[i] = floors.get(i);
        }
        if (currentPager < gridViewAdapterList.size()) {
            MoveSideGridViewAdapter adapter = gridViewAdapterList.get(currentPager);
            adapter.calledFloors = intFloors;
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public int getCount() {
        int totalFloors = Math.abs(floorsArray[0] - floorsArray[1]) + 1;
        return totalFloors <= PER_PAGER_SIZE ? 1
                : ((totalFloors - totalFloors % PER_PAGER_SIZE) / PER_PAGER_SIZE
                + (totalFloors % PER_PAGER_SIZE == 0 ? 0 : 1));
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        LayoutInflater mInflater = LayoutInflater.from(baseActivity);
        View contentView = mInflater.inflate(R.layout.move_side_view, null);
        firstRender = true;
        GridView gridView = (GridView) contentView.findViewById(R.id.grid_view);
        final MoveSideGridViewAdapter adapter = new MoveSideGridViewAdapter(getFloors(position));
        gridViewAdapterList.add(adapter);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {
                selectIndex = index;
                firstRender = false;
                if (selectListener != null) {
                    int[] floors = getFloors(currentPager);
                    int minFloor = Math.min(floors[0], floors[1]);
                    selectListener.onSelect(minFloor + index);
                }
                adapter.notifyDataSetChanged();
            }
        });
        container.addView(contentView);
        return contentView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    /**
     * Get Floors Int Array
     *
     * @param position Position
     * @return int Array
     */
    private int[] getFloors(int position) {
        int minFloor = Math.min(floorsArray[0], floorsArray[1]);
        int maxFloor = Math.max(floorsArray[0], floorsArray[1]);
        int totalFloors = Math.abs(minFloor - maxFloor) + 1;
        boolean aliquot = totalFloors % PER_PAGER_SIZE == 0;
        int startFloors = position * PER_PAGER_SIZE + minFloor;
        int endFloors = totalFloors <= PER_PAGER_SIZE
                ? maxFloor
                : (aliquot ? PER_PAGER_SIZE * (position + 1) + minFloor - 1 : (position == getCount() - 1
                ? maxFloor : PER_PAGER_SIZE * (position + 1) + minFloor - 1));
        return new int[]{startFloors, endFloors};
    }

    private void getContentViewHeight() {
        WindowManager wm = (WindowManager) baseActivity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        int screenHeight;
        if (android.os.Build.VERSION.SDK_INT >= 13) {
            display.getSize(size);
            screenHeight = size.y;
        } else {
            screenHeight = display.getHeight();
        }
        int actionBarHeight = Math.round(baseActivity.getResources()
                .getDimension(R.dimen.abc_action_bar_default_height));
        int statusBarHeight = 0;
        int statusBarResource = baseActivity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (statusBarResource > 0) {
            statusBarHeight = baseActivity.getResources().getDimensionPixelSize(statusBarResource);
        }
        contentViewHeight = screenHeight - statusBarHeight - actionBarHeight - getPixelFromDimension(180);
    }

    /**
     * Get Pixel From Dimension
     *
     * @param dp Dimension
     * @return Pixel
     */
    private int getPixelFromDimension(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                baseActivity.getResources().getDisplayMetrics()));
    }

    // =========================== Move Side Grid View Adapter ========================================== //
    private class MoveSideGridViewAdapter extends BaseAdapter {

        public int[] calledFloors;

        private int[] floors;

        public MoveSideGridViewAdapter(int[] floorsArray) {
            this.floors = floorsArray;
        }

        @Override
        public int getCount() {
            return Math.abs(floors[0] - floors[1]) + 1;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            ViewHolder holder;
            LayoutInflater mInflater = LayoutInflater.from(MoveSidePagerAdapter.this.baseActivity);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.move_side_item, null);
                convertView.setMinimumHeight(MoveSidePagerAdapter.this.contentViewHeight / 4);
                holder = new ViewHolder();
                holder.floorTextView = (TypefaceTextView) convertView.findViewById(R.id.floor_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.floorTextView.setText(String.valueOf(Math.min(floors[0], floors[1]) + position));
            if (selectIndex == position && !firstRender) {
                convertView.setBackgroundResource(R.drawable.elevator_button_highlighted);
                holder.floorTextView.setSelected(true);
            } else {
                convertView.setBackgroundResource(R.drawable.elevator_button_background);
                holder.floorTextView.setSelected(false);
            }
            if (calledFloors != null && calledFloors.length > 0) {
                int currentFloors = Math.min(floors[0], floors[1]) + position;
                for (int floor : calledFloors) {
                    if (currentFloors == floor) {
                        convertView.setBackgroundResource(R.drawable.elevator_button_highlighted);
                        holder.floorTextView.setSelected(true);
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