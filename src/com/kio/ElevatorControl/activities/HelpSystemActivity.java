package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v4.app.FragmentActivity;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.models.NavigationTabs;

/**
 * 帮助
 */
public class HelpSystemActivity extends FragmentActivity {

    protected String[] CONTENTS = null;
    protected Integer[] ICONS = null;

    /**
     * 注入页面元素
     */

    //初始化一级标签
    private void initTabConstant() {
        NavigationTabs uic = NavigationTabs.getTabInstance(this);
        CONTENTS = uic.getTexts();
        ICONS = uic.getIcons();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_system);
        //Views.inject(this);
        getFragmentManager().beginTransaction()
                .replace(R.id.help_system_content, new HelpSystemPreferenceFragment())
                .commit();
    }

    /**
     * 帮助 PreferenceFragment
     */
    private class HelpSystemPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.help_system_preference);
        }

    }
}
