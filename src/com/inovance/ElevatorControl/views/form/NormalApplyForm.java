package com.inovance.elevatorcontrol.views.form;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.models.NormalDevice;
import com.inovance.elevatorcontrol.web.WebInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by keith on 14-6-7.
 * User keith
 * Date 14-6-7
 * Time 下午9:59
 */
public class NormalApplyForm extends LinearLayout implements WebInterface.OnRequestListener {

    private static final int CountDownFinished = 1000;

    private static final int CountDownTick = 2000;

    private static final int MaxWaitTime = 30;

    private Spinner deviceListSpinner;

    private EditText remark;

    private List<NormalDevice> deviceList = new ArrayList<NormalDevice>();

    private View submitView;

    private View progressView;

    private TextView submitTextView;

    private TextView countDownTextView;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case CountDownTick:
                    String text = countDownTextView.getText().toString();
                    countDownTextView.setText(Integer.parseInt(text) - 1 + "");
                    break;
                case CountDownFinished:
                    submitView.setClickable(true);
                    submitView.setEnabled(true);
                    countDownTextView.setVisibility(View.GONE);
                    submitTextView.setAlpha(1.0f);
                    countDownTextView.setAlpha(1.0f);
                    break;
            }
        }
    };

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
                submitView.setClickable(false);
                submitView.setEnabled(false);
                countDownTextView.setText(String.valueOf(MaxWaitTime));
                countDownTextView.setVisibility(View.VISIBLE);
                submitTextView.setAlpha(0.5f);
                countDownTextView.setAlpha(0.5f);
                new CountDownTimer(MaxWaitTime * 1000, 1000) {

                    @Override
                    public void onTick(long l) {
                        handler.sendEmptyMessage(CountDownTick);
                    }

                    @Override
                    public void onFinish() {
                        handler.sendEmptyMessage(CountDownTick);
                        handler.sendEmptyMessage(CountDownFinished);
                    }
                }.start();
            }
        });
        submitView.setEnabled(false);
        progressView = findViewById(R.id.submit_progress);
        submitTextView = (TextView) findViewById(R.id.submit_text);
        countDownTextView = (TextView) findViewById(R.id.count_down_text);
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
        int deviceID = deviceList.get(deviceListSpinner.getSelectedItemPosition()).getID();
        WebInterface.getInstance().setOnRequestListener(this);
        WebInterface.getInstance().applyFirmware(getContext(), bluetoothAddress, deviceID, remark.getText().toString());
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
