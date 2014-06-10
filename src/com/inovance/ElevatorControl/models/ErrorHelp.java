package com.inovance.ElevatorControl.models;

import android.content.Context;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import net.tsz.afinal.annotation.sqlite.Id;
import net.tsz.afinal.annotation.sqlite.Property;

import java.util.Date;

public class ErrorHelp {

    @Id
    private int Id;

    @Property(column = "display")
    private String display;//故障显示
    private String name;//故障名称
    private String productId;// Id
    private String reason; //故障原因
    private String childIda;// 子Ida
    private String childIdb;// 子Idb
    private String solution;//故障解决方案
    private String level;//故障级

    private int deviceID;

    public int getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(int deviceID) {
        this.deviceID = deviceID;
    }

    private boolean Valid;
    private Date lastTime;

    private byte[] received;


    public boolean isValid() {
        return Valid;
    }

    public void setValid(boolean valid) {
        Valid = valid;
    }

    public Date getLastTime() {
        return lastTime;
    }

    public void setLastTime(Date lastTime) {
        this.lastTime = lastTime;
    }

    public byte[] getReceived() {
        return received;
    }

    /**
     * setReceived之时读出其他字段
     *
     * @param received
     * @throws Exception
     */
    public void setReceived(Context ctx, byte[] received) throws Exception {
        this.received = received;
        ErrorHelp eh = ParseSerialsUtils.getErrorHelpFromErrorHelp(ctx, this);
        if (eh != null) {
            this.setChildIda(eh.getChildIda());
            this.setChildIdb(eh.getChildIdb());
            this.setDisplay(eh.getDisplay());
            this.setId(eh.getId());
            this.setLastTime(eh.getLastTime());
            this.setLevel(eh.getLevel());
            this.setName(eh.getName());
            this.setProductId(eh.getProductId());
            this.setReason(eh.getReason());
            this.setSolution(eh.getSolution());
            this.setValid(eh.isValid());
        } else {
            throw new Exception();
        }
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getChildIda() {
        return childIda;
    }

    public void setChildIda(String childIda) {
        this.childIda = childIda;
    }

    public String getChildIdb() {
        return childIdb;
    }

    public void setChildIdb(String childIdb) {
        this.childIdb = childIdb;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

}
