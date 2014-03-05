package com.kio.ElevatorControl.models;

import android.content.Context;
import com.kio.ElevatorControl.R;

public class TroubleAnalyzeTabs {
    private String[] texts; // 标签文字

    private String[] layouts;

    private String[] functionName;

    /**
     * 二级故障标签
     *
     * @param ctx
     * @return
     */
    public static TroubleAnalyzeTabs getTabInstance(Context ctx) {
        TroubleAnalyzeTabs ft = new TroubleAnalyzeTabs();
        String[] txt = ctx.getResources().getStringArray(R.array.trouble_analyze_tab_text);// text
        String[] lot = ctx.getResources().getStringArray(
                R.array.trouble_analyze_tab_layout);// layout
        String[] fuc = ctx.getResources().getStringArray(R.array.trouble_analyze_function);
        if (null != txt && null != lot && null != fuc && txt.length > 0
                && lot.length > 0 && fuc.length > 0 && txt.length == lot.length
                && lot.length == fuc.length) {
            ft.setTexts(txt);
            ft.setFunctionName(fuc);
            ft.setLayouts(lot);
            return ft;
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
