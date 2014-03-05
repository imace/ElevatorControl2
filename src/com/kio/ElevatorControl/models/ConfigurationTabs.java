package com.kio.ElevatorControl.models;

import android.content.Context;
import com.kio.ElevatorControl.R;

public class ConfigurationTabs {
    private String[] texts;

    private String[] layouts;

    private String[] functionName;

    public static ConfigurationTabs getTabInstance(Context ctx) {
        ConfigurationTabs configurationTabs = new ConfigurationTabs();
        String[] t = ctx.getResources().getStringArray(
                R.array.configuration_tab_text);// 临时存放变量
        String[] ls = ctx.getResources().getStringArray(
                R.array.configuration_tab_layout);
        String[] fs = ctx.getResources().getStringArray(
                R.array.configuration_function);
        if (null != t && t.length > 0 && null != ls && null != fs
                && t.length > 0 && ls.length > 0 && fs.length > 0
                && t.length == ls.length
                && ls.length == fs.length) {
            configurationTabs.setTexts(t);
            configurationTabs.setLayouts(ls);
            configurationTabs.setFunctionName(fs);
            return configurationTabs;
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
