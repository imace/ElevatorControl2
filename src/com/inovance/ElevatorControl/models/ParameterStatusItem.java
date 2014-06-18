package com.inovance.ElevatorControl.models;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-28.
 * Time: 13:24.
 */
public class ParameterStatusItem {

    private String id;

    /**
     * 参数名称
     */
    private String name;

    /**
     * 状态
     */
    private boolean status;

    /**
     * 状态（语句描述）
     */
    private String statusString;

    /**
     * 是否为 F5-25 组
     */
    private boolean isSpecial = false;

    /**
     * 是否可以写入
     */
    private boolean canEdit;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getStatusString() {
        return statusString;
    }

    public void setStatusString(String statusString) {
        this.statusString = statusString;
    }

    public boolean getEditStatus() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isSpecial() {
        return isSpecial;
    }

    public void setSpecial(boolean isSpecial) {
        this.isSpecial = isSpecial;
    }

}
