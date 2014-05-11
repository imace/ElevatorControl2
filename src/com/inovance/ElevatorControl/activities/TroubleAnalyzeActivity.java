package com.inovance.ElevatorControl.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import butterknife.InjectView;
import butterknife.Views;
import com.bluetoothtool.*;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.adapters.TroubleAnalyzeAdapter;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.daos.ErrorHelpDao;
import com.inovance.ElevatorControl.daos.ParameterGroupSettingsDao;
import com.inovance.ElevatorControl.handlers.CurrentErrorHandler;
import com.inovance.ElevatorControl.handlers.GlobalHandler;
import com.inovance.ElevatorControl.handlers.HistoryErrorHandler;
import com.inovance.ElevatorControl.models.HistoryError;
import com.inovance.ElevatorControl.models.ObjectListHolder;
import com.inovance.ElevatorControl.models.ParameterGroupSettings;
import com.inovance.ElevatorControl.models.ParameterSettings;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import com.viewpagerindicator.TabPageIndicator;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 故障分析
 *
 * @author jch
 */
public class TroubleAnalyzeActivity extends Activity implements Runnable {

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

    private BluetoothTalk[] currentCommunications;

    public BluetoothTalk[] historyCommunications;

    private FCGroupHandler fcGroupHandler;

    public int pageIndex;

    public boolean isGetCurrentTrouble = false;

    private Handler getCurrentTroubleHandler;

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
        currentErrorHandler = new CurrentErrorHandler(this);
        historyErrorHandler = new HistoryErrorHandler(this);
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
                            }

                            @Override
                            public void onFinish() {
                                if (!isGetCurrentTrouble) {
                                    if (!BluetoothTool.getInstance(TroubleAnalyzeActivity.this)
                                            .hasAlertNotConnectMessage()) {
                                        GlobalHandler.getInstance(TroubleAnalyzeActivity.this)
                                                .sendMessage(GlobalHandler.NOT_CONNECTED);
                                        BluetoothTool.getInstance(TroubleAnalyzeActivity.this)
                                                .setHasAlertNotConnectMessage(true);
                                    }
                                }
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
        BluetoothTool.getInstance(TroubleAnalyzeActivity.this)
                .setHandler(null);
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
                            if (SerialUtility.isCRC16Valid(getReceivedBuffer())) {
                                byte[] received = SerialUtility.trimEnd(getReceivedBuffer());
                                String errorCode = ParseSerialsUtils.getErrorCode(received);
                                return ErrorHelpDao.findByDisplay(TroubleAnalyzeActivity.this, errorCode);
                            }
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
        if (BluetoothTool.getInstance(this).isConnected()) {
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
        if (BluetoothTool.getInstance(this).isConnected()) {
            handler.sendEmptyMessage(4);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    pool.execute(TroubleAnalyzeActivity.this);
                }
            }, 500);
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

    @Override
    public void run() {
        switch (pageIndex) {
            case 0: {
                if (((NavigationTabActivity) getParent()).hasGetDeviceTypeAndNumber) {
                    currentErrorHandler.sendCount = currentCommunications.length;
                    BluetoothTool.getInstance(TroubleAnalyzeActivity.this)
                            .setHandler(currentErrorHandler)
                            .setCommunications(currentCommunications)
                            .send();
                }
            }
            break;
            case 1: {
                if (historyCommunications != null && historyCommunications.length > 0) {
                    fcGroupHandler.sendCount = historyCommunications.length;
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
}
