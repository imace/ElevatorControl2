package com.inovance.ElevatorControl.views.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HSerial;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.activities.CheckAuthorizationActivity;
import com.inovance.ElevatorControl.adapters.DialogSwitchListViewAdapter;
import com.inovance.ElevatorControl.adapters.ParameterStatusAdapter;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.daos.ErrorHelpDao;
import com.inovance.ElevatorControl.models.*;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.TextView;
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
                            status.setName(value.optString("value"));
                            status.setStatus(bitsValue[Integer.parseInt(value.optString("id"))]);
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
                        .setTitle(settings.getCodeText() + " " + settings.getName())
                        .setNeutralButton(R.string.dialog_btn_cancel, null);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        // 开关弹出框
        if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[2]) {
            View dialogView = activity.getLayoutInflater().inflate(R.layout.parameter_switch_dialog, null);
            ListView listView = (ListView) dialogView.findViewById(R.id.switch_list);
            List<ParameterStatusItem> itemList = new ArrayList<ParameterStatusItem>();
            boolean[] booleanArray = HSerial.byte2BoolArr(settings.getReceived()[4], settings.getReceived()[5]);
            try {
                JSONArray jsonArray = new JSONArray(settings.getJSONDescription());
                int size = jsonArray.length();
                int length = booleanArray.length;
                for (int i = 0; i < size; i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    if (!jsonObject.optString("value").contains(ApplicationConfig.RETAIN_NAME)) {
                        if (i < length) {
                            ParameterStatusItem item = new ParameterStatusItem();
                            item.setId(jsonObject.optString("id"));
                            item.setName(jsonObject.optString("value"));
                            item.setStatus(booleanArray[i]);
                            item.setCanEdit(Integer.parseInt(settings.getMode()) == ApplicationConfig.modifyType[0]);
                            itemList.add(item);
                        }
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
                        HBluetooth.getInstance(activity).kill();
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
