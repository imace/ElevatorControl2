package com.inovance.elevatorcontrol.daos;

import android.content.Context;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.config.ParameterUpdateTool;
import com.inovance.elevatorcontrol.models.*;
import com.inovance.elevatorcontrol.utils.AssetUtils;
import net.tsz.afinal.FinalDb;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * 恢复出厂设置
 *
 * @author jch
 */
public class ParameterFactoryDao {

    private static final boolean DEBUG = false;

    /**
     * 记录是否为空
     *
     * @param context context
     * @return boolean
     */
    public static boolean checkEmpty(Context context) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        int parameterSettingsSize = db.findAll(ParameterSettings.class).size();
        int parameterGroupSettingsSize = db.findAll(ParameterGroupSettings.class).size();
        int realTimeMonitorSize = db.findAll(RealTimeMonitor.class).size();
        int errorHelpSize = db.findAll(ErrorHelp.class).size();
        return parameterSettingsSize == 0 || parameterGroupSettingsSize == 0
                || realTimeMonitorSize == 0 || errorHelpSize == 0;
    }

    /**
     * 恢复出厂设置
     */
    public static void dbInit(Context context) {
        Device device = new Device();
        device.setDeviceType(Device.NormalDevice);
        device.setDeviceName(ApplicationConfig.DefaultDeviceName);
        DeviceDao.save(context, device);

        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.deleteAll(ParameterSettings.class);
        db.deleteAll(ParameterGroupSettings.class);
        db.deleteAll(RealTimeMonitor.class);
        db.deleteAll(ErrorHelp.class);

        restoreFactoryParameterGroupSettings(context, device.getId());
        restoreFactoryRealTimeMonitor(context, device.getId());
        restoreFactoryErrorHelp(context, device.getId());
        ParameterUpdateTool.getInstance().init(context);
    }

    /**
     * 删除所有数据库中存在的条目
     *
     * @param context  Context
     * @param deviceID Device ID
     * @param type     功能码、状态码、故障帮助
     */
    public static void emptyRecordByDeviceID(Context context, int deviceID, int type) {
        switch (type) {
            case ApplicationConfig.FunctionCodeType:
                ParameterGroupSettingsDao.deleteAllByDeviceID(context, deviceID);
                ParameterSettingsDao.deleteAllByDeviceID(context, deviceID);
                break;
            case ApplicationConfig.StateCodeType:
                RealTimeMonitorDao.deleteAllByDeviceID(context, deviceID);
                break;
            case ApplicationConfig.ErrorHelpType:
                ErrorHelpDao.deleteAllByDeviceID(context, deviceID);
                break;
        }
    }

    /**
     * 恢复ParameterGroupSettings的出厂设置
     *
     * @param context context
     */
    public static void restoreFactoryParameterGroupSettings(Context context, int deviceID) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.deleteAll(ParameterSettings.class);
        db.deleteAll(ParameterGroupSettings.class);
        String JSON = AssetUtils.readDefaultFunCode(context, "NICE3000+_FunCode.json");
        saveFunctionCode(JSON, context, deviceID);
    }

    /**
     * 恢复实施监控数据
     *
     * @param context Context
     */
    public static void restoreFactoryRealTimeMonitor(Context context, int deviceID) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.deleteAll(RealTimeMonitor.class);
        String JSON = AssetUtils.readDefaultFunCode(context, "NICE3000+_State.json");
        saveStateCode(JSON, context, deviceID);
    }

    /**
     * 恢复写入错误帮助信息
     *
     * @param context Context
     */
    public static void restoreFactoryErrorHelp(Context context, int deviceID) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.deleteAll(ErrorHelp.class);
        String JSON = AssetUtils.readDefaultFunCode(context, "NICE3000+_ErrHelp.json");
        saveErrorHelp(JSON, context, deviceID);
    }

    public static void saveFunctionCode(String content, Context context, int deviceID) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        try {
            JSONArray groups = new JSONArray(content);
            // 遍历group
            int size = groups.length();
            for (int i = 0; i < size; i++) {
                JSONObject groupsJSONObject = groups.getJSONObject(i);
                ParameterGroupSettings settingsGroup = new ParameterGroupSettings();
                settingsGroup.setGroupText(groupsJSONObject.optString("groupText".toUpperCase()));
                settingsGroup.setGroupId(groupsJSONObject.optString("groupId".toUpperCase()));
                settingsGroup.setValid(true);
                settingsGroup.setLastTime(new Date());
                settingsGroup.setDeviceID(deviceID);
                // 保存groupEntity并且id设置为插入后的值
                db.saveBindId(settingsGroup);
                String groupID = groupsJSONObject.optString("groupId".toUpperCase());
                JSONArray settingJson = groupsJSONObject.getJSONArray("parameterSettings".toUpperCase());
                // 遍历settings
                int length = settingJson.length();
                for (int j = 0; j < length; j++) {
                    JSONObject jsonObject = settingJson.getJSONObject(j);
                    ParameterSettings settings = new ParameterSettings();
                    settings.setCode(jsonObject.optString("code".toUpperCase()).replace("-", ""));
                    settings.setName(jsonObject.optString("name".toUpperCase()));
                    settings.setProductId(jsonObject.optString("productId".toUpperCase()));
                    settings.setDescription(jsonObject.optString("description".toUpperCase()));
                    settings.setDescriptionType(ParameterSettings.ParseDescriptionToType(settings.getDescription()));
                    settings.setJSONDescription(ParameterSettings.GenerateJSONDescription(settings.getDescription()));
                    settings.setChildId(jsonObject.optString("childId".toUpperCase()));
                    settings.setScope(jsonObject.optString("scope".toUpperCase()).replaceAll("-", "").replace("～", "~"));
                    settings.setDefaultValue(jsonObject.optString("defaultValue".toUpperCase()));
                    settings.setScale(jsonObject.optString("scale".toUpperCase()));
                    settings.setUnit(jsonObject.optString("unit".toUpperCase()));
                    settings.setType(jsonObject.optString("type".toUpperCase()));
                    settings.setMode(jsonObject.optString("mode".toUpperCase()));
                    settings.setDeviceID(deviceID);
                    settings.setParametergroupsettings(settingsGroup);
                    // 保存setting
                    db.save(settings);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void saveStateCode(String content, Context context, int deviceID) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        try {
            JSONArray monitors = new JSONArray(content);
            for (int i = 0; i < monitors.length(); i++) {
                JSONObject jsonObject = monitors.getJSONObject(i);
                RealTimeMonitor state = new RealTimeMonitor();
                state.setName(jsonObject.optString("name".toUpperCase()));
                state.setCode(jsonObject.optString("code".toUpperCase()).replace("-", ""));
                state.setChildId(jsonObject.optString("childId".toUpperCase()));
                state.setUnit(jsonObject.optString("unit".toUpperCase()));
                state.setDescription(jsonObject.optString("description".toUpperCase()));
                state.setProductId(jsonObject.optString("productId".toUpperCase()));
                state.setScale(jsonObject.optString("scale".toUpperCase()));
                state.setScope(jsonObject.optString("scope".toUpperCase()));
                state.setStateID(jsonObject.optInt("stateID".toUpperCase()));
                state.setSort(jsonObject.optInt("sort".toUpperCase()));
                state.setDescriptionType(RealTimeMonitor.ParseDescriptionToType(state.getStateID(), state.getDescription()));
                state.setJSONDescription(RealTimeMonitor.GenerateJSONDescription(state.getStateID(), state.getDescription()));
                state.setValid(true);
                state.setLastTime(new Date());
                state.setDeviceID(deviceID);
                db.save(state);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void saveErrorHelp(String content, Context context, int deviceID) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        try {
            JSONArray errHelpList = new JSONArray(content);
            for (int i = 0; i < errHelpList.length(); i++) {
                JSONObject jsonObject = errHelpList.getJSONObject(i);
                ErrorHelp errorHelp = new ErrorHelp();
                errorHelp.setDisplay(jsonObject.optString("display".toUpperCase()));
                errorHelp.setName(jsonObject.optString("name".toUpperCase()));
                errorHelp.setChildIda(jsonObject.optString("childIda".toUpperCase()));
                errorHelp.setChildIdb(jsonObject.optString("childIdb".toUpperCase()));
                errorHelp.setLevel(jsonObject.optString("level".toUpperCase()));
                errorHelp.setProductId(String.valueOf(jsonObject.optInt("productId".toUpperCase())));
                errorHelp.setReason(jsonObject.optString("reason".toUpperCase()));
                errorHelp.setSolution(jsonObject.optString("solution".toUpperCase()));
                errorHelp.setValid(true);
                errorHelp.setLastTime(new Date());
                errorHelp.setDeviceID(deviceID);
                db.save(errorHelp);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
