package com.kio.ElevatorControl.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.daos.ErrorHelpDao;
import com.kio.ElevatorControl.models.ErrorHelp;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.models.RealTimeMonitor;
import org.holoeverywhere.widget.Toast;
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
            int value = getIntFromBytes(data);
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
            int value = getIntFromBytes(data);
            if (settings.getDescriptiontype() == ApplicationConfig.DESCRIPTION_TYPE[0]) {
                try {
                    return "" + value * Integer.parseInt(settings.getScale());
                } catch (Exception e) {
                    double doubleValue = (double) value * Double.parseDouble(settings.getScale());
                    return String.format("%." + (settings.getScale().length() - 2) + "f", doubleValue);
                }
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
        String[] scopeArray = settings.getScope().split("-");
        double[] array = new double[2];
        array[0] = Double.parseDouble(scopeArray[0]) / Double.parseDouble(settings.getScale());
        array[1] = Double.parseDouble(scopeArray[1]) / Double.parseDouble(settings.getScale());
        Double newValue = Double.parseDouble(userValue);
        if (Math.max(newValue, array[0]) == newValue && Math.min(newValue, array[1]) == newValue) {
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
     * @return short
     */
    @SuppressLint("GetIntFromBytes")
    public static int getIntFromBytes(byte[] data) {
        if (data.length == 8) {
            return data[4] << 8 & 0xFF00 | data[5] & 0xFF;
        }
        return -1;
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
     * 取得Error Code
     *
     * @param data byte[]
     * @return Error Code String
     */
    @SuppressLint("GetErrorCode")
    public static String getErrorCode(byte[] data) {
        if (data.length == 8) {
            int value = getIntFromBytes(data);
            return String.format("E%02d", value);
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
            int value = getIntFromBytes(data);
            String display = String.format("E%02d", value);
            return ErrorHelpDao.findByDisplay(ctx, display);
        }
        return null;
    }

    /**
     * 将后两位字符int值转换为16进制字符串
     *
     * @param code Code String
     * @return String
     */
    @SuppressLint("SplitAndConvertToHex")
    public static String splitAndConvertToHex(String code) {
        if (code.length() == 4) {
            String prefix = code.substring(0, 2);
            String suffix = code.substring(2, 4);
            return prefix + Integer.toHexString(Integer.parseInt(suffix));
        }
        return code;
    }

    /**
     * 判读字符是否是数字
     *
     * @param string Char String
     * @return Is Integer
     */
    @SuppressLint("IsInteger")
    public static boolean isInteger(String string) {
        try {
            Integer.parseInt(string);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}
