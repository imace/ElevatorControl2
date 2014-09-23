package com.inovance.elevatorcontrol.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Views;
import com.inovance.bluetoothtool.BluetoothTool;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.config.ParameterUpdateTool;
import com.inovance.elevatorcontrol.daos.ParameterFactoryDao;
import com.inovance.elevatorcontrol.models.User;
import com.inovance.elevatorcontrol.utils.UpdateApplication;
import com.inovance.elevatorcontrol.utils.UpdateApplication.OnNoUpdateFoundListener;
import com.inovance.elevatorcontrol.web.WebApi;
import com.inovance.elevatorcontrol.web.WebApi.OnGetResultListener;
import com.inovance.elevatorcontrol.web.WebApi.OnRequestFailureListener;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * 检查当前用户是否被授权
 */
public class CheckAuthorizationActivity extends Activity implements OnGetResultListener, OnRequestFailureListener {

    private static final int WRITE_FINISH = 0;

    /**
     * 登录按钮
     */
    @InjectView(R.id.btn_login)
    ImageButton btnLogin;

    /**
     * 注册按钮
     */
    @InjectView(R.id.btn_sign_up)
    ImageButton btnSignUp;

    /**
     * 操作指示器
     */
    @InjectView(R.id.progress_view)
    LinearLayout progressView;

    /**
     * 操作等待提示文字
     */
    @InjectView(R.id.wait_text)
    TextView waitTextView;

    private boolean hasGetNormalDeviceList = false;

    private boolean hasGetSpecialDeviceList = false;

    private boolean hasGetSpecialDeviceCodeList = false;

    private static final int REQUEST_BLUETOOTH_ENABLE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_authorization);
        if (getIntent().getBooleanExtra("Exit", false)) {
            finish();
            return;
        }
        setTitle(R.string.title_activity_login);
        Views.inject(this);
        UpdateApplication.getInstance().init(this);
        btnSignUp.setEnabled(false);
        btnLogin.setEnabled(false);
        UpdateApplication.getInstance().setOnNoUpdateFoundListener(new OnNoUpdateFoundListener() {
            @Override
            public void onNoUpdate() {
                initializeData();
                UpdateApplication.getInstance().setOnNoUpdateFoundListener(null);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebApi.getInstance().setOnResultListener(this);
        WebApi.getInstance().setOnFailureListener(this);
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            btnSignUp.setEnabled(false);
            btnLogin.setEnabled(false);
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLUETOOTH_ENABLE);
        } else {
            // 检查软件更新
            UpdateApplication.getInstance().checkUpdate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        WebApi.getInstance().removeListener();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BLUETOOTH_ENABLE) {
            if (resultCode == RESULT_OK) {
                btnSignUp.setEnabled(true);
                btnLogin.setEnabled(true);
                UpdateApplication.getInstance().checkUpdate();
            }
            if (resultCode == RESULT_CANCELED) {
                BluetoothTool.getInstance().kill();
                finish();
            }
        }
    }

    @OnClick(R.id.btn_sign_up)
    public void buttonSignUpClick(View view) {
        if (ApplicationConfig.isInternalVersion) {
            this.startActivity(new Intent(CheckAuthorizationActivity.this, InternalRegisterActivity.class));
        } else {
            this.startActivity(new Intent(CheckAuthorizationActivity.this, RegisterUserActivity.class));
        }
    }

    @OnClick(R.id.btn_login)
    public void btnLoginClick(View v) {
        WebApi.getInstance().setOnResultListener(this);
        WebApi.getInstance().setOnFailureListener(this);
        verifyCurrentUser();
    }

    /**
     * 验证用户登录
     */
    private void verifyCurrentUser() {
        progressView.setVisibility(View.VISIBLE);
        waitTextView.setText(R.string.verify_user_text);
        btnLogin.setEnabled(false);
        btnSignUp.setEnabled(false);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        WebApi.getInstance().verifyUser(this, bluetoothAdapter.getAddress());
    }

    /**
     * 写入功能码、帮助、状态数据
     */
    private void initializeData() {
        if (ParameterFactoryDao.checkEmpty(this)) {
            progressView.setVisibility(View.VISIBLE);
            waitTextView.setText(R.string.init_data_wait_text);
            Thread thread = new Thread(runnable);
            thread.start();
        } else {
            btnLogin.setEnabled(true);
            btnSignUp.setEnabled(true);
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WRITE_FINISH: {
                    progressView.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    btnSignUp.setEnabled(true);
                }
                break;
            }
            super.handleMessage(msg);
        }
    };

    Runnable runnable = new Runnable() {

        @Override
        public void run() {
            ParameterFactoryDao.dbInit(CheckAuthorizationActivity.this);
            mHandler.obtainMessage(WRITE_FINISH).sendToTarget();
        }

    };

    private void checkGetDeviceListComplete() {
        if (hasGetNormalDeviceList && hasGetSpecialDeviceList && hasGetSpecialDeviceCodeList) {
            progressView.setVisibility(View.INVISIBLE);
            startActivity(new Intent(CheckAuthorizationActivity.this, NavigationTabActivity.class));
        }
    }

    @Override
    public void onResult(String tag, String responseString) {
        if (tag.equalsIgnoreCase(ApplicationConfig.VerifyUser)) {
            if (responseString.equalsIgnoreCase("false")) {
                progressView.setVisibility(View.INVISIBLE);
                btnSignUp.setEnabled(true);
                btnLogin.setEnabled(true);
                Toast.makeText(CheckAuthorizationActivity.this,
                        R.string.unauthorized_message,
                        android.widget.Toast.LENGTH_SHORT).show();
            } else {
                try {
                    JSONArray jsonArray = new JSONArray(responseString);
                    int size = jsonArray.length();
                    if (size > 0) {
                        User user = new User(jsonArray.getJSONObject(0));
                        ParameterUpdateTool.getInstance().setCurrentUser(user);
                        WebApi.getInstance().setOnResultListener(this);
                        WebApi.getInstance().setOnFailureListener(this);
                        WebApi.getInstance().getNormalDeviceList(this);
                        WebApi.getInstance().getSpecialDeviceList(this);
                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        WebApi.getInstance().getSpecialDeviceCodeList(this, bluetoothAdapter.getAddress());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if (tag.equalsIgnoreCase(ApplicationConfig.GetNormalDeviceList)) {
            hasGetNormalDeviceList = true;
            checkGetDeviceListComplete();
        }
        if (tag.equalsIgnoreCase(ApplicationConfig.GetSpecialDeviceList)) {
            hasGetSpecialDeviceList = true;
            checkGetDeviceListComplete();
        }
        if (tag.equalsIgnoreCase(ApplicationConfig.GetSpecialDeviceList)) {
            hasGetSpecialDeviceCodeList = true;
            checkGetDeviceListComplete();
        }
    }

    @Override
    public void onFailure(int statusCode, Throwable throwable) {
        Toast.makeText(this, throwable.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        CheckAuthorizationActivity.this.progressView.setVisibility(View.INVISIBLE);
        CheckAuthorizationActivity.this.btnSignUp.setEnabled(true);
        CheckAuthorizationActivity.this.btnLogin.setEnabled(true);
    }
}
