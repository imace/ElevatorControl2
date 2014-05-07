package com.inovance.ElevatorControl.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
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
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import com.inovance.ElevatorControl.views.dialogs.CustomDialog;
import com.manuelpeinado.refreshactionitem.ProgressIndicatorType;
import com.manuelpeinado.refreshactionitem.RefreshActionItem;
import com.mobsandgeeks.adapters.InstantAdapter;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ParameterDetailActivity extends Activity implements RefreshActionItem.RefreshActionListener {

    private static final String TAG = ParameterDetailActivity.class.getSimpleName();

    private UpdateHandler updateHandler;

    private AlertDialog detailDialog;

    public List<ParameterSettings> settingsList;

    public List<ParameterSettings> listViewDataSource;

    private ParameterDetailHandler parameterDetailHandler;

    public InstantAdapter<ParameterSettings> instantAdapter;

    private BluetoothTalk[] communications;

    public boolean syncingParameter;

    private GetValueScopeHandler getValueScopeHandler;

    private Button cancelButton;

    private Button confirmButton;

    private TextView waitTextView;

    private TextView descriptionTextView;

    private LinearLayout pickerContainer;

    private LinearLayout pickerView;

    private boolean isWriteSuccessful;

    private List<NumberPicker> numberPickerList;

    private int dotIndex = -1;

    private long maxValueLong;

    private long minValueLong;

    public RefreshActionItem mRefreshActionItem;

    private boolean hasGetElevatorStatus;

    private GetElevatorStatusHandler getElevatorStatusHandler;

    private BluetoothTalk[] getElevatorStatusCommunication;

    private AlertDialog getElevatorStatusDialog;

    private String writeErrorString;

    /**
     * 功能参数详细列表
     */
    @InjectView(R.id.parameter_detail_list_view)
    public ListView parameterDetailListView;

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
                    if (BluetoothTool.getInstance(ParameterDetailActivity.this).isConnected()) {
                        final ParameterSettings settings = listViewDataSource.get(position);
                        int mode = Integer.parseInt(settings.getMode());
                        // 任意修改
                        if (mode == ApplicationConfig.modifyType[0]) {
                            settings.setElevatorRunning(false);
                            if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[0]) {
                                createPickerDialog(position, settings);
                            } else {
                                onClickListViewWithIndex(position);
                            }
                        }
                        // 停机修改
                        if (mode == ApplicationConfig.modifyType[1]) {
                            final int index = position;
                            settings.setElevatorRunning(false);
                            getElevatorStatusDialog = new AlertDialog.Builder(ParameterDetailActivity.this,
                                    R.style.CustomDialogStyle)
                                    .setTitle(settings.getCodeText() + " " + settings.getName())
                                    .setMessage(R.string.get_elevator_status_message)
                                    .create();
                            getElevatorStatusDialog.show();
                            getElevatorStatusDialog.setCancelable(false);
                            getElevatorStatusDialog.setCanceledOnTouchOutside(false);
                            ParameterDetailActivity.this.hasGetElevatorStatus = false;
                            new CountDownTimer(2000, 500) {

                                @Override
                                public void onTick(long l) {
                                    if (!ParameterDetailActivity.this.hasGetElevatorStatus) {
                                        ParameterDetailActivity.this.getElevatorStatus(index, settings);
                                    }
                                }

                                @Override
                                public void onFinish() {
                                    if (getElevatorStatusDialog != null) {
                                        getElevatorStatusDialog.dismiss();
                                    }
                                    if (!ParameterDetailActivity.this.hasGetElevatorStatus) {
                                        Toast.makeText(ParameterDetailActivity.this,
                                                R.string.get_elevator_status_failed,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }.start();
                        }
                        // 不可修改
                        if (mode == ApplicationConfig.modifyType[2]) {
                            if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[0] ||
                                    settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[1]) {
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
     * Get Elevator Status
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
                                this.setSendBuffer(SerialUtility.crc16(SerialUtility.hexStr2Ints("0103"
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
        if (BluetoothTool.getInstance(this).isConnected()) {
            getElevatorStatusHandler.index = index;
            getElevatorStatusHandler.settings = settings;
            BluetoothTool.getInstance(this)
                    .setCommunications(getElevatorStatusCommunication)
                    .setHandler(getElevatorStatusHandler)
                    .send();
        } else {
            Toast.makeText(this,
                    R.string.not_connect_device_error,
                    android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * Create Number Picker Dialog
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
                // TODO Get Value
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
            cancelButton.setEnabled(false);
            confirmButton.setEnabled(false);
            List<String> codeArray = ParameterDetailActivity.this.getCodeStringArray(settings);
            ParameterDetailActivity.this.createGetValueScopeCommunications(codeArray, index, settings);
        } else {
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
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.weight = 1;
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
                        if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[1]) {
                            if (Integer.parseInt(settings.getType()) == 3
                                    && !settings.getName().contains("X25")
                                    && !settings.getName().contains("X26")
                                    && !settings.getName().contains("X27")) {
                                ToggleButton toggleButton = (ToggleButton) detailDialog.findViewById(R.id.toggle_button);
                                ListView listView = (ListView) detailDialog.findViewById(R.id.list_view);
                                CheckedListViewAdapter adapter = (CheckedListViewAdapter)listView.getAdapterSource();
                                int checkedIndex = adapter.getCheckedIndex();
                                if (!toggleButton.isChecked()) {
                                    checkedIndex += 32;
                                }
                                startSetNewValueCommunications(index, String.format("%04x", checkedIndex));
                            } else if (Integer.parseInt(settings.getType()) == 25) {
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
                        if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[2]) {
                            ListView listView = (ListView) detailDialog.findViewById(R.id.switch_list);
                            DialogSwitchListViewAdapter adapter = (DialogSwitchListViewAdapter) listView
                                    .getAdapterSource();
                            List<ParameterStatusItem> list = adapter.getItemList();
                            byte[] data = settings.getReceived();
                            boolean[] booleans = SerialUtility.byte2BoolArr(data[4], data[5]);
                            int size = booleans.length;
                            String binaryString = "";
                            for (int j = 0; j < size; j++) {
                                boolean hasValue = false;
                                boolean settingValue = false;
                                for (ParameterStatusItem item : list) {
                                    if (Integer.parseInt(item.getId()) == j) {
                                        hasValue = true;
                                        settingValue = item.getStatus();
                                    }
                                }
                                if (hasValue) {
                                    binaryString += settingValue ? 1 : 0;
                                } else {
                                    binaryString += booleans[j] ? 1 : 0;
                                }
                            }
                            //TODO 写入参数错误
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

    private List<String> getCodeStringArray(ParameterSettings settings) {
        String[] array = settings.getScope().split("~");
        List<String> list = new ArrayList<String>();
        for (String code : array) {
            if (code.contains("F")) {
                list.add(code);
            }
        }
        return list;
    }

    /**
     * 生成取得取值范围Talk
     *
     * @param list     Code List
     * @param index    ListView Index
     * @param settings ParameterSettings
     */
    private void createGetValueScopeCommunications(List<String> list, int index, final ParameterSettings settings) {
        BluetoothTalk[] communications = new BluetoothTalk[list.size()];
        int position = 0;
        for (final String code : list) {
            communications[position] = new BluetoothTalk() {
                @Override
                public void beforeSend() {
                    this.setSendBuffer(SerialUtility.crc16(SerialUtility.hexStr2Ints("0103"
                            + code
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
                        if (data.length == 8) {
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
            if (BluetoothTool.getInstance(ParameterDetailActivity.this).isConnected()) {
                getValueScopeHandler.count = communications.length;
                getValueScopeHandler.index = index;
                BluetoothTool.getInstance(ParameterDetailActivity.this)
                        .setCommunications(communications)
                        .setHandler(getValueScopeHandler)
                        .send();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BluetoothTool.getInstance(ParameterDetailActivity.this).isConnected()) {
            ParameterDetailActivity.this.syncingParameter = true;
            startCombinationCommunications();
        } else {
            Toast.makeText(this,
                    R.string.not_connect_device_error,
                    android.widget.Toast.LENGTH_SHORT)
                    .show();
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
                        this.setSendBuffer(SerialUtility.crc16(SerialUtility.hexStr2Ints("0106"
                                + ParseSerialsUtils.getCalculatedCode(settings)
                                + userValue
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
                            settings.setReceived(data);
                            return settings;
                        }
                        return null;
                    }
                }
        };
        if (BluetoothTool.getInstance(ParameterDetailActivity.this).isConnected()) {
            ParameterDetailActivity.this.isWriteSuccessful = false;
            updateHandler.index = position;
            updateHandler.writeCode = userValue;
            new CountDownTimer(1500, 500) {
                public void onTick(long millisUntilFinished) {
                    if (!ParameterDetailActivity.this.isWriteSuccessful) {
                        updateHandler.index = position;
                        updateHandler.writeCode = userValue;
                        BluetoothTool.getInstance(ParameterDetailActivity.this)
                                .setHandler(updateHandler)
                                .setCommunications(communications)
                                .send();
                    } else {
                        if (detailDialog != null && detailDialog.isShowing()) {
                            detailDialog.dismiss();
                        }
                        Toast.makeText(ParameterDetailActivity.this,
                                R.string.write_parameter_successful,
                                Toast.LENGTH_SHORT).show();
                        ParameterDetailActivity.this.isWriteSuccessful = true;
                    }
                }

                public void onFinish() {
                    if (!ParameterDetailActivity.this.isWriteSuccessful) {
                        if (writeErrorString != null) {
                            Toast.makeText(ParameterDetailActivity.this,
                                    writeErrorString,
                                    android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }.start();
        } else {
            Toast.makeText(this,
                    R.string.not_connect_device_error,
                    android.widget.Toast.LENGTH_SHORT)
                    .show();
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
        if (BluetoothTool.getInstance(ParameterDetailActivity.this).isConnected()) {
            parameterDetailHandler.sendCount = communications.length;
            BluetoothTool.getInstance(ParameterDetailActivity.this)
                    .setHandler(parameterDetailHandler)
                    .setCommunications(communications)
                    .send();
        } else {
            Toast.makeText(this,
                    R.string.not_connect_device_error,
                    android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
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
        startCombinationCommunications();
    }

    // ===================================== Update ListView Data Handler ======================================== //

    /**
     * Update Handler
     */
    private class UpdateHandler extends BluetoothHandler {

        public int index;

        public String writeCode;

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
                ParameterSettings settings = (ParameterSettings) msg.obj;
                String returnCodeString = SerialUtility.byte2HexStr(settings.getReceived());
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
                if (writeSuccessful) {
                    ParameterDetailActivity.this.listViewDataSource.set(this.index, settings);
                    ParameterDetailActivity.this.instantAdapter.notifyDataSetChanged();
                    ParameterDetailActivity.this.isWriteSuccessful = true;
                }
            }
        }

    }

    // ================================= Get Max value handler ============================================== //
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
                final ParameterSettings settings = listViewDataSource.get(index);
                if (count == 2) {
                    double value1 = Double.parseDouble(stringList.get(0));
                    double value2 = Double.parseDouble(stringList.get(1));
                    if (value1 < value2) {
                        settings.setScope(stringList.get(0) + "~" + stringList.get(1));
                    } else {
                        settings.setScope(stringList.get(1) + "~" + stringList.get(0));
                    }
                }
                if (count == 1) {
                    String[] valueArray = settings.getScope().split("~");
                    List<String> tempList = new ArrayList<String>();
                    for (String value : valueArray) {
                        if (value.contains("F")) {
                            tempList.add(stringList.get(0));
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
                ParameterDetailActivity.this.createNumberPickerAndBindListener(settings);
            } else {
                ParameterSettings settings = listViewDataSource.get(index);
                List<String> codeArray = ParameterDetailActivity.this.getCodeStringArray(settings);
                ParameterDetailActivity.this.createGetValueScopeCommunications(codeArray, index, settings);
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
            ParameterSettings settings = listViewDataSource.get(index);
            List<String> codeArray = ParameterDetailActivity.this.getCodeStringArray(settings);
            ParameterDetailActivity.this.createGetValueScopeCommunications(codeArray, index, settings);
        }
    }

    // ============================= Get Elevator Status Handler ============================ //
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
                RealTimeMonitor monitor = (RealTimeMonitor) msg.obj;
                int status = ParseSerialsUtils.getIntFromBytes(monitor.getReceived());
                settings.setElevatorRunning(status != 3);
                if (settings.isElevatorRunning()) {
                    if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[2]) {
                        onClickListViewWithIndex(index);
                    }
                    Toast.makeText(ParameterDetailActivity.this,
                            R.string.elevator_running_message,
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[0]) {
                        createPickerDialog(index, settings);
                    } else {
                        onClickListViewWithIndex(index);
                    }
                }
                if (getElevatorStatusDialog != null) {
                    getElevatorStatusDialog.dismiss();
                }
                ParameterDetailActivity.this.hasGetElevatorStatus = true;
            }
        }

        @Override
        public void onTalkError(Message msg) {
            super.onTalkError(msg);
        }

    }

}