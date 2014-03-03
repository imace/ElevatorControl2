package com.kio.ElevatorControl.models;

import com.kio.ElevatorControl.R;
import com.mobsandgeeks.adapters.InstantText;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ParameterCopy {
    private int Id;

    private String name;


    public static List<ParameterCopy> PARAMCOPY() {
        ArrayList<ParameterCopy> arr = new ArrayList<ParameterCopy>();
        ParameterCopy pcy = null;
        pcy = new ParameterCopy();
        pcy.setName("参数上传");
        arr.add(pcy);
        pcy = new ParameterCopy();
        pcy.setName("参数下载");
        arr.add(pcy);
        pcy = new ParameterCopy();
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

    public Date getLasttime() {
        return lasttime;
    }

    public void setLasttime(Date lasttime) {
        this.lasttime = lasttime;
    }

    private boolean Valid;
    private Date lasttime;

}
