package com.kio.ElevatorControl.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.kio.ElevatorControl.daos.MenuValuesDao;
import com.kio.ElevatorControl.views.fragments.TroubleAnalyzeFragment;

public class TroubleAnalyzeAdapter extends FragmentPagerAdapter {

    private FragmentActivity fragmentActivity;

    public TroubleAnalyzeAdapter(FragmentManager fm) {
        super(fm);
    }

    public TroubleAnalyzeAdapter(FragmentActivity fa) {
        super(fa.getSupportFragmentManager());
        this.setFragmentActivity(fa);
    }

    @Override
    public Fragment getItem(int position) {
        return TroubleAnalyzeFragment.newInstance(MenuValuesDao
                .getTroubleAnalyzeTabsTextsPosition(position, fragmentActivity),
                fragmentActivity);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return MenuValuesDao.getTroubleAnalyzeTabsPageTitle(position, fragmentActivity);
    }

    @Override
    public int getCount() {
        return MenuValuesDao.getTroubleAnalyzeTabsCount(fragmentActivity);
    }

    public FragmentActivity getFragmentActivity() {
        return fragmentActivity;
    }

    public void setFragmentActivity(FragmentActivity activity) {
        this.fragmentActivity = activity;
    }

}
