package com.inovance.elevatorcontrol.models;

import net.tsz.afinal.annotation.sqlite.Id;
import net.tsz.afinal.annotation.sqlite.Table;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-31.
 * Time: 9:47.
 */
@Table(name = "SHORTCUT")
public class Shortcut {

    @Id
    private int Id;

    private String name;

    private String command;

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
