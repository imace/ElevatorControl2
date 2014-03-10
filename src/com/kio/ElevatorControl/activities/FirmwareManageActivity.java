package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import butterknife.InjectView;
import butterknife.Views;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.adapters.FirmwareManageAdapter;
import com.viewpagerindicator.TabPageIndicator;
import org.holoeverywhere.app.Activity;

/**
 * 固件管理
 */
public class FirmwareManageActivity extends Activity {

    private static final String TAG = FirmwareManageActivity.class.getSimpleName();

    /**
     * 注入页面元素
     */
    @InjectView(R.id.pager)
    public ViewPager pager;

    @InjectView(R.id.indicator)
    protected TabPageIndicator indicator;

    private FirmwareManageAdapter mFirmwareManageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firmware_manage);
        Views.inject(this);

        mFirmwareManageAdapter = new FirmwareManageAdapter(this);
        pager.setAdapter(mFirmwareManageAdapter);
        indicator.setViewPager(pager);
        indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {

            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFirmwareApplyView();
    }

    public void loadFirmwareApplyView() {

    }

    public void loadFirmwareDownloadView() {

    }

    public void loadFirmwareBurnView() {

    }
}
