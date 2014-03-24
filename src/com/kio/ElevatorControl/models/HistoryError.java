package com.kio.ElevatorControl.models;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    private byte[] data;

    public HistoryError() {
        /*
        int floor = data[4];
        floor = floor << 8;
        floor = floor | data[5];
        floor %= 100;
        this.errorFloor = String.valueOf(floor);

        // 月日
        int date = data[8];
        date = date << 8;
        date = date | data[9];
        String pattern = "0000";
        java.text.DecimalFormat df = new java.text.DecimalFormat(pattern);

        // 时间
        int time = data[10];
        time = time << 8;
        time = time | data[11];

        // 格式化时间
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String dateTimeString =
                formatter.format(new Date()).substring(0, 4) + "-" //year
                        + String.valueOf(df.format(date)).substring(0, 2) + "-"//month
                        + String.valueOf(df.format(date)).substring(2) + " "//day
                        + String.valueOf(time * 0.01).split("\\.")[0]//hour
                        + String.valueOf(time * 0.01).split("\\.")[1];
        ParsePosition position = new ParsePosition(0);
        this.errorDateTime = formatter.format(formatter.parse(dateTimeString, position));
        */
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
        this.data = data;
        int date = data[8];
        date = date << 8;
        date = date | data[9];
        String pattern = "0000";
        java.text.DecimalFormat df = new java.text.DecimalFormat(pattern);

        // 时间
        int time = data[10];
        time = time << 8;
        time = time | data[11];

        // 格式化时间
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String dateTimeString =
                formatter.format(new Date()).substring(0, 4) + "-" //year
                        + String.valueOf(df.format(date)).substring(0, 2) + "-"//month
                        + String.valueOf(df.format(date)).substring(2) + " "//day
                        + String.valueOf(time * 0.01).split("\\.")[0]//hour
                        + String.valueOf(time * 0.01).split("\\.")[1];
        ParsePosition position = new ParsePosition(0);
        this.errorDateTime = formatter.format(formatter.parse(dateTimeString, position));
    }

}
