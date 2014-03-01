package com.kio.ElevatorControl.models;

import android.annotation.SuppressLint;
import com.mobsandgeeks.adapters.InstantText;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;
import net.tsz.afinal.annotation.sqlite.Id;
import net.tsz.afinal.annotation.sqlite.Transient;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 只有db的demo listview 绑定这个版本是没有的
 *
 * @author jch
 */
public class RealtimeMonitor implements Cloneable {

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
    /**
     * 参数选项说明类型,根据description得出 无<0,数字=count,Bit=100000+bitcount
     * 0=error,100000=error
     */
    private int descriptiontype;
    private boolean Valid;
    private Date lasttime;

    /**
     * 收到的数据
     */
    @Transient
    private byte[] received;

    /**
     * 不持久化用来绑定到ui的listview
     */
    @Transient
    private String listViewItemText;

    public RealtimeMonitor() {
    }

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

    /**
     * 显示到listview上的内容
     *
     * @return
     */
    @InstantText(viewId = R.id.valuemonitoritem)
    public String getListViewItemText() {
        listViewItemText = ParseSerialsUtils.getValueTextFromRealtimeMonitor(this);
        return this.listViewItemText;
    }

    public void setListViewItemText(String str) {
        this.listViewItemText = str;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @InstantText(viewId = R.id.txttransaction)
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

    @InstantText(viewId = R.id.unitmonitoritem)
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

    public Date getLasttime() {
        return lasttime;
    }

    public void setLasttime(Date lasttime) {
        this.lasttime = lasttime;
    }

    public boolean isValid() {
        return Valid;
    }

    public void setValid(boolean valid) {
        Valid = valid;
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

    public int getDescriptiontype() {
        return descriptiontype;
    }

    public void setDescriptiontype(int descriptiontype) {
        this.descriptiontype = descriptiontype;
    }

    public byte[] getReceived() {
        return received;
    }

    public void setReceived(byte[] received) {
        this.received = received;
    }

    public Object clone() {
        RealtimeMonitor o = null;
        try {
            o = (RealtimeMonitor) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return o;
    }

}
