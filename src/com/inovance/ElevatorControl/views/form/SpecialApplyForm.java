package com.inovance.ElevatorControl.views.form;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.models.SpecialDevice;
import com.inovance.ElevatorControl.models.Vendor;
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
public class SpecialApplyForm extends LinearLayout implements OnGetResultListener, OnRequestFailureListener {

    private Spinner vendorListSpinner;

    private Spinner deviceListSpinner;

    private EditText remark;

    private View submitView;

    private View progressView;

    private View submitTextView;

    private List<Vendor> vendorList = new ArrayList<Vendor>();

    private List<SpecialDevice> deviceList = new ArrayList<SpecialDevice>();

    /**
     * 关联的设备列表
     */
    private List<SpecialDevice> relateDeviceList = new ArrayList<SpecialDevice>();

    public SpecialApplyForm(Context context) {
        super(context);
        init();
    }

    public SpecialApplyForm(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpecialApplyForm(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.apply_special_device, this, true);
        vendorListSpinner = (Spinner) findViewById(R.id.vendor);
        deviceListSpinner = (Spinner) findViewById(R.id.equipment_model);
        remark = (EditText) findViewById(R.id.remark);
        submitView = findViewById(R.id.submit_apply);
        submitView.setOnClickListener(new OnClickListener() {
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
     * 设置厂商列表
     *
     * @param vendorList 厂商列表
     */
    public void setVendorList(List<Vendor> vendorList) {
        this.vendorList = vendorList;
        int vendorListSize = vendorList.size();
        String[] vendorNames = new String[vendorListSize];
        for (int index = 0; index < vendorListSize; index++) {
            vendorNames[index] = vendorList.get(index).getName();
        }
        ArrayAdapter<String> vendorAdapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item,
                vendorNames);
        vendorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vendorListSpinner.setAdapter(vendorAdapter);
        if (deviceList.size() > 0 && vendorList.size() > 0) {
            vendorListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int index, long l) {

                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            submitView.setEnabled(true);
        }
    }

    /**
     * 设置专有设备列表
     *
     * @param deviceList 专有设备列表
     */

    public void setDeviceList(List<SpecialDevice> deviceList) {
        this.deviceList = deviceList;
        int deviceListSize = deviceList.size();
        String[] deviceNames = new String[deviceListSize];
        for (int index = 0; index < deviceListSize; index++) {
            deviceNames[index] = deviceList.get(index).getName();
        }
        ArrayAdapter<String> deviceAdapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item,
                deviceNames);
        deviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceListSpinner.setAdapter(deviceAdapter);
        if (deviceList.size() > 0 && vendorList.size() > 0) {
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
        if (tag.equalsIgnoreCase(ApplicationConfig.ApplySpecialFirmwareApplication)) {
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
