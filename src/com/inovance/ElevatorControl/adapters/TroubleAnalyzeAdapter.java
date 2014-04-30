package com.inovance.ElevatorControl.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.views.fragments.TroubleAnalyzeFragment;

import java.util.ArrayList;
import java.util.List;

public class TroubleAnalyzeAdapter extends FragmentPagerAdapter {

    private FragmentActivity fragmentActivity;

    private List<TroubleAnalyzeFragment> mFragments;

    private String[] titleArray;

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
            TroubleAnalyzeFragment troubleAnalyzeFragment = TroubleAnalyzeFragment
                    .newInstance(position, fragmentActivity);
            mFragments.add(troubleAnalyzeFragment);
            return troubleAnalyzeFragment;
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

    public FragmentActivity getFragmentActivity() {
        return fragmentActivity;
    }

    public void setFragmentActivity(FragmentActivity activity) {
        this.fragmentActivity = activity;
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
                    .getStringArray(R.array.trouble_analyze_tab_text);
        }
        return titleArray;
    }

}
