package com.kio.ElevatorControl.models;

import com.kio.ElevatorControl.R;
import com.mobsandgeeks.adapters.InstantText;
import net.tsz.afinal.annotation.sqlite.Id;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-10.
 * Time: 15:27.
 */
public class Device {

    @Id
    private int Id;

    private String name;

    private String model;

    private String description;

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    @InstantText(viewId = R.id.device_name)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @InstantText(viewId = R.id.device_model)
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
