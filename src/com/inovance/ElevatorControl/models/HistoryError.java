package com.inovance.elevatorcontrol.models;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-24.
 * Time: 15:56.
 */
public class HistoryError {

    private static final String TAG = HistoryError.class.getSimpleName();

    private String errorCode = "";

    private String errorFloor = "";

    private String errorDateTime = "";

    private boolean noError = false;

    private byte[] data;

    public HistoryError() {

    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorFloor() {
        return errorFloor;
    }

    public void setErrorFloor(String errorFloor) {
        this.errorFloor = errorFloor;
    }

    public String getErrorDateTime() {
        return errorDateTime;
    }

    public void setErrorDateTime(String errorDateTime) {
        this.errorDateTime = errorDateTime;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        if (data.length == 14) {
            int nErrorInfo = ((data[4] & 0xFF) << 8) | (data[5] & 0xFF); //故障信息
            int nErrCode = nErrorInfo % 100;                             //故障码
            if (nErrCode == 0) {
                this.noError = true;
            } else {
                this.noError = false;
                int nErrFloor = nErrorInfo / 100;                            //故障楼层
                int nDate = ((data[8] & 0xFF) << 8) | (data[9] & 0xFF);      //故障日期
                int nMonth = nDate / 100;                                    //故障月
                int nDay = nDate % 100;                                      //故障日
                int nTime = ((data[10] & 0xFF) << 8) | (data[11] & 0xFF);    //故障时间
                int nHour = nTime / 100;                                     //故障时
                int nMin = nTime % 100;                                      //故障分

                this.errorCode = String.format("E%02d", nErrCode);
                this.errorFloor = String.valueOf(nErrFloor);
                this.errorDateTime = nMonth + "月" + nDay + "日" + " " + nHour + ":" + nMin;
            }
        }
    }

    public boolean isNoError() {
        return noError;
    }

    public void setNoError(boolean noError) {
        this.noError = noError;
    }

}
