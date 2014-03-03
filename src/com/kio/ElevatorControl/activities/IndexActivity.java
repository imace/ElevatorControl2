package com.kio.ElevatorControl.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import butterknife.OnClick;
import butterknife.Views;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.daos.RestoreFactoryDao;

public class IndexActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);
        Views.inject(this);

    }

    /**
     * 恢复出厂设置
     */
    @OnClick(R.id.indexRestoreFactory)
    public void btnRestoreFactoryClick(View v) {
        try {
            RestoreFactoryDao.dbInit(this);
        } catch (Exception e) {//
            Log.e("IndexActivity.btnRestoreFactoryClick", e.getMessage());
        }
    }

}
