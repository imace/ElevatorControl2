package com.kio.ElevatorControl.models;

import android.content.Context;
import com.kio.ElevatorControl.R;

public class TransactionTabs {
    private String[] texts;

    private String[] layouts;

    private String[] llrids;

    private String[] funcs;

    public static TransactionTabs getTabInstance(Context ctx) {
        TransactionTabs ft = new TransactionTabs();
        String[] t = ctx.getResources().getStringArray(
                R.array.transaction_tab_text);// 临时存放变量
        String[] ls = ctx.getResources().getStringArray(
                R.array.transaction_tab_layout);
        String[] fs = ctx.getResources().getStringArray(
                R.array.transaction_function);
        if (null != t && t.length > 0 && null != ls && null != fs
                && t.length > 0 && ls.length > 0 && fs.length > 0
                && t.length == ls.length
                && ls.length == fs.length) {
            ft.setTexts(t);
            ft.setLayouts(ls);
            ft.setFuncs(fs);
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

    public String[] getLlrids() {
        return llrids;
    }

    public void setLlrids(String[] llrids) {
        this.llrids = llrids;
    }

    public String[] getFuncs() {
        return funcs;
    }

    public void setFuncs(String[] funcs) {
        this.funcs = funcs;
    }

}
