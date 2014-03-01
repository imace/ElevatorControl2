package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import butterknife.InjectView;
import butterknife.Views;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.models.RootTabs;
import com.kio.ElevatorControl.views.fragments.TestFragment;
import com.viewpagerindicator.IconPagerAdapter;
import com.viewpagerindicator.TabPageIndicator;

public class ContainerActivity extends FragmentActivity {

    protected String[] CONTENTS = null;
    protected Integer[] ICONS = null;

    /**
     * 注入页面元素
     */
    @InjectView(R.id.pager)
    protected ViewPager pager;
    @InjectView(R.id.indicator)
    protected TabPageIndicator indicator;


    //初始化一级标签
    private void initTabConstant() {
        RootTabs uic = RootTabs.getTabInstance(this);
        CONTENTS = uic.getTexts();
        ICONS = uic.getIcons();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);
        //注入以后才能使用@InjectView定义的对象
        Views.inject(this);

        initTabConstant();

        pager.setAdapter(new GoogleMusicAdapter(
                getSupportFragmentManager()));
        indicator.setViewPager(pager);
    }

    class GoogleMusicAdapter extends FragmentPagerAdapter implements
            IconPagerAdapter {
        public GoogleMusicAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return TestFragment.newInstance(CONTENTS[position % CONTENTS.length]);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return CONTENTS[position % CONTENTS.length].toUpperCase();
        }

        @Override
        public int getIconResId(int index) {
            return ICONS[index];
        }

        @Override
        public int getCount() {
            return CONTENTS.length;
        }
    }

}
