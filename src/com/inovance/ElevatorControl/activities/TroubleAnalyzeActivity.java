package com.inovance.elevatorcontrol.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.inovance.bluetoothtool.BluetoothTalk;
import com.inovance.bluetoothtool.BluetoothTool;
import com.inovance.bluetoothtool.SerialUtility;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.adapters.TroubleAnalyzeAdapter;
import com.inovance.elevatorcontrol.cache.ValueCache;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.config.ParameterUpdateTool;
import com.inovance.elevatorcontrol.daos.ErrorHelpDao;
import com.inovance.elevatorcontrol.daos.ParameterSettingsDao;
import com.inovance.elevatorcontrol.daos.RealTimeMonitorDao;
import com.inovance.elevatorcontrol.handlers.CurrentErrorHandler;
import com.inovance.elevatorcontrol.handlers.HistoryErrorHandler;
import com.inovance.elevatorcontrol.handlers.MessageHandler;
import com.inovance.elevatorcontrol.handlers.UnlockHandler;
import com.inovance.elevatorcontrol.models.ErrorHelp;
import com.inovance.elevatorcontrol.models.ObjectListHolder;
import com.inovance.elevatorcontrol.models.ParameterSettings;
import com.inovance.elevatorcontrol.models.RealTimeMonitor;
import com.inovance.elevatorcontrol.utils.LogUtils;
import com.inovance.elevatorcontrol.utils.ParseSerialsUtils;
import com.viewpagerindicator.TabPageIndicator;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.InjectView;
import butterknife.Views;

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

    private HistoryErrorHandler historyTroubleHandler;

    private BluetoothTalk[] currentCommunications;

    public BluetoothTalk[] historyCommunications;

    private RestoreErrorHandler restoreErrorHandler;

    public int pageIndex;

    private boolean isRunning = false;

    /**
     * 同步时间间隔
     */
    private static final int SYNC_TIME = 500;

    public boolean isSyncing = false;

    public boolean hasGetHistoryTrouble = false;

    /**
     * 读取当前故障历史故障
     */
    private Runnable syncTask;

    private Handler syncHandler = new Handler();

    private boolean isRestoreSuccessful = false;

    private RealTimeMonitor restoreTroubleMonitor;

    private static final int RestoreErrorSuccessful = 10;

    private ViewStatusHandler viewStatusHandler;

    private ExecutorService pool = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trouble_analyze);
        Views.inject(this);
        pager.setAdapter(new TroubleAnalyzeAdapter(this));
        pager.setOffscreenPageLimit(3);
        indicator.setViewPager(pager);
        viewStatusHandler = new ViewStatusHandler(this);
        currentErrorHandler = new CurrentErrorHandler(this);
        historyTroubleHandler = new HistoryErrorHandler(this);
        restoreErrorHandler = new RestoreErrorHandler(this);
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
                        hasGetHistoryTrouble = false;
                        loadHistoryTroubleView();
                        break;
                }
            }
        });
        syncTask = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    if (BluetoothTool.getInstance().isPrepared()) {
                        if (!isSyncing) {
                            pool.execute(TroubleAnalyzeActivity.this);
                        }
                        syncHandler.postDelayed(syncTask, SYNC_TIME);
                    }
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        hasGetHistoryTrouble = false;
        currentCommunications = null;
        historyCommunications = null;
        if (pageIndex != pager.getCurrentItem()) {
            pager.setCurrentItem(pageIndex);
        }
        if (BluetoothTool.getInstance().isPrepared()) {
            isRunning = true;
            isSyncing = false;
            syncHandler.postDelayed(syncTask, SYNC_TIME);
        }
        reSyncData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isRunning = false;
    }

    /**
     * Read error status cache from HomeActivity
     */
    private void readErrorStatusFromCache() {
        byte[] errorData = ValueCache.getInstance().getErrorData();
        if (errorData != null) {
            String errorCode = ParseSerialsUtils.getErrorCode(errorData);
            final ErrorHelp errorHelp = ErrorHelpDao.findByDisplay(TroubleAnalyzeActivity.this, errorCode);
            if (errorHelp != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayErrorInformation(errorHelp);
                    }
                });
            }
            // Clear cached error data
            ValueCache.getInstance().setErrorData(null);
        } else {
            viewStatusHandler.sendEmptyMessage(3);
        }
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
                    this.setSendBuffer(SerialUtility.crc16("0106"
                            + restoreTroubleMonitor.getCode()
                            + "0001"));
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
                                        .startTask();
                            }
                        } else {
                            this.cancel();
                        }

                    } else {
                        viewStatusHandler.sendEmptyMessage(RestoreErrorSuccessful);
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
            readErrorStatusFromCache();
        } else {
            viewStatusHandler.sendEmptyMessage(1);
        }
    }

    /**
     * 历史故障
     */
    public void loadHistoryTroubleView() {
        if (BluetoothTool.getInstance().isPrepared()) {
            viewStatusHandler.sendEmptyMessage(4);
        } else {
            viewStatusHandler.sendEmptyMessage(2);
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
                        this.setSendBuffer(SerialUtility.crc16("0103"
                                + monitor.getCode()
                                + "0001"));
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
        if (currentCommunications == null) {
            MessageHandler.getInstance(this).sendMessage(MessageHandler.CODE_DATA_ERROR);
        } else {
            if (BluetoothTool.getInstance().isPrepared()) {
                currentErrorHandler.sendCount = currentCommunications.length;
                isSyncing = true;
                BluetoothTool.getInstance()
                        .setHandler(currentErrorHandler)
                        .setCommunications(currentCommunications)
                        .startTask();
            }
        }
    }

    /**
     * Display error information
     *
     * @param errorHelp ErrorHelp
     */
    public void displayErrorInformation(ErrorHelp errorHelp) {
        View loadView = pager.findViewById(R.id.load_view);
        View errorView = pager.findViewById(R.id.error_view);
        View noErrorView = pager.findViewById(R.id.no_error_view);
        View noDeviceView = pager.findViewById(R.id.no_device_view);
        View viewSystemStatus = pager.findViewById(R.id.view_system_status);
        View restoreErrorStatus = pager.findViewById(R.id.restore_error_status);
        if (loadView != null && errorView != null && noErrorView != null && noDeviceView != null) {
            if (errorHelp != null) {
                TextView display = (TextView) pager.findViewById(R.id.current_error_help_display);
                TextView level = (TextView) pager.findViewById(R.id.current_error_help_level);
                TextView name = (TextView) pager.findViewById(R.id.current_error_help_name);
                TextView reason = (TextView) pager.findViewById(R.id.current_error_help_reason);
                TextView solution = (TextView) pager.findViewById(R.id.current_error_help_solution);
                name.setText(errorHelp.getName());
                display.setText(errorHelp.getDisplay());
                level.setText(errorHelp.getLevel());
                reason.setText(errorHelp.getReason());
                solution.setText(errorHelp.getSolution());
                loadView.setVisibility(View.GONE);
                noErrorView.setVisibility(View.GONE);
                noDeviceView.setVisibility(View.GONE);
                errorView.setVisibility(View.VISIBLE);
                viewSystemStatus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        TroubleAnalyzeActivity.this.viewCurrentSystemStatus();
                    }
                });
                restoreErrorStatus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TroubleAnalyzeActivity.this.restoreErrorStatus();
                    }
                });
            } else {
                loadView.setVisibility(View.GONE);
                noDeviceView.setVisibility(View.GONE);
                errorView.setVisibility(View.GONE);
                noErrorView.setVisibility(View.VISIBLE);
            }
        }
    }

    private String[] generateHistoryErrorStatusFilters() {
        String[] filters = new String[]{};
        String deviceType = ParameterUpdateTool.getInstance().getDeviceName();
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
            final List<ParameterSettings> settingsList = ParameterSettingsDao.findAllByCodes(this, filters);
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
                        this.setSendBuffer(SerialUtility.crc16("0103"
                                + ParseSerialsUtils.getCalculatedCode(firstItem)
                                + (length > 1 ? String.format("%04x", length) : "")));
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
                            if (data.length > 8) {
                                short bytesLength = ByteBuffer.wrap(new byte[]{data[2], data[3]}).getShort();
                                if (length * 2 == bytesLength) {
                                    List<ParameterSettings> tempList = new ArrayList<ParameterSettings>();
                                    for (int j = 0; j < length; j++) {
                                        if (position * 10 + j < settingsList.size()) {
                                            ParameterSettings item = settingsList.get(position * 10 + j);
                                            byte[] tempData = SerialUtility.crc16("01030002"
                                                    + SerialUtility.byte2HexStr(new byte[]{data[4 + j * 2], data[5 + j * 2]}));
                                            item.setReceived(tempData);
                                            tempList.add(item);
                                        }
                                    }
                                    ObjectListHolder holder = new ObjectListHolder();
                                    holder.setParameterSettingsList(tempList);
                                    return holder;
                                }
                            } else {
                                List<ParameterSettings> tempList = new ArrayList<ParameterSettings>();
                                ParameterSettings item = settingsList.get(position * 10);
                                item.setReceived(data);
                                tempList.add(item);
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
        if (historyCommunications == null) {
            MessageHandler.getInstance(this).sendMessage(MessageHandler.CODE_DATA_ERROR);
        } else {
            if (BluetoothTool.getInstance().isPrepared()) {
                isSyncing = true;
                historyTroubleHandler.sendCount = historyCommunications.length;
                BluetoothTool.getInstance()
                        .setHandler(historyTroubleHandler)
                        .setCommunications(historyCommunications)
                        .startTask();
            }
        }
    }

    private static class ViewStatusHandler extends Handler {

        private final WeakReference<TroubleAnalyzeActivity> mActivity;

        public ViewStatusHandler(TroubleAnalyzeActivity activity) {
            mActivity = new WeakReference<TroubleAnalyzeActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            TroubleAnalyzeActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case 0: {
                        Toast.makeText(activity, R.string.not_connect_device_error, android.widget.Toast.LENGTH_SHORT).show();
                    }
                    break;
                    case 1: {
                        View loadView = activity.pager.findViewById(R.id.load_view);
                        View noDeviceView = activity.pager.findViewById(R.id.no_device_view);
                        if (loadView != null && noDeviceView != null) {
                            loadView.setVisibility(View.GONE);
                            noDeviceView.setVisibility(View.VISIBLE);
                        }
                    }
                    break;
                    case 2: {
                        View loadView = activity.pager.findViewWithTag("history_load_view");
                        View noDeviceView = activity.pager.findViewWithTag("history_no_device_view");
                        if (loadView != null && noDeviceView != null) {
                            loadView.setVisibility(View.GONE);
                            noDeviceView.setVisibility(View.VISIBLE);
                        }
                    }
                    break;
                    case 3: {
                        View loadView = activity.pager.findViewWithTag("load_view");
                        View noDeviceView = activity.pager.findViewWithTag("no_device_view");
                        View errorView = activity.pager.findViewWithTag("error_view");
                        View noErrorView = activity.pager.findViewWithTag("no_error_view");
                        if (loadView != null && errorView != null && noErrorView != null && noDeviceView != null) {
                            noDeviceView.setVisibility(View.GONE);
                            errorView.setVisibility(View.GONE);
                            noErrorView.setVisibility(View.GONE);
                            loadView.setVisibility(View.VISIBLE);
                        }
                    }
                    break;
                    case 4: {
                        View loadView = activity.pager.findViewWithTag("history_load_view");
                        View noDeviceView = activity.pager.findViewWithTag("history_no_device_view");
                        View errorView = activity.pager.findViewWithTag("history_error_view");
                        View noErrorView = activity.pager.findViewWithTag("history_no_error_view");
                        if (loadView != null && noDeviceView != null && errorView != null && noErrorView != null) {
                            noErrorView.setVisibility(View.GONE);
                            noDeviceView.setVisibility(View.GONE);
                            errorView.setVisibility(View.GONE);
                            loadView.setVisibility(View.VISIBLE);
                        }
                    }
                    break;
                    case RestoreErrorSuccessful: {
                        Toast.makeText(activity, R.string.restore_error_successful_text, Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
            }
        }

    }

    @Override
    public void run() {
        switch (pageIndex) {
            case 0:
                startGetCurrentTroubleCommunication();
                break;
            case 1:
                if (!hasGetHistoryTrouble) {
                    startGetCurrentHistoryCommunication();
                }
                break;
        }
    }

    // ================================== 故障复位 ========================= //

    private class RestoreErrorHandler extends UnlockHandler {

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

    }
}
