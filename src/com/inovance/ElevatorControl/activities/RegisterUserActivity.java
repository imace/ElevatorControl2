package com.inovance.ElevatorControl.activities;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.MenuItem;
import android.view.View;
import butterknife.InjectView;
import butterknife.Views;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.models.User;
import com.inovance.ElevatorControl.web.WebApi;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.ProgressBar;
import org.holoeverywhere.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-10.
 * Time: 11:23.
 */
public class RegisterUserActivity extends Activity {

    @InjectView(R.id.user_name)
    EditText userName;

    @InjectView(R.id.company)
    EditText company;

    @InjectView(R.id.cell_phone)
    EditText cellPhone;

    @InjectView(R.id.tel_phone)
    EditText telPhone;

    @InjectView(R.id.email)
    EditText email;

    @InjectView(R.id.submit)
    LinearLayout submitButton;

    @InjectView(R.id.submit_progress)
    ProgressBar submitProgress;

    @InjectView(R.id.submit_text)
    TextView submitTextView;

    @InjectView(R.id.error_text)
    TextView errorTextView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setContentView(R.layout.activity_register_user_layout);
        setTitle(R.string.register_user_text);
        Views.inject(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber = telephonyManager.getLine1Number();
        if (phoneNumber != null) {
            phoneNumber.replace("-", "");
            phoneNumber.replace("(", "");
            phoneNumber.replace(")", "");
            cellPhone.setText(phoneNumber);
        }
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitProgress.setVisibility(View.VISIBLE);
                submitTextView.setVisibility(View.INVISIBLE);
                submitRegisterRequest();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        WebApi.getInstance().removeListener();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    @Override
    protected void onStop() {
        super.onStop();
        WebApi.getInstance().removeListener();
    }

    /**
     * 提交用户注册
     */
    private void submitRegisterRequest() {
        User user = new User();
        user.setName(userName.getText().toString());
        user.setCompany(company.getText().toString());
        user.setCellPhone(cellPhone.getText().toString());
        user.setTelephone(telPhone.getText().toString());
        user.setEmail(email.getText().toString());
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        user.setBluetoothAddress(bluetoothAdapter.getAddress());
        WebApi.getInstance().setOnResultListener(new WebApi.onGetResultListener() {
            @Override
            public void onResult(String tag, String responseString) {
                if (tag.equalsIgnoreCase(ApplicationConfig.RegisterUser)) {
                    boolean successful = false;
                    try {
                        JSONArray jsonArray = new JSONArray(responseString);
                        RegisterUserActivity
                                .this
                                .startActivity(new Intent(RegisterUserActivity.this, NavigationTabActivity.class));
                    } catch (JSONException e) {
                        submitProgress.setVisibility(View.INVISIBLE);
                        submitTextView.setVisibility(View.VISIBLE);
                        errorTextView.setText(responseString);
                    }
                }
            }
        });
        WebApi.getInstance().setOnFailureListener(new WebApi.onRequestFailureListener() {
            @Override
            public void onFailure(int statusCode, Throwable throwable) {

            }
        });
        WebApi.getInstance().registerUser(this, user);
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