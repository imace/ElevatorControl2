package com.kio.ElevatorControl.activities;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import butterknife.InjectView;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HHandler;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.adapters.DialogSwitchListViewAdapter;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.daos.ParameterGroupSettingsDao;
import com.kio.ElevatorControl.handlers.ParameterDetailHandler;
import com.kio.ElevatorControl.models.ListHolder;
import com.kio.ElevatorControl.models.ParameterGroupSettings;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.models.ParameterStatusItem;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;
import com.kio.ElevatorControl.views.dialogs.CustomDialog;
import com.mobsandgeeks.adapters.InstantAdapter;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ParameterDetailActivity extends Activity {

    private static final String TAG = ParameterDetailActivity.class.getSimpleName();

    private UpdateHandler updateHandler;

    private AlertDialog detailDialog;

    public List<ParameterSettings> settingsList;

    private ParameterDetailHandler parameterDetailHandler;

    public InstantAdapter<ParameterSettings> instantAdapter;

    private HCommunication[] communications;

    public boolean isSynced;

    private static final int SYNC_TIME = 1500;

    private Handler handler = new Handler();

    private Runnable syncTask;

    private boolean syncParameterRunning = false;

    private GetValueScopeHandler getValueScopeHandler;

    private Button cancelButton;

    private Button confirmButton;

    private TextView waitTextView;

    private TextView descriptionTextView;

    private LinearLayout pickerContainer;

    private LinearLayout pickerView;

    private boolean isWritingData = false;

    private boolean isWriteSuccessful = false;

    private List<NumberPicker> numberPickerList;

    private int dotIndex = -1;

    private long maxValueLong;

    private long minValueLong;

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
        updateHandler = new UpdateHandler(this);
        initListViewData();
        bindListViewItemClickListener();
        syncTask = new Runnable() {
            @Override
            public void run() {
                if (syncParameterRunning) {
                    if (!isWritingData) {
                        ParameterDetailActivity.this.startCombinationCommunications();
                    }
                    handler.postDelayed(this, SYNC_TIME);
                }
            }
        };
    }

    private void initListViewData() {
        int SelectedId = this.getIntent().getIntExtra("SelectedId", 0);
        ParameterGroupSettings parameterGroupSettings = ParameterGroupSettingsDao.findById(
                this, SelectedId);
        this.setTitle(parameterGroupSettings.getGroupText());
        settingsList = parameterGroupSettings.getParametersettings().getList();
        List<ParameterSettings> tempList = new ArrayList<ParameterSettings>();
        for (ParameterSettings item : settingsList) {
            if (!item.getName().contains(ApplicationConfig.RETAIN_NAME)) {
                tempList.add(item);
            }
        }
        instantAdapter = new InstantAdapter<ParameterSettings>(this,
                R.layout.list_parameter_group_item,
                ParameterSettings.class,
                tempList);
        parameterDetailListView.setAdapter(instantAdapter);
    }

    /**
     * 绑定ListView Item点击时间
     */
    private void bindListViewItemClickListener() {
        parameterDetailListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (isSynced) {
                    final ParameterSettings settings = settingsList.get(position);
                    int mode = Integer.parseInt(settings.getMode());
                    if (mode == ApplicationConfig.modifyType[0]) {
                        if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[0]) {
                            createPickerDialog(position, settings);
                        } else {
                            onClickListViewWithIndex(position);
                        }
                    }
                    if (mode == ApplicationConfig.modifyType[1]) {
                        new AlertDialog.Builder(ParameterDetailActivity.this,
                                R.style.CustomDialogStyle)
                                .setTitle(settings.getCodeText() + " " + settings.getName())
                                .setMessage(R.string.stop_to_modify_message)
                                .setPositiveButton(R.string.dialog_btn_ok, null)
                                .create()
                                .show();
                    }
                    if (mode == ApplicationConfig.modifyType[2]) {
                        new AlertDialog.Builder(ParameterDetailActivity.this,
                                R.style.CustomDialogStyle)
                                .setTitle(settings.getCodeText() + " " + settings.getName())
                                .setMessage(R.string.cannot_modify_message)
                                .setPositiveButton(R.string.dialog_btn_ok, null)
                                .create()
                                .show();
                    }
                }
            }
        });
    }

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
                        startSetNewValueCommunications(index, String.format("%04x ", userValueLong.intValue()));
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
            ParameterDetailActivity.this.syncParameterRunning = false;
            List<String> codeArray = ParameterDetailActivity.this.getCodeStringArray(settings);
            ParameterDetailActivity.this.createCommunications(codeArray, index, settings);
        } else {
            createNumberPickerAndBindListener(settings);
        }
    }

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
            for (int i = 0; i < totals; i++) {
                String valueChar = Character.toString(maxValueString.charAt(i));
                String currentValueChar = Character.toString(currentValueString.charAt(i));
                int margin = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5,
                        getResources().getDisplayMetrics()));
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                if (ParseSerialsUtils.isInteger(valueChar)) {
                    NumberPicker picker = new NumberPicker(this);
                    picker.setWrapSelectorWheel(false);
                    picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
                    picker.setMinValue(0);
                    picker.setMaxValue(9);
                    picker.setValue(Integer.parseInt(currentValueChar));
                    layoutParams.setMargins(0, 0, margin, 0);
                    pickerView.addView(picker, layoutParams);
                    numberPickerList.add(picker);
                }
                if (valueChar.equalsIgnoreCase(".")) {
                    TextView textView = new TextView(this);
                    textView.setText(".");
                    textView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                    layoutParams.setMargins(0, 15, margin, 0);
                    pickerView.addView(textView, layoutParams);
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
        String metaText = "取值范围:" + scopeString + " "
                + " 默认值:" + settings.getDefaultValue() + " "
                + unit;
        descriptionTextView.setText(metaText);
        waitTextView.setVisibility(View.GONE);
        pickerContainer.setVisibility(View.VISIBLE);
        cancelButton.setEnabled(true);
        confirmButton.setEnabled(true);
    }

    private void onClickListViewWithIndex(final int index) {
        final ParameterSettings settings = settingsList.get(index);
        AlertDialog.Builder builder = CustomDialog.parameterDetailDialog(ParameterDetailActivity.this,
                settings);
        builder.setPositiveButton(R.string.dialog_btn_ok, null);
        detailDialog = builder.create();
        detailDialog.show();
        detailDialog.setCancelable(false);
        detailDialog.setCanceledOnTouchOutside(false);
        cancelButton = detailDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        confirmButton = detailDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        detailDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[1]) {
                    int checkedIndex = detailDialog.getListView().getCheckedItemPosition();
                    if (checkedIndex != ParseSerialsUtils.getIntFromBytes(settings.getReceived())) {
                        startSetNewValueCommunications(index, String.format("%04x ", checkedIndex));
                    }
                    detailDialog.dismiss();
                }
                if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[2]) {
                    ListView listView = (ListView) detailDialog.findViewById(R.id.switch_list);
                    DialogSwitchListViewAdapter adapter = (DialogSwitchListViewAdapter) listView
                            .getAdapterSource();
                    List<ParameterStatusItem> list = adapter.getItemList();
                    byte[] data = settings.getReceived();
                    boolean[] booleans = HSerial.byte2BoolArr(data[4], data[5]);
                    int size = booleans.length;
                    String binaryString = "";
                    for (int j = 0; j < size; j++) {
                        boolean hasValue = false;
                        boolean settingValue = false;
                        for (ParameterStatusItem item : list) {
                            if (Integer.parseInt(item.id) == j) {
                                hasValue = true;
                                settingValue = item.status;
                            }
                        }
                        if (hasValue) {
                            binaryString += settingValue ? 1 : 0;
                        } else {
                            binaryString += booleans[j] ? 1 : 0;
                        }
                    }
                    startSetNewValueCommunications(index,
                            String.format("%04x ", Integer.parseInt(binaryString, 2)));
                    detailDialog.dismiss();
                }
            }
        });
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

    private void createCommunications(List<String> list, int index, final ParameterSettings settings) {
        HCommunication[] communications = new HCommunication[list.size()];
        int position = 0;
        for (final String code : list) {
            communications[position] = new HCommunication() {
                @Override
                public void beforeSend() {
                    this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103"
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
                    if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                        byte[] data = HSerial.trimEnd(getReceivedBuffer());
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
            if (HBluetooth.getInstance(ParameterDetailActivity.this).isPrepared()) {
                getValueScopeHandler.count = communications.length;
                getValueScopeHandler.index = index;
                HBluetooth.getInstance(ParameterDetailActivity.this)
                        .setCommunications(communications)
                        .setHandler(getValueScopeHandler)
                        .Start();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncParameterRunning = true;
        handler.postDelayed(syncTask, SYNC_TIME);
    }

    /**
     * Start Set New Value Communications
     *
     * @param position ListView Item Position
     * @param value    New Setting value (Hex String)
     */
    private void startSetNewValueCommunications(int position, String value) {
        final String newSettingValue = value;
        final ParameterSettings settings = settingsList.get(position);
        final HCommunication[] communications = new HCommunication[]{
                new HCommunication() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0106"
                                + settings.getCode()
                                + newSettingValue
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
                            settings.setReceived(received);
                            return settings;
                        }
                        return null;
                    }
                }
        };
        if (HBluetooth.getInstance(ParameterDetailActivity.this).isPrepared()) {
            ParameterDetailActivity.this.isWritingData = true;
            ParameterDetailActivity.this.isWriteSuccessful = false;
            updateHandler.index = position;
            new CountDownTimer(1500, 500) {
                public void onTick(long millisUntilFinished) {
                    if (!ParameterDetailActivity.this.isWriteSuccessful) {
                        HBluetooth.getInstance(ParameterDetailActivity.this)
                                .setHandler(updateHandler)
                                .setCommunications(communications)
                                .Start();
                    } else {
                        ParameterDetailActivity.this.isWritingData = false;
                        if (detailDialog != null) {
                            detailDialog.dismiss();
                        }
                    }
                }

                public void onFinish() {
                    if (!ParameterDetailActivity.this.isWriteSuccessful) {
                        Toast.makeText(ParameterDetailActivity.this,
                                R.string.write_failed_text,
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                    ParameterDetailActivity.this.isWritingData = true;
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
        syncParameterRunning = false;
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    /**
     * Start Combination Communications
     * 组合发送指令
     */
    public void startCombinationCommunications() {
        isSynced = false;
        if (communications == null) {
            final int size = settingsList.size();
            final int count = size <= 10 ? 1 : ((size - size % 10) / 10 + (size % 10 == 0 ? 0 : 1));
            communications = new HCommunication[count];
            for (int i = 0; i < count; i++) {
                final int position = i;
                final ParameterSettings firstItem = settingsList.get(position * 10);
                final int length = size <= 10 ? size : (size % 10 == 0 ? 10 : ((position == count - 1) ? size % 10 : 10));
                communications[i] = new HCommunication() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(HSerial.crc16(HSerial
                                .hexStr2Ints("0103"
                                        + ParseSerialsUtils.getCalculatedCode(firstItem)
                                        + String.format("%04x ", length)
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
                            byte[] data = HSerial.trimEnd(getReceivedBuffer());
                            short bytesLength = ByteBuffer.wrap(new byte[]{data[2], data[3]}).getShort();
                            if (length * 2 == bytesLength) {
                                List<ParameterSettings> tempList = new ArrayList<ParameterSettings>();
                                for (int j = 0; j < length; j++) {
                                    if (position * 10 + j < settingsList.size()) {
                                        ParameterSettings item = settingsList.get(position * 10 + j);
                                        byte[] tempData = HSerial.crc16(HSerial.hexStr2Ints("01030002"
                                                + HSerial.byte2HexStr(new byte[]{data[4 + j * 2], data[5 + j * 2]})));
                                        item.setReceived(tempData);
                                        tempList.add(item);
                                    }
                                }
                                ListHolder holder = new ListHolder();
                                holder.setParameterSettingsList(tempList);
                                return holder;
                            }
                        }
                        return null;
                    }
                };
            }
        }
        if (HBluetooth.getInstance(ParameterDetailActivity.this).isPrepared()) {
            parameterDetailHandler.sendCount = communications.length;
            HBluetooth.getInstance(ParameterDetailActivity.this)
                    .setHandler(parameterDetailHandler)
                    .setCommunications(communications)
                    .Start();
        } else {
            Toast.makeText(this,
                    R.string.not_connect_device_error,
                    android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
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

    // ===================================== Update ListView Data Handler ======================================== //

    /**
     * Update Handler
     */
    private class UpdateHandler extends HHandler {

        private int index;

        public UpdateHandler(android.app.Activity activity) {
            super(activity);
            TAG = UpdateHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {

        }

        @Override
        public void onMultiTalkEnd(Message msg) {

        }

        @Override
        public void onTalkReceive(Message msg) {
            if (msg.obj instanceof ParameterSettings) {
                ParameterSettings settings = (ParameterSettings) msg.obj;
                ParameterDetailActivity.this.settingsList.set(index, settings);
                ParameterDetailActivity.this.instantAdapter.notifyDataSetChanged();
                ParameterDetailActivity.this.isWriteSuccessful = true;
            }
        }

    }

    // ================================= Get Max value handler ============================================== //
    private class GetValueScopeHandler extends HHandler {

        private List<String> stringList;

        public int count;

        private int index;

        public GetValueScopeHandler(Activity activity) {
            super(activity);
            TAG = GetValueScopeHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            stringList = new ArrayList<String>();
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            if (stringList.size() == count) {
                final ParameterSettings settings = settingsList.get(index);
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
                ParameterDetailActivity.this.syncParameterRunning = true;
            } else {
                ParameterSettings settings = settingsList.get(index);
                List<String> codeArray = ParameterDetailActivity.this.getCodeStringArray(settings);
                ParameterDetailActivity.this.createCommunications(codeArray, index, settings);
            }
        }

        @Override
        public void onTalkReceive(Message msg) {
            if (msg.obj != null && msg.obj instanceof String) {
                stringList.add((String) msg.obj);
            }
        }

        @Override
        public void onTalkError(Message msg) {
            ParameterSettings settings = settingsList.get(index);
            List<String> codeArray = ParameterDetailActivity.this.getCodeStringArray(settings);
            ParameterDetailActivity.this.createCommunications(codeArray, index, settings);
        }
    }

}