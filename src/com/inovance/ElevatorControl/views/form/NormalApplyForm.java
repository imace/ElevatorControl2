package com.inovance.ElevatorControl.views.form;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.models.NormalDevice;
import com.inovance.ElevatorControl.web.WebApi;
import com.inovance.ElevatorControl.web.WebApi.OnGetResultListener;
import com.inovance.ElevatorControl.web.WebApi.OnRequestFailureListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by keith on 14-6-7.
 * User keith
 * Date 14-6-7
 * Time 下午9:59
 */
public class NormalApplyForm extends LinearLayout implements OnGetResultListener, OnRequestFailureListener {

    private Spinner deviceListSpinner;

    private EditText remark;

    private List<NormalDevice> deviceList = new ArrayList<NormalDevice>();

    private View submitView;

    private View progressView;

    private View submitTextView;

    public NormalApplyForm(Context context) {
        super(context);
        init();
    }

    public NormalApplyForm(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NormalApplyForm(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.apply_normal_device, this, true);
        deviceListSpinner = (Spinner) findViewById(R.id.equipment_model);
        remark = (EditText) findViewById(R.id.remark);
        submitView = findViewById(R.id.submit_apply);
        submitView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitApply();
            }
        });
        submitView.setEnabled(false);
        progressView = findViewById(R.id.submit_progress);
        submitTextView = findViewById(R.id.submit_text);
    }

    /**
     * 更新设备型号列表
     *
     * @param deviceList 设备型号列表
     */
    public void setSpinnerDataSource(List<NormalDevice> deviceList) {
        this.deviceList = deviceList;
        int size = deviceList.size();
        String[] deviceNames = new String[size];
        for (int index = 0; index < size; index++) {
            deviceNames[index] = deviceList.get(index).getName();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item,
                deviceNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceListSpinner.setAdapter(adapter);
        if (size > 0) {
            submitView.setEnabled(true);
        }
    }

    /**
     * 提交申请
     */
    private void submitApply() {
        String bluetoothAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
        String deviceID = deviceList.get(deviceListSpinner.getSelectedItemPosition()).getNumber();
        WebApi.getInstance().setOnResultListener(this);
        WebApi.getInstance().setOnFailureListener(this);
        WebApi.getInstance().applyFirmware(getContext(), bluetoothAddress, deviceID, remark.getText().toString());
    }

    @Override
    public void onResult(String tag, String responseString) {
        if (tag.equalsIgnoreCase(ApplicationConfig.ApplyFirmwareApplication)) {
            if (responseString.equalsIgnoreCase("True")) {
                progressView.setVisibility(View.GONE);
                submitTextView.setVisibility(View.VISIBLE);
                submitView.setEnabled(true);
                Toast.makeText(getContext(), R.string.wait_for_approve_text, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), responseString, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onFailure(int statusCode, Throwable throwable) {
        progressView.setVisibility(View.GONE);
        submitTextView.setVisibility(View.VISIBLE);
        submitView.setEnabled(true);
    }
}
