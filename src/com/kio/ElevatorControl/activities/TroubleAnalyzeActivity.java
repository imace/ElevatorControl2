package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import butterknife.InjectView;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.adapters.TroubleAnalyzeAdapter;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.daos.ErrorHelpDao;
import com.kio.ElevatorControl.daos.ParameterSettingsDao;
import com.kio.ElevatorControl.handlers.CurrentErrorHandler;
import com.kio.ElevatorControl.handlers.HistoryErrorHandler;
import com.kio.ElevatorControl.models.HistoryError;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;
import com.viewpagerindicator.TabPageIndicator;
import org.holoeverywhere.app.Activity;

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

    private CurrentErrorHandler currentErrorHandler;

    private HistoryErrorHandler historyErrorHandler;

    private HCommunication[] currentCommunications;

    public HCommunication[] historyCommunications;

    private int pageIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trouble_analyze);
        Views.inject(this);
        pager.setAdapter(new TroubleAnalyzeAdapter(this));
        pager.setOffscreenPageLimit(3);
        indicator.setViewPager(pager);
        getHistoryErrorCode();
        historyErrorHandler = new HistoryErrorHandler(TroubleAnalyzeActivity.this);
        indicator.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageSelected(int index) {
                pageIndex = index;
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        switch (pageIndex) {
                            case 0:
                                loadCurrentTroubleView();
                                break;
                            case 1:
                                loadHistoryTroubleView();
                                break;
                            case 2:
                                loadSearchTroubleView();
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
        if (pageIndex == 0) {
            loadCurrentTroubleView();
        }
    }

    /**
     * 取得历史故障查询CODE
     */
    private void getHistoryErrorCode() {
        ArrayList<String> names = new ArrayList<String>();
        for (int i = 0; i < 10; i++) {
            names.add(ApplicationConfig.HISTORY_ERROR_CODE_NAME.replace("&", String.valueOf(i + 1)));
        }
        names.add(ApplicationConfig.LAST_HISTORY_ERROR_CODE_NAME);
        List<ParameterSettings> list = ParameterSettingsDao.findByNames(TroubleAnalyzeActivity.this, names);
        int size = list.size();
        historyCommunications = new HCommunication[size];
        for (int i = 0; i < size; i++) {
            final ParameterSettings item = list.get(i);
            historyCommunications[i] = new HCommunication() {
                @Override
                public void beforeSend() {
                    this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103"
                            + ParseSerialsUtils.splitAndConvertToHex(item.getCode())
                            + "0004"
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
                        HistoryError historyError = new HistoryError();
                        historyError.setData(received);
                        return historyError;
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
        if (currentCommunications == null) {
            currentCommunications = new HCommunication[]{
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
                                byte[] received = HSerial.trimEnd(getReceivedBuffer());
                                String errorCode = ParseSerialsUtils.getErrorCode(received);
                                return ErrorHelpDao.findByDisplay(TroubleAnalyzeActivity.this, errorCode);
                            }
                            return null;
                        }
                    }
            };
        }
        if (currentErrorHandler == null)
            currentErrorHandler = new CurrentErrorHandler(this);
        if (HBluetooth.getInstance(this).isPrepared())
            currentErrorHandler.sendCount = currentCommunications.length;
        HBluetooth.getInstance(this)
                .setHandler(currentErrorHandler)
                .setCommunications(currentCommunications)
                .Start();
    }

    /**
     * 历史故障
     */
    public void loadHistoryTroubleView() {
        if (HBluetooth.getInstance(this).isPrepared()) {
            HBluetooth.getInstance(this)
                    .setHandler(historyErrorHandler)
                    .setCommunications(historyCommunications)
                    .Start();
        }
    }

    /**
     * 故障查询
     */
    public void loadSearchTroubleView() {
        HBluetooth.getInstance(this).setHandler(currentErrorHandler);
    }
}
