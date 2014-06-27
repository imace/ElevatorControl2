package com.inovance.ElevatorControl.daos;

import android.content.Context;
import android.util.Log;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.config.ConfigFactory;
import com.inovance.ElevatorControl.models.*;
import com.inovance.ElevatorControl.utils.AssetUtils;
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
        Log.v("ParameterFactoryDao", parameterSettingsSize + ":" + parameterGroupSettingsSize + ":" + realTimeMonitorSize + ":" + errorHelpSize);
        return parameterSettingsSize == 0 || parameterGroupSettingsSize == 0
                || realTimeMonitorSize == 0 || errorHelpSize == 0;
    }

    /**
     * 就是恢复出厂设置
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
        ConfigFactory.getInstance().init(context);
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
                settingsGroup.setGroupText(groupsJSONObject.optString("groupText"));
                settingsGroup.setGroupId(groupsJSONObject.optString("groupId"));
                settingsGroup.setValid(true);
                settingsGroup.setLasttime(new Date());
                settingsGroup.setDeviceID(deviceID);
                // 保存groupEntity并且id设置为插入后的值
                db.saveBindId(settingsGroup);
                String groupID = groupsJSONObject.optString("groupId");
                JSONArray settingJson = groupsJSONObject.getJSONArray("parameterSettings");
                // 遍历settings
                int length = settingJson.length();
                for (int j = 0; j < length; j++) {
                    JSONObject jsonObject = settingJson.getJSONObject(j);
                    ParameterSettings settings = new ParameterSettings();
                    settings.setCode(jsonObject.optString("code").replace("-", ""));
                    settings.setName(jsonObject.optString("name"));
                    settings.setProductId(jsonObject.optString("productId"));
                    settings.setDescription(jsonObject.optString("description"));
                    settings.setDescriptionType(ParameterSettings.ParseDescriptionToType(settings.getDescription()));
                    settings.setJSONDescription(ParameterSettings.GenerateJSONDescription(settings.getDescription()));
                    settings.setChildId(jsonObject.optString("childId"));
                    settings.setScope(jsonObject.optString("scope").replaceAll("-", "").replace("～", "~"));
                    settings.setDefaultValue(jsonObject.optString("defaultValue"));
                    settings.setScale(jsonObject.optString("scale"));
                    settings.setUnit(jsonObject.optString("unit"));
                    settings.setType(jsonObject.optString("type"));
                    settings.setMode(jsonObject.optString("mode"));
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
                state.setName(jsonObject.optString("name"));
                state.setCode(jsonObject.optString("code").replace("-", ""));
                state.setChildId(jsonObject.optString("childId"));
                state.setUnit(jsonObject.optString("unit"));
                state.setDescription(jsonObject.optString("description"));
                state.setProductId(jsonObject.optString("productId"));
                state.setScale(jsonObject.optString("scale"));
                state.setScope(jsonObject.optString("scope"));
                state.setStateID(jsonObject.optInt("stateID"));
                state.setSort(jsonObject.optInt("sort"));
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
                errorHelp.setDisplay(jsonObject.optString("display"));
                errorHelp.setName(jsonObject.optString("name"));
                errorHelp.setChildIda(jsonObject.optString("childIda"));
                errorHelp.setChildIdb(jsonObject.optString("childIdb"));
                errorHelp.setLevel(jsonObject.optString("level"));
                errorHelp.setProductId(String.valueOf(jsonObject.optInt("productId")));
                errorHelp.setReason(jsonObject.optString("reason"));
                errorHelp.setSolution(jsonObject.optString("solution"));
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
