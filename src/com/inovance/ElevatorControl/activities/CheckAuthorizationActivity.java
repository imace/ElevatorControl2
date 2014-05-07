package com.inovance.ElevatorControl.activities;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Views;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.daos.RestoreFactoryDao;
import com.inovance.ElevatorControl.web.WebApi;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.ImageButton;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;

/**
 * 检查当前用户是否被授权
 */
public class CheckAuthorizationActivity extends Activity {

    private static final int WRITE_FINISH = 0;

    @InjectView(R.id.btn_login)
    ImageButton btnLogin;

    @InjectView(R.id.btn_sign_up)
    ImageButton btnSignUp;

    @InjectView(R.id.progress_view)
    LinearLayout progressView;

    @InjectView(R.id.wait_text)
    TextView waitTextView;

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
        initializeData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        WebApi.getInstance().removeListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        WebApi.getInstance().removeListener();
    }

    @OnClick(R.id.btn_sign_up)
    public void buttonSignUpClick(View view) {
        this.startActivity(new Intent(CheckAuthorizationActivity.this, RegisterUserActivity.class));
    }

    @OnClick(R.id.btn_login)
    public void btnLoginClick(View v) {
        //this.startActivity(new Intent(CheckAuthorizationActivity.this, ChooseDeviceActivity.class));
        this.startActivity(new Intent(CheckAuthorizationActivity.this, NavigationTabActivity.class));
        //this.startActivity(new Intent(CheckAuthorizationActivity.this, SelectDeviceTypeActivity.class));
        //verifyCurrentUser();
    }

    /**
     * 验证用户登录
     */
    private void verifyCurrentUser() {
        WebApi.getInstance().setOnResultListener(new WebApi.onGetResultListener() {
            @Override
            public void onResult(String tag, String responseString) {
                if (tag.equalsIgnoreCase(ApplicationConfig.VerifyUser)) {
                    if (responseString.equalsIgnoreCase("TRUE")) {
                        CheckAuthorizationActivity.this
                                .startActivity(new Intent(CheckAuthorizationActivity.this,
                                        NavigationTabActivity.class));
                    } else {
                        CheckAuthorizationActivity.this.progressView.setVisibility(View.INVISIBLE);
                        CheckAuthorizationActivity.this.btnSignUp.setEnabled(true);
                        CheckAuthorizationActivity.this.btnLogin.setEnabled(true);
                        Toast.makeText(CheckAuthorizationActivity.this,
                                R.string.unauthorized_message,
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                /*
                CheckAuthorizationActivity.this.progressView.setVisibility(View.INVISIBLE);
                CheckAuthorizationActivity.this.btnSignUp.setEnabled(true);
                CheckAuthorizationActivity.this.btnLogin.setEnabled(true);
                CheckAuthorizationActivity.this
                        .startActivity(new Intent(CheckAuthorizationActivity.this,
                                NavigationTabActivity.class));
                                */
                }
            }
        });
        WebApi.getInstance().setOnFailureListener(new WebApi.onRequestFailureListener() {
            @Override
            public void onFailure(int statusCode, Throwable throwable) {
                CheckAuthorizationActivity.this.progressView.setVisibility(View.INVISIBLE);
                CheckAuthorizationActivity.this.btnSignUp.setEnabled(true);
                CheckAuthorizationActivity.this.btnLogin.setEnabled(true);
            }
        });
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        WebApi.getInstance().verifyUser(CheckAuthorizationActivity.this, bluetoothAdapter.getAddress());
        CheckAuthorizationActivity.this.progressView.setVisibility(View.VISIBLE);
        CheckAuthorizationActivity.this.waitTextView.setText(R.string.verify_user_text);
        CheckAuthorizationActivity.this.btnLogin.setEnabled(false);
        CheckAuthorizationActivity.this.btnSignUp.setEnabled(false);
    }

    /**
     * 写入功能码、帮助、状态数据
     */
    private void initializeData() {
        if (RestoreFactoryDao.dbEmpty(CheckAuthorizationActivity.this)) {
            progressView.setVisibility(View.VISIBLE);
            waitTextView.setText(R.string.init_data_wait_text);
            btnLogin.setEnabled(false);
            btnSignUp.setEnabled(false);
            Thread thread = new Thread(runnable);
            thread.start();
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
            RestoreFactoryDao.dbInit(CheckAuthorizationActivity.this);
            mHandler.obtainMessage(WRITE_FINISH).sendToTarget();
        }

    };

}
