package com.kio.ElevatorControl.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.kio.ElevatorControl.daos.ValuesDao;
import com.kio.ElevatorControl.views.fragments.TransactionFragment;

public class TransactionAdapter extends FragmentPagerAdapter {

    private FragmentActivity fg;

    public FragmentActivity getFg() {
        return fg;
    }

    public void setFg(FragmentActivity fg) {
        this.fg = fg;
    }

    public TransactionAdapter(FragmentActivity frga) {
        super(frga.getSupportFragmentManager());
        fg = frga;
    }

    public TransactionAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        return TransactionFragment.newInstance(
                ValuesDao.getTransactionTabsTextsPosition(position, fg), fg);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return ValuesDao.getTransactionTabsPageTitle(position, fg);
    }

    @Override
    public int getCount() {
        return ValuesDao.getTransactionTabsTextsLength(fg);
    }

}
