package com.inovance.elevatorcontrol.models;

import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.utils.LogUtils;
import com.mobsandgeeks.adapters.InstantText;

import net.tsz.afinal.annotation.sqlite.Id;
import net.tsz.afinal.annotation.sqlite.Table;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-5-30.
 * Time: 10:19.
 */
@Table(name = "SYSTEM_LOG")
public class SystemLog {

    private static final String TAG = SystemLog.class.getSimpleName();

    @Id
    private int Id;

    /**
     * 日志类型
     * 调试参数日志
     * 1、单个参数修改（设备型号；命令名称：修改F0-00，0，1）；发命令；返回命令；日期时间）
     * 2、故障复位（设备型号；故障复位；发命令；返回命令；日期时间）
     * 3、烧录程序（设备型号；烧录程序；成功/失败；失败信息字符串；日期时间）
     * 4、电梯内召（设备型号；电梯内召；召唤楼层；发命令；返回命令；日期时间）
     * 5、电梯外召（设备型号；电梯外召；召唤楼层；发命令；返回命令；日期时间）
     * 6、参数批量读取（设备型号；命令名称：批量读取；日期时间）
     * 7、参数批量写入（设备型号；命令名称：批量读取；日期时间）
     * 8、恢复出厂设置（设备型号；恢复出厂设置；发命令；返回命令；日期时间）
     */
    private int type;

    /**
     * 设备型号
     */
    private String deviceType;

    /**
     * 发送的指令
     */
    private String sendCommand;

    /**
     * 修改之前的值
     */
    private String startValue;

    /**
     * 修改之后的值
     */
    private String finalValue;

    /**
     * 返回的指令
     */
    private String returnCommand;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 1 for successful
     * 2 for failed
     */
    private int burnStatus;

    public void setBurnStatus(int burnStatus) {
        this.burnStatus = burnStatus;
    }

    /**
     * 烧录出错信息
     */
    private String burnErrorMessage;

    /**
     * 召唤楼层
     */
    private int moveSideFloor;

    /**
     * 召唤方式
     * 1. 上召
     * 2. 下召
     */
    private int direction;

    public static final int BurnSuccessful = 1;

    public static final int BurnFailed = 2;

    public int getId() {
        return Id;
    }

    public void setId(int Id) {
        this.Id = Id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getSendCommand() {
        return sendCommand;
    }

    public void setSendCommand(String sendCommand) {
        this.sendCommand = sendCommand;
    }

    public String getStartValue() {
        return startValue;
    }

    public void setStartValue(String startValue) {
        this.startValue = startValue;
    }

    public String getFinalValue() {
        return finalValue;
    }

    public void setFinalValue(String finalValue) {
        this.finalValue = finalValue;
    }

    public String getReturnCommand() {
        return returnCommand;
    }

    public void setReturnCommand(String returnCommand) {
        this.returnCommand = returnCommand;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getBurnErrorMessage() {
        return burnErrorMessage;
    }

    public void setBurnErrorMessage(String burnErrorMessage) {
        this.burnErrorMessage = burnErrorMessage;
    }

    public int getMoveSideFloor() {
        return moveSideFloor;
    }

    public void setMoveSideFloor(int moveSideFloor) {
        this.moveSideFloor = moveSideFloor;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    @InstantText(viewId = R.id.log_content)
    public String getLogContent() {
        String logName = LogUtils.getInstance().getLogTypeName(type);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeString = dateFormat.format(new Date(getTimestamp()));
        String send = LogUtils.getInstance().parseCommand(sendCommand);
        String receive = LogUtils.getInstance().getResultString(returnCommand);
        String content = "";
        switch (type) {
            // 单个参数写入
            case ApplicationConfig.LogWriteParameter:
                String parameterName = send.replace("D2", "FR");
                parameterName = parameterName.replace(" ", "-");
                content = LogUtils.getInstance().LogDeviceLabel + deviceType + "\n"
                        + LogUtils.getInstance().LogNameLabel + logName + " " + parameterName + "\n"
                        + LogUtils.getInstance().LogStartValueLabel + startValue + "\n"
                        + LogUtils.getInstance().LogFinalValueLabel + finalValue + "\n"
                        + LogUtils.getInstance().LogSendLabel + send + "\n"
                        + LogUtils.getInstance().LogResultLabel + receive + "\n"
                        + LogUtils.getInstance().LogTimeLabel + timeString;
                break;
            // 恢复故障状态
            case ApplicationConfig.LogRestoreErrorStatus:
                content = LogUtils.getInstance().LogDeviceLabel + deviceType + "\n"
                        + LogUtils.getInstance().LogNameLabel + logName + "\n"
                        + LogUtils.getInstance().LogSendLabel + send + "\n"
                        + LogUtils.getInstance().LogResultLabel + receive + "\n"
                        + LogUtils.getInstance().LogTimeLabel + timeString;
                break;
            // 烧录
            case ApplicationConfig.LogBurn:
                boolean burnSuccessful = burnStatus == BurnSuccessful;
                if (burnSuccessful) {
                    content = LogUtils.getInstance().LogDeviceLabel + deviceType + "\n"
                            + LogUtils.getInstance().LogNameLabel + logName + "\n"
                            + LogUtils.getInstance().LogResultLabel
                            + LogUtils.getInstance().getResultString(burnSuccessful) + "\n"
                            + LogUtils.getInstance().LogTimeLabel + timeString;
                } else {
                    content = LogUtils.getInstance().LogDeviceLabel + deviceType + "\n"
                            + LogUtils.getInstance().LogNameLabel + logName + "\n"
                            + LogUtils.getInstance().LogResultLabel
                            + LogUtils.getInstance().getResultString(burnSuccessful) + "\n"
                            + LogUtils.getInstance().LogErrorMessageLabel + getBurnErrorMessage() + "\n"
                            + LogUtils.getInstance().LogTimeLabel + timeString;
                }
                break;
            // 内召
            case ApplicationConfig.LogMoveInside:
                content = LogUtils.getInstance().LogDeviceLabel + deviceType + "\n"
                        + LogUtils.getInstance().LogNameLabel + logName + "\n"
                        + LogUtils.getInstance().LogFloorLabel + moveSideFloor + "\n"
                        + LogUtils.getInstance().LogSendLabel + send + "\n"
                        + LogUtils.getInstance().LogResultLabel + receive + "\n"
                        + LogUtils.getInstance().LogTimeLabel + timeString;
                break;
            // 外召
            case ApplicationConfig.LogMoveOutside:
                content = LogUtils.getInstance().LogDeviceLabel + deviceType + "\n"
                        + LogUtils.getInstance().LogNameLabel + logName + "\n"
                        + LogUtils.getInstance().LogFloorLabel + moveSideFloor + "\n"
                        + LogUtils.getInstance().LogDirectionLabel
                        + LogUtils.getInstance().getDirectionString(direction) + "\n"
                        + LogUtils.getInstance().LogSendLabel + send + "\n"
                        + LogUtils.getInstance().LogResultLabel + receive + "\n"
                        + LogUtils.getInstance().LogTimeLabel + timeString;
                break;
            // 批量下载参数
            case ApplicationConfig.LogDownloadProfile:
                content = LogUtils.getInstance().LogDeviceLabel + deviceType + "\n"
                        + LogUtils.getInstance().LogNameLabel + logName + "\n"
                        + LogUtils.getInstance().LogTimeLabel + timeString;
                break;
            // 批量写入参数
            case ApplicationConfig.LogUploadProfile:
                content = LogUtils.getInstance().LogDeviceLabel + deviceType + "\n"
                        + LogUtils.getInstance().LogNameLabel + logName + "\n"
                        + LogUtils.getInstance().LogTimeLabel + timeString;
                break;
            // 恢复出厂设置
            case ApplicationConfig.LogRestoreFactory:
                content = LogUtils.getInstance().LogDeviceLabel + deviceType + "\n"
                        + LogUtils.getInstance().LogNameLabel + logName + "\n"
                        + LogUtils.getInstance().LogSendLabel + send + "\n"
                        + LogUtils.getInstance().LogResultLabel + receive + "\n"
                        + LogUtils.getInstance().LogTimeLabel + timeString;
                break;
        }
        return content;
    }
}
