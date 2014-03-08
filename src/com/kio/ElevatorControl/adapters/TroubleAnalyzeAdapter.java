package com.kio.ElevatorControl.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import com.kio.ElevatorControl.daos.MenuValuesDao;
import com.kio.ElevatorControl.views.fragments.TroubleAnalyzeFragment;

import java.util.ArrayList;
import java.util.List;

public class TroubleAnalyzeAdapter extends FragmentPagerAdapter {

    private FragmentActivity fragmentActivity;

    private List<TroubleAnalyzeFragment> mFragments;

    public TroubleAnalyzeAdapter(FragmentManager fm) {
        super(fm);
        mFragments = new ArrayList<TroubleAnalyzeFragment>();
    }

    public TroubleAnalyzeAdapter(FragmentActivity fa) {
        super(fa.getSupportFragmentManager());
        this.setFragmentActivity(fa);
        mFragments = new ArrayList<TroubleAnalyzeFragment>();
    }

    @Override
    public Fragment getItem(int position) {
        if (position > mFragments.size() - 1) {
            TroubleAnalyzeFragment troubleAnalyzeFragment = TroubleAnalyzeFragment.newInstance(MenuValuesDao
                    .getTroubleAnalyzeTabsTextsPosition(position, fragmentActivity),
                    fragmentActivity);
            mFragments.add(troubleAnalyzeFragment);
            return troubleAnalyzeFragment;
        } else {
            return mFragments.get(position);
        }
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
