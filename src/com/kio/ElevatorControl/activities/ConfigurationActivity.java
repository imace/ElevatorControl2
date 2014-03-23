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
                /*
                HBluetooth.getInstance(ConfigurationActivity.this).setHandler(configurationHandler);
                try {
                    // 反射执行
                    String mName = MenuValuesDao.getConfigurationLoadMethodName(index, ConfigurationActivity.this);
                    Log.v(TAG, String.valueOf(index) + " : " + mName);
                    ConfigurationActivity.this.getClass().getMethod(mName).invoke(ConfigurationActivity.this);
                } catch (NoSuchMethodException e) {
                    Log.e(TAG, e.getMessage());
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, e.getMessage());
                } catch (IllegalAccessException e) {
                    Log.e(TAG, e.getMessage());
                } catch (InvocationTargetException e) {
                    Log.e(TAG, "InvocationTargetException");
                } finally {

                }
                */
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        HBluetooth.getInstance(ConfigurationActivity.this).setHandler(configurationHandler);
                        try {
                            // 反射执行
                            String mName = MenuValuesDao.getConfigurationLoadMethodName(mCurrentPageIndex, ConfigurationActivity.this);
                            Log.v(TAG, String.valueOf(mCurrentPageIndex) + " : " + mName);
                            ((Object) ConfigurationActivity.this).getClass().getMethod(mName).invoke(ConfigurationActivity.this);
                        } catch (NoSuchMethodException e) {
                            Log.e(TAG, e.getMessage());
                        } catch (IllegalArgumentException e) {
                            Log.e(TAG, e.getMessage());
                        } catch (IllegalAccessException e) {
                            Log.e(TAG, e.getMessage());
                        } catch (InvocationTargetException e) {
                            Log.e(TAG, "InvocationTargetException");
                        } finally {

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
        if (mCurrentPageIndex == 0){
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
                            RealTimeMonitor monitor = (RealTimeMonitor) realTimeMonitor.clone();
                            monitor.setReceived(received);
                            return monitor;
                        }
                        return null;
                    }
                };
            }
            if (HBluetooth.getInstance(this).isPrepared()){
                HBluetooth.getInstance(this)
                        .setHandler(configurationHandler)
                        .setCommunications(hCommunications)
                        .Start();
            }
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
