package com.inovance.ElevatorControl.adapters;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.models.RealTimeMonitor;
import com.inovance.ElevatorControl.views.fragments.ConfigurationFragment;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationAdapter extends FragmentPagerAdapter {

    private FragmentActivity fragmentActivity;

    private List<ConfigurationFragment> mFragments;

    private String[] titleArray;

    private List<RealTimeMonitor> monitorListToShow;

    public ConfigurationAdapter(FragmentActivity activity, List<RealTimeMonitor> monitorListToShow) {
        super(activity.getSupportFragmentManager());
        mFragments = new ArrayList<ConfigurationFragment>();
        fragmentActivity = activity;
        this.monitorListToShow = monitorListToShow;
    }

    @Override
    public ConfigurationFragment getItem(int position) {
        if (position > mFragments.size() - 1) {
            ConfigurationFragment configurationFragment = ConfigurationFragment
                    .newInstance(position, fragmentActivity, monitorListToShow);
            mFragments.add(configurationFragment);
            return configurationFragment;
        } else {
            return mFragments.get(position);
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return getTabTextArray()[position];
    }

    @Override
    public int getCount() {
        return getTabTextArray().length;
    }

    /**
     * Get Tab Text Array
     *
     * @return String[]
     */
    private String[] getTabTextArray() {
        if (titleArray == null) {
            titleArray = fragmentActivity
                    .getResources()
                    .getStringArray(R.array.configuration_tab_text);
        }
        return titleArray;
    }

}
