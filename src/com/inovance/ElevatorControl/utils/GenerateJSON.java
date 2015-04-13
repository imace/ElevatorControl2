package com.inovance.elevatorcontrol.utils;

import com.inovance.elevatorcontrol.models.ParameterGroupSettings;
import com.inovance.elevatorcontrol.models.ParameterSettings;

import org.json.JSONException;
import org.json.JSONStringer;

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
     * @param groupList Group List
     */
    public String generateProfileJSON(List<ParameterGroupSettings> groupList) {
        JSONStringer jsonStringer = new JSONStringer();
        try {
            jsonStringer.array();
            for (ParameterGroupSettings groupSetting : groupList) {
                jsonStringer.object();
                jsonStringer.key("groupText".toUpperCase()).value(groupSetting.getGroupText());
                jsonStringer.key("groupId".toUpperCase()).value(groupSetting.getGroupId());
                // Parameter Settings
                jsonStringer.key("parameterSettings".toUpperCase());
                jsonStringer.array();
                for (ParameterSettings detailSetting : groupSetting.getParametersettings().getList()) {
                    jsonStringer.object();
                    jsonStringer.key("code".toUpperCase()).value(detailSetting.getCode());
                    jsonStringer.key("name".toUpperCase()).value(detailSetting.getName());
                    jsonStringer.key("productId".toUpperCase()).value(detailSetting.getProductId());
                    jsonStringer.key("description".toUpperCase()).value(detailSetting.getDescription());
                    jsonStringer.key("childId".toUpperCase()).value(detailSetting.getChildId());
                    jsonStringer.key("scope".toUpperCase()).value(detailSetting.getScope());
                    jsonStringer.key("userValue".toUpperCase()).value(detailSetting.getUserValue());
                    jsonStringer.key("hexValue".toUpperCase()).value(detailSetting.getHexValueString());
                    jsonStringer.key("defaultValue".toUpperCase()).value(detailSetting.getDefaultValue());
                    jsonStringer.key("scale".toUpperCase()).value(detailSetting.getScale());
                    jsonStringer.key("unit".toUpperCase()).value(detailSetting.getUnit());
                    jsonStringer.key("type".toUpperCase()).value(detailSetting.getType());
                    jsonStringer.key("mode".toUpperCase()).value(detailSetting.getMode());
                    jsonStringer.endObject();
                }
                jsonStringer.endArray();
                jsonStringer.endObject();
            }
            jsonStringer.endArray();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonStringer.toString();
    }

}
