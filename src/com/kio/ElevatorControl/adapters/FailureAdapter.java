package com.kio.ElevatorControl.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.kio.ElevatorControl.daos.ValuesDao;
import com.kio.ElevatorControl.views.fragments.FailureFragment;

public class FailureAdapter extends FragmentPagerAdapter {

    private FragmentActivity fragmntactivity;

    public FailureAdapter(FragmentManager fm) {
        super(fm);
    }

    public FailureAdapter(FragmentActivity fa) {
        super(fa.getSupportFragmentManager());
        this.setFragmntactivity(fa);
    }

    @Override
    public Fragment getItem(int position) {
        return FailureFragment.newInstance(ValuesDao
                .getFailureTabsTextsPosition(position, fragmntactivity),
                fragmntactivity);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return ValuesDao.getFailureTabsPageTitle(position, fragmntactivity);
    }

    @Override
    public int getCount() {
        return ValuesDao.getFailureTabsCount(fragmntactivity);
    }

    public FragmentActivity getFragmntactivity() {
        return fragmntactivity;
    }

    public void setFragmntactivity(FragmentActivity fragmntactivity) {
        this.fragmntactivity = fragmntactivity;
    }

}
