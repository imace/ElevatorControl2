package com.kio.ElevatorControl.models;

import com.kio.ElevatorControl.R;
import com.mobsandgeeks.adapters.InstantText;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ParameterDuplicate {
    private int Id;

    private String name;

    public static List<ParameterDuplicate> getParamDuplicateLists() {
        ArrayList<ParameterDuplicate> arr = new ArrayList<ParameterDuplicate>();
        ParameterDuplicate pcy = null;
        pcy = new ParameterDuplicate();
        pcy.setName("参数上传");
        arr.add(pcy);
        pcy = new ParameterDuplicate();
        pcy.setName("参数下载");
        arr.add(pcy);
        pcy = new ParameterDuplicate();
        pcy.setName("恢复出厂设置");
        arr.add(pcy);
        return arr;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    @InstantText(viewId = R.id.text_transaction)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    private boolean Valid;
    private Date lastTime;

}
