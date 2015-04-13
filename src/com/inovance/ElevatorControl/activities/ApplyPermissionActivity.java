package com.inovance.elevatorcontrol.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Spinner;
import android.widget.Toast;

import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.config.ParameterUpdateTool;
import com.inovance.elevatorcontrol.models.SpecialDevice;
import com.inovance.elevatorcontrol.models.User;
import com.inovance.elevatorcontrol.models.Vendor;
import com.inovance.elevatorcontrol.utils.ParseSerialsUtils;
import com.inovance.elevatorcontrol.web.WebInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;
import butterknife.Views;

/**
 * Created by keith on 14-6-7.
 * User keith
 * Date 14-6-7
 * Time 上午1:15
 */
public class ApplyPermissionActivity extends Activity implements WebInterface.OnRequestListener {

    private static final String TAG = ApplyPermissionActivity.class.getSimpleName();

    @InjectView(R.id.vendor_name)
    AutoCompleteTextView vendorTextView;

    @InjectView(R.id.equipment_model)
    Spinner deviceSpinner;

    @InjectView(R.id.user_name)
    EditText userName;

    @InjectView(R.id.company)
    EditText companyName;

    @InjectView(R.id.position)
    EditText positionName;

    @InjectView(R.id.tel_phone)
    EditText phoneNumber;

    @InjectView(R.id.email)
    EditText email;

    @InjectView(R.id.submit)
    View submitView;

    @InjectView(R.id.submit_progress)
    View submitProgressView;

    @InjectView(R.id.submit_text)
    View submitTextView;

    private List<Vendor> mVendorList = new ArrayList<Vendor>();

    private List<SpecialDevice> mDeviceList = new ArrayList<SpecialDevice>();

    private List<SpecialDevice> mCurrentDeviceList = new ArrayList<SpecialDevice>();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setContentView(R.layout.activity_apply_permission);
        setTitle(R.string.apply_permission_text);
        Views.inject(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        submitView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateAndSubmitApply();
            }
        });
        writeUserData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebInterface.getInstance().setOnRequestListener(this);
        WebInterface.getInstance().getVendorList(this);
        WebInterface.getInstance().getSpecialDeviceList(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        WebInterface.getInstance().removeListener();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    private void writeUserData() {
        User user = ParameterUpdateTool.getInstance().getCurrentUser();
        if (user != null) {
            email.setText(user.getEmail());
            companyName.setText(user.getCompany());
        }
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

    /**
     * 验证并提交注册
     */
    private void validateAndSubmitApply() {
        boolean vendorNameCheck = vendorTextView.getText().toString().length() > 0;
        boolean deviceCheck = mCurrentDeviceList.size() > 0;
        boolean userNameCheck = userName.getText().toString().length() > 0;
        boolean companyNameCheck = companyName.getText().toString().length() > 0;
        boolean positionNameCheck = positionName.getText().toString().length() > 0;
        boolean phoneCheck = phoneNumber.getText().toString().length() > 0;
        boolean emailCheck = ParseSerialsUtils.isValidEmail(email.getText().toString());
        boolean isValidated = true;
        String validateResult = "";
        if (!vendorNameCheck) {
            isValidated = false;
            validateResult += getResources().getString(R.string.vendor_name_error) + "\n";
        }
        if (!deviceCheck) {
            isValidated = false;
            validateResult += getResources().getString(R.string.device_type_error) + "\n";
        }
        if (!userNameCheck) {
            isValidated = false;
            validateResult += getResources().getString(R.string.true_name_error) + "\n";
        }
        if (!companyNameCheck) {
            isValidated = false;
            validateResult += getResources().getString(R.string.company_name_error) + "\n";
        }
        if (!positionNameCheck) {
            isValidated = false;
            validateResult += getResources().getString(R.string.position_name_error) + "\n";
        }
        if (!phoneCheck) {
            isValidated = false;
            validateResult += getResources().getString(R.string.cellphone_error) + "\n";
        }
        if (!emailCheck) {
            isValidated = false;
            validateResult += getResources().getString(R.string.email_address_error) + "\n";
        }
        if (isValidated) {
            submitTextView.setVisibility(View.GONE);
            submitProgressView.setVisibility(View.VISIBLE);
            submitView.setEnabled(false);
            int deviceID = mCurrentDeviceList.get(deviceSpinner.getSelectedItemPosition()).getId();
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            String bluetoothAddress = bluetoothAdapter.getAddress();
            WebInterface.getInstance().applySpecialDevicePermission(this, deviceID,
                    bluetoothAddress,
                    userName.getText().toString(),
                    companyName.getText().toString(),
                    positionName.getText().toString(),
                    phoneNumber.getText().toString(),
                    email.getText().toString());
        } else {
            Toast.makeText(this, validateResult.trim(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setVendorList(List<Vendor> vendorList) {
        mVendorList = vendorList;
        int vendorListSize = vendorList.size();
        String[] vendorNames = new String[vendorListSize];
        for (int index = 0; index < vendorListSize; index++) {
            vendorNames[index] = vendorList.get(index).getName();
        }
        /*
        ArrayAdapter<String> vendorAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line,
                vendorNames);
                */
        FullSuggestAdapter vendorAdapter = new FullSuggestAdapter(this,
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
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(ApplyPermissionActivity.this,
                                android.R.layout.simple_spinner_item,
                                nameList.toArray(new String[nameList.size()]));
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        deviceSpinner.setAdapter(adapter);
                        submitView.setEnabled(true);
                    }
                }
            }
        });
    }

    @Override
    public void onResult(String tag, String responseString) {
        if (responseString != null && responseString.length() > 0) {
            if (tag.equalsIgnoreCase(ApplicationConfig.GetVendorList)) {
                try {
                    List<Vendor> vendorList = new ArrayList<Vendor>();
                    JSONArray jsonArray = new JSONArray(responseString);
                    int size = jsonArray.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        Vendor vendor = new Vendor(object);
                        vendorList.add(vendor);
                    }
                    setVendorList(vendorList);
                } catch (JSONException e) {
                    Toast.makeText(this, R.string.read_data_error, Toast.LENGTH_SHORT).show();
                }
            }
            if (tag.equalsIgnoreCase(ApplicationConfig.GetSpecialDeviceList)) {
                try {
                    List<SpecialDevice> deviceList = new ArrayList<SpecialDevice>();
                    JSONArray jsonArray = new JSONArray(responseString);
                    int size = jsonArray.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        SpecialDevice device = new SpecialDevice(object);
                        deviceList.add(device);
                    }
                    mDeviceList = deviceList;
                } catch (JSONException e) {
                    Toast.makeText(this, R.string.read_data_error, Toast.LENGTH_SHORT).show();
                }
            }
            // 申请专有设备权限
            if (tag.equalsIgnoreCase(ApplicationConfig.ApplySpecialDevicePermission)) {
                if (responseString.equalsIgnoreCase("True")) {
                    Toast.makeText(this, R.string.submit_successful_text, Toast.LENGTH_SHORT).show();
                    submitTextView.setVisibility(View.VISIBLE);
                    submitProgressView.setVisibility(View.GONE);
                    submitView.setEnabled(true);
                }
            }
        }
    }

    @Override
    public void onFailure(int statusCode, Throwable throwable) {
        Toast.makeText(this, R.string.server_error_text, Toast.LENGTH_SHORT).show();
        submitTextView.setVisibility(View.VISIBLE);
        submitProgressView.setVisibility(View.GONE);
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