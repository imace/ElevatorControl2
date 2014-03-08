package com.kio.ElevatorControl.daos;

import android.annotation.SuppressLint;
import android.content.Context;
import com.kio.ElevatorControl.models.ConfigurationTabs;
import com.kio.ElevatorControl.models.FirmwareManageTabs;
import com.kio.ElevatorControl.models.NavigationTabs;
import com.kio.ElevatorControl.models.TroubleAnalyzeTabs;

public class MenuValuesDao {

    // ==================== ConfigurationActivity 电梯配置 ========================

    /**
     * 找出选中的tabIndex所对应的layout
     */
    public static int getConfigurationTabsLayoutId(int tabIndex, Context context) {
        return context.getResources().getIdentifier(
                ConfigurationTabs.getTabInstance(context).getLayouts()[tabIndex],
                "layout", "com.kio.ElevatorControl");
    }

    public static String getConfigurationLoadMethodName(int tabIndex,
                                                        Context context) {
        return ConfigurationTabs.getTabInstance(context).getFunctionName()[tabIndex];
    }

    public static int getConfigurationTabsTextsPosition(int position, Context context) {
        return position
                % (ConfigurationTabs.getTabInstance(context).getTexts().length);
    }

    public static String[] getTransactionTabsTexts(Context context) {
        return ConfigurationTabs.getTabInstance(context).getTexts();
    }

    @SuppressLint("DefaultLocale")
    public static String getConfigurationTabsPageTitle(int position, Context context) {
        ConfigurationTabs tb = ConfigurationTabs.getTabInstance(context);
        String[] strings = tb.getTexts();
        return strings[position % strings.length].toUpperCase();
    }

    public static int getConfigurationTabsCount(Context context) {
        return (ConfigurationTabs.getTabInstance(context).getTexts().length);
    }

    // ==================== TroubleAnalyzeActivity 故障分析 ==============================

    public static int getTroubleAnalyzeTabsLayoutId(int tabIndex, Context context) {
        return context.getResources().getIdentifier(
                TroubleAnalyzeTabs.getTabInstance(context).getLayouts()[tabIndex],
                "layout", "com.kio.ElevatorControl");
    }

    public static String getTroubleAnalyzeTabsLoadMethodName(int tabIndex,
                                                             Context context) {
        return TroubleAnalyzeTabs.getTabInstance(context).getFunctionName()[tabIndex];
    }

    public static int getTroubleAnalyzeTabsTextsPosition(int position, Context context) {
        return position
                % (TroubleAnalyzeTabs.getTabInstance(context).getTexts().length);
    }

    public static int getTroubleAnalyzeTabsCount(Context context) {
        return TroubleAnalyzeTabs.getTabInstance(context).getTexts().length;
    }

    public static String getTroubleAnalyzeTabsPageTitle(int position, Context context) {
        TroubleAnalyzeTabs troubleAnalyzeTabs = TroubleAnalyzeTabs.getTabInstance(context);
        return troubleAnalyzeTabs.getTexts()[position % troubleAnalyzeTabs.getTexts().length];
    }

    // ==================== FirmwareManageActivity 固件管理 ================================

    public static int getFirmwareManageTabsLayoutId(int tabIndex, Context context) {
        return context.getResources().getIdentifier(
                FirmwareManageTabs.getTabInstance(context).getLayouts()[tabIndex],
                "layout", "com.kio.ElevatorControl");
    }

    public static String getFirmwareManageTabsLoadMethodName(int tabIndex,
                                                             Context context) {
        return FirmwareManageTabs.getTabInstance(context).getFunctionName()[tabIndex];
    }

    public static int getFirmwareManageTabsTextsPosition(int position, Context context) {
        return position
                % (FirmwareManageTabs.getTabInstance(context).getTexts().length);
    }

    public static int getFirmwareManageTabsCount(Context context) {
        return FirmwareManageTabs.getTabInstance(context).getTexts().length;
    }

    public static String getFirmwareManageTabsPageTitle(int position, Context context) {
        FirmwareManageTabs firmwareManageTabs = FirmwareManageTabs.getTabInstance(context);
        return firmwareManageTabs.getTexts()[position % firmwareManageTabs.getTexts().length];
    }

    // ==================== NavigationTabs 首页导航 ===========================================

    public static String[] getNavigationTabsTexts(Context context) {
        return NavigationTabs.getTabInstance(context).getTexts();
    }

    public static Integer[] getNavigationTabsIcons(Context context) {
        return NavigationTabs.getTabInstance(context).getIcons();
    }

    public static Class<?>[] getNavigationTabsClazz(Context context) {
        return NavigationTabs.getTabInstance(context).getClazz();
    }

}
