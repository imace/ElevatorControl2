package com.inovance.ElevatorControl.models;

import net.tsz.afinal.annotation.sqlite.Id;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-23.
 * Time: 16:48.
 */
public class LocalParameterTable {

    @Id
    private int Id;

    private String stateTableName;

    private String updateTime;

    private String meta;

}
