package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import butterknife.InjectView;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.adapters.TransactionAdapter;
import com.kio.ElevatorControl.daos.RealtimeMonitorDao;
import com.kio.ElevatorControl.daos.ValuesDao;
import com.kio.ElevatorControl.handlers.TransactionHandler;
import com.kio.ElevatorControl.models.RealtimeMonitor;
import com.viewpagerindicator.TabPageIndicator;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * 大标签卡 电梯调试
 *
 * @author jch
 */
public class TransactionActivity extends FragmentActivity {

    private final String HTAG = "TransactionActivity";

    /**
     * 注入页面元素
     */
    @InjectView(R.id.pager)
    public ViewPager pager;

    @InjectView(R.id.indicator)
    protected TabPageIndicator indicator;

    private TransactionHandler transHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        Views.inject(this);
        // TransactionActivity->TransactionAdapter->TransactionFragment->按照tabIndex初始化各个标签对应的子页面
        pager.setAdapter(new TransactionAdapter(this));
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
                HBluetooth.getInstance(TransactionActivity.this).setHandler(transHandler);
                try {
                    // 反射执行
                    String mName = ValuesDao.getTransactionTabsLoadMethodName(index, TransactionActivity.this);
                    Log.v(HTAG, String.valueOf(index) + " : " + mName);
                    TransactionActivity.this.getClass().getMethod(mName).invoke(TransactionActivity.this);
                } catch (NoSuchMethodException e) {
                    Log.e(HTAG, e.getMessage());
                } catch (IllegalArgumentException e) {
                    Log.e(HTAG, e.getMessage());
                } catch (IllegalAccessException e) {
                    Log.e(HTAG, e.getMessage());
                } catch (InvocationTargetException e) {
                    Log.e(HTAG, "InvocationTargetException");
                } finally {
                }
            }

        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (transHandler == null)
            transHandler = new TransactionHandler(this);
        HBluetooth.getInstance(this).setHandler(transHandler);
        indicator.setCurrentItem(0);
        loadMonitorLV();
    }

    /**
     * 实时监控,加载内容 实时监控列表比较特殊,要在标签切换之后刷新 而标签切换只在一个activity中进行的
     * 于是放在activity中加载,另外三个标签都是静态内容放在Fragement中加载
     */
    public void loadMonitorLV() {
        // 连接成功的情况下
        if (HBluetooth.getInstance(null).isPrepared()) {
            List<RealtimeMonitor> rmlst = RealtimeMonitorDao.findAll(this);
            HCommunication[] cumms = new HCommunication[rmlst.size()];
            for (int index = 0; index < rmlst.size(); index++) {
                final String code = rmlst.get(index).getCode();
                final RealtimeMonitor rmontor = rmlst.get(index);
                // 生成发送的指令,逐个读取各个参数
                cumms[index] = new HCommunication() {
                    @Override
                    public void beforeSend() {
                        this.setSendbuffer(HSerial.crc16(HSerial.hexStr2Ints("0103" + code + "0001")));
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
                        if (HSerial.isCRC16Valid(getReceivebuffer())) {
                            // 通过验证
                            byte[] received = HSerial.trimEnd(getReceivebuffer());

                            RealtimeMonitor rm = (RealtimeMonitor) rmontor.clone();
                            rm.setReceived(received);
                            return rm;
                        }
                        return null;
                    }
                };
            }
            if (HBluetooth.getInstance(this).isPrepared())
                HBluetooth.getInstance(this).setCommunications(cumms).HStart();
        }
    }

    /***
     *
     */
    public void loadSettingsLV() {
        // 停止串口通信
        HBluetooth.getInstance(this).setHandler(transHandler);
    }

    public void loadTestLV() {
        // 停止串口通信
        HBluetooth.getInstance(this).setHandler(transHandler);
    }

    public void loadCopyLV() {
        // 停止串口通信
        HBluetooth.getInstance(this).setHandler(transHandler);
    }

}
