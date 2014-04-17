package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import butterknife.InjectView;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.adapters.ConfigurationAdapter;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.daos.RealTimeMonitorDao;
import com.kio.ElevatorControl.handlers.ConfigurationHandler;
import com.kio.ElevatorControl.models.RealTimeMonitor;
import com.viewpagerindicator.TabPageIndicator;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * 大标签卡 电梯调试
 *
 * @author jch
 */
public class ConfigurationActivity extends Activity {

    private static final String TAG = ConfigurationActivity.class.getSimpleName();

    private int mCurrentPageIndex = 0;

    private ConfigurationHandler configurationHandler;

    private HCommunication[] communications;

    /**
     * 注入页面元素
     */
    @InjectView(R.id.pager)
    public ViewPager pager;

    @InjectView(R.id.indicator)
    protected TabPageIndicator indicator;

    public ConfigurationAdapter mConfigurationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        Views.inject(this);
        mConfigurationAdapter = new ConfigurationAdapter(this);
        pager.setAdapter(mConfigurationAdapter);
        pager.setOffscreenPageLimit(3);
        indicator.setViewPager(pager);
        configurationHandler = new ConfigurationHandler(this);
        indicator.setOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int arg0) {

            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageSelected(int index) {
                mCurrentPageIndex = index;
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        switch (mCurrentPageIndex) {
                            case 0:
                                loadMonitorView();
                                break;
                        }
                    }
                };
                Thread thread = new Thread(runnable);
                thread.start();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reSyncData();
    }

    public void reSyncData() {
        switch (mCurrentPageIndex) {
            case 0:
                loadMonitorView();
                break;
        }
    }

    /**
     * 实时监控,加载内容 实时监控列表比较特殊,要在标签切换之后刷新 而标签切换只在一个activity中进行的
     * 于是放在activity中加载,另外三个标签都是静态内容放在Fragment中加载
     */
    public void loadMonitorView() {
        if (communications == null) {
            List<RealTimeMonitor> monitorList = RealTimeMonitorDao.findByNames(this, ApplicationConfig.stateFilters);
            communications = new HCommunication[monitorList.size()];
            int commandSize = monitorList.size();
            for (int index = 0; index < commandSize; index++) {
                final String code = monitorList.get(index).getCode();
                final RealTimeMonitor monitor = monitorList.get(index);
                communications[index] = new HCommunication() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103" + code + "0001")));
                    }

                    @Override
                    public void afterSend() {

                    }

                    @Override
                    public void beforeReceive() {

                    }

                    @Override
                    public void afterReceive() {

                    }

                    @Override
                    public Object onParse() {
                        if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                            byte[] received = HSerial.trimEnd(getReceivedBuffer());
                            monitor.setReceived(received);
                            return monitor;
                        }
                        return null;
                    }
                };
            }
        }
        if (HBluetooth.getInstance(this).isPrepared()) {
            configurationHandler.sendCount = communications.length;
            HBluetooth.getInstance(this)
                    .setHandler(configurationHandler)
                    .setCommunications(communications)
                    .Start();
        } else {
            handler.sendEmptyMessage(0);
        }
    }

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0: {
                    Toast.makeText(ConfigurationActivity.this,
                            R.string.not_connect_device_error,
                            android.widget.Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            }
            super.handleMessage(msg);
        }

    };

}
