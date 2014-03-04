package com.kio.ElevatorControl.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import com.kio.ElevatorControl.R;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-4.
 * Time: 10:36.
 */

public class HelpSystemApplicationConfigActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.application_config_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_help_system_application_config);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}