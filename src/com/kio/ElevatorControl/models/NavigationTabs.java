package com.kio.ElevatorControl.models;

import android.content.Context;
import com.kio.ElevatorControl.R;

import java.util.ArrayList;

public class NavigationTabs {
    private String[] texts; //标签文字
    private Integer[] icons; //标签图标
    @SuppressWarnings("rawtypes")
    private Class[] clazz;//点击后跳转的activity

    /**
     * 构造函数
     *
     * @param ctx 上下文
     * @return NavigationTabs
     */
    @SuppressWarnings("rawtypes")
    public static NavigationTabs getTabInstance(Context ctx) {
        NavigationTabs navigationTabs = new NavigationTabs();
        String[] contents = ctx.getResources().getStringArray(R.array.navigation_tab_text);// 临时存放变量
        ArrayList<Integer> icons = new ArrayList<Integer>();// 临时存放变量
        ArrayList<Class> clazz = new ArrayList<Class>();// 临时存放变量
        // 根据资源名称获取属性对应的内存地址
        for (String itemValue : ctx.getResources().getStringArray(R.array.navigation_tab_icon)) {
            icons.add(ctx.getResources().getIdentifier(itemValue,
                    R.drawable.class.getSimpleName(),
                    R.class.getPackage().getName()));
        }
        //根据类名获取要跳转的类
        for (String className : ctx.getResources().getStringArray(R.array.navigation_tab_class_name)) {
            try {
                clazz.add(Class.forName(className));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (null != contents && null != icons && null != clazz
                && contents.length == icons.size() && contents.length == clazz.size()) {
            navigationTabs.setTexts(contents);
            navigationTabs.setIcons(icons.toArray(new Integer[icons.size()]));
            navigationTabs.setClassNames(clazz.toArray(new Class[clazz.size()]));
            return navigationTabs;
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
    public void setClassNames(Class[] classNames) {
        this.clazz = classNames;
    }
}
