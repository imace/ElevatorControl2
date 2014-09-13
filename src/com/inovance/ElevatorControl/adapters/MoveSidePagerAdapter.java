package com.inovance.elevatorcontrol.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.TypedValue;
import android.view.*;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.activities.MoveInsideActivity;
import com.inovance.elevatorcontrol.activities.MoveOutsideActivity;
import com.inovance.elevatorcontrol.views.TypefaceTextView;
import com.inovance.elevatorcontrol.views.component.ExpandGridView;
import com.inovance.elevatorcontrol.views.viewpager.PagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-2.
 * Time: 16:02.
 */
public class MoveSidePagerAdapter extends PagerAdapter {

    private static final String TAG = MoveSidePagerAdapter.class.getSimpleName();

    private int pagerSize = 9;

    private int[] floorsArray;

    private Activity baseActivity;

    private int verticalSpacing;

    private int selectIndex;

    private boolean firstRender;

    public int currentPager;

    private List<MoveSideGridViewAdapter> gridViewAdapterList;

    public interface OnSelectFloorListener {
        void onSelect(int floor);
    }

    private OnSelectFloorListener selectListener;

    public void setOnSelectFloorListener(OnSelectFloorListener listener) {
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
        calculatorPagerSizeAndVerticalSpacing();
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
        calculatorPagerSizeAndVerticalSpacing();
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
        return totalFloors <= pagerSize ? 1
                : ((totalFloors - totalFloors % pagerSize) / pagerSize
                + (totalFloors % pagerSize == 0 ? 0 : 1));
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        LayoutInflater mInflater = LayoutInflater.from(baseActivity);
        View contentView = mInflater.inflate(R.layout.move_side_view, null);
        firstRender = true;
        ExpandGridView gridView = (ExpandGridView) contentView.findViewById(R.id.grid_view);
        gridView.setVerticalSpacing(verticalSpacing);
        final MoveSideGridViewAdapter adapter = new MoveSideGridViewAdapter(getFloors(position));
        gridViewAdapterList.add(adapter);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
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
        boolean aliquot = totalFloors % pagerSize == 0;
        int startFloors = position * pagerSize + minFloor;
        int endFloors = totalFloors <= pagerSize
                ? maxFloor
                : (aliquot ? pagerSize * (position + 1) + minFloor - 1 : (position == getCount() - 1
                ? maxFloor : pagerSize * (position + 1) + minFloor - 1));
        return new int[]{startFloors, endFloors};
    }

    private void calculatorPagerSizeAndVerticalSpacing() {
        WindowManager windowManager = (WindowManager) baseActivity.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        int screenWidth;
        int screenHeight;
        if (android.os.Build.VERSION.SDK_INT >= 13) {
            display.getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;
        } else {
            screenWidth = display.getWidth();
            screenHeight = display.getHeight();
        }
        int actionBarHeight = Math.round(baseActivity.getResources()
                .getDimension(R.dimen.rai__action_bar_default_height));
        int statusBarHeight = 0;
        int statusBarResource = baseActivity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (statusBarResource > 0) {
            statusBarHeight = baseActivity.getResources().getDimensionPixelSize(statusBarResource);
        }
        int margin = getPixelFromDimension(10);
        int space = getPixelFromDimension(130);
        int contentViewHeight = screenHeight - statusBarHeight - actionBarHeight - space;
        int cellSize = (screenWidth - 6 * margin) / 3;
        if (contentViewHeight >= cellSize * 4 + margin * 5) {
            pagerSize = 12;
            verticalSpacing = (contentViewHeight - cellSize * 4 - margin * 3) / 3;
        } else {
            pagerSize = 9;
            verticalSpacing = (contentViewHeight - cellSize * 3 - margin * 2) / 2;
        }
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
            if (calledFloors != null) {
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
