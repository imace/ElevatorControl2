package com.kio.ElevatorControl.views.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.adapters.DialogSwitchListViewAdapter;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.daos.ErrorHelpDao;
import com.kio.ElevatorControl.models.*;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;
import com.mobsandgeeks.adapters.InstantAdapter;
import org.holoeverywhere.widget.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class CustomDialog {

    // ==================================== Terminal Detail Dialog ================================== //
    public static AlertDialog.Builder terminalDetailDialog(final Activity activity, final RealTimeMonitor monitor) {
        byte[] data = monitor.getReceived();
        if (data.length != 8) {
            return new AlertDialog.Builder(activity)
                    .setTitle(R.string.data_error_text)
                    .setNeutralButton(R.string.dialog_btn_ok, null);
        }
        View dialogView = activity.getLayoutInflater().inflate(R.layout.terminal_detail_dialog, null);
        ListView listView = (ListView) dialogView.findViewById(R.id.status_list);
        List<ParameterStatusItem> statusList = new ArrayList<ParameterStatusItem>();
        if (monitor.getDescriptionType() == ApplicationConfig.DESCRIPTION_TYPE[2]) {
            boolean[] bitsValue = HSerial.byte2BoolArr(data[4], data[5]);
            int bitsSize = bitsValue.length;
            try {
                JSONArray valuesArray = new JSONArray(monitor.getJSONDescription());
                int size = valuesArray.length();
                for (int i = 0; i < size; i++) {
                    JSONObject value = valuesArray.getJSONObject(i);
                    if (i < bitsSize) {
                        if (!value.optString("value").contains(ApplicationConfig.RETAIN_NAME)) {
                            ParameterStatusItem status = new ParameterStatusItem();
                            status.name = value.optString("value");
                            status.status = bitsValue[Integer.parseInt(value.optString("id"))];
                            statusList.add(status);
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (monitor.getDescriptionType() == ApplicationConfig.DESCRIPTION_TYPE[3]) {
            try {
                JSONArray valuesArray = new JSONArray(monitor.getJSONDescription());
                int size = valuesArray.length();
                Pattern pattern = Pattern.compile("^\\d*\\-\\d*:.*", Pattern.CASE_INSENSITIVE);
                for (int i = 0; i < size; i++) {
                    JSONObject value = valuesArray.getJSONObject(i);
                    ParameterStatusItem status = new ParameterStatusItem();
                    for (Iterator iterator = value.keys(); iterator.hasNext(); ) {
                        String name = (String) iterator.next();
                        if (name.equalsIgnoreCase("value")) {
                            if (!value.optString("value").contains(ApplicationConfig.RETAIN_NAME)) {
                                status.name = value.optString("value");
                            }
                        }
                        if (name.equalsIgnoreCase("id")) {
                            status.status = ParseSerialsUtils
                                    .getIntValueFromBytesInSection(new byte[]{data[4], data[5]},
                                            new int[]{Integer.parseInt(value.optString("id"))}) == 1;
                        }
                        if (pattern.matcher(name).matches()) {
                            String[] intStringArray = name.split(":")[0].split("-");
                            status.name = name.replaceAll("\\d*\\-\\d*:", "");
                            JSONArray subArray = value.optJSONArray(name);
                            int intValue = ParseSerialsUtils
                                    .getIntValueFromBytesInSection(new byte[]{data[4], data[5]}, new int[]{
                                            Integer.parseInt(intStringArray[0]),
                                            Integer.parseInt(intStringArray[1])
                                    });
                            int subArraySize = subArray.length();
                            for (int j = 0; j < subArraySize; j++) {
                                int index = Integer.parseInt(subArray.getJSONObject(j).optString("id"));
                                if (index == intValue) {
                                    status.statusString = subArray.getJSONObject(j).optString("value");
                                }
                            }
                        }
                    }
                    if (status.name != null) {
                        statusList.add(status);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        InstantAdapter<ParameterStatusItem> adapter = new InstantAdapter<ParameterStatusItem>(
                activity.getBaseContext(),
                R.layout.terminal_status_item,
                ParameterStatusItem.class,
                statusList);
        listView.setAdapter(adapter);
        return new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                .setView(dialogView)
                .setTitle(monitor.getName())
                .setNeutralButton(R.string.dialog_btn_ok, null);
    }

    // ================================== Parameter Detail Dialog =========================================== //
    public static AlertDialog.Builder parameterDetailDialog(final Activity activity,
                                                            final ParameterSettings settings) {
        if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[0]) {
            View dialogView = activity.getLayoutInflater().inflate(R.layout.parameter_picker_dialog, null);
            LinearLayout containView = (LinearLayout) dialogView.findViewById(R.id.picker_view);
            // 滑动数值选择器
            final SeekBar seekBar = (SeekBar) dialogView.findViewById(R.id.seek_bar);
            String maxValueString = settings.getScope().split("-")[1];
            int totals = maxValueString.length();
            String[] hexValue = new String[]{"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "A", "B", "C", "D", "E", "F"};
            // 数值滚动选择器
            for (int i = 0; i < totals; i++) {
                String valueChar = Character.toString(maxValueString.charAt(i));
                NumberPicker picker = new NumberPicker(activity);
                picker.setWrapSelectorWheel(false);
                picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
                if (ParseSerialsUtils.isInteger(valueChar)) {
                    picker.setMinValue(0);
                    picker.setMaxValue(Integer.parseInt(valueChar));
                }
                if (valueChar.equalsIgnoreCase(".")) {
                    picker.setMinValue(0);
                    picker.setMaxValue(0);
                    picker.setDisplayedValues(new String[]{"."});
                }
                picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker picker, int oldValue, int newValue) {

                    }
                });
                int margin = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5,
                        activity.getResources().getDisplayMetrics()));
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(0, 0, margin, 0);
                containView.addView(picker, layoutParams);
            }
            Long maxValueLong = Math.round(Double.parseDouble(maxValueString)
                    / (Double.parseDouble(settings.getScale())));
            int maxValueInt = maxValueLong.intValue();
            seekBar.setMax(maxValueInt);
            seekBar.setProgress(1);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    Log.v("AAABBB", String.valueOf(seekBar.getProgress()));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            return new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                    .setView(dialogView)
                    .setTitle(settings.getName())
                    .setNeutralButton(R.string.dialog_btn_cancel, null);
            /*
            View dialogView = activity.getLayoutInflater().inflate(R.layout.parameter_edit_dialog, null);
            EditText settingValueEditText = (EditText) dialogView.findViewById(R.id.setting_value);
            TextView scaleNewValue = (TextView) dialogView.findViewById(R.id.scale_new_value);
            TextView unitTextView = (TextView) dialogView.findViewById(R.id.unit);
            TextView defaultValueTextView = (TextView) dialogView.findViewById(R.id.default_value);
            TextView valueScopeTextView = (TextView) dialogView.findViewById(R.id.value_scope);
            TextView scaleTextView = (TextView) dialogView.findViewById(R.id.scale);
            settingValueEditText.setText(String.valueOf(ParseSerialsUtils.getIntFromBytes(settings.getReceived())));
            scaleNewValue.setText(settings.getScale());
            defaultValueTextView.setText(settings.getDefaultValue());
            unitTextView.setText(settings.getUnit());
            valueScopeTextView.setText(settings.getScope());
            scaleTextView.setText(settings.getScale());
            return new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                    .setView(dialogView)
                    .setTitle(settings.getName())
                    .setNeutralButton(R.string.dialog_btn_cancel, null);
                    */
        }
        if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[1]) {
            try {
                JSONArray jsonArray = new JSONArray(settings.getJSONDescription());
                int size = jsonArray.length();
                CharSequence[] charArray = new CharSequence[size];
                for (int i = 0; i < size; i++) {
                    JSONObject value = jsonArray.getJSONObject(i);
                    charArray[i] = value.optString("value");
                }
                return new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                        .setSingleChoiceItems(charArray,
                                ParseSerialsUtils.getIntFromBytes(settings.getReceived()),
                                null)
                        .setTitle(settings.getName())
                        .setNeutralButton(R.string.dialog_btn_cancel, null);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[2]) {
            View dialogView = activity.getLayoutInflater().inflate(R.layout.parameter_switch_dialog, null);
            ListView listView = (ListView) dialogView.findViewById(R.id.switch_list);
            List<ParameterStatusItem> itemList = new ArrayList<ParameterStatusItem>();
            try {
                JSONArray jsonArray = new JSONArray(settings.getJSONDescription());
                int size = jsonArray.length();
                for (int i = 0; i < size; i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    if (!jsonObject.optString("value").contains(ApplicationConfig.RETAIN_NAME)) {
                        ParameterStatusItem item = new ParameterStatusItem();
                        item.id = jsonObject.optString("id");
                        item.name = jsonObject.optString("value");
                        itemList.add(item);
                    }
                }
                DialogSwitchListViewAdapter adapter = new DialogSwitchListViewAdapter(itemList, activity);
                listView.setAdapter(adapter);
                return new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                        .setView(dialogView)
                        .setTitle(settings.getName())
                        .setNeutralButton(R.string.dialog_btn_cancel, null);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                .setTitle(settings.getName())
                .setNeutralButton(R.string.dialog_btn_cancel, null);
    }

    // ===================================== Exit dialog ========================================== //
    public static AlertDialog.Builder exitDialog(final Activity activity) {
        return new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                .setMessage(activity.getResources().getString(R.string.are_you_sure_exit))
                .setTitle(activity.getResources().getString(R.string.are_you_sure_message))
                .setNegativeButton(activity.getResources().getString(R.string.dialog_btn_cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                arg0.cancel();
                            }
                        }).setPositiveButton(activity.getResources().getString(R.string.dialog_btn_ok)
                        , new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        HBluetooth.getInstance(activity).kill();
                        activity.finish();
                        System.exit(0);
                    }
                });
    }

    // ========================================== History Error Dialog ================================== //
    public static AlertDialog.Builder historyErrorDialog(final HistoryError historyError, final Activity activity) {
        final View dialogView = activity.getLayoutInflater().inflate(R.layout.error_detail_dialog, null);
        assert dialogView != null;
        TextView errorCode = ((TextView) dialogView.findViewById(R.id.current_error_help_display));
        TextView level = ((TextView) dialogView.findViewById(R.id.current_error_help_level));
        TextView name = ((TextView) dialogView.findViewById(R.id.current_error_help_name));
        TextView reason = ((TextView) dialogView.findViewById(R.id.current_error_help_reason));
        TextView solution = ((TextView) dialogView.findViewById(R.id.current_error_help_solution));
        ErrorHelp errorHelp = ErrorHelpDao.findByDisplay(activity, historyError.getErrorCode());
        if (errorHelp != null) {
            errorCode.setText(errorHelp.getDisplay());
            level.setText(errorHelp.getLevel());
            name.setText(errorHelp.getName());
            reason.setText(errorHelp.getReason());
            solution.setText(errorHelp.getSolution());
        }
        return new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                .setView(dialogView)
                .setNeutralButton(R.string.dialog_btn_ok, null);
    }
}
