package com.kio.ElevatorControl.adapters;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.kio.ElevatorControl.daos.MenuValues;
import com.kio.ElevatorControl.views.fragments.FirmwareManageFragment;

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
            FirmwareManageFragment firmwareManageFragment = FirmwareManageFragment.newInstance(
                    MenuValues.getConfigurationTabsTextsPosition(position, fragmentActivity),
                    fragmentActivity);
            mFragments.add(firmwareManageFragment);
            return firmwareManageFragment;
        } else {
            return mFragments.get(position);
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return MenuValues.getFirmwareManageTabsPageTitle(position, fragmentActivity);
    }

    @Override
    public int getCount() {
        return MenuValues.getFirmwareManageTabsCount(fragmentActivity);
    }
}
