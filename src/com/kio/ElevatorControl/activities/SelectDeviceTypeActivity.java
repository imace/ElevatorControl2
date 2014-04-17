package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import butterknife.InjectView;
import butterknife.Views;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.models.Device;
import com.kio.ElevatorControl.web.WebApi;
import com.mobsandgeeks.adapters.InstantAdapter;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.ProgressBar;
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

    private List<Device> deviceList;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setContentView(R.layout.activity_select_device_type_layout);
        setTitle(R.string.select_device_type_text);
        Views.inject(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
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
        WebApi.getInstance().setOnResultListener(new WebApi.onGetResultListener() {
            @Override
            public void onResult(String responseString) {
                try {
                    deviceList = new ArrayList<Device>();
                    JSONArray jsonArray = new JSONArray(responseString);
                    int size = jsonArray.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        Device device = new Device();
                        device.setName(jsonObject.optString("DeviceName"));
                        device.setModel(jsonObject.optString("DeviceNum"));
                        device.setDescription(jsonObject.optString("DeviceDescription"));
                        deviceList.add(device);
                    }
                    InstantAdapter<Device> adapter = new InstantAdapter<Device>(
                            SelectDeviceTypeActivity.this,
                            R.layout.devlice_list_item,
                            Device.class,
                            deviceList);
                    listView.setAdapter(adapter);
                    progressBar.setVisibility(View.GONE);
                    deviceView.setVisibility(View.VISIBLE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        WebApi.getInstance().setOnFailureListener(new WebApi.onRequestFailureListener() {
            @Override
            public void onFailure(int statusCode, Throwable throwable) {

            }
        });
        WebApi.getInstance().getDeviceList();
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