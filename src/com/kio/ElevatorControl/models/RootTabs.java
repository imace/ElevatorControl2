package com.kio.ElevatorControl.models;

import android.content.Context;
import com.kio.ElevatorControl.R;

import java.util.ArrayList;

public class RootTabs {
    private String[] texts; //标签文字
    private Integer[] icons; //标签图标
    @SuppressWarnings("rawtypes")
    private Class[] clazz;//点击后跳转的activity

    /**
     * 一级标签
     *
     * @param ctx
     * @param txtrid  键的id
     * @param iconrid 值的id
     * @return 取不到或取道键值对的长度不等返回null
     */
    @SuppressWarnings("rawtypes")
    public static RootTabs getTabInstance(Context ctx) {
        RootTabs uic = new RootTabs();
        String[] contents = ctx.getResources().getStringArray(R.array.RootTabTxt);// 临时存放变量
        ArrayList<Integer> icons = new ArrayList<Integer>();// 临时存放变量
        ArrayList<Class> clazz = new ArrayList<Class>();// 临时存放变量
        // 根据资源名称获取属性对应的内存地址
        for (String itemvalue : ctx.getResources().getStringArray(R.array.RootTabIcon)) {
            icons.add(ctx.getResources().getIdentifier(itemvalue,
                    R.drawable.class.getSimpleName(),
                    R.class.getPackage().getName()));
        }
        //根据类名获取要跳转的类
        for (String clsnm : ctx.getResources().getStringArray(R.array.RootTabClazz)) {
            try {
                clazz.add(Class.forName(clsnm));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (null != contents && null != icons && null != clazz
                && contents.length == icons.size() && contents.length == clazz.size()) {
            uic.setTexts(contents);
            uic.setIcons(icons.toArray(new Integer[icons.size()]));
            uic.setClazznames(clazz.toArray(new Class[clazz.size()]));
            return uic;
        }
        return null;
    }

    public Integer[] getIcons() {
        return icons;
    }

    public void setIcons(Integer[] icons) {
        this.icons = icons;
    }

    public String[] getTexts() {
        return texts;
    }

    public void setTexts(String[] texts) {
        this.texts = texts;
    }

    @SuppressWarnings("rawtypes")
    public Class[] getClazz() {
        return clazz;
    }

    @SuppressWarnings("rawtypes")
    public void setClazznames(Class[] clazznames) {
        this.clazz = clazznames;
    }
}
