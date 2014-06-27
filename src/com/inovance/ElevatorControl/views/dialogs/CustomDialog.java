package com.inovance.ElevatorControl.views.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.bluetoothtool.BluetoothTool;
import com.bluetoothtool.SerialUtility;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.activities.CheckAuthorizationActivity;
import com.inovance.ElevatorControl.adapters.CheckedListViewAdapter;
import com.inovance.ElevatorControl.adapters.DialogSwitchListViewAdapter;
import com.inovance.ElevatorControl.adapters.ParameterStatusAdapter;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.daos.ErrorHelpDao;
import com.inovance.ElevatorControl.models.*;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import com.inovance.ElevatorControl.web.WebApi;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class CustomDialog {
    // ==================================== Terminal Detail Dialog ================================== //
    public static AlertDialog.Builder terminalDetailDialog(final Activity activity, final RealTimeMonitor monitor) {
        byte[] data = monitor.getReceived();
        View dialogView = activity.getLayoutInflater().inflate(R.layout.terminal_detail_dialog, null);
        ListView listView = (ListView) dialogView.findViewById(R.id.status_list);
        List<ParameterStatusItem> statusList = new ArrayList<ParameterStatusItem>();
        if (monitor.getDescriptionType() == ApplicationConfig.DESCRIPTION_TYPE[2]) {
            boolean[] booleanArray = ParseSerialsUtils.getBooleanValueArray(new byte[]{data[4], data[5]});
            int bitsSize = booleanArray.length;
            try {
                JSONArray valuesArray = new JSONArray(monitor.getJSONDescription());
                int size = valuesArray.length();
                for (int i = 0; i < size; i++) {
                    JSONObject value = valuesArray.getJSONObject(i);
                    if (i < bitsSize) {
                        if (!value.optString("value").contains(ApplicationConfig.RETAIN_NAME)) {
                            ParameterStatusItem status = new ParameterStatusItem();
                            status.setName(value.optString("value"));
                            status.setStatus(booleanArray[Integer.parseInt(value.optString("id"))]);
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
                                status.setName(value.optString("value"));
                            }
                        }
                        if (name.equalsIgnoreCase("id")) {
                            status.setStatus(ParseSerialsUtils
                                    .getIntValueFromBytesInSection(new byte[]{data[4], data[5]},
                                            new int[]{Integer.parseInt(value.optString("id"))}) == 1);
                        }
                        if (pattern.matcher(name).matches()) {
                            String[] intStringArray = name.split(":")[0].split("-");
                            status.setName(name.replaceAll("\\d*\\-\\d*:", ""));
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
                                    status.setStatusString(subArray.getJSONObject(j).optString("value"));
                                }
                            }
                        }
                    }
                    if (status.getName() != null) {
                        statusList.add(status);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        ParameterStatusAdapter adapter = new ParameterStatusAdapter(activity, statusList);
        listView.setAdapter(adapter);
        return new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                .setView(dialogView)
                .setTitle(monitor.getCodeText() + " " + monitor.getName())
                .setNeutralButton(R.string.dialog_btn_ok, null);
    }

    // ================================== Parameter Detail Dialog =========================================== //
    public static AlertDialog.Builder parameterDetailDialog(final Activity activity,
                                                            final ParameterSettings settings) {
        // 单选弹出框
        if (settings.getDescriptionType() == ApplicationConfig.DESCRIPTION_TYPE[1]) {
            try {
                JSONArray jsonArray = new JSONArray(settings.getJSONDescription());
                int size = jsonArray.length();
                String[] statusList = new String[size];
                String[] spinnerList = new String[size];
                int type = Integer.parseInt(settings.getType());
                for (int i = 0; i < size; i++) {
                    JSONObject value = jsonArray.getJSONObject(i);
                    int alwaysClose = Integer.parseInt(value.optString("id")) + 32;
                    if (type == 3 || type == 25) {
                        if (i == 0) {
                            statusList[i] = value.optString("id") + ":" + value.optString("value");
                        } else {
                            statusList[i] = value.optString("id") + "/" + alwaysClose + ":" + value.optString("value");
                        }
                    } else {
                        statusList[i] = value.optString("id") + ":" + value.optString("value");
                    }
                    spinnerList[i] = value.optString("value");
                }
                // 端子 X1 - x24
                if (Integer.parseInt(settings.getType()) == ApplicationConfig.InputTerminalType) {
                    View dialogView = activity.getLayoutInflater()
                            .inflate(R.layout.parameter_terminal_status_dialog, null);
                    final ListView listView = (ListView) dialogView.findViewById(R.id.list_view);
                    TextView defaultValue = (TextView) dialogView.findViewById(R.id.default_value);
                    final ToggleButton toggleButton = (ToggleButton) dialogView.findViewById(R.id.toggle_button);
                    boolean toggleButtonStatus = true;
                    defaultValue.setText("出厂值: " + settings.getDefaultValue());
                    int index = ParseSerialsUtils.getIntFromBytes(settings.getReceived());
                    if (index >= 32 && index < 64) {
                        index = index - 32;
                        toggleButtonStatus = false;
                    }
                    if (index >= 96 && index < 127) {
                        index = index - 32;
                        toggleButtonStatus = false;
                    }
                    toggleButton.setChecked(toggleButtonStatus);
                    final CheckedListViewAdapter adapter = new CheckedListViewAdapter(activity, statusList, index);
                    listView.setAdapter(adapter);
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                            listView.setSelection(position);
                            adapter.setCheckedIndex(position);
                        }
                    });
                    return new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                            .setView(dialogView)
                            .setTitle(settings.getCodeText() + " " + settings.getName());
                } else if (Integer.parseInt(settings.getType()) == ApplicationConfig.FloorShowType) {
                    View dialogView = activity.getLayoutInflater()
                            .inflate(R.layout.parameter_type25_dialog, null);
                    Spinner modSpinner = (Spinner) dialogView.findViewById(R.id.mod_value);
                    Spinner remSpinner = (Spinner) dialogView.findViewById(R.id.rem_value);
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,
                            android.R.layout.simple_spinner_item,
                            spinnerList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    modSpinner.setAdapter(adapter);
                    remSpinner.setAdapter(adapter);
                    int value = ParseSerialsUtils.getIntFromBytes(settings.getReceived());
                    int modValue = value / 100;
                    int remValue = value % 100;
                    if (modValue < statusList.length && remValue < statusList.length) {
                        modSpinner.setSelection(modValue);
                        remSpinner.setSelection(remValue);
                    }
                    return new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                            .setView(dialogView)
                            .setTitle(settings.getCodeText() + " " + settings.getName());
                } else {
                    return new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                            .setSingleChoiceItems(statusList,
                                    ParseSerialsUtils.getIntFromBytes(settings.getReceived()),
                                    null)
                            .setTitle(settings.getCodeText() + " " + settings.getName());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        // 开关弹出框
        if (settings.getDescriptionType() == ApplicationConfig.DESCRIPTION_TYPE[2]) {
            View dialogView = activity.getLayoutInflater().inflate(R.layout.parameter_switch_dialog, null);
            ListView listView = (ListView) dialogView.findViewById(R.id.switch_list);
            List<ParameterStatusItem> itemList = new ArrayList<ParameterStatusItem>();
            boolean[] booleanArray = ParseSerialsUtils
                    .getBooleanValueArray(new byte[]{settings.getReceived()[4], settings.getReceived()[5]});
            boolean isSpecial = Integer.parseInt(settings.getType()) == ApplicationConfig.InputSelectType;
            try {
                JSONArray jsonArray = new JSONArray(settings.getJSONDescription());
                int size = jsonArray.length();
                int length = booleanArray.length;
                for (int i = 0; i < size; i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    if (i < length) {
                        ParameterStatusItem item = new ParameterStatusItem();
                        item.setId(jsonObject.optString("id"));
                        item.setName(jsonObject.optString("value"));
                        item.setStatus(booleanArray[i]);
                        if (Integer.parseInt(settings.getMode()) == ApplicationConfig.modifyType[2]) {
                            item.setCanEdit(false);
                        } else {
                            item.setCanEdit(!settings.isElevatorRunning());
                        }
                        item.setSpecial(isSpecial);
                        itemList.add(item);
                    }
                }
                DialogSwitchListViewAdapter adapter = new DialogSwitchListViewAdapter(itemList, activity);
                listView.setAdapter(adapter);
                return new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                        .setView(dialogView)
                        .setTitle(settings.getCodeText() + " " + settings.getName());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                .setTitle(settings.getCodeText() + " " + settings.getName())
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
                        })
                .setPositiveButton(activity.getResources().getString(R.string.dialog_btn_ok)
                        , new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        WebApi.getInstance().removeListener();
                        BluetoothTool.getInstance().setHandler(null);
                        BluetoothTool.getInstance().kill();
                        Intent intent = new Intent(activity, CheckAuthorizationActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("Exit", true);
                        activity.startActivity(intent);
                        activity.finish();
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
