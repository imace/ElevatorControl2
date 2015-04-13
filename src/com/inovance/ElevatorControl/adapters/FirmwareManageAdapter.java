package com.inovance.elevatorcontrol.adapters;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.views.fragments.FirmwareManageFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-5.
 * Time: 15:29.
 */
public class FirmwareManageAdapter extends FragmentPagerAdapter {

    private FragmentActivity fragmentActivity;

    private List<FirmwareManageFragment> mFragments;

    private String[] titleArray;

    public FragmentActivity getFragmentActivity() {
        return fragmentActivity;
    }

    public void setFragmentActivity(FragmentActivity fragmentActivity) {
        this.fragmentActivity = fragmentActivity;
    }

    public FirmwareManageAdapter(FragmentActivity activity) {
        super(activity.getSupportFragmentManager());
        mFragments = new ArrayList<FirmwareManageFragment>();
        fragmentActivity = activity;
    }

    public FirmwareManageAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
        mFragments = new ArrayList<FirmwareManageFragment>();
    }

    @Override
    public FirmwareManageFragment getItem(int position) {
        if (position > mFragments.size() - 1) {
            FirmwareManageFragment firmwareManageFragment = FirmwareManageFragment
                    .newInstance(position, fragmentActivity);
            mFragments.add(firmwareManageFragment);
            return firmwareManageFragment;
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
                    .getStringArray(R.array.firmware_manage_tab_text);
        }
        return titleArray;
    }
}
