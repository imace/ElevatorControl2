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

    @InjectView(R.id.init_data_progress_view)
    View progressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_authorization);
        setTitle(R.string.title_activity_login);
        Views.inject(this);
        initializeData();
    }

    @OnClick(R.id.btn_sign_up)
    public void buttonSignUpClick(View view) {
        this.startActivity(new Intent(CheckAuthorizationActivity.this, RegisterUserActivity.class));
    }

    @OnClick(R.id.btn_login)
    public void btnLoginClick(View v) {
        //this.startActivity(new Intent(CheckAuthorizationActivity.this, ChooseDeviceActivity.class));
        //this.startActivity(new Intent(CheckAuthorizationActivity.this, NavigationTabActivity.class));
        //this.startActivity(new Intent(CheckAuthorizationActivity.this, SelectDeviceTypeActivity.class));
        verifyCurrentUser();
    }

    /**
     * 验证用户登录
     */
    private void verifyCurrentUser() {
        WebApi.getInstance().setOnResultListener(new WebApi.onGetResultListener() {
            @Override
            public void onResult(String responseString) {
                if (responseString.equalsIgnoreCase("TRUE")) {
                    CheckAuthorizationActivity.this
                            .startActivity(new Intent(CheckAuthorizationActivity.this,
                                    NavigationTabActivity.class));
                } else {
                    Toast.makeText(CheckAuthorizationActivity.this,
                            R.string.unauthorized_message,
                            android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
        WebApi.getInstance().setOnFailureListener(new WebApi.onRequestFailureListener() {
            @Override
            public void onFailure(int statusCode, Throwable throwable) {

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
