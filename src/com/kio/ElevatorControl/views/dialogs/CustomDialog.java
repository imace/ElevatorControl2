package com.kio.ElevatorControl.views.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.models.ErrorHelp;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;
import org.holoeverywhere.widget.Switch;

public class CustomDialog {

    public static AlertDialog.Builder switchersDialog(Activity activity, byte b1, byte b2) {
        boolean[] vars = HSerial.byte2BoolArr(b1, b2);
        return switchersDialog(activity, vars);
    }

    // ===================================== Switch Dialog ========================================
    public static AlertDialog.Builder switchersDialog(Activity activity, boolean[] vars) {
        if (vars.length != 16) {
            return new AlertDialog.Builder(activity)
                    .setTitle(R.string.data_error_text)
                    .setNeutralButton(R.string.dialog_btn_ok, null);
        }

        View layout = activity.getLayoutInflater().inflate(R.layout.custom_dialog_switchers,
                (ViewGroup) activity.findViewById(R.id.custom_dialog_switchers));
        ((Switch) layout.findViewById(R.id.X01)).setChecked(vars[0]);
        ((Switch) layout.findViewById(R.id.X02)).setChecked(vars[1]);
        ((Switch) layout.findViewById(R.id.X03)).setChecked(vars[2]);
        ((Switch) layout.findViewById(R.id.X04)).setChecked(vars[3]);
        ((Switch) layout.findViewById(R.id.X05)).setChecked(vars[4]);
        ((Switch) layout.findViewById(R.id.X06)).setChecked(vars[5]);
        ((Switch) layout.findViewById(R.id.X07)).setChecked(vars[6]);
        ((Switch) layout.findViewById(R.id.X08)).setChecked(vars[7]);
        ((Switch) layout.findViewById(R.id.X09)).setChecked(vars[8]);
        ((Switch) layout.findViewById(R.id.X10)).setChecked(vars[9]);
        ((Switch) layout.findViewById(R.id.X11)).setChecked(vars[10]);
        ((Switch) layout.findViewById(R.id.X12)).setChecked(vars[11]);
        ((Switch) layout.findViewById(R.id.X13)).setChecked(vars[12]);
        ((Switch) layout.findViewById(R.id.X14)).setChecked(vars[13]);
        ((Switch) layout.findViewById(R.id.X15)).setChecked(vars[14]);
        ((Switch) layout.findViewById(R.id.X16)).setChecked(vars[15]);

        // ======================= Disable switch =============================//
        layout.findViewById(R.id.X01).setEnabled(false);
        layout.findViewById(R.id.X02).setEnabled(false);
        layout.findViewById(R.id.X03).setEnabled(false);
        layout.findViewById(R.id.X04).setEnabled(false);
        layout.findViewById(R.id.X05).setEnabled(false);
        layout.findViewById(R.id.X06).setEnabled(false);
        layout.findViewById(R.id.X07).setEnabled(false);
        layout.findViewById(R.id.X08).setEnabled(false);
        layout.findViewById(R.id.X09).setEnabled(false);
        layout.findViewById(R.id.X10).setEnabled(false);
        layout.findViewById(R.id.X11).setEnabled(false);
        layout.findViewById(R.id.X12).setEnabled(false);
        layout.findViewById(R.id.X13).setEnabled(false);
        layout.findViewById(R.id.X14).setEnabled(false);
        layout.findViewById(R.id.X15).setEnabled(false);
        layout.findViewById(R.id.X16).setEnabled(false);
        return new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                .setView(layout)
                .setNeutralButton(R.string.dialog_btn_ok, null);
    }

    // ===================================== EditText Dialog ========================================
    public static AlertDialog.Builder parameterSettingDialog(final Activity activity,
                                                             final ParameterSettings parameterSettings) {
        final View layout = activity.getLayoutInflater().inflate(R.layout.custom_dialog_parameterform,
                (ViewGroup) activity.findViewById(R.id.custom_dialog_parameter_form));
        ((EditText) layout.findViewById(R.id.parameter_setting_value)).
                setText(ParseSerialsUtils.getValueTextFromParameterSetting(parameterSettings));
        ((TextView) layout.findViewById(R.id.parameter_setting_code)).setText(parameterSettings.getCode());
        ((TextView) layout.findViewById(R.id.parameter_setting_name)).setText(parameterSettings.getName());
        ((TextView) layout.findViewById(R.id.parameter_setting_type)).setText(parameterSettings.getType());
        ((TextView) layout.findViewById(R.id.parameter_setting_unit)).
                setText("(" + ((parameterSettings.getUnit() == null ||
                        parameterSettings.getUnit().length() <= 0) ? "-" : parameterSettings.getUnit()) + ")");
        ((TextView) layout.findViewById(R.id.parameter_setting_id)).setText(parameterSettings.getProductId());
        return new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                .setView(layout)
                .setNeutralButton(R.string.dialog_btn_cancel, null);
    }

    // ===================================== Exit dialog ========================================
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

    // ======================================== Failure History Dialog ================================
    public static AlertDialog.Builder failureHistoryDialog(final ErrorHelp errorHelp, final Activity activity) {
        final View layout = activity.getLayoutInflater().inflate(R.layout.trouble_analyze_tab_current, null);
        assert layout != null;
        ((TextView) layout.findViewById(R.id.current_error_help_display)).setText(errorHelp.getDisplay());
        ((TextView) layout.findViewById(R.id.current_error_help_level)).setText(errorHelp.getLevel());
        ((TextView) layout.findViewById(R.id.current_error_help_name)).setText(errorHelp.getName());
        ((TextView) layout.findViewById(R.id.current_error_help_reason)).setText(errorHelp.getReason());
        ((TextView) layout.findViewById(R.id.current_error_help_solution)).setText(errorHelp.getSolution());
        return new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                .setView(layout)
                .setNeutralButton(R.string.dialog_btn_ok, null);
    }

}
