package com.inovance.ElevatorControl.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import butterknife.InjectView;
import butterknife.Views;
import com.bluetoothtool.BluetoothHandler;
import com.bluetoothtool.BluetoothTalk;
import com.bluetoothtool.BluetoothTool;
import com.bluetoothtool.SerialUtility;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.adapters.CheckedListViewAdapter;
import com.inovance.ElevatorControl.adapters.DialogSwitchListViewAdapter;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.daos.ParameterGroupSettingsDao;
import com.inovance.ElevatorControl.daos.RealTimeMonitorDao;
import com.inovance.ElevatorControl.handlers.ParameterDetailHandler;
import com.inovance.ElevatorControl.models.*;
import com.inovance.ElevatorControl.utils.LogUtils;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import com.inovance.ElevatorControl.views.dialogs.CustomDialog;
import com.manuelpeinado.refreshactionitem.ProgressIndicatorType;
import com.manuelpeinado.refreshactionitem.RefreshActionItem;
import com.mobsandgeeks.adapters.InstantAdapter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 参数详细
 */
public class ParameterDetailActivity extends Activity implements RefreshActionItem.RefreshActionListener, Runnable {

    private static final String TAG = ParameterDetailActivity.class.getSimpleName();

    /**
     * 用于写入参数的 Handler
     */
    private UpdateHandler updateHandler;

    /**
     * 参数 Item 详细 Dialog
     */
    private AlertDialog detailDialog;

    /**
     * 取得的所有参数列表
     */
    public List<ParameterSettings> settingsList;

    /**
     * 需要显示的参数列表
     */
    public List<ParameterSettings> listViewDataSource;

    /**
     * 读取参数状态的 Handler
     */
    private ParameterDetailHandler parameterDetailHandler;

    /**
     * List View Adapter
     */
    public InstantAdapter<ParameterSettings> instantAdapter;

    /**
     * 用于读取参数状态的同学内容
     */
    private BluetoothTalk[] communications;

    /**
     * 是否正在同步参数状态
     */
    public boolean syncingParameter;

    /**
     * 用于读取参数数值范围的 Handler
     */
    private GetValueScopeHandler getValueScopeHandler;

    /**
     * Dialog 取消按钮
     */
    private Button cancelButton;

    /**
     * Dialog 确认按钮
     */
    private Button confirmButton;

    /**
     * Dialog 读取状态文字
     */
    private TextView waitTextView;

    /**
     * 参数数值范围、默认值、单位信息
     */
    private TextView descriptionTextView;

    /**
     * 数值选择器 Parent View
     */
    private LinearLayout pickerContainer;

    /**
     * 数值选择器 Top View
     */
    private LinearLayout pickerView;

    /**
     * 参数是否写入成功
     */
    private boolean isWriteSuccessful;

    /**
     * 动态生成的数值选择器列表
     */
    private List<NumberPicker> numberPickerList;

    /**
     * 选择器数值分号位置索引
     */
    private int dotIndex = -1;

    /**
     * 参数最大值
     */
    private long maxValueLong;

    /**
     * 参数最小值
     */
    private long minValueLong;

    /**
     * 刷新按钮
     */
    public RefreshActionItem mRefreshActionItem;

    /**
     * 是否读取到电梯状态
     */
    private boolean hasGetElevatorStatus;

    /**
     * 是否读取到参数数值设置范围
     */
    private boolean hasGetValueScope;

    /**
     * 取得电梯运行状态
     */
    private GetElevatorStatusHandler getElevatorStatusHandler;

    /**
     * 用于取得电梯运行状态的通信内容
     */
    private BluetoothTalk[] getElevatorStatusCommunication;

    /**
     * 写入参数错误提示信息
     */
    private String writeErrorString;

    /**
     * 取得参数数值范围
     */
    private static final int onGetValueScope = 1;

    /**
     * 取得电梯运行状态
     */
    private static final int onGetElevatorStatus = 2;

    private ExecutorService pool = Executors.newSingleThreadExecutor();

    /**
     * 功能参数详细列表
     */
    @InjectView(R.id.parameter_detail_list_view)
    public ListView parameterDetailListView;

    private static final int GetParameterDetail = 1;

    private static final int GetValueScope = 2;

    private static final int GetElevatorStatus = 3;

    private int currentTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setContentView(R.layout.activity_parameter_detail);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        Views.inject(this);
        parameterDetailHandler = new ParameterDetailHandler(this);
        getValueScopeHandler = new GetValueScopeHandler(this);
        getElevatorStatusHandler = new GetElevatorStatusHandler(this);
        updateHandler = new UpdateHandler(this);
        initListViewData();
        bindListViewItemClickListener();
    }

    /**
     * 设置 ListView Adapter
     */
    private void initListViewData() {
        int SelectedId = this.getIntent().getIntExtra("SelectedId", 0);
        ParameterGroupSettings parameterGroupSettings = ParameterGroupSettingsDao.findById(
                this, SelectedId);
        this.setTitle(parameterGroupSettings.getGroupText());
        settingsList = parameterGroupSettings.getParametersettings().getList();
        listViewDataSource = new ArrayList<ParameterSettings>();
        for (ParameterSettings item : settingsList) {
            if (!item.getName().contains(ApplicationConfig.RETAIN_NAME)) {
                listViewDataSource.add(item);
            }
        }
        instantAdapter = new InstantAdapter<ParameterSettings>(this,
                R.layout.list_parameter_group_item,
                ParameterSettings.class,
                listViewDataSource);
        parameterDetailListView.setAdapter(instantAdapter);
    }

    /**
     * 绑定ListView Item点击时间
     */
    private void bindListViewItemClickListener() {
        parameterDetailListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (!syncingParameter) {
                    if (BluetoothTool.getInstance().isPrepared()) {
                        final ParameterSettings settings = listViewDataSource.get(position);
                        int mode = Integer.parseInt(settings.getMode());
                        /**
                         * 任意修改
                         */
                        if (mode == ApplicationConfig.modifyType[0]) {
                            settings.setElevatorRunning(false);
                            if (settings.getDescriptionType() == ApplicationConfig.DESCRIPTION_TYPE[0]) {
                                createPickerDialog(position, settings);
                            } else {
                                onClickListViewWithIndex(position);
                            }
                        }
                        /**
                         * 停机修改
                         */
                        if (mode == ApplicationConfig.modifyType[1]) {
                            final int index = position;
                            settings.setElevatorRunning(false);
                            ParameterDetailActivity.this.hasGetElevatorStatus = false;
                            /**
                             * 读取电梯运行状态
                             */
                            new CountDownTimer(2400, 800) {

                                @Override
                                public void onTick(long l) {
                                    if (!ParameterDetailActivity.this.hasGetElevatorStatus) {
                                        ParameterDetailActivity.this.getElevatorStatus(index, settings);
                                    } else {
                                        this.cancel();
                                    }
                                }

                                @Override
                                public void onFinish() {

                                }
                            }.start();
                        }
                        /**
                         * 不可修改
                         */
                        if (mode == ApplicationConfig.modifyType[2]) {
                            if (settings.getDescriptionType() == ApplicationConfig.DESCRIPTION_TYPE[0] ||
                                    settings.getDescriptionType() == ApplicationConfig.DESCRIPTION_TYPE[1]) {
                                new AlertDialog.Builder(ParameterDetailActivity.this,
                                        R.style.CustomDialogStyle)
                                        .setTitle(settings.getCodeText() + " " + settings.getName())
                                        .setMessage(R.string.cannot_modify_message)
                                        .setPositiveButton(R.string.dialog_btn_ok, null)
                                        .create()
                                        .show();
                            } else {
                                onClickListViewWithIndex(position);
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * 取得当前电梯运行状态
     *
     * @param index    ListView Index
     * @param settings Selected ParameterSettings
     */
    private void getElevatorStatus(final int index, final ParameterSettings settings) {
        if (getElevatorStatusCommunication == null) {
            List<RealTimeMonitor> monitorList = RealTimeMonitorDao
                    .findByNames(this, new String[]{ApplicationConfig.STATUS_WORD_NAME});
            if (monitorList.size() == 1) {
                final RealTimeMonitor monitor = monitorList.get(0);
                getElevatorStatusCommunication = new BluetoothTalk[]{
                        new BluetoothTalk() {
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
                                    byte[] data = SerialUtility.trimEnd(getReceivedBuffer());
                                    if (data.length == 8) {
                                        monitor.setReceived(data);
                                        return monitor;
                                    }
                                }
                                return null;
                            }
                        }
                };
            }
        }
        if (BluetoothTool.getInstance().isPrepared()) {
            if (!ParameterDetailActivity.this.hasGetElevatorStatus) {
                getElevatorStatusHandler.index = index;
                getElevatorStatusHandler.settings = settings;
                BluetoothTool.getInstance()
                        .setCommunications(getElevatorStatusCommunication)
                        .setHandler(getElevatorStatusHandler)
                        .send();
            }

        }
    }

    /**
     * 生成 Number Picker Dialog
     *
     * @param index    ListView Index
     * @param settings ParameterSettings
     */
    private void createPickerDialog(final int index, final ParameterSettings settings) {
        char toCheck = 'F';
        int count = 0;
        for (char ch : settings.getScope().toCharArray()) {
            if (ch == toCheck) {
                count++;
            }
        }
        View dialogView = getLayoutInflater().inflate(R.layout.parameter_picker_dialog, null);
        descriptionTextView = (TextView) dialogView.findViewById(R.id.description_text);
        waitTextView = (TextView) dialogView.findViewById(R.id.wait_text);
        pickerContainer = (LinearLayout) dialogView.findViewById(R.id.picker_container);
        pickerView = (LinearLayout) dialogView.findViewById(R.id.picker_view);
        AlertDialog.Builder builder = new AlertDialog.Builder(ParameterDetailActivity.this,
                R.style.CustomDialogStyle)
                .setView(dialogView)
                .setTitle(settings.getCodeText() + " " + settings.getName())
                .setNeutralButton(R.string.dialog_btn_cancel, null)
                .setPositiveButton(R.string.dialog_btn_ok, null);
        detailDialog = builder.create();
        detailDialog.show();
        detailDialog.setCancelable(false);
        detailDialog.setCanceledOnTouchOutside(false);
        cancelButton = detailDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        confirmButton = detailDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        detailDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pickerView != null) {
                    List<String> stringList = new ArrayList<String>();
                    for (NumberPicker picker : numberPickerList) {
                        stringList.add(String.valueOf(picker.getValue()));
                    }
                    if (dotIndex != -1) {
                        stringList.add(dotIndex, ".");
                    }
                    String valueString = "";
                    for (String string : stringList) {
                        valueString += string;
                    }
                    String userValueString = valueString.replaceFirst("^0+(?!$)", "");
                    final Long userValueLong = Math.round(Double.parseDouble(userValueString)
                            / (Double.parseDouble(settings.getScale())));
                    if (userValueLong >= minValueLong && userValueLong <= maxValueLong) {
                        startSetNewValueCommunications(index, String.format("%04x", userValueLong.intValue()));
                    } else {
                        Toast.makeText(ParameterDetailActivity.this,
                                R.string.new_value_over_max,
                                android.widget.Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            }
        });
        detailDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detailDialog.dismiss();
            }
        });
        if (count != 0) {
            /**
             * 需要远程获取数值的，在获取到数值之后再生成 Number Picker Dialog
             */
            cancelButton.setEnabled(false);
            confirmButton.setEnabled(false);
            final String[] codeArray = ParameterDetailActivity.this.getCodeStringArray(settings);
            ParameterDetailActivity.this.hasGetValueScope = false;
            BluetoothTool.getInstance().setHandler(null);
            new CountDownTimer(2400, 800) {

                @Override
                public void onTick(long millisUntilFinished) {
                    if (!hasGetValueScope) {
                        ParameterDetailActivity.this.createGetValueScopeCommunications(codeArray, index, settings);
                    } else {
                        this.cancel();
                    }
                }

                @Override
                public void onFinish() {
                    if (!hasGetValueScope) {
                        if (detailDialog != null && detailDialog.isShowing()) {
                            detailDialog.dismiss();
                        }
                        Toast.makeText(ParameterDetailActivity.this,
                                R.string.get_value_scope_failed_text,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }.start();
        } else {
            /**
             * 不需要获取数值范围的直接生成 Number Picker Dialog
             */
            createNumberPickerAndBindListener(settings);
        }
    }

    /**
     * Create Number Picker And Bind Picker Listener
     *
     * @param settings ParameterSettings
     */
    private void createNumberPickerAndBindListener(final ParameterSettings settings) {
        String[] scopeArray;
        if (settings.getTempScope() != null) {
            scopeArray = settings.getTempScope().split("~");
        } else {
            scopeArray = settings.getScope().split("~");
        }
        if (scopeArray.length == 2) {
            String minValueString = scopeArray[0];
            String maxValueString = scopeArray[1];
            minValueLong = Math.round(Double.parseDouble(minValueString)
                    / (Double.parseDouble(settings.getScale())));
            maxValueLong = Math.round(Double.parseDouble(maxValueString)
                    / (Double.parseDouble(settings.getScale())));
            String currentValueString = settings.getFinalValue();
            if (settings.getFinalValue().length() < maxValueString.length()) {
                int leadZeroCount = maxValueString.length() - currentValueString.length();
                String leadZeroString = "";
                for (int i = 0; i < leadZeroCount; i++) {
                    leadZeroString += "0";
                }
                currentValueString = leadZeroString + currentValueString;
            }
            int totals = maxValueString.length();
            dotIndex = -1;
            numberPickerList = new ArrayList<NumberPicker>();
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            if (totals > 3) {
                layoutParams = new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.weight = 1;
            }
            layoutParams.setMargins(0, 0, 10, 0);
            LinearLayout.LayoutParams textViewLayoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            for (int i = 0; i < totals; i++) {
                String valueChar = Character.toString(maxValueString.charAt(i));
                String currentValueChar = Character.toString(currentValueString.charAt(i));
                if (ParseSerialsUtils.isInteger(valueChar)) {
                    NumberPicker picker = new NumberPicker(this);
                    picker.setWrapSelectorWheel(false);
                    picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
                    picker.setMinValue(0);
                    picker.setMaxValue(9);
                    picker.setValue(Integer.parseInt(currentValueChar));
                    pickerView.addView(picker, layoutParams);
                    numberPickerList.add(picker);
                }
                if (valueChar.equalsIgnoreCase(".")) {
                    TextView textView = new TextView(this);
                    textView.setText(".");
                    textView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                    textViewLayoutParams.setMargins(0, 18, 10, 0);
                    pickerView.addView(textView, textViewLayoutParams);
                    dotIndex = i;
                }
            }
        }
        if (settings.getTempScope() != null) {
            settings.getTempScope().split("~");
        }
        int index = 0;
        String scopeString = "";
        for (String scope : settings.getScope().split("~")) {
            if (scope.contains("F")) {
                scopeString += settings.getScope().split("~")[index]
                        + "(" + settings.getTempScope().split("~")[index] + ")";
            } else {
                scopeString += settings.getScope().split("~")[index];
            }
            if (index != settings.getScope().split("~").length - 1) {
                scopeString += "-";
            }
            index++;
        }
        String unit = settings.getUnit().length() == 0 ? "" : " 单位:" + settings.getUnit();
        String metaText = "取值范围:" + scopeString + "\n"
                + "出厂值:" + settings.getDefaultValue() + " "
                + unit;
        descriptionTextView.setText(metaText);
        waitTextView.setVisibility(View.GONE);
        pickerContainer.setVisibility(View.VISIBLE);
        cancelButton.setEnabled(true);
        confirmButton.setEnabled(true);
    }

    /**
     * 弹出单选对话框或者开关对话框
     *
     * @param index ListView Index
     */
    private void onClickListViewWithIndex(final int index) {
        final ParameterSettings settings = listViewDataSource.get(index);
        AlertDialog.Builder builder = CustomDialog.parameterDetailDialog(ParameterDetailActivity.this,
                settings);
        int mode = Integer.parseInt(settings.getMode());
        if (mode != ApplicationConfig.modifyType[2]) {
            if (!settings.isElevatorRunning()) {
                builder.setPositiveButton(R.string.dialog_btn_ok, null);
                builder.setNegativeButton(R.string.dialog_btn_cancel, null);
            } else {
                builder.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (detailDialog != null) {
                            detailDialog.dismiss();
                        }
                    }
                });
            }
        } else {
            builder.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (detailDialog != null) {
                        detailDialog.dismiss();
                    }
                }
            });
        }
        detailDialog = builder.create();
        detailDialog.show();
        if (mode != ApplicationConfig.modifyType[2]) {
            if (!settings.isElevatorRunning()) {
                detailDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (settings.getDescriptionType() == ApplicationConfig.DESCRIPTION_TYPE[1]) {
                            if (Integer.parseInt(settings.getType()) == ApplicationConfig.InputTerminalType) {
                                ToggleButton toggleButton = (ToggleButton) detailDialog.findViewById(R.id.toggle_button);
                                ListView listView = (ListView) detailDialog.findViewById(R.id.list_view);
                                CheckedListViewAdapter adapter = (CheckedListViewAdapter) listView.getAdapter();
                                int value = getTerminalStatus(adapter.getItem(adapter.getCheckedIndex()),
                                        toggleButton.isChecked());
                                if (value != -1) {
                                    if (adapter.getCheckedIndex() == 0) {
                                        value = 0;
                                    }
                                    startSetNewValueCommunications(index, String.format("%04x", value));
                                }
                            } else if (Integer.parseInt(settings.getType()) == ApplicationConfig.FloorShowType) {
                                Spinner modSpinner = (Spinner) detailDialog.findViewById(R.id.mod_value);
                                Spinner remSpinner = (Spinner) detailDialog.findViewById(R.id.rem_value);
                                int userValue = modSpinner.getSelectedItemPosition() * 100
                                        + remSpinner.getSelectedItemPosition();
                                startSetNewValueCommunications(index, String.format("%04x", userValue));
                            } else {
                                int checkedIndex = detailDialog.getListView().getCheckedItemPosition();
                                if (checkedIndex != ParseSerialsUtils.getIntFromBytes(settings.getReceived())) {
                                    startSetNewValueCommunications(index, String.format("%04x", checkedIndex));
                                }
                            }
                            detailDialog.dismiss();
                        }
                        if (settings.getDescriptionType() == ApplicationConfig.DESCRIPTION_TYPE[2]) {
                            ListView listView = (ListView) detailDialog.findViewById(R.id.switch_list);
                            DialogSwitchListViewAdapter adapter = (DialogSwitchListViewAdapter) listView
                                    .getAdapter();
                            List<ParameterStatusItem> list = adapter.getItemList();
                            String binaryString = "";
                            int size = list.size();
                            for (int i = size - 1; i >= 0; i--) {
                                binaryString += list.get(i).getStatus() ? 1 : 0;
                            }
                            startSetNewValueCommunications(index,
                                    String.format("%04x", Integer.parseInt(binaryString, 2)));
                        }
                    }
                });
            }
        }
        detailDialog.setCancelable(false);
        detailDialog.setCanceledOnTouchOutside(false);
    }

    /**
     * 截取端子状态数值
     *
     * @param text         显示字符串
     * @param isAlwaysOpen 是否常开
     * @return 街区后的数值
     */
    private int getTerminalStatus(String text, boolean isAlwaysOpen) {
        String[] parts = text.trim().split(":");
        if (parts.length == 2) {
            String[] items = parts[0].split("/");
            if (items.length == 2) {
                return isAlwaysOpen ? Integer.parseInt(items[0]) : Integer.parseInt(items[1]);
            }
        }
        return -1;
    }

    /**
     * 取得需要获取的参数范围指令
     *
     * @param settings ParameterSettings
     * @return 指令数组
     */
    private String[] getCodeStringArray(ParameterSettings settings) {
        String[] array = settings.getScope().split("~");
        List<String> list = new ArrayList<String>();
        for (String code : array) {
            if (code.contains("F")) {
                list.add(code);
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * 生成取得取值范围Talk
     *
     * @param list     Code List
     * @param index    ListView Index
     * @param settings ParameterSettings
     */
    private void createGetValueScopeCommunications(final String[] codeArray, int index,
                                                   final ParameterSettings settings) {
        int length = codeArray.length;
        BluetoothTalk[] communications = new BluetoothTalk[length];
        for (int i = 0; i < length; i++) {
            final int position = i;
            communications[i] = new BluetoothTalk() {
                @Override
                public void beforeSend() {
                    this.setSendBuffer(SerialUtility.crc16("0103"
                            + codeArray[position]
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
                        byte[] data = SerialUtility.trimEnd(getReceivedBuffer());
                        if (data.length > 6) {
                            int intValue = ParseSerialsUtils.getIntFromBytes(data);
                            try {
                                return intValue * Integer.parseInt(settings.getScale()) + "";
                            } catch (Exception e) {
                                double doubleValue = (double) intValue * Double.parseDouble(settings.getScale());
                                return String.format("%." + (settings.getScale().length() - 2) + "f", doubleValue);
                            }
                        }
                    }
                    return null;
                }
            };
        }
        if (communications.length > 0) {
            if (BluetoothTool.getInstance().isPrepared()) {
                if (!ParameterDetailActivity.this.hasGetValueScope) {
                    getValueScopeHandler.count = communications.length;
                    getValueScopeHandler.index = index;
                    BluetoothTool.getInstance()
                            .setCommunications(communications)
                            .setHandler(getValueScopeHandler)
                            .send();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BluetoothTool.getInstance().isPrepared()) {
            ParameterDetailActivity.this.syncingParameter = true;
            currentTask = GetParameterDetail;
            pool.execute(this);
        }
    }

    /**
     * Start Set New Value Communications
     *
     * @param position  ListView Item Position
     * @param userValue New Setting value (Hex String)
     */
    private void startSetNewValueCommunications(final int position, final String userValue) {
        final ParameterSettings settings = listViewDataSource.get(position);
        final BluetoothTalk[] communications = new BluetoothTalk[]{
                new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16("0106"
                                + ParseSerialsUtils.getCalculatedCode(settings)
                                + userValue
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
                            settings.setReceived(getReceivedBuffer());
                            return settings;
                        }
                        return null;
                    }
                }
        };
        if (BluetoothTool.getInstance().isPrepared()) {
            ParameterDetailActivity.this.isWriteSuccessful = false;
            updateHandler.index = position;
            updateHandler.writeCode = userValue;
            updateHandler.startValue = settings.getUserValue();
            new CountDownTimer(3200, 800) {
                public void onTick(long millisUntilFinished) {
                    if (!ParameterDetailActivity.this.isWriteSuccessful) {
                        updateHandler.index = position;
                        updateHandler.writeCode = userValue;
                        updateHandler.startValue = settings.getUserValue();
                        BluetoothTool.getInstance()
                                .setHandler(updateHandler)
                                .setCommunications(communications)
                                .send();
                    } else {
                        this.cancel();
                    }
                }

                public void onFinish() {
                    if (!ParameterDetailActivity.this.isWriteSuccessful) {
                        if (detailDialog != null && detailDialog.isShowing()) {
                            detailDialog.dismiss();
                        }
                        if (writeErrorString != null) {
                            Toast.makeText(ParameterDetailActivity.this,
                                    writeErrorString,
                                    android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    /**
     * Start Combination Communications
     * 组合发送指令
     */
    public void startCombinationCommunications() {
        if (communications == null) {
            final int size = settingsList.size();
            final int count = size <= 10 ? 1 : ((size - size % 10) / 10 + (size % 10 == 0 ? 0 : 1));
            communications = new BluetoothTalk[count];
            for (int i = 0; i < count; i++) {
                final int position = i;
                final ParameterSettings firstItem = settingsList.get(position * 10);
                final int length = size <= 10 ? size : (size % 10 == 0 ? 10 : ((position == count - 1) ? size % 10 : 10));
                communications[i] = new BluetoothTalk() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(SerialUtility.crc16("0103"
                                        + ParseSerialsUtils.getCalculatedCode(firstItem)
                                        + String.format("%04x", length)
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
                            byte[] data = SerialUtility.trimEnd(getReceivedBuffer());
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
                        }
                        return null;
                    }
                };
            }
        }
        if (BluetoothTool.getInstance().isPrepared()) {
            parameterDetailHandler.sendCount = communications.length;
            BluetoothTool.getInstance()
                    .setHandler(parameterDetailHandler)
                    .setCommunications(communications)
                    .send();
        }
    }

    /**
     * 写入数据成功
     *
     * @param index    ListView Index
     * @param settings ParameterSettings
     */
    private void onWriteDataSuccessful(int index, ParameterSettings settings) {
        ParameterDetailActivity.this.listViewDataSource.set(index, settings);
        ParameterDetailActivity.this.instantAdapter.notifyDataSetChanged();
        if (detailDialog != null && detailDialog.isShowing()) {
            detailDialog.dismiss();
        }
        Toast.makeText(ParameterDetailActivity.this,
                R.string.write_parameter_successful,
                Toast.LENGTH_SHORT).show();
    }

    /**
     * 已经取得电梯状态
     *
     * @param index    ListView Index
     * @param status   Elevator status
     * @param settings ParameterSettings
     */
    private void onGetElevatorStatus(final int index, int status, final ParameterSettings settings) {
        settings.setElevatorRunning(status != 3);
        if (settings.isElevatorRunning()) {
            if (settings.getDescriptionType() == ApplicationConfig.DESCRIPTION_TYPE[2]) {
                onClickListViewWithIndex(index);
            }
            Toast.makeText(ParameterDetailActivity.this,
                    R.string.elevator_running_message,
                    Toast.LENGTH_SHORT).show();
        } else {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Message message = new Message();
                    message.what = onGetElevatorStatus;
                    message.obj = settings;
                    onGetValueHandler.sendMessage(message);
                }
            }, 350);
        }
    }

    /**
     * 取得数值范围
     *
     * @param position  ListView Item Position
     * @param count     Value Scope Count
     * @param valueList Value List
     */
    private void onGetValueScope(int position, int count, List<String> valueList) {
        final ParameterSettings settings = listViewDataSource.get(position);
        if (count == 2) {
            double value1 = Double.parseDouble(valueList.get(0));
            double value2 = Double.parseDouble(valueList.get(1));
            if (value1 < value2) {
                settings.setScope(valueList.get(0) + "~" + valueList.get(1));
            } else {
                settings.setScope(valueList.get(1) + "~" + valueList.get(0));
            }
        }
        if (count == 1) {
            String[] valueArray = settings.getScope().split("~");
            List<String> tempList = new ArrayList<String>();
            for (String value : valueArray) {
                if (value.contains("F")) {
                    tempList.add(valueList.get(0));
                } else {
                    tempList.add(value);
                }
            }
            String scopeString = "";
            int size = tempList.size();
            int index = 0;
            for (String temp : tempList) {
                scopeString += temp;
                if (index != size - 1) {
                    scopeString += "~";
                }
                index++;
            }
            settings.setTempScope(scopeString);
        }
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = onGetValueScope;
                message.obj = settings;
                onGetValueHandler.sendMessage(message);
            }
        }, 350);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu, menu);
        MenuItem item = menu.findItem(R.id.refresh_button);
        assert item != null;
        mRefreshActionItem = (RefreshActionItem) item.getActionView();
        assert mRefreshActionItem != null;
        mRefreshActionItem.setMenuItem(item);
        if (BluetoothTool.getInstance().isPrepared()) {
            mRefreshActionItem.showProgress(true);
        }
        mRefreshActionItem.setProgressIndicatorType(ProgressIndicatorType.INDETERMINATE);
        mRefreshActionItem.setRefreshActionListener(ParameterDetailActivity.this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                BluetoothTool.getInstance()
                        .setHandler(null);
                setResult(RESULT_OK);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefreshButtonClick(RefreshActionItem sender) {
        mRefreshActionItem.showProgress(true);
        syncingParameter = true;
        currentTask = GetParameterDetail;
        pool.execute(ParameterDetailActivity.this);
    }

    @Override
    public void run() {
        switch (currentTask) {
            case GetParameterDetail:
                startCombinationCommunications();
                break;
        }
    }

    private Handler onGetValueHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case onGetElevatorStatus: {
                    if (msg.obj != null && msg.obj instanceof ParameterSettings) {
                        ParameterSettings settings = (ParameterSettings) msg.obj;
                        int index = listViewDataSource.indexOf(settings);
                        if (settings.getDescriptionType() == ApplicationConfig.DESCRIPTION_TYPE[0]) {
                            createPickerDialog(index, settings);
                        } else {
                            onClickListViewWithIndex(index);
                        }
                    }
                }
                break;
                case onGetValueScope: {
                    if (msg.obj != null && msg.obj instanceof ParameterSettings) {
                        createNumberPickerAndBindListener((ParameterSettings) msg.obj);
                    }
                }
                break;
            }
        }
    };

    // ===================================== 写入参数 Handler ======================================== //

    /**
     * Update Handler
     */
    private class UpdateHandler extends BluetoothHandler {

        public int index;

        public String writeCode;

        public String startValue;

        public UpdateHandler(android.app.Activity activity) {
            super(activity);
            TAG = UpdateHandler.class.getSimpleName();
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
            if (msg.obj != null && msg.obj instanceof ParameterSettings) {
                ParameterSettings receiveObject = (ParameterSettings) msg.obj;
                String returnCodeString = SerialUtility.byte2HexStr(receiveObject.getReceived());
                boolean writeSuccessful = true;
                int index = 0;
                for (String item : ApplicationConfig.ERROR_CODE_ARRAY) {
                    if (returnCodeString.contains(item)) {
                        writeSuccessful = false;
                        writeErrorString = ApplicationConfig.ERROR_NAME_ARRAY[index];
                        break;
                    }
                    index++;
                }
                if (!SerialUtility.byte2HexStr(receiveObject.getReceived()).contains(writeCode.toUpperCase())) {
                    writeSuccessful = false;
                }
                if (writeSuccessful) {
                    ParameterDetailActivity.this.isWriteSuccessful = writeSuccessful;
                    onWriteDataSuccessful(this.index, receiveObject);
                    // 写入日志
                    String startValueText = "";
                    String finalValueText = "";
                    try {
                        startValueText = Integer.parseInt(startValue)
                                * Integer.parseInt(receiveObject.getScale()) + receiveObject.getUnit();
                        finalValueText = Integer.parseInt(receiveObject.getUserValue())
                                * Integer.parseInt(receiveObject.getScale()) + receiveObject.getUnit();
                    } catch (Exception e) {
                        double startDoubleValue = Double.parseDouble(startValue)
                                * Double.parseDouble(receiveObject.getScale());
                        double finalDoubleValue = Double.parseDouble(receiveObject.getUserValue())
                                * Double.parseDouble(receiveObject.getScale());
                        startValueText = String.format("%."
                                + (receiveObject.getScale().length() - 2) + "f", startDoubleValue)
                                + receiveObject.getUnit();
                        finalValueText = String.format("%."
                                + (receiveObject.getScale().length() - 2) + "f", finalDoubleValue)
                                + receiveObject.getUnit();
                    }
                    LogUtils.getInstance().write(ApplicationConfig.LogWriteParameter,
                            writeCode,
                            returnCodeString,
                            startValueText,
                            finalValueText);
                }
            }
        }

    }

    // ================================= 取得参数数值设置范围 Handler ============================================== //
    private class GetValueScopeHandler extends BluetoothHandler {

        private List<String> stringList;

        public int count;

        private int index;

        public GetValueScopeHandler(Activity activity) {
            super(activity);
            TAG = GetValueScopeHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            stringList = new ArrayList<String>();
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (stringList.size() == count) {
                ParameterDetailActivity.this.hasGetValueScope = true;
                onGetValueScope(this.index, count, stringList);
            }
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof String) {
                stringList.add((String) msg.obj);
            }
        }

        @Override
        public void onTalkError(Message msg) {
            super.onTalkError(msg);
        }
    }

    // ============================= 取得当前电梯运行状态 Handler ============================ //
    private class GetElevatorStatusHandler extends BluetoothHandler {

        public int index;

        public ParameterSettings settings;

        public GetElevatorStatusHandler(Activity activity) {
            super(activity);
            TAG = GetElevatorStatusHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkBegin(msg);
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
                ParameterDetailActivity.this.hasGetElevatorStatus = true;
                int status = ParseSerialsUtils.getIntFromBytes(((RealTimeMonitor) msg.obj).getReceived());
                onGetElevatorStatus(this.index, status, this.settings);
            }
        }

        @Override
        public void onTalkError(Message msg) {
            super.onTalkError(msg);
        }

    }

}