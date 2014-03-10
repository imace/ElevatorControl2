package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import butterknife.InjectView;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.views.TypefaceTextView;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.GridView;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-6.
 * Time: 11:03.
 */
public class MoveInsideActivity extends Activity {

    @InjectView(R.id.grid_view)GridView mGridView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.move_inside_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_move_inside);
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
        InsideFloorAdapter adapter = new InsideFloorAdapter(50);
        mGridView.setAdapter(adapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            }
        });
    }

    private void loadDataAndRenderView() {
        HCommunication[] hCommunications = new HCommunication[1];
        HCommunication communication = new HCommunication() {
            @Override
            public void beforeSend() {

            }

            @Override
            public void afterSend() {

            }

            @Override
            public void beforeReceive() {

            }

            @Override
            public void afterReceive() {

            }

            @Override
            public Object onParse() {
                if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                    // 校验数据
                    byte[] received = HSerial.trimEnd(getReceivedBuffer());
                    Log.v("parameterSettingDialog receive", HSerial.byte2HexStr(received));
                    return null;
                }
                return null;
            }
        };
        if (HBluetooth.getInstance(MoveInsideActivity.this).isPrepared()) {
            HBluetooth.getInstance(MoveInsideActivity.this)
                    .setCommunications(hCommunications)
                    .Start();
        }
    }

    /**
     * 电梯层数 GridView adapter
     */
    private class InsideFloorAdapter extends BaseAdapter{

        private int mFloors;

        public InsideFloorAdapter(int floors){
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
            LayoutInflater mInflater = LayoutInflater.from(MoveInsideActivity.this);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.move_inside_row_item, null);
                holder = new ViewHolder();
                holder.mFloorTextView = (TypefaceTextView)convertView.findViewById(R.id.floor_text);
                convertView.setTag(holder);
            }
            else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mFloorTextView.setText(String.valueOf(position + 1));
            return convertView;
        }

        private class ViewHolder {
            TypefaceTextView mFloorTextView;
        }
    }

}