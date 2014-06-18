package com.inovance.ElevatorControl.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.widget.Toast;
import butterknife.InjectView;
import butterknife.Views;
import com.bluetoothtool.*;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.adapters.TroubleAnalyzeAdapter;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.daos.ErrorHelpDao;
import com.inovance.ElevatorControl.daos.ParameterSettingsDao;
import com.inovance.ElevatorControl.daos.RealTimeMonitorDao;
import com.inovance.ElevatorControl.handlers.CurrentErrorHandler;
import com.inovance.ElevatorControl.handlers.HistoryErrorHandler;
import com.inovance.ElevatorControl.models.*;
import com.inovance.ElevatorControl.utils.LogUtils;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import com.viewpagerindicator.TabPageIndicator;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 故障分析
 */
public class TroubleAnalyzeActivity extends FragmentActivity implements Runnable {

    private static final String TAG = TroubleAnalyzeActivity.class.getSimpleName();
    /**
     * View Pager
     */
    @InjectView(R.id.pager)
    public ViewPager pager;

    /**
     * View Pager Indicator
     */
    @InjectView(R.id.indicator)
    public TabPageIndicator indicator;

    private CurrentErrorHandler currentErrorHandler;

    private HistoryErrorHandler historyErrorHandler;

    private BluetoothTalk[] currentCommunications;

    public BluetoothTalk[] historyCommunications;

    private FCGroupHandler fcGroupHandler;

    private RestoreErrorHandler restoreErrorHandler;

    public int pageIndex;

    private boolean isRunning = false;

    public boolean isGetCurrentTrouble = false;

    private Handler getCurrentTroubleHandler;

    private boolean isRestoreSuccessful = false;

    private RealTimeMonitor restoreTroubleMonitor;

    private static final int RestoreErrorSuccessful = 10;

    private ExecutorService pool = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trouble_analyze);
        Views.inject(this);
        pager.setAdapter(new TroubleAnalyzeAdapter(this));
        pager.setOffscreenPageLimit(3);
        indicator.setViewPager(pager);
        currentErrorHandler = new CurrentErrorHandler(this);
        historyErrorHandler = new HistoryErrorHandler(this);
        restoreErrorHandler = new RestoreErrorHandler(this);
        fcGroupHandler = new FCGroupHandler(this);
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
                switch (pageIndex) {
                    case 0:
                        loadCurrentTroubleView();
                        break;
                    case 1:
                        loadHistoryTroubleView();
                        break;
                }
            }
        });
        getCurrentTroubleHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1: {
                        isGetCurrentTrouble = false;
                        new CountDownTimer(2000, 500) {

                            @Override
                            public void onTick(long l) {
                                if (isRunning) {
                                    if (!isGetCurrentTrouble) {
                                        new Timer().schedule(new TimerTask() {
                                            @Override
                                            public void run() {
                                                pool.execute(TroubleAnalyzeActivity.this);
                                            }
                                        }, 500);
                                    } else {
                                        this.cancel();
                                    }
                                } else {
                                    this.cancel();
                                }
                            }

                            @Override
                            public void onFinish() {

                            }
                        }.start();
                    }
                    break;
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        isRunning = true;
        if (pageIndex != pager.getCurrentItem()) {
            pager.setCurrentItem(pageIndex);
        }
        reSyncData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isRunning = false;
    }

    /**
     * Change Pager Index
     *
     * @param index Index
     */
    public void changePagerIndex(int index) {
        if (index != pager.getCurrentItem()) {
            pager.setCurrentItem(index);
        }
    }

    public void reSyncData() {
        currentCommunications = null;
        historyCommunications = null;
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
        startActivity(new Intent(this, ViewErrorStatusActivity.class));
    }

    /**
     * 恢复故障状态
     */
    public void restoreErrorStatus() {
        restoreTroubleMonitor = RealTimeMonitorDao.findByStateID(this, ApplicationConfig.RestoreTroubleStateCode);
        if (restoreTroubleMonitor != null) {
            final BluetoothTalk[] talks = new BluetoothTalk[1];
            talks[0] = new BluetoothTalk() {
                @Override
                public void beforeSend() {
                    this.setSendBuffer(SerialUtility.crc16(SerialUtility.hexStringToInt("0106"
                            + restoreTroubleMonitor.getCode()
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
                    byte[] received = SerialUtility.trimEnd(getReceivedBuffer());
                    return SerialUtility.byte2HexStr(received);
                }
            };
            isRestoreSuccessful = false;
            new CountDownTimer(1500, 500) {
                public void onTick(long millisUntilFinished) {
                    if (!isRestoreSuccessful) {
                        if (isRunning) {
                            if (BluetoothTool.getInstance().isPrepared()) {
                                restoreErrorHandler.sendCode = restoreTroubleMonitor.getCode();
                                BluetoothTool.getInstance()
                                        .setHandler(restoreErrorHandler)
                                        .setCommunications(talks)
                                        .send();
                            }
                        } else {
                            this.cancel();
                        }

                    } else {
                        handler.sendEmptyMessage(RestoreErrorSuccessful);
                        this.cancel();
                        this.onFinish();
                    }
                }

                public void onFinish() {

                }
            }.start();
        }
    }

    /**
     * 当前故障
     */
    public void loadCurrentTroubleView() {
        if (BluetoothTool.getInstance().isPrepared()) {
            handler.sendEmptyMessage(3);
            getCurrentTroubleHandler.sendEmptyMessage(1);
        } else {
            handler.sendEmptyMessage(1);
        }
    }

    /**
     * 历史故障
     */
    public void loadHistoryTroubleView() {
        if (BluetoothTool.getInstance().isPrepared()) {
            handler.sendEmptyMessage(4);
            pool.execute(TroubleAnalyzeActivity.this);
        } else {
            handler.sendEmptyMessage(2);
        }
    }

    /**
     * 开始取得当前故障通信
     */
    private void startGetCurrentTroubleCommunication() {
        if (currentCommunications == null) {
            final RealTimeMonitor monitor = RealTimeMonitorDao
                    .findByStateID(this, ApplicationConfig.CurrentTroubleStateCode);
            if (monitor != null) {
                currentCommunications = new BluetoothTalk[1];
                currentCommunications[0] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16(SerialUtility.hexStringToInt("0103"
                                + monitor.getCode()
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
                        if (SerialUtility.isCRC16Valid(getReceivedBuffer())) {
                            byte[] received = SerialUtility.trimEnd(getReceivedBuffer());
                            String errorCode = ParseSerialsUtils.getErrorCode(received);
                            return ErrorHelpDao.findByDisplay(TroubleAnalyzeActivity.this, errorCode);
                        }
                        return null;
                    }
                };
            }
        }
        if (BluetoothTool.getInstance().isPrepared()) {
            currentErrorHandler.sendCount = currentCommunications.length;
            BluetoothTool.getInstance()
                    .setHandler(currentErrorHandler)
                    .setCommunications(currentCommunications)
                    .send();
        }
    }

    private String[] generateHistoryErrorStatusFilters() {
        String[] filters = new String[]{};
        String deviceType = ConfigFactory.getInstance().getDeviceName();
        int[] index = new int[]{};
        if (deviceType.equalsIgnoreCase(ApplicationConfig.NormalDeviceType[0])) {
            index = new int[]{4, 13};
        }
        if (deviceType.equalsIgnoreCase(ApplicationConfig.NormalDeviceType[1])) {
            index = new int[]{16, 47};
        }
        if (deviceType.equalsIgnoreCase(ApplicationConfig.NormalDeviceType[2])) {
            index = new int[]{6, 32};
        }
        if (deviceType.equalsIgnoreCase(ApplicationConfig.NormalDeviceType[3])) {
            index = new int[]{20, 73};
        }
        if (index.length == 2) {
            List<String> codeList = new ArrayList<String>();
            for (int i = index[0]; i < index[1]; i++) {
                codeList.add(String.format("FC%02d", i));
            }
            filters = codeList.toArray(new String[codeList.size()]);
        }
        return filters;
    }

    /**
     * 开始取得历史故障通信
     */
    private void startGetCurrentHistoryCommunication() {
        if (historyCommunications == null) {
            String[] filters = generateHistoryErrorStatusFilters();
            final List<ParameterSettings> settingsList = ParameterSettingsDao.findByCodes(this, filters);
            final int size = settingsList.size();
            final int count = size <= 10 ? 1 : ((size - size % 10) / 10 + (size % 10 == 0 ? 0 : 1));
            historyCommunications = new BluetoothTalk[count];
            for (int i = 0; i < count; i++) {
                final int position = i;
                final ParameterSettings firstItem = settingsList.get(position * 10);
                final int length = size <= 10 ? size : (size % 10 == 0 ? 10 : ((position == count - 1) ? size % 10 : 10));
                historyCommunications[i] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16(SerialUtility
                                .hexStringToInt("0103"
                                        + ParseSerialsUtils.getCalculatedCode(firstItem)
                                        + String.format("%04x", length)
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
                        if (SerialUtility.isCRC16Valid(getReceivedBuffer())) {
                            byte[] data = SerialUtility.trimEnd(getReceivedBuffer());
                            short bytesLength = ByteBuffer.wrap(new byte[]{data[2], data[3]}).getShort();
                            if (length * 2 == bytesLength) {
                                List<ParameterSettings> tempList = new ArrayList<ParameterSettings>();
                                for (int j = 0; j < length; j++) {
                                    if (position * 10 + j < settingsList.size()) {
                                        ParameterSettings item = settingsList.get(position * 10 + j);
                                        byte[] tempData = SerialUtility.crc16(SerialUtility.hexStringToInt("01030002"
                                                + SerialUtility.byte2HexStr(new byte[]{data[4 + j * 2], data[5 + j * 2]})));
                                        item.setReceived(tempData);
                                        tempList.add(item);
                                    }
                                }
                                ObjectListHolder holder = new ObjectListHolder();
                                holder.setParameterSettingsList(tempList);
                                return holder;
                            }
                        }
                        return null;
                    }
                };
            }
        }
        if (BluetoothTool.getInstance().isPrepared()) {
            if (historyCommunications != null && historyCommunications.length > 0) {
                fcGroupHandler.sendCount = historyCommunications.length;
                BluetoothTool.getInstance()
                        .setHandler(fcGroupHandler)
                        .setCommunications(historyCommunications)
                        .send();
            }
        }
    }

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
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
                case RestoreErrorSuccessful: {
                    Toast.makeText(TroubleAnalyzeActivity.this,
                            R.string.restore_error_successful_text,
                            Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            }
        }
    };

    @Override
    public void run() {
        switch (pageIndex) {
            case 0:
                startGetCurrentTroubleCommunication();
                break;
            case 1:
                startGetCurrentHistoryCommunication();
                break;
        }
    }

    // ================================== FC Group Handler ========================= //
    private class FCGroupHandler extends BluetoothHandler {

        public int sendCount = 0;

        private int receiveCount = 0;

        public List<ParameterSettings> tempList;

        public FCGroupHandler(android.app.Activity activity) {
            super(activity);
            TAG = FCGroupHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            receiveCount = 0;
            tempList = new ArrayList<ParameterSettings>();
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (receiveCount == sendCount) {
                int startIndex = 0;
                int index = 0;
                for (ParameterSettings item : tempList) {
                    if (item.getCode().equalsIgnoreCase("FC20")) {
                        startIndex = index;
                        break;
                    }
                    index++;
                }
                int size = tempList.size();
                List<ParameterSettings> errorList = new ArrayList<ParameterSettings>();
                for (int m = 0; m < 44; m++) {
                    int position = startIndex + m;
                    if (position < size) {
                        errorList.add(tempList.get(position));
                    }
                }
                int errorSize = errorList.size();
                historyErrorHandler.sendEmptyMessage(BluetoothState.onMultiTalkBegin);
                for (int n = 0; n < 11; n++) {
                    int index01 = n * 4;
                    int index02 = n * 4 + 1;
                    int index03 = n * 4 + 2;
                    int index04 = n * 4 + 3;
                    if (index01 < errorSize && index02 < errorSize && index03 < errorSize && index04 < errorSize) {
                        byte[] data01 = errorList.get(index01).getReceived();
                        byte[] data02 = errorList.get(index02).getReceived();
                        byte[] data03 = errorList.get(index03).getReceived();
                        byte[] data04 = errorList.get(index04).getReceived();
                        if (data01 != null && data01.length > 6
                                && data02 != null && data02.length > 6
                                && data03 != null && data03.length > 6
                                && data04 != null && data04.length > 6) {
                            byte[] errorData = new byte[]{
                                    data01[4], data01[5],
                                    data02[4], data02[5],
                                    data03[4], data03[5],
                                    data04[4], data04[5]
                            };
                            HistoryError historyError = new HistoryError();
                            historyError.setData(errorData);

                            Message message = new Message();
                            message.what = BluetoothState.onTalkReceive;
                            historyErrorHandler.sendMessage(message);
                        }
                    }
                }
                historyErrorHandler.sendEmptyMessage(BluetoothState.onMultiTalkEnd);
            }
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj instanceof ObjectListHolder) {
                ObjectListHolder holder = (ObjectListHolder) msg.obj;
                for (ParameterSettings item : holder.getParameterSettingsList()) {
                    if (!item.getName().contains(ApplicationConfig.RETAIN_NAME)) {
                        if (tempList == null) {
                            tempList = new ArrayList<ParameterSettings>();
                        }
                        tempList.add(item);
                    }
                }
                receiveCount++;
            }
        }

        @Override
        public void onTalkError(Message msg) {
            super.onTalkError(msg);
        }

    }

    // ================================== 故障复位 ========================= //

    private class RestoreErrorHandler extends BluetoothHandler {

        public String sendCode;

        public RestoreErrorHandler(android.app.Activity activity) {
            super(activity);
            TAG = RestoreErrorHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof String) {
                String receive = (String) msg.obj;
                if (receive.contains(sendCode)) {
                    isRestoreSuccessful = true;
                    LogUtils.getInstance().write(ApplicationConfig.LogRestoreErrorStatus,
                            "0106" + restoreTroubleMonitor.getCode() + "0001",
                            receive);
                }
            }
        }

        @Override
        public void onTalkError(Message msg) {
            super.onTalkError(msg);
        }

    }
}
