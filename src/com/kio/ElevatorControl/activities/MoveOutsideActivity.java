package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import butterknife.InjectView;
import butterknife.Views;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.views.TintedImageButton;
import com.kio.ElevatorControl.views.TypefaceTextView;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.GridView;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-6.
 * Time: 11:04.
 */
public class MoveOutsideActivity extends Activity {

    @InjectView(R.id.grid_view)
    GridView mGridView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.move_outside_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_move_outside);
        Views.inject(this);
        updateGridViewDataSource();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 更新 GridView 数据源
     *
     */
    public void updateGridViewDataSource(){
        OutsideFloorAdapter adapter = new OutsideFloorAdapter(20);
        mGridView.setAdapter(adapter);
    }

    private class OutsideFloorAdapter extends BaseAdapter{

        private int mFloors;

        public OutsideFloorAdapter(int floors){
            mFloors = floors;
        }

        @Override
        public int getCount() {
            return mFloors;
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
            ViewHolder holder = null;
            LayoutInflater mInflater = LayoutInflater.from(MoveOutsideActivity.this);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.move_outside_row_item, null);
                holder = new ViewHolder();
                holder.mFloorTextView = (TypefaceTextView)convertView.findViewById(R.id.floor_text);
                holder.mUpButton = (TintedImageButton)convertView.findViewById(R.id.up_button);
                holder.mDownButton = (TintedImageButton)convertView.findViewById(R.id.down_button);
                convertView.setTag(holder);
            }
            else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mFloorTextView.setText(String.valueOf(position + 1));
            holder.mUpButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
            holder.mDownButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
            if (position == 0){
                holder.mUpButton.setVisibility(View.VISIBLE);
                holder.mDownButton.setVisibility(View.INVISIBLE);
            }
            else if (position == (mFloors - 1)){
                holder.mUpButton.setVisibility(View.INVISIBLE);
                holder.mDownButton.setVisibility(View.VISIBLE);
            }
            else {
                holder.mUpButton.setVisibility(View.VISIBLE);
                holder.mDownButton.setVisibility(View.VISIBLE);
            }
            return convertView;
        }

        private class ViewHolder {
            TypefaceTextView mFloorTextView;
            TintedImageButton mUpButton;
            TintedImageButton mDownButton;
        }
    }

}