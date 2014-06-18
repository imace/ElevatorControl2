package com.inovance.ElevatorControl.models;

import net.tsz.afinal.annotation.sqlite.Id;
import org.json.JSONObject;

/**
 * Created by keith on 14-4-7.
 * User keith
 * Date 14-4-7
 * Time 下午5:29
 */
public class ChatMessage {

    /**
     * 文本
     */
    public static final int TYPE_TEXT = 0;

    /**
     * 参数
     */
    public static final int TYPE_PROFILE = 1;

    /**
     * 图片
     */
    public static final int TYPE_PICTURE = 2;

    /**
     * 视频
     */
    public static final int TYPE_VIDEO = 3;

    /**
     * 音频
     */
    public static final int TYPE_AUDIO = 4;

    /**
     * 发送的消息
     */
    public static final int SEND = 1;

    /**
     * 接收的消息
     */
    public static final int RECEIVE = 2;

    @Id
    private int ID;

    /**
     * 标题
     */
    private String title;

    /**
     * 发送方手机号码
     */
    private String fromNumber;

    /**
     * 接收方手机号码
     */
    private String toNumber;

    /**
     * 发送、接收时间
     */
    private String timeString;

    /**
     * 发送 / 接收
     */
    private int chatType;

    /**
     * 内容类型
     */
    private int contentType;

    /**
     * 附件的 URL 地址
     */
    private String fileUrl;

    public ChatMessage() {

    }

    public ChatMessage(JSONObject object) {
        this.ID = object.optInt("ID");
        this.title = object.optString("Title");
        this.fromNumber = object.optString("FromNum");
        this.toNumber = object.optString("ToNum");
        this.timeString = object.optString("CreateDate");
        int type = object.optInt("FileType");
        switch (type) {
            case 0:
                this.contentType = TYPE_TEXT;
                break;
            case 1:
                this.contentType = TYPE_PROFILE;
                break;
            case 2:
                this.contentType = TYPE_PICTURE;
                break;
            case 3:
                this.contentType = TYPE_VIDEO;
                break;
            case 4:
                this.contentType = TYPE_AUDIO;
                break;
        }
    }

    public int getId() {
        return ID;
    }

    public void setId(int id) {
        ID = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTimeString() {
        return timeString;
    }

    public void setTimeString(String timeString) {
        this.timeString = timeString;
    }

    public String getFromNumber() {
        return fromNumber;
    }

    public void setFromNumber(String fromNumber) {
        this.fromNumber = fromNumber;
    }

    public String getToNumber() {
        return toNumber;
    }

    public void setToNumber(String toNumber) {
        this.toNumber = toNumber;
    }

    public int getChatType() {
        return chatType;
    }

    public void setChatType(int chatType) {
        this.chatType = chatType;
    }

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

}
