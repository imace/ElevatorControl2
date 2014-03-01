package com.kio.ElevatorControl.daos;

import android.annotation.SuppressLint;
import android.content.Context;
import com.kio.ElevatorControl.models.FailureTabs;
import com.kio.ElevatorControl.models.RootTabs;
import com.kio.ElevatorControl.models.TransactionTabs;

public class ValuesDao {

    // TransactionTabs

    /**
     * 找出选中的tabIndex所对应的layout
     */
    public static int getTransactionTabsLayoutId(int tabIndex, Context ctx) {
        return ctx.getResources().getIdentifier(
                TransactionTabs.getTabInstance(ctx).getLayouts()[tabIndex],
                "layout", "com.kio.ElevatorControl");
    }

    public static String getTransactionTabsLoadMethodName(int tabIndex,
                                                          Context ctx) {
        return TransactionTabs.getTabInstance(ctx).getFuncs()[tabIndex];
    }

    public static int getTransactionTabsTextsPosition(int position, Context ctx) {
        return position
                % (TransactionTabs.getTabInstance(ctx).getTexts().length);
    }

    public static String[] getTransactionTabsTexts(Context ctx) {
        return TransactionTabs.getTabInstance(ctx).getTexts();
    }

    @SuppressLint("DefaultLocale")
    public static String getTransactionTabsPageTitle(int position, Context ctx) {
        TransactionTabs tb = TransactionTabs.getTabInstance(ctx);
        String[] slst = tb.getTexts();
        return slst[position % slst.length].toUpperCase();
    }

    public static int getTransactionTabsTextsLength(Context ctx) {
        return (TransactionTabs.getTabInstance(ctx).getTexts().length);
    }


    //FailureTabs

    public static int getFailureTabsLayoutId(int tabIndex, Context ctx) {
        return ctx.getResources().getIdentifier(
                FailureTabs.getTabInstance(ctx).getLayouts()[tabIndex],
                "layout", "com.kio.ElevatorControl");
    }

    public static String getFailureTabsLoadMethodName(int tabIndex,
                                                      Context ctx) {
        return FailureTabs.getTabInstance(ctx).getFuncs()[tabIndex];
    }

    public static int getFailureTabsTextsPosition(int position, Context ctx) {
        return position
                % (FailureTabs.getTabInstance(ctx).getTexts().length);
    }

    public static int getFailureTabsCount(Context ctx) {
        return FailureTabs.getTabInstance(ctx).getTexts().length;
    }

    public static String getFailureTabsPageTitle(int position, Context ctx) {
        FailureTabs failuretabs = FailureTabs.getTabInstance(ctx);
        return failuretabs.getTexts()[position % failuretabs.getTexts().length];
    }


    // RootTabs

    public static String[] getRootTabsTexts(Context ctx) {
        return RootTabs.getTabInstance(ctx).getTexts();
    }

    public static Integer[] getRootTabsIcons(Context ctx) {
        return RootTabs.getTabInstance(ctx).getIcons();
    }

    public static Class<?>[] getRootTabsClazz(Context ctx) {
        return RootTabs.getTabInstance(ctx).getClazz();
    }

}
