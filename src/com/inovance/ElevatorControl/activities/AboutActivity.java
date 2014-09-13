package com.inovance.elevatorcontrol.activities;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import butterknife.InjectView;
import butterknife.Views;
import com.inovance.elevatorcontrol.R;

/**
 * Created by IntelliJ IDEA.
 * 关于页面
 * User: keith.
 * Date: 14-4-10.
 * Time: 11:32.
 */
public class AboutActivity extends Activity {

    @InjectView(R.id.version)
    TextView versionTextView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setContentView(R.layout.activity_about_layout);
        setTitle(R.string.about_text);
        Views.inject(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        try {
            String versionName = getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
            versionTextView.setText("Ver " + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
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