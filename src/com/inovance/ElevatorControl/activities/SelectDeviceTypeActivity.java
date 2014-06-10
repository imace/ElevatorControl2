package com.inovance.ElevatorControl.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import butterknife.InjectView;
import butterknife.Views;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.models.SpecialDevice;
import com.inovance.ElevatorControl.web.WebApi;
import com.mobsandgeeks.adapters.InstantAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-10.
 * Time: 15:14.
 */
public class SelectDeviceTypeActivity extends Activity {

    @InjectView(R.id.progress_bar)
    ProgressBar progressBar;

    @InjectView(R.id.device_list)
    ListView listView;

    @InjectView(R.id.device_view)
    LinearLayout deviceView;

    private List<SpecialDevice> deviceList;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setContentView(R.layout.activity_select_device_type_layout);
        setTitle(R.string.select_device_type_text);
        Views.inject(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        getDeviceList();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    /**
     * 取得设备列表
     */
    private void getDeviceList() {
        WebApi.getInstance().setOnResultListener(new WebApi.OnGetResultListener() {
            @Override
            public void onResult(String tag, String responseString) {
                try {
                    deviceList = new ArrayList<SpecialDevice>();
                    JSONArray jsonArray = new JSONArray(responseString);
                    int size = jsonArray.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        //@TODO Select SpecialDevice
                    }
                    InstantAdapter<SpecialDevice> adapter = new InstantAdapter<SpecialDevice>(
                            SelectDeviceTypeActivity.this,
                            R.layout.devlice_list_item,
                            SpecialDevice.class,
                            deviceList);
                    listView.setAdapter(adapter);
                    progressBar.setVisibility(View.GONE);
                    deviceView.setVisibility(View.VISIBLE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        WebApi.getInstance().setOnFailureListener(new WebApi.OnRequestFailureListener() {
            @Override
            public void onFailure(int statusCode, Throwable throwable) {

            }
        });
        WebApi.getInstance().getDeviceList(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}