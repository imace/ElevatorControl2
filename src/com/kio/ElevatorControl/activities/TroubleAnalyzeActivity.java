package com.kio.ElevatorControl.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
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
import org.holoeverywhere.widget.Toast;

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
        currentErrorHandler = new CurrentErrorHandler(this);
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
        switch (pageIndex) {
            case 0:
                loadCurrentTroubleView();
                break;
            case 1:
                loadHistoryTroubleView();
                break;
        }
    }

    /**
     * 查看系统状态
     */
    public void viewCurrentSystemStatus() {
        startActivity(new Intent(this, ViewSystemStatusActivity.class));
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
        if (HBluetooth.getInstance(this).isPrepared()) {
            currentErrorHandler.sendCount = currentCommunications.length;
            handler.sendEmptyMessage(3);
            HBluetooth.getInstance(this)
                    .setHandler(currentErrorHandler)
                    .setCommunications(currentCommunications)
                    .Start();
        } else {
            handler.sendEmptyMessage(1);
        }
    }

    /**
     * 历史故障
     */
    public void loadHistoryTroubleView() {
        if (HBluetooth.getInstance(this).isPrepared()) {
            handler.sendEmptyMessage(4);
            historyErrorHandler.sendCount = historyCommunications.length;
            HBluetooth.getInstance(this)
                    .setHandler(historyErrorHandler)
                    .setCommunications(historyCommunications)
                    .Start();
        } else {
            handler.sendEmptyMessage(2);
        }
    }

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0: {
                    Toast.makeText(TroubleAnalyzeActivity.this,
                            R.string.not_connect_device_error,
                            android.widget.Toast.LENGTH_SHORT)
                            .show();
                }
                break;
                case 1: {
                    View loadView = pager.findViewById(R.id.load_view);
                    View noDeviceView = pager.findViewById(R.id.no_device_view);
                    if (loadView != null && noDeviceView != null) {
                        loadView.setVisibility(View.GONE);
                        noDeviceView.setVisibility(View.VISIBLE);
                    }
                }
                break;
                case 2: {
                    View loadView = pager.findViewWithTag("history_load_view");
                    View noDeviceView = pager.findViewWithTag("history_no_device_view");
                    if (loadView != null && noDeviceView != null) {
                        loadView.setVisibility(View.GONE);
                        noDeviceView.setVisibility(View.VISIBLE);
                    }
                }
                break;
                case 3: {
                    View loadView = pager.findViewWithTag("load_view");
                    View noDeviceView = pager.findViewWithTag("no_device_view");
                    View errorView = pager.findViewWithTag("error_view");
                    View noErrorView = pager.findViewWithTag("no_error_view");
                    if (loadView != null && errorView != null && noErrorView != null && noDeviceView != null) {
                        noDeviceView.setVisibility(View.GONE);
                        errorView.setVisibility(View.GONE);
                        noErrorView.setVisibility(View.GONE);
                        loadView.setVisibility(View.VISIBLE);
                    }
                }
                break;
                case 4: {
                    View loadView = pager.findViewWithTag("history_load_view");
                    View noDeviceView = pager.findViewWithTag("history_no_device_view");
                    View errorView = pager.findViewWithTag("history_error_view");
                    View noErrorView = pager.findViewWithTag("history_no_error_view");
                    if (loadView != null && noDeviceView != null && errorView != null && noErrorView != null) {
                        noErrorView.setVisibility(View.GONE);
                        noDeviceView.setVisibility(View.GONE);
                        errorView.setVisibility(View.GONE);
                        loadView.setVisibility(View.VISIBLE);
                    }
                }
                break;
            }
            super.handleMessage(msg);
        }

    };
}
