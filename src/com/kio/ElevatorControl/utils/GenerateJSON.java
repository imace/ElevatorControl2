package com.kio.ElevatorControl.utils;

import com.kio.ElevatorControl.models.ParameterGroupSettings;

import java.util.List;

/**
 * Created by keith on 14-3-23.
 * User keith
 * Date 14-3-23
 * Time 下午11:03
 */
public class GenerateJSON {
    private static GenerateJSON ourInstance = new GenerateJSON();

    public static GenerateJSON getInstance() {
        return ourInstance;
    }

    private GenerateJSON() {

    }

    /**
     * Group Parameter Setting List
     *
     * @param list List
     */
    public void generateProfileJSON(List<ParameterGroupSettings> list){

    }

}
