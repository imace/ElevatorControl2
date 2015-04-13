package com.inovance.elevatorcontrol.window;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;

import com.inovance.elevatorcontrol.R;

public class CallFloorAdapter extends FragmentPagerAdapter {

    private CallInsideFragment callInsideFragment;

    private CallOutsideFragment callOutsideFragment;

    private FragmentActivity fragmentActivity;

    private String[] titleArray;

    public CallFloorAdapter(CallFloorWindow activity) {
        super(activity.getSupportFragmentManager());
        fragmentActivity = activity;
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            if (callInsideFragment == null) {
                callInsideFragment = new CallInsideFragment();
            }
            return callInsideFragment;
        }
        if (position == 1) {
            if (callOutsideFragment == null) {
                callOutsideFragment = new CallOutsideFragment();
            }
            return callOutsideFragment;
        }
        return new Fragment();
    }

    @Override
    public int getCount() {
        return getTabTextArray().length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return getTabTextArray()[position];
    }

    private String[] getTabTextArray() {
        if (titleArray == null) {
            titleArray = fragmentActivity
                    .getResources()
                    .getStringArray(R.array.call_floor_tab_array);
        }
        return titleArray;
    }
}
