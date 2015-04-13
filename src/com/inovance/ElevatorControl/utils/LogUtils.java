package com.inovance.elevatorcontrol.utils;

import android.content.Context;

import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.config.ParameterUpdateTool;
import com.inovance.elevatorcontrol.models.SystemLog;

import net.tsz.afinal.FinalDb;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * 日志记录工具
 * User: keith.
 * Date: 14-5-30.
 * Time: 10:24.
 */
public class LogUtils {

    private static final boolean DEBUG = false;

    private static LogUtils instance = new LogUtils();

    private Context context;

    private String[] logNameArray;

    private String[] logResultArray;

    private String[] logDirectionArray;

    public String LogDeviceLabel;

    public String LogNameLabel;

    public String LogStartValueLabel;

    public String LogFinalValueLabel;

    public String LogSendLabel;

    public String LogResultLabel;

    public String LogFloorLabel;

    public String LogDirectionLabel;

    public String LogErrorMessageLabel;

    public String LogTimeLabel;

    public static LogUtils getInstance() {
        return instance;
    }

    private LogUtils() {

    }

    public void init(Context context) {
        if (instance.context == null) {
            instance.context = context;
            logNameArray = context.getResources().getStringArray(R.array.log_name_array);
            logResultArray = context.getResources().getStringArray(R.array.log_result_array);
            logDirectionArray = context.getResources().getStringArray(R.array.log_direction_array);
            LogDeviceLabel = context.getResources().getString(R.string.log_device);
            LogNameLabel = context.getResources().getString(R.string.log_name);
            LogStartValueLabel = context.getResources().getString(R.string.log_start_value);
            LogFinalValueLabel = context.getResources().getString(R.string.log_final_value);
            LogSendLabel = context.getResources().getString(R.string.log_send);
            LogResultLabel = context.getResources().getString(R.string.log_result);
            LogFloorLabel = context.getResources().getString(R.string.log_floor);
            LogDirectionLabel = context.getResources().getString(R.string.log_direction);
            LogErrorMessageLabel = context.getResources().getString(R.string.log_error_message);
            LogTimeLabel = context.getResources().getString(R.string.log_time);
        }
    }

    public String getLogTypeName(int type) {
        if (type - 1 < logNameArray.length - 1) {
            return logNameArray[type - 1];
        }
        return "";
    }

    public String parseCommand(String command) {
        if (command != null) {
            if (command.length() >= 8) {
                command = command.substring(4, 8);
            }
            if (command.length() == 4) {
                String parse = "";
                int index = 0;
                for (char item : command.toCharArray()) {
                    if (index % 2 != 0) {
                        parse += item + " ";
                    } else {
                        parse += item;
                    }
                    index++;
                }
                return parse.trim();
            }
        }
        return "00 00";
    }

    /**
     * 取得外召方向文字标签
     *
     * @param direction direction
     * @return String
     */
    public String getDirectionString(int direction) {
        switch (direction) {
            case 1:
                return logDirectionArray[0];
            case 2:
                return logDirectionArray[1];
        }
        return "";
    }

    public String getResultString(boolean successful) {
        return successful ? logResultArray[0] : logResultArray[1];
    }

    public String getResultString(String result) {
        if (result != null) {
            if (result.contains("8001")) {
                String temp = result.replace("0106", "").replace("0103", "").replace("8001", "");
                return logResultArray[2] + parseCommand(temp.substring(0, 4));
            }
            return logResultArray[0];
        }
        return "";
    }

    /**
     * 单个参数写入
     *
     * @param type       日志类型
     * @param send       发送的指令
     * @param result     返回的指令
     * @param startValue 起始值
     * @param finalValue 写入后的值
     */
    public void write(int type, String send, String result, String startValue, String finalValue) {
        SystemLog systemLog = new SystemLog();
        systemLog.setType(type);
        systemLog.setDeviceType(ParameterUpdateTool.getInstance().getDeviceName());
        systemLog.setSendCommand(send);
        systemLog.setReturnCommand(result);
        systemLog.setStartValue(startValue);
        systemLog.setFinalValue(finalValue);
        systemLog.setTimestamp(System.currentTimeMillis());
        save(systemLog);
    }

    /**
     * 故障复位 / 恢复出厂设置
     *
     * @param type   日志类型
     * @param send   发送的指令
     * @param result 返回的指令
     */
    public void write(int type, String send, String result) {
        SystemLog systemLog = new SystemLog();
        systemLog.setType(type);
        systemLog.setDeviceType(ParameterUpdateTool.getInstance().getDeviceName());
        systemLog.setSendCommand(send);
        systemLog.setReturnCommand(result);
        systemLog.setTimestamp(System.currentTimeMillis());
        save(systemLog);
    }

    /**
     * 烧录日志
     *
     * @param type         日志类型
     * @param result       烧录结果
     * @param errorMessage 烧录出错信息
     */
    public void write(int type, boolean result, String errorMessage) {
        SystemLog systemLog = new SystemLog();
        systemLog.setType(type);
        systemLog.setDeviceType(ParameterUpdateTool.getInstance().getDeviceName());
        systemLog.setBurnStatus(result ? SystemLog.BurnSuccessful : SystemLog.BurnFailed);
        systemLog.setBurnErrorMessage(errorMessage);
        systemLog.setTimestamp(System.currentTimeMillis());
        save(systemLog);
    }

    /**
     * 电梯内召
     *
     * @param type   日志类型
     * @param send   发送的指令
     * @param result 返回的指令
     * @param floor  召唤的楼层
     */
    public void write(int type, String send, String result, int floor) {
        SystemLog systemLog = new SystemLog();
        systemLog.setType(type);
        systemLog.setDeviceType(ParameterUpdateTool.getInstance().getDeviceName());
        systemLog.setSendCommand(send);
        systemLog.setReturnCommand(result);
        systemLog.setMoveSideFloor(floor);
        systemLog.setTimestamp(System.currentTimeMillis());
        save(systemLog);
    }

    /**
     * 电梯外召
     *
     * @param type      日志类型
     * @param send      发送的指令
     * @param result    返回的指令
     * @param floor     召唤的楼层
     * @param direction 召唤方式
     */
    public void write(int type, String send, String result, int floor, int direction) {
        SystemLog systemLog = new SystemLog();
        systemLog.setType(type);
        systemLog.setDeviceType(ParameterUpdateTool.getInstance().getDeviceName());
        systemLog.setSendCommand(send);
        systemLog.setReturnCommand(result);
        systemLog.setMoveSideFloor(floor);
        systemLog.setDirection(direction);
        systemLog.setTimestamp(System.currentTimeMillis());
        save(systemLog);
    }

    /**
     * 参数批量读取 / 写入
     *
     * @param type 日志类型
     */
    public void write(int type) {
        SystemLog systemLog = new SystemLog();
        systemLog.setType(type);
        systemLog.setDeviceType(ParameterUpdateTool.getInstance().getDeviceName());
        systemLog.setTimestamp(System.currentTimeMillis());
        save(systemLog);
    }

    public List<SystemLog> readLogs() {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        return db.findAll(SystemLog.class);
    }

    public void save(SystemLog systemLog) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.saveBindId(systemLog);
    }

    public void delete(SystemLog systemLog) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.delete(systemLog);
    }

    public void deleteAll() {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.deleteAll(SystemLog.class);
    }

    public void update(SystemLog systemLog) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.update(systemLog);
    }
}
