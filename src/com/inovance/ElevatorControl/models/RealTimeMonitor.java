package com.inovance.elevatorcontrol.models;

import android.annotation.SuppressLint;

import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.utils.ParseSerialsUtils;
import com.inovance.elevatorcontrol.utils.TextLocalize;
import com.mobsandgeeks.adapters.InstantText;

import net.tsz.afinal.annotation.sqlite.Id;
import net.tsz.afinal.annotation.sqlite.Table;
import net.tsz.afinal.annotation.sqlite.Transient;

import org.json.JSONException;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 只有db的demo ListView 绑定这个版本是没有的
 *
 * @author jch
 */
@Table(name = "REAL_TIME_MONITOR")
public class RealTimeMonitor implements Cloneable {

    @Id
    private int Id;

    /**
     * 功能码
     */
    private String code;

    /**
     * 名称
     */
    private String name;

    /**
     * productId
     */
    private String productId;

    /**
     * 参数选项说明
     */
    private String description;

    /**
     * 子Id
     */
    private String childId;

    /**
     * 范围
     */
    private String scope;

    /**
     * 最小单位
     */
    private String scale;

    /**
     * 单位名称
     */
    private String unit;

    /**
     * 实时监控唯一ID
     */
    private int stateID;

    /**
     * 实时监控顺序
     */
    private int sort;

    @Transient
    private byte[] HVInputTerminalBytes;

    @Transient
    private byte[] combineBytes;

    /**
     * 无描述返回     0
     * 数值计算匹配   1
     * Bit位值匹配    2
     * Bit多位值匹配  3
     */
    private int descriptionType;

    /**
     * 解析后的JSON Array String
     */
    private String JSONDescription;

    @Transient
    private boolean isValid;

    @Transient
    private Date lastTime;

    /**
     * 收到的数据
     */
    @Transient
    private byte[] received;

    /**
     * 不持久化用来绑定到ui的ListView
     */
    @Transient
    private String listViewItemText;

    private int deviceID;

    public int getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(int deviceID) {
        this.deviceID = deviceID;
    }

    public RealTimeMonitor() {

    }

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
    public static int ParseDescriptionToType(int type, String description) {
        if (description != null) {
            if (description.length() > 0 && !description.contains("null")) {
                if (type == ApplicationConfig.HomeStateCode[1]) {
                    String part0 = "Bit4-7";
                    String part1 = "Bit8-11";
                    String part2 = "Bit12";
                    description.indexOf(part0);
                    StringBuilder stringBuilder = new StringBuilder(description);
                    stringBuilder.insert(description.indexOf(part0), "|");
                    stringBuilder.insert(description.indexOf(part1) + 1, "|");
                    stringBuilder.insert(description.indexOf(part2) + 2, "|");
                    description = stringBuilder.toString();
                }
                if (description.contains("Bit") || description.contains("bit")) {
                    if (description.contains("|")) {
                        return ApplicationConfig.DESCRIPTION_TYPE[3];
                    } else {
                        return ApplicationConfig.DESCRIPTION_TYPE[2];
                    }
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

    /**
     * 显示到ListView上的内容
     *
     * @return String
     */
    @InstantText(viewId = R.id.value_monitor_item)
    public String getListViewItemText() {
        if (stateID == ApplicationConfig.MonitorStateCode[5]
                || stateID == ApplicationConfig.MonitorStateCode[6]
                || stateID == ApplicationConfig.MonitorStateCode[14]) {
            return received.length == 0 ? "" : TextLocalize.getInstance().getViewDetailText();
        }
        listViewItemText = ParseSerialsUtils.getValueTextFromRealTimeMonitor(this);
        if (listViewItemText.contains("E00")) {
            return "无故障";
        }
        return this.listViewItemText;
    }

    public void setListViewItemText(String str) {
        this.listViewItemText = str;
    }

    public String getCode() {
        return code;
    }

    public String getCodeText() {
        if (getCode().length() == 4) {
            return getCode().substring(0, 2) + "-" + getCode().substring(2, 4);
        }
        return "";
    }

    public void setCode(String code) {
        this.code = code;
    }

    @InstantText(viewId = R.id.text_transaction)
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

    public String getScale() {
        return scale;
    }

    public void setScale(String scale) {
        this.scale = scale;
    }

    @InstantText(viewId = R.id.unit_monitor_item)
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

    public Date getLastTime() {
        return lastTime;
    }

    public void setLastTime(Date lastTime) {
        this.lastTime = lastTime;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public int getDescriptionType() {
        return descriptionType;
    }

    public void setDescriptionType(int descriptionType) {
        this.descriptionType = descriptionType;
    }

    public byte[] getReceived() {
        return received;
    }

    public void setReceived(byte[] received) {
        this.received = received;
    }

    /**
     * 生成 JSON Description Array String
     *
     * @param description Description String
     * @return JSON String
     */
    public static String GenerateJSONDescription(int type, String description) {
        if (description != null) {
            if (description.length() > 0 && !description.contains("null")) {
                if (type == ApplicationConfig.HomeStateCode[1]) {
                    String part0 = "Bit4-7";
                    String part1 = "Bit8-11";
                    String part2 = "Bit12";
                    description.indexOf(part0);
                    StringBuilder stringBuilder = new StringBuilder(description);
                    stringBuilder.insert(description.indexOf(part0), "|");
                    stringBuilder.insert(description.indexOf(part1) + 1, "|");
                    stringBuilder.insert(description.indexOf(part2) + 2, "|");
                    description = stringBuilder.toString();
                }
                JSONStringer jsonStringer = new JSONStringer();
                try {
                    if (description.contains("|")) {
                        String[] part = description.split("\\|");
                        List<String> tempList = new ArrayList<String>();
                        Pattern pattern = Pattern.compile("^Bit\\d*\\-\\d*:.*", Pattern.CASE_INSENSITIVE);
                        for (String item : part) {
                            if (pattern.matcher(item).matches()) {
                                tempList.add(item);
                            } else {
                                Collections.addAll(tempList, item.split("#"));
                            }
                        }
                        jsonStringer.array();
                        for (String unit : tempList) {
                            if (pattern.matcher(unit).matches()) {
                                String[] entity = unit.split("#");
                                jsonStringer.object();
                                jsonStringer.key(entity[0]
                                        .replaceAll("(?i)bit", "")
                                        .replaceFirst("^0+(?!$)", ""));
                                jsonStringer.array();
                                for (int i = 1; i < entity.length; i++) {
                                    String[] sub = entity[i].split(":");
                                    jsonStringer.object();
                                    jsonStringer.key("id").value(sub.length > 0 ? sub[0] : "");
                                    jsonStringer.key("value").value(sub.length > 1 ? sub[1] : "");
                                    jsonStringer.endObject();
                                }
                                jsonStringer.endArray();
                                jsonStringer.endObject();
                            } else {
                                String[] entity = unit.split(":");
                                jsonStringer.object();
                                jsonStringer.key("id").value(entity.length > 0 ? entity[0]
                                        .replaceAll("(?i)bit", "")
                                        .replaceFirst("^0+(?!$)", "") : "");
                                jsonStringer.key("value").value(entity.length > 1 ? entity[1] : "");
                                jsonStringer.endObject();
                            }
                        }
                        jsonStringer.endArray();
                    } else {
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
                    }
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

    public byte[] getCombineBytes() {
        return combineBytes;
    }

    public void setCombineBytes(byte[] combineBytes) {
        this.combineBytes = combineBytes;
    }

    public int getStateID() {
        return stateID;
    }

    public void setStateID(int stateID) {
        this.stateID = stateID;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public byte[] getHVInputTerminalBytes() {
        return HVInputTerminalBytes;
    }

    public void setHVInputTerminalBytes(byte[] HVInputTerminalBytes) {
        this.HVInputTerminalBytes = HVInputTerminalBytes;
    }

    public Object clone() throws CloneNotSupportedException {
        RealTimeMonitor o = null;
        try {
            o = (RealTimeMonitor) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return o;
    }

}
