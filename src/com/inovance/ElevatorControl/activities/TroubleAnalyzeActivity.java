package com.inovance.ElevatorControl.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.widget.TextView;
import butterknife.InjectView;
import butterknife.Views;
import com.bluetoothtool.*;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.adapters.TroubleAnalyzeAdapter;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.daos.ErrorHelpDao;
import com.inovance.ElevatorControl.daos.ParameterGroupSettingsDao;
import com.inovance.ElevatorControl.handlers.HistoryErrorHandler;
import com.inovance.ElevatorControl.models.*;
import com.inovance.ElevatorControl.utils.LogUtils;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import com.viewpagerindicator.TabPageIndicator;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.Toast;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 故障分析
 */
public class TroubleAnalyzeActivity extends Activity implements Runnable {

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

    private HistoryErrorHandler historyErrorHandler;

    private BluetoothTalk[] currentCommunications;

    public BluetoothTalk[] historyCommunications;

    private FCGroupHandler fcGroupHandler;

    private RestoreErrorHandler restoreErrorHandler;

    public int pageIndex;

    private Handler getCurrentTroubleHandler;

    private boolean isRestoreSuccessful = false;

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
        currentGetCurrentTroubleCommunications();
        createGetFCGroupValueCommunications();
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
                    case 0:
                        readCurrentTroubleStatus();
                        break;
                    case 1: {
                        new CountDownTimer(2000, 500) {

                            @Override
                            public void onTick(long l) {
                                pool.execute(TroubleAnalyzeActivity.this);
                                sendEmptyMessage(0);
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
        if (pageIndex != pager.getCurrentItem()) {
            pager.setCurrentItem(pageIndex);
        }
        reSyncData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        BluetoothTool.getInstance(this).setTalkType(BluetoothTalk.NORMAL_TALK);
        BluetoothTool.getInstance(TroubleAnalyzeActivity.this).setHandler(null);
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
        final BluetoothTalk[] talks = new BluetoothTalk[]{
                new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16(SerialUtility.hexStr2Ints("0106"
                                + DeviceFactory.getInstance().getRestoreErrorCode()
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
                }
        };
        isRestoreSuccessful = false;
        new CountDownTimer(1500, 500) {
            public void onTick(long millisUntilFinished) {
                if (!isRestoreSuccessful) {
                    if (BluetoothTool.getInstance(TroubleAnalyzeActivity.this).isPrepared()) {
                        restoreErrorHandler.sendCode = DeviceFactory.getInstance().getRestoreErrorCode();
                        BluetoothTool.getInstance(TroubleAnalyzeActivity.this)
                                .setHandler(restoreErrorHandler)
                                .setCommunications(talks)
                                .send();
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

    /**
     * 生成读取当前故障通信内容
     */
    private void currentGetCurrentTroubleCommunications() {
        if (currentCommunications == null) {
            currentCommunications = new BluetoothTalk[]{
                    new BluetoothTalk() {

                        @Override
                        public void beforeSend() {
                            this.setSendBuffer(SerialUtility.crc16(SerialUtility.hexStr2Ints("010380000001")));
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
                    }
            };
        }
    }

    public void createGetFCGroupValueCommunications() {
        if (historyCommunications == null) {
            ParameterGroupSettings parameterGroupSettings = ParameterGroupSettingsDao.findById(
                    this, 13);
            final List<ParameterSettings> settingsList = parameterGroupSettings.getParametersettings().getList();
            if (settingsList != null) {
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
                                    .hexStr2Ints("0103"
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
                                            byte[] tempData = SerialUtility.crc16(SerialUtility.hexStr2Ints("01030002"
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
        }
    }

    /**
     * 当前故障
     */
    public void loadCurrentTroubleView() {
        if (BluetoothTool.getInstance(this).isPrepared()) {
            handler.sendEmptyMessage(3);
            BluetoothTool.getInstance(this).setHandler(null);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    getCurrentTroubleHandler.sendEmptyMessage(1);
                }
            }, 1000);
        } else {
            handler.sendEmptyMessage(1);
        }
    }

    /**
     * 读取当前故障状态
     */
    private void readCurrentTroubleStatus() {
        Hashtable<String, byte[]> trouble = BluetoothTool.getInstance(this).getCurrentTroubleValueSet();
        if (trouble != null && trouble.size() == 1) {
            byte[] received = SerialUtility.trimEnd(trouble.get("8000"));
            String errorCode = ParseSerialsUtils.getErrorCode(received);
            ErrorHelp errorHelp = ErrorHelpDao.findByDisplay(TroubleAnalyzeActivity.this, errorCode);
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
                            viewCurrentSystemStatus();
                        }
                    });
                    restoreErrorStatus.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            restoreErrorStatus();
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
    }

    /**
     * 历史故障
     */
    public void loadHistoryTroubleView() {
        if (BluetoothTool.getInstance(this).isPrepared()) {
            handler.sendEmptyMessage(4);
            BluetoothTool.getInstance(this).setHandler(null);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    pool.execute(TroubleAnalyzeActivity.this);
                }
            }, 1000);
        } else {
            handler.sendEmptyMessage(2);
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
            case 0: {
                if (((NavigationTabActivity) getParent()).hasGetDeviceTypeAndNumber) {
                    BluetoothTool.getInstance(TroubleAnalyzeActivity.this).setTalkType(BluetoothTalk.CURRENT_TROUBLE_TALK);
                    BluetoothTool.getInstance(TroubleAnalyzeActivity.this)
                            .setHandler(null)
                            .setCommunications(currentCommunications)
                            .send();
                }
            }
            break;
            case 1: {
                if (historyCommunications != null && historyCommunications.length > 0) {
                    fcGroupHandler.sendCount = historyCommunications.length;
                    BluetoothTool.getInstance(TroubleAnalyzeActivity.this).setTalkType(BluetoothTalk.NORMAL_TALK);
                    BluetoothTool.getInstance(TroubleAnalyzeActivity.this)
                            .setHandler(fcGroupHandler)
                            .setCommunications(historyCommunications)
                            .send();
                }
            }
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
            } else {
                TroubleAnalyzeActivity.this.loadHistoryTroubleView();
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
                            "0106" + DeviceFactory.getInstance().getRestoreErrorCode() + "0001",
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
