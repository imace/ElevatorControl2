package com.kio.ElevatorControl.adapters;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.views.fragments.ConfigurationFragment;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationAdapter extends FragmentPagerAdapter {

    private FragmentActivity fragmentActivity;

    private List<ConfigurationFragment> mFragments;

    private String[] titleArray;

    public FragmentActivity getFragmentActivity() {
        return fragmentActivity;
    }

    public void setFragmentActivity(FragmentActivity fragmentActivity) {
        this.fragmentActivity = fragmentActivity;
    }

    public ConfigurationAdapter(FragmentActivity activity) {
        super(activity.getSupportFragmentManager());
        mFragments = new ArrayList<ConfigurationFragment>();
        fragmentActivity = activity;
    }

    public ConfigurationAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
        mFragments = new ArrayList<ConfigurationFragment>();
    }

    @Override
    public ConfigurationFragment getItem(int position) {
        if (position > mFragments.size() - 1) {
            ConfigurationFragment configurationFragment = ConfigurationFragment
                    .newInstance(position, fragmentActivity);
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
