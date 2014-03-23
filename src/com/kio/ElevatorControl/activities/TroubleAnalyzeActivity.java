package com.kio.ElevatorControl.activities;

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
import com.kio.ElevatorControl.adapters.TroubleAnalyzeAdapter;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.daos.MenuValuesDao;
import com.kio.ElevatorControl.daos.ParameterSettingsDao;
import com.kio.ElevatorControl.handlers.FailureCurrentHandler;
import com.kio.ElevatorControl.handlers.HistoryErrorHandler;
import com.kio.ElevatorControl.models.ErrorHelp;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.viewpagerindicator.TabPageIndicator;
import org.holoeverywhere.app.Activity;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * 故障分析
 *
 * @author jch
 */
public class TroubleAnalyzeActivity extends Activity {

    private static final String TAG = TroubleAnalyzeActivity.class.getSimpleName();
    /**
     * 注入页面元素
     */
    @InjectView(R.id.pager)
    public ViewPager pager;

    @InjectView(R.id.indicator)
    public TabPageIndicator indicator;

    private FailureCurrentHandler currentErrorHandler;

    private List<ParameterSettings> historyErrorSettingsLists;

    private HistoryErrorHandler historyErrorHandler;

    private HCommunication[] historyCommunications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trouble_analyze);

        // 注入以后才能使用@InjectView定义的对象
        Views.inject(this);

        // TroubleAnalyzeActivity --> TroubleAnalyzeAdapter --> TroubleAnalyzeFragment
        pager.setAdapter(new TroubleAnalyzeAdapter(this));
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
                final int currentPageIndex = index;
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // 反射执行
                            String mName = MenuValuesDao.getTroubleAnalyzeTabsLoadMethodName(currentPageIndex,
                                    TroubleAnalyzeActivity.this);
                            ((Object) TroubleAnalyzeActivity.this)
                                    .getClass()
                                    .getMethod(mName)
                                    .invoke(TroubleAnalyzeActivity.this);
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
        getHistoryErrorCode();
        historyErrorHandler = new HistoryErrorHandler(TroubleAnalyzeActivity.this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCurrentTroubleView();
    }

    /**
     * 取得历史故障查询CODE
     */
    private void getHistoryErrorCode() {
        ArrayList<String> names = new ArrayList<String>();
        for (int i = 0; i < 10; i++) {
            names.add(ApplicationConfig.HISTORY_ERROR_NAME.replace("&", String.valueOf(i + 1)));
        }
        names.add(ApplicationConfig.LAST_HISTORY_ERROR_NAME);
        historyErrorSettingsLists = ParameterSettingsDao.findByNames(TroubleAnalyzeActivity.this, names);
        int size = historyErrorSettingsLists.size();
        historyCommunications = new HCommunication[size];
        for (int i = 0; i < size; i++) {
            final ParameterSettings setting = historyErrorSettingsLists.get(i);
            historyCommunications[i] = new HCommunication() {
                @Override
                public void beforeSend() {
                    this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103"
                            + setting.getCode()
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
                        byte[] received = HSerial.trimEnd(getReceivedBuffer());
                        Log.v(TAG, HSerial.byte2HexStr(received));
                        ErrorHelp errorHelp = new ErrorHelp();
                        try {
                            errorHelp.setReceived(TroubleAnalyzeActivity.this, received);
                            return errorHelp;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                }
            };
        }
    }

    /**
     * 当前故障
     */
    public void loadCurrentTroubleView() {
        currentErrorHandler = new FailureCurrentHandler(this);
        HCommunication[] hCommunications = new HCommunication[]{
                new HCommunication() {

                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("010380000001")));
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
                            Log.v(TAG, HSerial.byte2HexStr(received));
                            ErrorHelp errorHelp = new ErrorHelp();
                            try {
                                errorHelp.setReceived(TroubleAnalyzeActivity.this, received);
                                // 将ep存入数据库
                                // ErrorHelpLogDao.Insert(TroubleAnalyzeActivity.this,
                                // ErrorHelpLog.Instance(ep));
                                return errorHelp;
                            } catch (Exception e) {
                                // 失败
                                Log.e(TAG, e.getLocalizedMessage());
                            }
                        }
                        return null;
                    }
                }
        };

        if (currentErrorHandler == null)
            currentErrorHandler = new FailureCurrentHandler(this);
        if (HBluetooth.getInstance(this).isPrepared())
            HBluetooth.getInstance(this)
                    .setHandler(currentErrorHandler)
                    .setCommunications(hCommunications)
                    .Start();
    }


    /**
     * 历史故障
     */
    public void loadHistoryTroubleView() {
        if (HBluetooth.getInstance(this).isPrepared())
            HBluetooth.getInstance(this)
                    .setHandler(historyErrorHandler)
                    .setCommunications(historyCommunications)
                    .Start();
    }

    /**
     * 故障查询
     */
    public void loadSearchTroubleView() {
        // 停止串口通信
        HBluetooth.getInstance(this).setHandler(currentErrorHandler);
    }
}
