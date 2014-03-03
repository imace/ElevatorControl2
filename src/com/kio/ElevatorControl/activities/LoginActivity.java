package com.kio.ElevatorControl.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Views;
import com.kio.ElevatorControl.R;

public class LoginActivity extends Activity {

    @InjectView(R.id.edit_text_login) EditText editTextLogin;
    @InjectView(R.id.btn_login) Button btnLogin;
    @InjectView(R.id.btn_sign_up) Button btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setTitle(R.string.title_activity_login);
        Views.inject(this);
    }

    @OnClick(R.id.btn_login)
    public void btnLoginClick(View v) {
        this.startActivity(new Intent(LoginActivity.this, CoreActivity.class));
    }

}
