package com.kio.ElevatorControl.models;

import android.content.Context;
import com.kio.ElevatorControl.R;

public class FailureTabs {
    private String[] texts; // 标签文字

    private String[] layouts;

    private String[] funcs;

    /**
     * 二级故障标签
     *
     * @param ctx
     * @return
     */
    public static FailureTabs getTabInstance(Context ctx) {
        FailureTabs ft = new FailureTabs();
        String[] txt = ctx.getResources().getStringArray(R.array.failure_tab_text);// text
        String[] lot = ctx.getResources().getStringArray(
                R.array.failure_tab_layout);// layout
        String[] fuc = ctx.getResources().getStringArray(R.array.failure_function);
        if (null != txt && null != lot && null != fuc && txt.length > 0
                && lot.length > 0 && fuc.length > 0 && txt.length == lot.length
                && lot.length == fuc.length) {
            ft.setTexts(txt);
            ft.setFuncs(fuc);
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

    public String[] getFuncs() {
        return funcs;
    }

    public void setFuncs(String[] funcs) {
        this.funcs = funcs;
    }
}
