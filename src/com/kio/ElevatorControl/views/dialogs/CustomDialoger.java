package com.kio.ElevatorControl.views.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.models.ErrorHelp;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;

public class CustomDialoger {

    public static AlertDialog.Builder switchersDialog(Activity act, byte b1, byte b2) {
        boolean[] vars = HSerial.byte2BoolArr(b1, b2);
        return switchersDialog(act, vars);
    }

    public static AlertDialog.Builder switchersDialog(Activity act, boolean[] vars) {
        if (vars.length != 16) {
            return new AlertDialog.Builder(act).setTitle("Error").setNeutralButton("确定", null);
        }

        View layout = act.getLayoutInflater().inflate(R.layout.custom_dialog_switchers, (ViewGroup) act.findViewById(R.id.custom_dialog_switchers));
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

        //
        ((Switch) layout.findViewById(R.id.X01)).setEnabled(false);
        ((Switch) layout.findViewById(R.id.X02)).setEnabled(false);
        ((Switch) layout.findViewById(R.id.X03)).setEnabled(false);
        ((Switch) layout.findViewById(R.id.X04)).setEnabled(false);
        ((Switch) layout.findViewById(R.id.X05)).setEnabled(false);
        ((Switch) layout.findViewById(R.id.X06)).setEnabled(false);
        ((Switch) layout.findViewById(R.id.X07)).setEnabled(false);
        ((Switch) layout.findViewById(R.id.X08)).setEnabled(false);
        ((Switch) layout.findViewById(R.id.X09)).setEnabled(false);
        ((Switch) layout.findViewById(R.id.X10)).setEnabled(false);
        ((Switch) layout.findViewById(R.id.X11)).setEnabled(false);
        ((Switch) layout.findViewById(R.id.X12)).setEnabled(false);
        ((Switch) layout.findViewById(R.id.X13)).setEnabled(false);
        ((Switch) layout.findViewById(R.id.X14)).setEnabled(false);
        ((Switch) layout.findViewById(R.id.X15)).setEnabled(false);
        ((Switch) layout.findViewById(R.id.X16)).setEnabled(false);

        return new AlertDialog.Builder(act, R.style.SwitcherDailog).setView(layout).setNeutralButton("确定", null);
    }

    public static AlertDialog.Builder parameterSettingDialog(final Activity act, final ParameterSettings p) {
        final View layout = act.getLayoutInflater().inflate(R.layout.custom_dialog_parameterform, (ViewGroup) act.findViewById(R.id.custom_dialog_parameterform));
        ((EditText) layout.findViewById(R.id.parametersettingvalue)).setText(ParseSerialsUtils.getValueTextFromParameterSetting(p));
        ((TextView) layout.findViewById(R.id.parametersettingcode)).setText(p.getCode());
        ((TextView) layout.findViewById(R.id.parametersettingname)).setText(p.getName());
        ((TextView) layout.findViewById(R.id.parametersettingtype)).setText(p.getType());
        ((TextView) layout.findViewById(R.id.parametersettingunit)).setText("(" + ((p.getUnit() == null || p.getUnit().length() <= 0) ? "-" : p.getUnit()) + ")");
        ((TextView) layout.findViewById(R.id.parametersettingid)).setText(p.getProductId());

        return new AlertDialog.Builder(act, R.style.AppBaseTheme).setView(layout)
                .setNeutralButton(act.getResources().getString(R.string.dialog_btn_ok), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 读取作用的cummunication
                        HCommunication[] cumms = new HCommunication[1];
                        cumms[0] = new HCommunication() {

                            @Override
                            public void beforeSend() {
                                String inputdata = ((EditText) layout.findViewById(R.id.parametersettingvalue)).getText().toString();
                                String parsedata = ParseSerialsUtils.getHexStringFromUserInputParameterSetting(inputdata, p);
                                setSendbuffer(HSerial.crc16(HSerial.hexStr2Ints("0206" + p.getCode() + parsedata)));
                                Log.v("parameterSettingDialog write : ", "0206" + p.getCode() + parsedata);
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
                                if (HSerial.isCRC16Valid(getReceivebuffer())) {
                                    // 通过验证
                                    byte[] received = HSerial.trimEnd(getReceivebuffer());
                                    Log.v("parameterSettingDialog receive", HSerial.byte2HexStr(received));
                                    return null;
                                }
                                return null;
                            }
                        };

                        HBluetooth.getInstance(act).setCommunications(cumms).HStart();

                    }
                }).setNegativeButton(act.getResources().getString(R.string.dialog_btn_cancel), null).setCancelable(false);
    }

    public static AlertDialog.Builder exitDialog(final Activity act) {
        return new AlertDialog.Builder(act).setMessage(act.getResources().getString(R.string.are_you_sure_exit))
                .setTitle(act.getResources().getString(R.string.are_you_sure_message))
                .setNegativeButton(act.getResources().getString(R.string.dialog_btn_cancel), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        arg0.cancel();
                    }
                }).setPositiveButton(act.getResources().getString(R.string.dialog_btn_ok), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        HBluetooth.getInstance(act).kill();
                        act.finish();
                        System.exit(0);
                    }
                });
    }

    public static AlertDialog.Builder failureHistoryDailog(final ErrorHelp errorhlep, final Activity act) {
        final View layout = act.getLayoutInflater().inflate(R.layout.tab_failure_current, null);
        ((TextView) layout.findViewById(R.id.currenterrorhelpdisplay)).setText(errorhlep.getDisplay());
        ((TextView) layout.findViewById(R.id.currenterrorhelplevel)).setText(errorhlep.getLevel());
        ((TextView) layout.findViewById(R.id.currenterrorhelpname)).setText(errorhlep.getName());
        ((TextView) layout.findViewById(R.id.currenterrorhelpreason)).setText(errorhlep.getReason());
        ((TextView) layout.findViewById(R.id.currenterrorhelpsolution)).setText(errorhlep.getSolution());

        return new AlertDialog.Builder(act, R.style.AppBaseTheme).setView(layout).setNeutralButton("确定", null);

    }

}
