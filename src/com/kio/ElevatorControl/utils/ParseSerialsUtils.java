package com.kio.ElevatorControl.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.daos.ErrorHelpDao;
import com.kio.ElevatorControl.models.ErrorHelp;
import com.kio.ElevatorControl.models.ErrorHelpLog;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.models.RealTimeMonitor;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ParseSerialsUtils {

    @SuppressLint("DefaultLocale")
    public static String getValueTextFromRealTimeMonitor(RealTimeMonitor rm) {
        byte[] data = rm.getReceived();
        if (data.length == 8) {
            int value = data[4];
            value = value << 8;
            value = value | data[5];
            Float floatValue = value * Float.parseFloat(rm.getScale());
            String txt = String.format("%.2f", floatValue);
            return txt;
        }
        return "Error";
    }

    @SuppressLint("DefaultLocale")
    public static String getValueTextFromParameterSetting(ParameterSettings rm) {
        byte[] data = rm.getReceived();
        if (data.length == 8) {
            int value = data[4];
            value = value << 8;
            value = value | data[5];
            Float aFloat = value * Float.parseFloat(rm.getScale());
            String txt = String.format("%.2f", aFloat);
            return txt;
        }
        return "Error";
    }

    /**
     * 功能码: 前2位16进制 后两位10进制
     *
     * @return 统一为16进制
     */
    public static String getCalculatedCode(ParameterSettings rm) {
        String r2 = rm.getCode().substring(0, 2);
        String r4 = rm.getCode().substring(2);
        r4 = HSerial.int2HexStr(new int[]{
                Integer.parseInt(r4)
        });
        return r2 + r4;
    }

    /**
     * 取得十进制数
     *
     * @param data byte[]
     * @return int
     */
    public static int getIntFromBytes(byte[] data){
        if (data.length == 8) {
            int value = data[4];
            value = value << 8;
            value = value | data[5];
            return value;
        }
        return 0;
    }

    /**
     * 根据单位换算,把用户输入转换成二位16进制
     *
     * @param str String
     * @return String
     */
    public static String getHexStringFromUserInputParameterSetting(String str, ParameterSettings rm) {
        try {
            double dt = Double.parseDouble(str);
            double scale = Double.parseDouble(rm.getScale());
            Log.v("scale", rm.getScale());
            int i = (int) (dt / scale);
            return Integer.toHexString(i);
        } catch (Exception e) {
        }
        return "0000";
    }

    /**
     * 取得Error Code
     *
     * @param data byte[]
     * @return Error Code String
     */
    @SuppressLint("DefaultLocale")
    public static String getErrorCode(byte[] data) {
        if (data.length == 8) {
            int value = data[4];
            value = value << 8;
            value = value | data[5];
            // 2位整数
            return String.format("E%02d", value);
        }
        return null;
    }

    /**
     * 取得十进制数据
     *
     * @param monitor RealTimeMonitor
     * @return Int String
     */
    @SuppressLint("DefaultLocale")
    public static String getIntString(RealTimeMonitor monitor) {
        byte[] data = monitor.getReceived();
        if (data.length == 8) {
            int value = data[4];
            value = value << 8;
            value = value | data[5];
            // 2位整数
            return String.valueOf(value);
        }
        return null;
    }

    /**
     * 取得轿厢状态Code Bit 8-11
     *
     * @param monitor RealTimeMonitor
     * @return code
     */
    @SuppressLint("DefaultLocale")
    public static int getSystemStatusCode(RealTimeMonitor monitor) {
        byte[] data = monitor.getReceived();
        if (data.length == 8) {
            return data[5] & 0x0F;
        }
        return 0;
    }

    /**
     * 把received解析成ErrorHelp对象
     *
     * @param errorHelp ErrorHelp
     * @return ErrorHelpLog
     */
    @SuppressLint("DefaultLocale")
    public static ErrorHelp getErrorHelpFromErrorHelp(Context ctx, ErrorHelp errorHelp) {
        byte[] data = errorHelp.getReceived();
        if (data.length == 8) {
            int value = data[4];
            value = value << 8;
            value = value | data[5];
            // 2位整数
            String display = String.format("E%02d", value);
            return ErrorHelpDao.findByDisplay(ctx, display);
        }
        return null;
    }


    /**
     * 把received解析成ErrorHelpLog对象
     *
     * @param errorHelpLog ErrorHelpLog
     * @return ErrorHelpLog
     */
    @SuppressLint("SimpleDateFormat")
    public static ErrorHelpLog getErrorHelpLog(Context ctx, ErrorHelpLog errorHelpLog) {
        byte[] data = errorHelpLog.getReceived();

        // 信息
        int value1 = data[4];
        value1 = value1 << 8;
        value1 = value1 | data[5];
        value1 %= 100;

        // 子码
        int value2 = data[6];
        value2 = value2 << 8;
        value2 = value2 | data[7];

        // 月日
        int value3 = data[8];
        value3 = value3 << 8;
        value3 = value3 | data[9];
        String pattern = "0000";
        java.text.DecimalFormat df = new java.text.DecimalFormat(pattern);

        // 时间
        int value4 = data[10];
        value4 = value4 << 8;
        value4 = value4 | data[11];

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String strDate =
                formatter.format(new Date()).substring(0, 4) + "-" //year
                        + String.valueOf(df.format(value3)).substring(0, 2) + "-"//month
                        + String.valueOf(df.format(value3)).substring(2) + " "//day
                        + String.valueOf(value4 * 0.01).split("\\.")[0]//hour
                        + String.valueOf(value4 * 0.01).split("\\.")[1]//minute
                ;
        ParsePosition pos = new ParsePosition(0);
        errorHelpLog.setErrorTime(formatter.parse(strDate, pos));

        String eptn = "00";
        java.text.DecimalFormat eptdf = new java.text.DecimalFormat(eptn);
        errorHelpLog.setErrorHelpId(ErrorHelpDao.findByDisplay(ctx, "E" + eptdf.format(value1)).getId());

        return errorHelpLog;
    }

}
