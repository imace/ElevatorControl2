package com.inovance.ElevatorControl.models;

import android.annotation.SuppressLint;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import com.mobsandgeeks.adapters.InstantText;
import net.tsz.afinal.annotation.sqlite.Id;
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
public class RealTimeMonitor implements Cloneable {

    @Id
    private int Id;

    private String code;// 功能码
    private String name;// 名称
    private String productId;// Id
    private String description;// 参数选项说明
    private String childId;// 子Id
    private String scope;// 范围
    private String scale;// 最小单位
    private String unit;// 单位名称
    private String type;// 种类
    private boolean showBit = false;// 是否详细描述每一位上的值,默认是false

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

    private boolean isValid;
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
    public static int ParseDescriptionToType(String description) {
        if (description != null) {
            if (description.length() > 0 && !description.contains("null")) {
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
        if (descriptionType == ApplicationConfig.specialTypeInput ||
                descriptionType == ApplicationConfig.specialTypeOutput) {
            return received.length == 0 ? "" : "查看详细->";
        }
        listViewItemText = ParseSerialsUtils.getValueTextFromRealTimeMonitor(this);
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public boolean isShowBit() {
        return showBit;
    }

    public void setShowBit(boolean showBit) {
        this.showBit = showBit;
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
    @SuppressLint("DefaultLocale")
    public static String GenerateJSONDescription(String description) {
        if (description != null) {
            if (description.length() > 0 && !description.contains("null")) {
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
                                        .replace("Bit", "")
                                        .replace("bit", "")
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
                                        .replace("Bit", "")
                                        .replace("bit", "")
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
                                    .replace("Bit", "")
                                    .replace("bit", "")
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
