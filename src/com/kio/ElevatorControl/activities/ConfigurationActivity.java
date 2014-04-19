package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import butterknife.InjectView;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HHandler;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.adapters.ConfigurationAdapter;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.daos.ParameterSettingsDao;
import com.kio.ElevatorControl.daos.RealTimeMonitorDao;
import com.kio.ElevatorControl.handlers.ConfigurationHandler;
import com.kio.ElevatorControl.models.ParameterSettings;
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

    private GetX1ToX27StatusHandler getX1ToX27StatusHandler;

    private HCommunication[] getX1ToX27Communications;

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
        getX1ToX27StatusHandler = new GetX1ToX27StatusHandler(this);
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

    /**
     * 查看输入端状态
     *
     * @param monitor RealTimeMonitor
     */
    public void seeInputTerminalStatus(RealTimeMonitor monitor) {
        List<ParameterSettings> terminalList = ParameterSettingsDao.findByType(this, 3);
        if (getX1ToX27Communications == null) {
            int size = terminalList.size();
            getX1ToX27Communications = new HCommunication[size];
            for (int i = 0; i < size; i++) {
                final ParameterSettings settings = terminalList.get(i);
                communications[i] = new HCommunication() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103"
                                + settings.getCode()
                                + "0001")));
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
                        return null;
                    }
                };
            }
        }
    }

    /**
     * Start Get X1ToX27 Communications
     */
    private void startGetX1ToX27Communications() {
        if (getX1ToX27Communications != null) {
            if (HBluetooth.getInstance(this).isPrepared()) {
                getX1ToX27StatusHandler.sendCount = getX1ToX27Communications.length;
                HBluetooth.getInstance(this)
                        .setHandler(getX1ToX27StatusHandler)
                        .setCommunications(getX1ToX27Communications)
                        .Start();
            } else {
                Toast.makeText(ConfigurationActivity.this,
                        R.string.not_connect_device_error,
                        android.widget.Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    /**
     * 查看输出端子状态
     *
     * @param monitor RealTimeMonitor
     */
    public void seeOutputTerminalStatus(RealTimeMonitor monitor) {
        Log.v("AAABBB", HSerial.byte2HexStr(monitor.getCombineBytes()));
    }

    /**
     * Bluetooth socket error handler
     */
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

    // =============================== Get X1 - X24 Status Handler ====================================== //
    private class GetX1ToX27StatusHandler extends HHandler {

        public int sendCount;

        private int receiveCount;

        private List<ParameterSettings> settingsList;

        public GetX1ToX27StatusHandler(android.app.Activity activity) {
            super(activity);
            TAG = GetX1ToX27StatusHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
            settingsList = new ArrayList<ParameterSettings>();
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (sendCount == receiveCount) {

            } else {
                ConfigurationActivity.this.startGetX1ToX27Communications();
            }
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof ParameterSettings) {
                settingsList.add((ParameterSettings) msg.obj);
                receiveCount++;
            }
        }

        @Override
        public void onTalkError(Message msg) {
            super.onTalkError(msg);
            ConfigurationActivity.this.startGetX1ToX27Communications();
        }
    }

}
