package com.inovance.elevatorcontrol.models;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-9-15.
 * Time: 16:02.
 */
public class TroubleGroup {
    private String name;

    private List<ParameterSettings> troubleChildList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ParameterSettings> getTroubleChildList() {
        return troubleChildList;
    }

    public void setTroubleChildList(List<ParameterSettings> troubleChildList) {
        this.troubleChildList = troubleChildList;
    }
}