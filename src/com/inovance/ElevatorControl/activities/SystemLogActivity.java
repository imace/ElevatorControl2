package com.inovance.elevatorcontrol.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.models.SystemLog;
import com.inovance.elevatorcontrol.utils.LogUtils;
import com.mobsandgeeks.adapters.InstantAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.InjectView;
import butterknife.Views;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-5-30.
 * Time: 11:06.
 */
public class SystemLogActivity extends Activity {

    @InjectView(R.id.list_view)
    ListView listView;

    private List<SystemLog> systemLogList = new ArrayList<SystemLog>();

    private InstantAdapter instantAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setContentView(R.layout.activity_system_log);
        setTitle(R.string.system_log_title);
        Views.inject(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        instantAdapter = new InstantAdapter<SystemLog>(this, R.layout.system_log_item, SystemLog.class, systemLogList);
        listView.setAdapter(instantAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                reloadSystemLog();
            }
        }, 300);
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.system_log_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;
            case R.id.action_clear:
                clearSystemLog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * 读取日志
     */
    private void reloadSystemLog() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<SystemLog> logList = LogUtils.getInstance().readLogs();
                Collections.sort(logList, new SortComparator());
                systemLogList.clear();
                systemLogList.addAll(logList);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        instantAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    /**
     * 清空日志
     */
    private void clearSystemLog() {
        if (systemLogList.size() > 0) {
            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setIndeterminate(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.show();
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    LogUtils.getInstance().deleteAll();
                    systemLogList.clear();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            instantAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }).start();
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