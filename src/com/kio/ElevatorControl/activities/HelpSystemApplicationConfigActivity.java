package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.MenuItem;
import com.kio.ElevatorControl.R;
import org.holoeverywhere.app.Activity;

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
        getFragmentManager().beginTransaction()
                .replace(R.id.application_config_content, new ApplicationConfigFragment())
                .commit();
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

    /**
     * 应用程序设置 Fragment
     */
    private class ApplicationConfigFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.application_preference);
        }

    }

}