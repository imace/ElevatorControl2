package com.inovance.ElevatorControl.utils;

import com.inovance.ElevatorControl.models.ParameterGroupSettings;
import com.inovance.ElevatorControl.models.ParameterSettings;
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
                jsonStringer.key("groupText").value(groupSetting.getGroupText());
                jsonStringer.key("groupId").value(groupSetting.getGroupId());
                // Parameter Settings
                jsonStringer.key("parameterSettings");
                jsonStringer.array();
                for (ParameterSettings detailSetting : groupSetting.getParametersettings().getList()) {
                    jsonStringer.object();
                    jsonStringer.key("code").value(detailSetting.getCode());
                    jsonStringer.key("name").value(detailSetting.getName());
                    jsonStringer.key("productId").value(detailSetting.getProductId());
                    jsonStringer.key("description").value(detailSetting.getDescription());
                    jsonStringer.key("childId").value(detailSetting.getChildId());
                    jsonStringer.key("scope").value(detailSetting.getScope());
                    jsonStringer.key("userValue").value(detailSetting.getUserValue());
                    jsonStringer.key("hexValue").value(detailSetting.getHexValueString());
                    jsonStringer.key("defaultValue").value(detailSetting.getDefaultValue());
                    jsonStringer.key("scale").value(detailSetting.getScale());
                    jsonStringer.key("unit").value(detailSetting.getUnit());
                    jsonStringer.key("type").value(detailSetting.getType());
                    jsonStringer.key("mode").value(detailSetting.getMode());
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
