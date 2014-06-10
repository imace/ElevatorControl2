package com.inovance.ElevatorControl.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import butterknife.Views;
import com.inovance.ElevatorControl.R;

/**
 * Created by keith on 14-6-7.
 * User keith
 * Date 14-6-7
 * Time 上午1:15
 */
public class ApplyPermissionActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setContentView(R.layout.activity_apply_permission);
        setTitle(R.string.apply_permission_text);
        Views.inject(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
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