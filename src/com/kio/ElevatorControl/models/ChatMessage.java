package com.kio.ElevatorControl.models;

import net.tsz.afinal.annotation.sqlite.Id;

/**
 * Created by keith on 14-4-7.
 * User keith
 * Date 14-4-7
 * Time 下午5:29
 */
public class ChatMessage {

    @Id
    private int Id;

    private String message;

    private boolean isSend;

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public boolean isSend() {
        return isSend;
    }

    public void setSend(boolean isSend) {
        this.isSend = isSend;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
