package com.inovance.ElevatorControl.models;

import android.annotation.SuppressLint;
import com.bluetoothtool.SerialUtility;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import com.mobsandgeeks.adapters.InstantText;
import net.tsz.afinal.annotation.sqlite.Id;
import net.tsz.afinal.annotation.sqlite.ManyToOne;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.Date;

/**
 * 参数
 *
 * @author jch
 */
public class ParameterSettings implements Cloneable {

    @Id
    private int Id;

    private String code;// 功能码

    private String name;// 名称

    private String productId;// Id

    private String description;// 参数选项说明

    private String childId;// 子Id

    private String scope;// 取值范围

    private String defaultValue;// 出厂设定

    private String scale;// 最小单位

    private String unit;// 单位名称

    private String type;// 种类

    private String tempScope; // 取得的取值范围

    /**
     * 解析后的JSON Array String
     */
    private String JSONDescription;

    /**
     * 用户设定值
     */
    private String userValue;

    /**
     * 16进制值
     */
    private String hexValueString;

    /**
     * 无描述返回     0
     * 数值计算匹配   1
     * Bit位值匹配    2
     * Bit多位值匹配  3
     */
    private int descriptionType;

    /**
     * 修改方式 '★' : 1 任意修改 '☆' : 2 停机修改 '*' : 3 不可修改
     */
    private String mode;

    private boolean Valid;

    private Date lastTime;

    private byte[] received;

    private String finalValue;

    /**
     * 参数写入错误代码
     */
    private int writeErrorCode = -1;

    private boolean elevatorRunning = true;

    private int deviceID;

    public int getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(int deviceID) {
        this.deviceID = deviceID;
    }

    @ManyToOne(column = "FKGroupId")
    private ParameterGroupSettings parametergroupsettings;

    /**
     * 无描述返回     0
     * 数值计算匹配   1
     * Bit位值匹配    2
     * Bit多位值匹配  3
     *
     * @param description Description String
     * @return Type
     */
    @SuppressLint("DefaultLocale")
    public static int ParseDescriptionToType(String description) {
        if (description != null) {
            if (description.length() > 0 && !description.contains("null")) {
                if (description.contains("Bit") || description.contains("bit")) {
                    return ApplicationConfig.DESCRIPTION_TYPE[2];
                } else {
                    return ApplicationConfig.DESCRIPTION_TYPE[1];
                }
            } else {
                return ApplicationConfig.DESCRIPTION_TYPE[0];
            }
        } else {
            return ApplicationConfig.DESCRIPTION_TYPE[0];
        }
    }

    public ParameterSettings() {

    }

    public ParameterSettings(JSONObject object) {
        this.code = object.optString("code".toUpperCase());
        this.name = object.optString("name".toUpperCase());
        this.productId = object.optString("productId".toUpperCase());
        this.description = object.optString("description".toUpperCase());
        this.descriptionType = ParameterSettings.ParseDescriptionToType(this.getDescription());
        this.childId = object.optString("childId".toUpperCase());
        this.scope = object.optString("scope".toUpperCase());
        this.userValue = object.optString("userValue".toUpperCase());
        this.hexValueString = object.optString("hexValue".toUpperCase());
        this.defaultValue = object.optString("defaultValue".toUpperCase());
        this.scale = object.optString("scale".toUpperCase());
        this.unit = object.optString("unit".toUpperCase());
        this.type = object.optString("type".toUpperCase());
        this.mode = object.optString("mode".toUpperCase());
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getCode() {
        return code.replace("FR", "D2");
    }

    @InstantText(viewId = R.id.code_text)
    public String getCodeText() {
        String codeText = getCode();
        if (codeText.length() == 4) {
            return codeText.substring(0, 2).replace("D2", "FR")
                    + "-" + codeText.substring(2, 4);
        }
        return "";
    }

    public void setCode(String code) {
        this.code = code;
    }

    @InstantText(viewId = R.id.text_parameter_setting)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getTempScope() {
        return tempScope;
    }

    public void setTempScope(String tempScope) {
        this.tempScope = tempScope;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getUserValue() {
        return userValue;
    }

    public void setUserValue(String userValue) {
        this.userValue = userValue;
    }

    public String getHexValueString() {
        return hexValueString;
    }

    public void setHexValueString(String hexValueString) {
        this.hexValueString = hexValueString;
    }

    public String getScale() {
        return scale;
    }

    public void setScale(String scale) {
        this.scale = scale;
    }

    @InstantText(viewId = R.id.unit_parameter_setting)
    public String getUnit() {
        if (unit != null && unit.length() > 0 && !unit.equalsIgnoreCase("null")) {
            return unit;
        } else {
            return "";
        }
    }

    public void setUnit(String unit) {
        this.unit = unit.replace("(", "").replace(")", "");
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
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

    public ParameterGroupSettings getParametergroupsettings() {
        return parametergroupsettings;
    }

    public void setParametergroupsettings(ParameterGroupSettings parametergroupsettings) {
        this.parametergroupsettings = parametergroupsettings;
    }

    public int getDescriptionType() {
        return descriptionType;
    }

    public void setDescriptionType(int descriptionType) {
        this.descriptionType = descriptionType;
    }

    public Object clone() {
        ParameterSettings o = null;
        try {
            o = (ParameterSettings) super.clone();
        } catch (CloneNotSupportedException e) {
        }
        return o;
    }

    public byte[] getReceived() {
        return received;
    }

    public void setReceived(byte[] data) {
        this.received = data;
        if (data.length == 8) {
            this.userValue = String.valueOf(ParseSerialsUtils.getIntFromBytes(data));
            this.hexValueString = SerialUtility.byte2HexStr(new byte[]{data[4], data[5]});
            this.finalValue = ParseSerialsUtils.getValueTextFromParameterSetting(this);
        }
    }

    @InstantText(viewId = R.id.value_parameter_setting)
    public String getFinalValue() {
        return finalValue;
    }

    public void setFinalValue(String finalValue) {
        this.finalValue = finalValue;
    }

    public int getWriteErrorCode() {
        return writeErrorCode;
    }

    public void setWriteErrorCode(int writeErrorCode) {
        this.writeErrorCode = writeErrorCode;
    }

    /**
     * 生成 JSON Description Array String
     *
     * @param description Description String
     * @return JSON String
     */
    @SuppressLint("DefaultLocale")
    public static String GenerateJSONDescription(String description) {
        if (description != null) {
            if (description.length() > 0 && !description.contains("null")) {
                JSONStringer jsonStringer = new JSONStringer();
                try {
                    jsonStringer.array();
                    for (String item : description.split("#")) {
                        String[] part = item.split(":");
                        jsonStringer.object();
                        jsonStringer.key("id").value(part.length > 0 ? part[0]
                                .replaceAll("(?i)bit", "")
                                .replaceFirst("^0+(?!$)", "") : "");
                        jsonStringer.key("value").value(part.length > 1 ? part[1] : "");
                        jsonStringer.endObject();
                    }
                    jsonStringer.endArray();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return jsonStringer.toString();
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

    public String getJSONDescription() {
        return JSONDescription;
    }

    public void setJSONDescription(String JSONDescription) {
        this.JSONDescription = JSONDescription;
    }

    public boolean isElevatorRunning() {
        return elevatorRunning;
    }

    public void setElevatorRunning(boolean elevatorRunning) {
        this.elevatorRunning = elevatorRunning;
    }

}
