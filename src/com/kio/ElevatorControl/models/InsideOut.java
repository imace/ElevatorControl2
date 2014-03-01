package com.kio.ElevatorControl.models;

import com.mobsandgeeks.adapters.InstantText;
import com.kio.ElevatorControl.R;

import java.util.ArrayList;
import java.util.List;

public class InsideOut {

    private String name;//内招或外招

    public static List<InsideOut> INSIDEOUT() {
        List<InsideOut> iso = new ArrayList<InsideOut>();
        InsideOut io = null;
        io = new InsideOut();
        io.setName("内招");
        iso.add(io);
        io = new InsideOut();
        io.setName("外招");
        iso.add(io);
        return iso;
    }

    @InstantText(viewId = R.id.txttransaction)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
