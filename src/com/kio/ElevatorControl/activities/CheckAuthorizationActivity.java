package com.kio.ElevatorControl.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Views;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.daos.RestoreFactoryDao;
import org.holoeverywhere.app.Activity;

/**
 * 检查当前用户是否被授权
 */
public class CheckAuthorizationActivity extends Activity {

    private static final int WRITE_FINISH = 0;

    @InjectView(R.id.edit_text_login)
    EditText editTextLogin;

    @InjectView(R.id.btn_login)
    Button btnLogin;

    @InjectView(R.id.btn_sign_up)
    Button btnSignUp;

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

    @OnClick(R.id.btn_login)
    public void btnLoginClick(View v) {
        this.startActivity(new Intent(CheckAuthorizationActivity.this, NavigationTabActivity.class));
    }

    /**
     * 写入功能码、帮助、状态数据
     */
    private void initializeData() {
        if (RestoreFactoryDao.dbEmpty(CheckAuthorizationActivity.this)) {
            progressView.setVisibility(View.VISIBLE);
            editTextLogin.setEnabled(false);
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
                    editTextLogin.setEnabled(true);
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
