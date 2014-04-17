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
 * Date: 14-4-17.
 * Time: 10:48.
 */
public class ViewSystemStatusActivity extends Activity {

    @InjectView(R.id.list_view)
    ListView listView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setTitle(R.string.system_status_title);
        setContentView(R.layout.activity_view_system_status);
        Views.inject(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
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

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

}