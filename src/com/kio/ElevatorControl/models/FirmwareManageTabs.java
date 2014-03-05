package com.kio.ElevatorControl.models;

import android.content.Context;
import com.kio.ElevatorControl.R;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-5.
 * Time: 15:20.
 */
public class FirmwareManageTabs {

    private String[] texts;

    private String[] layouts;

    private String[] functionName;

    public static FirmwareManageTabs getTabInstance(Context ctx) {
        FirmwareManageTabs firmwareManageTabs = new FirmwareManageTabs();
        String[] t = ctx.getResources().getStringArray(
                R.array.firmware_manage_tab_text);// 临时存放变量
        String[] ls = ctx.getResources().getStringArray(
                R.array.firmware_manage_tab_layout);
        String[] fs = ctx.getResources().getStringArray(
                R.array.firmware_manage_tab_function);
        if (null != t && t.length > 0 && null != ls && null != fs
                && t.length > 0 && ls.length > 0 && fs.length > 0
                && t.length == ls.length
                && ls.length == fs.length) {
            firmwareManageTabs.setTexts(t);
            firmwareManageTabs.setLayouts(ls);
            firmwareManageTabs.setFunctionName(fs);
            return firmwareManageTabs;
        }
        return null;
    }

    public String[] getTexts() {
        return texts;
    }

    public void setTexts(String[] texts) {
        this.texts = texts;
    }

    public String[] getLayouts() {
        return layouts;
    }

    public void setLayouts(String[] layouts) {
        this.layouts = layouts;
    }

    public String[] getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String[] functionName) {
        this.functionName = functionName;
    }

}
