package com.inovance.elevatorcontrol.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;
import butterknife.InjectView;
import butterknife.Views;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.models.SystemLog;
import com.inovance.elevatorcontrol.utils.LogUtils;
import com.mobsandgeeks.adapters.InstantAdapter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-5-30.
 * Time: 11:06.
 */
public class SystemLogActivity extends Activity {

    @InjectView(R.id.list_view)
    ListView listView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setContentView(R.layout.activity_system_log);
        setTitle(R.string.system_log_title);
        Views.inject(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<SystemLog> systemLogList = LogUtils.getInstance().readLogs();
        Collections.sort(systemLogList, new SortComparator());
        InstantAdapter adapter = new InstantAdapter<SystemLog>(this,
                R.layout.system_log_item,
                SystemLog.class,
                systemLogList);
        listView.setAdapter(adapter);
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

    private class SortComparator implements Comparator<SystemLog> {

        @Override
        public int compare(SystemLog object1, SystemLog object2) {
            if (object1.getTimestamp() < object2.getTimestamp()) {
                return 1;
            } else if (object1.getTimestamp() > object2.getTimestamp()) {
                return -1;
            } else {
                return 0;
            }
        }

    }

}