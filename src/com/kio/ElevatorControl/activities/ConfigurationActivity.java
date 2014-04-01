package com.kio.ElevatorControl.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import butterknife.InjectView;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.adapters.ConfigurationAdapter;
import com.kio.ElevatorControl.daos.MenuValuesDao;
import com.kio.ElevatorControl.daos.RealTimeMonitorDao;
import com.kio.ElevatorControl.handlers.ConfigurationHandler;
import com.kio.ElevatorControl.models.RealTimeMonitor;
import com.viewpagerindicator.TabPageIndicator;
import org.holoeverywhere.app.Activity;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * 大标签卡 电梯调试
 *
 * @author jch
 */
public class ConfigurationActivity extends Activity {

    private static final String TAG = ConfigurationActivity.class.getSimpleName();

    private int mCurrentPageIndex;

    /**
     * 注入页面元素
     */
    @InjectView(R.id.pager)
    public ViewPager pager;

    @InjectView(R.id.indicator)
    protected TabPageIndicator indicator;

    private ConfigurationHandler configurationHandler;

    public ConfigurationAdapter mConfigurationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        Views.inject(this);
        // ConfigurationActivity->ConfigurationAdapter->ConfigurationFragment->按照tabIndex初始化各个标签对应的子页面
        configurationHandler = new ConfigurationHandler(this);
        mCurrentPageIndex = 0;
        mConfigurationAdapter = new ConfigurationAdapter(this);
        pager.setAdapter(mConfigurationAdapter);
        pager.setOffscreenPageLimit(3);
        indicator.setViewPager(pager);
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
                        HBluetooth.getInstance(ConfigurationActivity.this).setHandler(configurationHandler);
                        try {
                            // 反射执行
                            String mName = MenuValuesDao
                                    .getConfigurationLoadMethodName(mCurrentPageIndex, ConfigurationActivity.this);
                            ((Object) ConfigurationActivity.this).getClass().getMethod(mName)
                                    .invoke(ConfigurationActivity.this);
                        } catch (NoSuchMethodException e) {
                            Log.e(TAG, e.getMessage());
                        } catch (IllegalArgumentException e) {
                            Log.e(TAG, e.getMessage());
                        } catch (IllegalAccessException e) {
                            Log.e(TAG, e.getMessage());
                        } catch (InvocationTargetException e) {
                            Log.e(TAG, "InvocationTargetException");
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
        if (mCurrentPageIndex == 0) {
            loadMonitorView();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
    }

    /**
     * 实时监控,加载内容 实时监控列表比较特殊,要在标签切换之后刷新 而标签切换只在一个activity中进行的
     * 于是放在activity中加载,另外三个标签都是静态内容放在Fragment中加载
     */
    public void loadMonitorView() {
        // 连接成功的情况下
        /*
        if (HBluetooth.getInstance(null).isPrepared()) {
            List<RealTimeMonitor> monitorList = RealTimeMonitorDao.findAll(this);
            HCommunication[] hCommunications = new HCommunication[monitorList.size()];
            int commandSize = monitorList.size();
            for (int index = 0; index < commandSize; index++) {
                final String code = monitorList.get(index).getCode();
                final RealTimeMonitor realTimeMonitor = monitorList.get(index);
                // 生成发送的指令,逐个读取各个参数
                hCommunications[index] = new HCommunication() {
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
                            // 通过验证
                            byte[] received = HSerial.trimEnd(getReceivedBuffer());
                            RealTimeMonitor monitor = null;
                            try {
                                monitor = (RealTimeMonitor) realTimeMonitor.clone();
                                monitor.setReceived(received);
                                return monitor;
                            } catch (CloneNotSupportedException e) {
                                e.printStackTrace();
                            }
                        }
                        return null;
                    }
                };
            }
            if (HBluetooth.getInstance(this).isPrepared()) {
                HBluetooth.getInstance(this)
                        .setHandler(configurationHandler)
                        .setCommunications(hCommunications)
                        .Start();
            }
        }
        */
        combinationCommunicationsAndSend();
    }

    /**
     * Combination Communications And Send
     */
    private void combinationCommunicationsAndSend() {
        List<RealTimeMonitor> monitorList = RealTimeMonitorDao.findAll(this);
        final int size = monitorList.size();
        List<int[]> sections = new ArrayList<int[]>();
        int startIndex = 0;
        for (int index = 0; index < size; index++) {
            int startPrefix = Integer.parseInt(monitorList.get(startIndex).getCode().substring(0, 1));
            int prefix = Integer.parseInt(monitorList.get(index).getCode().substring(0, 1));
            int nextPrefix = index < size - 1
                    ? Integer.parseInt(monitorList.get(index + 1).getCode().substring(0, 1))
                    : prefix;
            if (index - startIndex < 9) {
                if (prefix != startPrefix) {
                    sections.add(new int[]{startIndex, index - 1});
                    startIndex = index + 1;
                    if (prefix != nextPrefix || index == size - 1) {
                        sections.add(new int[]{index, index});
                    }
                }
            } else if (index - startIndex == 9) {
                sections.add(new int[]{startIndex, index});
                startIndex = index + 1;
            }
        }
        int sectionSize = sections.size();
        HCommunication[] communications = new HCommunication[sectionSize];
        for (int position = 0; position < sectionSize; position++) {
            final int[] section = sections.get(position);
            final RealTimeMonitor firstItem = monitorList.get(section[0]);
            final int length = section[1] - section[0] + 1;
            Log.v("AAABBB", firstItem.getCode() + ":" + length);
            communications[position] = new HCommunication() {
                @Override
                public void beforeSend() {
                    this.setSendBuffer(HSerial.crc16(HSerial
                            .hexStr2Ints("0103"
                                    + firstItem.getCode()
                                    + String.format("%04x ", length)
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
                    if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                        byte[] data = HSerial.trimEnd(getReceivedBuffer());
                        Log.v("AAABBB", HSerial.byte2HexStr(data));
                    }
                    return null;
                }
            };
        }
        if (HBluetooth.getInstance(this).isPrepared()) {
            HBluetooth.getInstance(this)
                    .setCommunications(communications)
                    .Start();
        }
    }

    /***
     *
     */
    public void loadSettingView() {
        // 停止串口通信
        HBluetooth.getInstance(this).setHandler(configurationHandler);
    }

    public void loadDebugView() {
        // 停止串口通信
        HBluetooth.getInstance(this).setHandler(configurationHandler);
    }

    public void loadDuplicateView() {
        // 停止串口通信
        HBluetooth.getInstance(this).setHandler(configurationHandler);
    }

}
