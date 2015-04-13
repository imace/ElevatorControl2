package com.inovance.elevatorcontrol.views.form;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.models.SpecialDevice;
import com.inovance.elevatorcontrol.models.Vendor;
import com.inovance.elevatorcontrol.web.WebInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by keith on 14-6-7.
 * User keith
 * Date 14-6-7
 * Time 下午9:59
 */
public class SpecialApplyForm extends LinearLayout implements WebInterface.OnRequestListener {

    private static final int CountDownFinished = 1000;

    private static final int CountDownTick = 2000;

    private static final int MaxWaitTime = 30;

    private AutoCompleteTextView vendorTextView;

    private Spinner deviceListSpinner;

    private EditText remark;

    private View submitView;

    private View progressView;

    private TextView submitTextView;

    private TextView countDownTextView;

    private List<Vendor> mVendorList = new ArrayList<Vendor>();

    private List<SpecialDevice> mDeviceList = new ArrayList<SpecialDevice>();

    private List<SpecialDevice> mCurrentDeviceList = new ArrayList<SpecialDevice>();

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
        vendorTextView = (AutoCompleteTextView) findViewById(R.id.vendor);
        deviceListSpinner = (Spinner) findViewById(R.id.equipment_model);
        remark = (EditText) findViewById(R.id.remark);
        submitView = findViewById(R.id.submit_apply);
        submitView.setOnClickListener(new OnClickListener() {
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
     * 设置厂商列表
     *
     * @param vendorList 厂商列表
     */
    public void setVendorList(List<Vendor> vendorList) {
        mVendorList = vendorList;
        int vendorListSize = vendorList.size();
        String[] vendorNames = new String[vendorListSize];
        for (int index = 0; index < vendorListSize; index++) {
            vendorNames[index] = vendorList.get(index).getName();
        }
        FullSuggestAdapter vendorAdapter = new FullSuggestAdapter(getContext(),
                android.R.layout.simple_dropdown_item_1line,
                vendorNames);
        vendorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vendorTextView.setAdapter(vendorAdapter);
        vendorTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                for (Vendor item : mVendorList) {
                    if (item.getName().equalsIgnoreCase(vendorTextView.getText().toString())) {
                        List<String> nameList = new ArrayList<String>();
                        for (SpecialDevice device : mDeviceList) {
                            if (device.getVendorID() == item.getId()) {
                                nameList.add(device.getName());
                                mCurrentDeviceList.add(device);
                            }
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                                android.R.layout.simple_spinner_item,
                                nameList.toArray(new String[nameList.size()]));
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        deviceListSpinner.setAdapter(adapter);
                        submitView.setEnabled(true);
                    }
                }
            }
        });
        vendorTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                ArrayAdapter<String> deviceAdapter = new ArrayAdapter<String>(getContext(),
                        android.R.layout.simple_spinner_item,
                        new String[]{});
                deviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                deviceListSpinner.setAdapter(deviceAdapter);
                submitView.setEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    /**
     * 设置专有设备列表
     *
     * @param deviceList 专有设备列表
     */

    public void setDeviceList(List<SpecialDevice> deviceList) {
        mDeviceList = deviceList;
    }

    /**
     * 提交申请
     */
    private void submitApply() {
        String bluetoothAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
        int deviceID = mCurrentDeviceList.get(deviceListSpinner.getSelectedItemPosition()).getId();
        WebInterface.getInstance().setOnRequestListener(this);
        WebInterface.getInstance().applySpecialFirmware(getContext(), bluetoothAddress, deviceID, remark.getText().toString());
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

    private class FullSuggestAdapter extends ArrayAdapter implements Filterable {

        private String[] dataSource = new String[]{};

        private List<String> resultList = new ArrayList<String>();

        public FullSuggestAdapter(Context context, int resource, String[] items) {
            super(context, resource);
            this.dataSource = items;
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int position) {
            return resultList.get(position);
        }

        @Override
        public Filter getFilter() {
            return new ItemFilter();
        }

        private class ItemFilter extends Filter {

            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                FilterResults results = new FilterResults();
                if (charSequence != null) {
                    int count = 0;
                    resultList = new ArrayList<String>();
                    for (String item : dataSource) {
                        if (item.contains(charSequence)) {
                            count++;
                            resultList.add(item);
                        }
                    }
                    results.count = count;
                    results.values = resultList;
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                if (filterResults != null && filterResults.count > 0) {
                    notifyDataSetChanged();
                }
            }
        }
    }
}
