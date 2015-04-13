package com.inovance.elevatorcontrol.models;

import net.tsz.afinal.annotation.sqlite.Table;

import org.json.JSONObject;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-6-26.
 * Time: 20:11.
 */
@Table(name = "COMMUNICATION_CODE")
public class CommunicationCode {

    /**
     * 通信码
     */
    private String code;

    /**
     * 过期时间
     */
    private long expirationTime;

    public CommunicationCode(JSONObject object) {
        this.code = object.optString("Code".toUpperCase());
        this.expirationTime = object.optLong("Ldate");
    }

    public CommunicationCode() {

    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setExpirationTime(long time) {
        this.expirationTime = time;
    }

    /**
     * 取得通信码 int 值
     *
     * @return Int value
     */
    public int getCrcValue() {
        return Integer.parseInt(code.replace("0x", ""), 16);
    }

    /**
     * 是否过期
     *
     * @return True or false
     */
    public boolean isExpire() {
        long currentTime = System.currentTimeMillis() / 1000;
        return currentTime > expirationTime;
    }

}
