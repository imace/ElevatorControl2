package com.kio.ElevatorControl.models;

import android.annotation.SuppressLint;
import com.mobsandgeeks.adapters.InstantText;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;
import net.tsz.afinal.annotation.sqlite.Id;
import net.tsz.afinal.annotation.sqlite.ManyToOne;
import net.tsz.afinal.annotation.sqlite.OneToMany;
import net.tsz.afinal.db.sqlite.OneToManyLazyLoader;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    /**
     * 参数选项说明类型,根据description得出 无<0,数字=count,Bit=100000+bitcount
     * 0=error,100000=error
     */
    private int descriptiontype;
    /**
     * 修改方式 '★' : 1 停机修改 '☆' : 2 任意修改 '*' : 3 不可修改
     */
    private String mode;
    private boolean Valid;
    private Date lasttime;


    private byte[] received;

    private String finalValue;


    @ManyToOne(column = "FKGroupId")
    private ParameterGroupSettings parametergroupsettings;

    @OneToMany(manyColumn = "OptExplainId")
    private OneToManyLazyLoader<ParameterOptExplain, ParameterOptExplain> parameteroptexplain;


    /**
     * 无<0,数字=count,Bit=100000+bitcount 0=error,100000=error
     *
     * @return
     */
    @SuppressLint("DefaultLocale")
    public static int ParseDescriptionToType(String des) {
        if (des != null && des.length() > 0) {
            // 不区分大小写
            String source = des.toUpperCase();
            String part = "bit".toUpperCase();
            // Bit=100000000+bitcount
            if (source.contains(part)) {
                String pattern = "#bit";// 正则表达式
                Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(source);
                int c = 0;
                while (m.find()) {
                    c++;
                }
                return 100000000 + c;
            } else {
                String pattern = "#[0-7]+";// 正则表达式
                Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(source);
                int c = 0;
                while (m.find()) {
                    c++;
                }
                return c;
            }
        } else {
            // 无<0
            return -1;
        }
    }


    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @InstantText(viewId = R.id.txtparametersetting)
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

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getScale() {
        return scale;
    }

    public void setScale(String scale) {
        this.scale = scale;
    }

    @InstantText(viewId = R.id.unitparametersetting)
    public String getUnit() {
        return (unit == null || unit.length() <= 0 || unit.equalsIgnoreCase("null")) ? "(-)" : "(" + unit + ")";
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

    public Date getLasttime() {
        return lasttime;
    }

    public void setLasttime(Date lasttime) {
        this.lasttime = lasttime;
    }

    public ParameterGroupSettings getParametergroupsettings() {
        return parametergroupsettings;
    }

    public void setParametergroupsettings(ParameterGroupSettings parametergroupsettings) {
        this.parametergroupsettings = parametergroupsettings;
    }

    public OneToManyLazyLoader<ParameterOptExplain, ParameterOptExplain> getParameteroptexplain() {
        return parameteroptexplain;
    }

    public void setParameteroptexplain(OneToManyLazyLoader<ParameterOptExplain, ParameterOptExplain> parameteroptexplain) {
        this.parameteroptexplain = parameteroptexplain;
    }

    public int getDescriptiontype() {
        return descriptiontype;
    }

    public void setDescriptiontype(int descriptiontype) {
        this.descriptiontype = descriptiontype;
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


    public void setReceived(byte[] received) {
        this.received = received;
    }


    @InstantText(viewId = R.id.valueparametersetting)
    public String getFinalValue() {
        finalValue = ParseSerialsUtils.getValueTextFromParameterSetting(this);
        return finalValue;
    }


    public void setFinalValue(String finalValue) {
        this.finalValue = finalValue;
    }

}
