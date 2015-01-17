package com.inovance.elevatorcontrol.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.inovance.bluetoothtool.BluetoothHandler;
import com.inovance.bluetoothtool.BluetoothTalk;
import com.inovance.bluetoothtool.BluetoothTool;
import com.inovance.bluetoothtool.SerialUtility;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.adapters.CheckedListViewAdapter;
import com.inovance.elevatorcontrol.adapters.DialogSwitchListViewAdapter;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.config.ParameterUpdateTool;
import com.inovance.elevatorcontrol.daos.ParameterGroupSettingsDao;
import com.inovance.elevatorcontrol.factory.ParameterFactory;
import com.inovance.elevatorcontrol.handlers.ParameterDetailHandler;
import com.inovance.elevatorcontrol.models.ObjectListHolder;
import com.inovance.elevatorcontrol.models.ParameterGroupSettings;
import com.inovance.elevatorcontrol.models.ParameterSettings;
import com.inovance.elevatorcontrol.models.ParameterStatusItem;
import com.inovance.elevatorcontrol.utils.LogUtils;
import com.inovance.elevatorcontrol.utils.ParseSerialsUtils;
import com.inovance.elevatorcontrol.views.dialogs.CustomDialog;
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

import butterknife.InjectView;
import butterknife.Views;

/**
 * 参数详细
 */
public class ParameterDetailActivity extends Activity implements RefreshActionItem.RefreshActionListener,
        Runnable,
        DialogInterface.OnDismissListener {

    private static final String TAG = ParameterDetailActivity.class.getSimpleName();

    /**
     * 用于写入参数的 Handler
     */
    private WriteHandler writeHandler;

    /**
     * 参数 Item 详细 Dialog
     */
    private AlertDialog detailDialog;

    /**
     * 写入参数重试计时器
     */
    private CountDownTimer countDownTimer;

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
     * 参数是否写入错误
     */
    private boolean isWriteError;

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
     * 是否读取到参数数值设置范围
     */
    private boolean hasGetValueScope;

    /**
     * 写入参数错误提示信息
     */
    private String writeErrorString;

    /**
     * 取得参数数值范围
     */
    private static final int onGetValueScope = 1;

    private ExecutorService pool = Executors.newSingleThreadExecutor();

    /**
     * 功能参数详细列表
     */
    @InjectView(R.id.parameter_detail_list_view)
    public ListView parameterDetailListView;

    private static final int GetParameterDetail = 1;

    private static final int GetValueScope = 2;

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
        writeHandler = new WriteHandler(this);
        BluetoothTool.getInstance().setHandler(null);
        initListViewData();
        bindListViewItemClickListener();
    }

    /**
     * 设置 ListView Adapter
     */
    private void initListViewData() {
        int SelectedId = this.getIntent().getIntExtra("SelectedId", 0);
        ParameterGroupSettings parameterGroupSettings = ParameterGroupSettingsDao.findById(this, SelectedId);
        this.setTitle(parameterGroupSettings.getGroupText());
        settingsList = parameterGroupSettings.getParametersettings().getList();
        // NICE 1000 / NICE 3000 设备提示用户选择同步异步
        String deviceName = ParameterUpdateTool.getInstance().getDeviceName();
        if (deviceName.equals(ApplicationConfig.NormalDeviceType[0]) ||
                deviceName.equals(ApplicationConfig.NormalDeviceType[2])) {
            List<ParameterSettings> tempList = new ArrayList<ParameterSettings>();
            if (ParameterUpdateTool.getInstance().isSync()) {
                // 同步
                for (ParameterSettings settings : settingsList) {
                    int type = Integer.parseInt(settings.getType());
                    if (type != ApplicationConfig.AsyncType) {
                        tempList.add(settings);
                    }
                }
                settingsList.clear();
                settingsList.addAll(tempList);
            } else {
                // 异步
                for (ParameterSettings settings : settingsList) {
                    int type = Integer.parseInt(settings.getType());
                    if (type != ApplicationConfig.SyncType) {
                        tempList.add(settings);
                    }
                }
                settingsList.clear();
                settingsList.addAll(tempList);
            }
        }
        listViewDataSource = new ArrayList<ParameterSettings>();
        for (ParameterSettings item : settingsList) {
            listViewDataSource.add(item);
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
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                        countDownTimer = null;
                    }
                    if (BluetoothTool.getInstance().isPrepared()) {
                        final ParameterSettings settings = listViewDataSource.get(position);
                        int mode = Integer.parseInt(settings.getMode());
                        // 任意修改 & 停机修改
                        if (mode == ApplicationConfig.modifyType[0] || mode == ApplicationConfig.modifyType[1]) {
                            if (settings.getDescriptionType() == ApplicationConfig.DESCRIPTION_TYPE[0]) {
                                createPickerDialog(position, settings);
                            } else {
                                onClickListViewWithIndex(position);
                            }
                        }
                        // 不可修改
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
        detailDialog.setOnDismissListener(this);
        detailDialog.show();
        detailDialog.setCancelable(false);
        detailDialog.setCanceledOnTouchOutside(false);
        cancelButton = detailDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        confirmButton = detailDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        detailDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
            public void onClick(View view) {
                detailDialog.dismiss();
            }
        });
        if (count != 0) {
            // 需要远程获取数值的，在获取到数值之后再生成 Number Picker Dialog
            cancelButton.setEnabled(false);
            confirmButton.setEnabled(false);
            final String[] codeArray = ParameterDetailActivity.this.getCodeStringArray(settings);
            ParameterDetailActivity.this.hasGetValueScope = false;
            BluetoothTool.getInstance().setHandler(null);
            countDownTimer = new CountDownTimer(2400, 800) {

                @Override
                public void onTick(long millisUntilFinished) {
                    if (!hasGetValueScope) {
                        ParameterDetailActivity.this.createGetValueScopeCommunications(codeArray, index, settings);
                    } else {
                        this.cancel();
                        countDownTimer = null;
                    }
                }

                @Override
                public void onFinish() {
                    countDownTimer = null;
                    if (!hasGetValueScope) {
                        if (detailDialog != null && detailDialog.isShowing()) {
                            detailDialog.dismiss();
                        }
                        Toast.makeText(ParameterDetailActivity.this,
                                R.string.get_value_scope_failed_text,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            };
            countDownTimer.start();
        } else {
            // 不需要获取数值范围的直接生成 Number Picker Dialog
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
        AlertDialog.Builder builder = CustomDialog.parameterDetailDialog(this, settings);
        builder.setPositiveButton(R.string.dialog_btn_ok, null);
        builder.setNegativeButton(R.string.dialog_btn_cancel, null);
        detailDialog = builder.create();
        detailDialog.setOnDismissListener(this);
        detailDialog.show();
        int mode = Integer.parseInt(settings.getMode());
        if (mode != ApplicationConfig.modifyType[2]) {
            detailDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (settings.getDescriptionType() == ApplicationConfig.DESCRIPTION_TYPE[1]) {
                        // 输入端子类型
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
                            } else {
                                // 第一个
                                startSetNewValueCommunications(index, String.format("%04x", 0));
                            }
                        } else if (Integer.parseInt(settings.getType()) == ApplicationConfig.FloorShowType) {
                            Spinner modSpinner = (Spinner) detailDialog.findViewById(R.id.mod_value);
                            Spinner remSpinner = (Spinner) detailDialog.findViewById(R.id.rem_value);
                            int userValue = modSpinner.getSelectedItemPosition() * 100
                                    + remSpinner.getSelectedItemPosition();
                            startSetNewValueCommunications(index, String.format("%04x", userValue));
                        } else {
                            int checkedIndex = detailDialog.getListView().getCheckedItemPosition();
                            int writeValue = ParameterFactory.getParameter().getWriteValue(settings, checkedIndex);
                            startSetNewValueCommunications(index, String.format("%04x", writeValue));
                        }
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
                        startSetNewValueCommunications(index, String.format("%04x", Integer.parseInt(binaryString, 2)));
                    }
                }
            });
            detailDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    detailDialog.dismiss();
                }
            });
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
                return ParameterFactory.getParameter().getWriteInputTerminalValue(Integer.parseInt(items[0]),
                        Integer.parseInt(items[1]),
                        isAlwaysOpen);
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
                            .startTask();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BluetoothTool.getInstance().setHandler(null);
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
                        String hexString = "0106"
                                + ParseSerialsUtils.getCalculatedCode(settings)
                                + userValue.toUpperCase();
                        this.setSendBuffer(SerialUtility.crc16(hexString));
                        Log.v("ForTest+++++++++++++", hexString);
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
            isWriteSuccessful = false;
            isWriteError = false;
            writeHandler.index = position;
            writeHandler.writeCode = userValue;
            writeHandler.startValue = settings.getUserValue();
            countDownTimer = new CountDownTimer(3200, 800) {
                public void onTick(long millisUntilFinished) {
                    if (!isWriteSuccessful) {
                        writeHandler.index = position;
                        writeHandler.writeCode = userValue;
                        writeHandler.startValue = settings.getUserValue();
                        BluetoothTool.getInstance()
                                .setHandler(writeHandler)
                                .setCommunications(communications)
                                .startTask();
                    } else {
                        this.cancel();
                        countDownTimer = null;
                    }
                }

                public void onFinish() {
                    countDownTimer = null;
                    if (detailDialog != null && detailDialog.isShowing()) {
                        detailDialog.dismiss();
                    }
                    if (!isWriteSuccessful) {
                        Toast.makeText(ParameterDetailActivity.this,
                                "写入出错",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            };
            countDownTimer.start();
        }
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
                                + String.format("%04x", length)));
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
                    .startTask();
        }
    }

    /**
     * 写入数据出错
     *
     * @param errorString ErrorString
     */
    private void onWriteDataError(String errorString) {
        if (detailDialog != null && detailDialog.isShowing()) {
            detailDialog.dismiss();
        }
        Toast.makeText(ParameterDetailActivity.this,
                errorString,
                Toast.LENGTH_SHORT).show();
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
        // 刷新当前数据
        refreshParameterData();
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
        mRefreshActionItem.setProgressIndicatorType(ProgressIndicatorType.INDETERMINATE);
        mRefreshActionItem.setRefreshActionListener(ParameterDetailActivity.this);
        if (BluetoothTool.getInstance().isPrepared()) {
            mRefreshActionItem.showProgress(true);
            ParameterDetailActivity.this.syncingParameter = true;
            currentTask = GetParameterDetail;
            pool.execute(this);
        }
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
        refreshParameterData();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        detailDialog = null;
    }

    /**
     * 刷新当前数据
     */
    private void refreshParameterData() {
        mRefreshActionItem.showProgress(true);
        syncingParameter = true;
        currentTask = GetParameterDetail;
        pool.execute(ParameterDetailActivity.this);
    }

    @Override
    public void run() {
        switch (currentTask) {
            // 读取取值范围
            case GetValueScope:
                break;
            // 读取参数
            case GetParameterDetail:
                startCombinationCommunications();
                break;
        }
    }

    private Handler onGetValueHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
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
    private class WriteHandler extends BluetoothHandler {

        public int index;

        public String writeCode;

        public String startValue;

        public WriteHandler(android.app.Activity activity) {
            super(activity);
            TAG = WriteHandler.class.getSimpleName();
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
                String valueString = SerialUtility.byte2HexStr(receiveObject.getReceived());
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
                final String result = ParseSerialsUtils.isWriteSuccess(valueString);
                if (result != null) {
                    isWriteError = true;
                    onWriteDataError(result);
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }
                    // 写入日志
                    LogUtils.getInstance().write(ApplicationConfig.LogWriteParameter,
                            writeCode,
                            valueString,
                            startValueText,
                            finalValueText);
                } else {
                    if (valueString.contains(writeCode.toUpperCase())) {
                        isWriteSuccessful = true;
                        onWriteDataSuccessful(this.index, receiveObject);
                        if (countDownTimer != null) {
                            countDownTimer.cancel();
                        }
                        // 写入日志
                        LogUtils.getInstance().write(ApplicationConfig.LogWriteParameter,
                                writeCode,
                                valueString,
                                startValueText,
                                finalValueText);
                    }
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
    }

}