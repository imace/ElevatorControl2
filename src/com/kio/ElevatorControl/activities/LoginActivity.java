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

    @InjectView(R.id.editxtlogin)
    EditText editxtlogin;
    @InjectView(R.id.btnlogin)
    Button btnlogin;
    @InjectView(R.id.btnregist)
    Button btnregist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Views.inject(this);

    }

    @OnClick(R.id.btnlogin)
    public void btnloginClick(View v) {
        this.startActivity(new Intent(LoginActivity.this, CoreActivity.class));
    }

}
