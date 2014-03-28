package com.kio.ElevatorControl.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.widget.Toast;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.daos.ErrorHelpDao;
import com.kio.ElevatorControl.models.ErrorHelp;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.models.RealTimeMonitor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ParseSerialsUtils {

    private static final String TAG = ParseSerialsUtils.class.getSimpleName();

    /**
     * 取得数值
     *
     * @param monitor RealTimeMonitor
     * @return Value String
     */
    @SuppressLint("GetValueTextFromRealTimeMonitor")
    public static String getValueTextFromRealTimeMonitor(RealTimeMonitor monitor) {
        byte[] data = monitor.getReceived();
        if (data.length == 8) {
            if (monitor.isShowBit()) {
                return "查看详细->";
            }
            int value = data[4];
            value = value << 8;
            value = value | data[5];
            if (monitor.getUnit() == null || monitor.getUnit().length() <= 0) {
                if (monitor.getCode().equalsIgnoreCase("8000")) {
                    return String.format("E%02d", value);
                }
                return String.format("%d", value);
            }
            Float floatValue = value * Float.parseFloat(monitor.getScale());
            return String.format("%.2f", floatValue);
        }
        return "Error";
    }

    /**
     * 根据 DESCRIPTION_TYPE 取得相应的值
     *
     * @param settings ParameterSettings
     * @return Value String
     */
    @SuppressLint("DefaultLocale")
    public static String getValueTextFromParameterSetting(ParameterSettings settings) {
        byte[] data = settings.getReceived();
        if (data.length == 8) {
            int value = data[4];
            value = value << 8;
            value = value | data[5];
            Float aFloat = value * Float.parseFloat(settings.getScale());
            if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[0]) {
                return String.format("%.2f", aFloat);
            }
            if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[1]) {
                try {
                    JSONArray jsonArray = new JSONArray(settings.getJSONDescription());
                    int size = jsonArray.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        if (i == value) {
                            return jsonObject.optString("value");
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[2]) {
                return "查看详细->";
            }
        }
        return "";
    }

    /**
     * 验证用户输入值是否在范围以内
     *
     * @param activity  Toast 显示 Activity
     * @param settings  ParameterSettings
     * @param userValue User Input String
     * @return 验证是否通过
     */
    @SuppressLint("ValidateUserInputValue")
    public static boolean validateUserInputValue(Activity activity, ParameterSettings settings, String userValue) {
        Float settingValue = Float.parseFloat(userValue);
        String[] scopeArray = settings.getScope().split("-");
        if (Math.max(settingValue, Float.parseFloat(scopeArray[0])) == settingValue &&
                Math.min(settingValue, Float.parseFloat(scopeArray[1])) == settingValue) {
            return true;
        } else {
            Toast.makeText(activity,
                    activity.getResources().getString(R.string.not_validated_input_value_text),
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 功能码: 前2位16进制 后两位10进制
     *
     * @return 统一为16进制
     */
    public static String getCalculatedCode(ParameterSettings settings) {
        String r2 = settings.getCode().substring(0, 2);
        String r4 = settings.getCode().substring(2);
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
    @SuppressLint("GetIntFromBytes")
    public static int getIntFromBytes(byte[] data) {
        if (data.length == 8) {
            int value = data[4];
            value = value << 8;
            value = value | data[5];
            return value;
        }
        return 0;
    }

    /**
     * 返回指定Bit区间的int值
     *
     * @param data    byte[]
     * @param section int[]
     * @return int Value
     */
    @SuppressLint("GetIntValueFromBytesInSection")
    public static int getIntValueFromBytesInSection(byte[] data, int[] section) {
        String binaryString = "";
        for (byte subByte : data) {
            binaryString += String.format("%8s", Integer.toBinaryString(subByte & 0xFF)).replace(' ', '0');
        }
        if (section.length == 1) {
            String bitsString = Character.toString(binaryString.charAt(section[0]));
            return Integer.parseInt(bitsString, 2);
        }
        if (section.length == 2) {
            String bitsString = binaryString.substring(section[0] - 1, section[1] + 1);
            return Integer.parseInt(bitsString, 2);
        }
        return -1;
    }

    /**
     * 根据单位换算,把用户输入转换成二位16进制
     *
     * @param string String
     * @return String
     */
    @SuppressLint("GetHexStringFromUserInputParameterSetting")
    public static String getHexStringFromUserInputParameterSetting(String string, ParameterSettings settings) {
        try {
            double dt = Double.parseDouble(string);
            double scale = Double.parseDouble(settings.getScale());
            int i = (int) (dt / scale);
            return Integer.toHexString(i);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0000";
    }

    /**
     * 取得Error Code
     *
     * @param data byte[]
     * @return Error Code String
     */
    @SuppressLint("GetErrorCode")
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
    @SuppressLint("GetIntString")
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
     * 取得电梯状态Code Bit 4-7
     *
     * @param monitor RealTimeMonitor
     * @return System Status Code
     */
    @SuppressLint("GetElevatorStatusCode")
    public static int getElevatorStatusCode(RealTimeMonitor monitor) {
        byte[] data = monitor.getReceived();
        if (data.length == 8) {
            String bitString = ""
                    + (byte) ((data[4] >> 3) & 0x1) + (byte) ((data[4] >> 2) & 0x1)
                    + (byte) ((data[4] >> 1) & 0x1) + (byte) ((data[4]) & 0x1);
            return Integer.parseInt(bitString, 2);
        }
        return -1;
    }

    /**
     * 取得轿厢状态Code Bit 8-11
     *
     * @param monitor RealTimeMonitor
     * @return System Status Code
     */
    @SuppressLint("GetElevatorBoxStatusCode")
    public static int getElevatorBoxStatusCode(RealTimeMonitor monitor) {
        byte[] data = monitor.getReceived();
        if (data.length == 8) {
            String bitString = ""
                    + (byte) ((data[5] >> 7) & 0x1) + (byte) ((data[5] >> 6) & 0x1)
                    + (byte) ((data[5] >> 5) & 0x1) + (byte) ((data[5] >> 4) & 0x1);
            return Integer.parseInt(bitString, 2);
        }
        return -1;
        //String binaryString = String.format("%8s", Integer.toBinaryString(data[5] & 0xFF)).replace(' ', '0');
    }

    /**
     * 把received解析成ErrorHelp对象
     *
     * @param errorHelp ErrorHelp
     * @return ErrorHelpLog
     */
    @SuppressLint("GetErrorHelpFromErrorHelp")
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

    public static String splitAndConvertToHex(String code) {
        if (code.length() == 4) {
            String prefix = code.substring(0, 2);
            String suffix = code.substring(2, 4);
            return prefix + Integer.toHexString(Integer.parseInt(suffix));
        }
        return code;
    }
}
