package com.inovance.elevatorcontrol.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.utils.ParseSerialsUtils;
import com.inovance.elevatorcontrol.web.WebInterface;

import org.json.JSONArray;
import org.json.JSONException;

import butterknife.InjectView;
import butterknife.Views;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-6-26.
 * Time: 17:17.
 */
public class InternalRegisterActivity extends Activity implements WebInterface.OnRequestListener {

    @InjectView(R.id.internal_username)
    EditText internalUsername;

    @InjectView(R.id.internal_number)
    EditText internalNumber;

    @InjectView(R.id.internal_cellphone)
    EditText internalCellphone;

    @InjectView(R.id.internal_department)
    EditText internalDepartment;

    @InjectView(R.id.internal_email)
    EditText internalEmail;

    @InjectView(R.id.internal_remark)
    EditText internalRemark;

    @InjectView(R.id.submit)
    LinearLayout submitButton;

    @InjectView(R.id.submit_progress)
    ProgressBar submitProgress;

    @InjectView(R.id.submit_text)
    TextView submitTextView;

    @InjectView(R.id.error_text)
    TextView errorTextView;

    private static final int REQUEST_BLUETOOTH_ENABLE = 1;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setContentView(R.layout.activity_internal_register_layout);
        setTitle(R.string.internal_register_user_text);
        Views.inject(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber = telephonyManager.getLine1Number();
        if (phoneNumber != null) {
            phoneNumber.replace("-", "");
            phoneNumber.replace("(", "");
            phoneNumber.replace(")", "");
            internalCellphone.setText(phoneNumber);
        }
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitRegisterRequest();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebInterface.getInstance().setOnRequestListener(this);
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLUETOOTH_ENABLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        WebInterface.getInstance().removeListener();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    @Override
    protected void onStop() {
        super.onStop();
        WebInterface.getInstance().removeListener();
    }

    /**
     * 提交用户注册
     */
    private void submitRegisterRequest() {
        if (validateUserInputInformation()) {
            submitProgress.setVisibility(View.VISIBLE);
            submitTextView.setVisibility(View.GONE);
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            WebInterface.getInstance().registerInternalUser(this, internalUsername.getText().toString(),
                    internalNumber.getText().toString(),
                    internalCellphone.getText().toString(),
                    internalDepartment.getText().toString(),
                    internalEmail.getText().toString(),
                    internalRemark.getText().toString(),
                    bluetoothAdapter.getAddress());
        }
    }

    /**
     * 验证用户注册信息
     *
     * @return 验证结果
     */
    private boolean validateUserInputInformation() {
        boolean userNameCheck = internalUsername.getText().toString().length() > 0
                && internalUsername.getText().toString().length() <= 20;
        boolean numberCheck = internalNumber.getText().toString().length() > 0;
        boolean cellPhoneCheck = internalCellphone.getText().toString().length() > 0;
        boolean departmentCheck = internalDepartment.getText().toString().length() > 0;
        boolean emailCheck = ParseSerialsUtils.isValidEmail(internalEmail.getText().toString());
        boolean isValidated = true;
        String validateResult = "";
        if (!userNameCheck) {
            isValidated = false;
            validateResult += getResources().getString(R.string.internal_validate_username_error) + "\n";
        }
        if (!numberCheck) {
            isValidated = false;
            validateResult += getResources().getString(R.string.internal_validate_number_error) + "\n";
        }
        if (!cellPhoneCheck) {
            isValidated = false;
            validateResult += getResources().getString(R.string.internal_validate_cellphone_error) + "\n";
        }
        if (!departmentCheck) {
            isValidated = false;
            validateResult += getResources().getString(R.string.internal_validate_department_error) + "\n";
        }
        if (!emailCheck) {
            isValidated = false;
            validateResult += getResources().getString(R.string.internal_validate_email_error) + "\n";
        }
        if (!isValidated) {
            Toast.makeText(this, validateResult.trim(), Toast.LENGTH_SHORT)
                    .show();
        }
        return isValidated;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BLUETOOTH_ENABLE) {
            if (resultCode == RESULT_CANCELED) {
                setResult(RESULT_OK);
                finish();
            }
        }
    }

    @Override
    public void onResult(String tag, String responseString) {
        if (tag.equalsIgnoreCase(ApplicationConfig.RegisterInternalUser)) {
            try {
                JSONArray jsonArray = new JSONArray(responseString);
                submitProgress.setVisibility(View.GONE);
                submitTextView.setVisibility(View.VISIBLE);
                submitButton.setEnabled(false);
                Toast.makeText(InternalRegisterActivity.this,
                        R.string.regist_successful_wait_text,
                        Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                Toast.makeText(this, responseString, Toast.LENGTH_SHORT).show();
                submitProgress.setVisibility(View.GONE);
                submitTextView.setVisibility(View.VISIBLE);
                errorTextView.setText(responseString);
            }
        }
    }

    @Override
    public void onFailure(int statusCode, Throwable throwable) {
        Toast.makeText(InternalRegisterActivity.this, R.string.register_failed_text, Toast.LENGTH_SHORT).show();
        submitProgress.setVisibility(View.GONE);
        submitTextView.setVisibility(View.VISIBLE);
    }
}