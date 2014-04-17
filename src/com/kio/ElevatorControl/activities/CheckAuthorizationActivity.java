package com.kio.ElevatorControl.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.view.View;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Views;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.daos.RestoreFactoryDao;
import com.kio.ElevatorControl.web.WebApi;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.ImageButton;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.TextView;

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
            public void onResult(String responseString) {
                /*
                if (responseString.equalsIgnoreCase("TRUE")) {
                    CheckAuthorizationActivity.this
                            .startActivity(new Intent(CheckAuthorizationActivity.this,
                                    NavigationTabActivity.class));
                } else {
                    Toast.makeText(CheckAuthorizationActivity.this,
                            R.string.unauthorized_message,
                            android.widget.Toast.LENGTH_SHORT).show();
                }
                */
                CheckAuthorizationActivity.this.progressView.setVisibility(View.INVISIBLE);
                CheckAuthorizationActivity.this.btnSignUp.setEnabled(true);
                CheckAuthorizationActivity.this.btnLogin.setEnabled(true);
                CheckAuthorizationActivity.this
                        .startActivity(new Intent(CheckAuthorizationActivity.this,
                                NavigationTabActivity.class));
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
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber = telephonyManager.getLine1Number();
        if (phoneNumber != null) {
            phoneNumber.replace("-", "");
            phoneNumber.replace("(", "");
            phoneNumber.replace(")", "");
            WebApi.getInstance().verifyUser(phoneNumber);
        } else {
            View dialogView = getLayoutInflater().inflate(R.layout.phone_number_dialog, null);
            final EditText cellPhone = (EditText) dialogView.findViewById(R.id.cell_phone);
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogStyle)
                    .setTitle(R.string.input_cell_phone_number)
                    .setView(dialogView)
                    .setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            WebApi.getInstance().verifyUser(cellPhone.getText().toString());
                            CheckAuthorizationActivity.this.progressView.setVisibility(View.VISIBLE);
                            CheckAuthorizationActivity.this.waitTextView.setText(R.string.verify_user_text);
                            CheckAuthorizationActivity.this.btnLogin.setEnabled(false);
                            CheckAuthorizationActivity.this.btnSignUp.setEnabled(false);
                        }
                    });
            builder.create().show();
        }
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
