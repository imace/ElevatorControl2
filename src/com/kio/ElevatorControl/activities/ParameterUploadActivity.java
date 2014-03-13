package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.view.MenuItem;
import butterknife.InjectView;
import butterknife.Views;
import com.kio.ElevatorControl.R;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.ListView;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-10.
 * Time: 11:35.
 */
public class ParameterUploadActivity extends Activity {

    @InjectView(R.id.upload_list)
    ListView listView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.parameter_upload_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_parameter_upload);
        Views.inject(this);
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