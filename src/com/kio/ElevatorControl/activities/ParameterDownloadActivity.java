package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.view.MenuItem;
import com.kio.ElevatorControl.R;
import org.holoeverywhere.app.Activity;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-10.
 * Time: 11:35.
 */
public class ParameterDownloadActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.parameter_download_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_parameter_download);
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