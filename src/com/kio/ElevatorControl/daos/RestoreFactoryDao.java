package com.kio.ElevatorControl.daos;

import android.content.Context;
import android.util.Log;
import com.kio.ElevatorControl.models.ErrorHelp;
import com.kio.ElevatorControl.models.ParameterGroupSettings;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.models.RealTimeMonitor;
import com.kio.ElevatorControl.utils.AssetUtils;
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
public class RestoreFactoryDao {

    private static final boolean DEBUG = true;

    /**
     * 记录是否为空
     *
     * @param ctx
     * @return
     */
    public static boolean dbEmpty(Context ctx) {
        // (android:label).db
        FinalDb db = FinalDb.create(ctx,
                ctx.getString(ctx.getApplicationInfo().labelRes) + ".db",
                DEBUG);
        int parameterSettingsSize = db.findAll(ParameterSettings.class).size();
        int parameterGroupSettingsSize = db.findAll(
                ParameterGroupSettings.class).size();
        int realTimeMonitorSize = db.findAll(RealTimeMonitor.class).size();
        int errorHelpSize = db.findAll(ErrorHelp.class).size();
        if (parameterSettingsSize == 0 || parameterGroupSettingsSize == 0
                || realTimeMonitorSize == 0 || errorHelpSize == 0) {
            return true;
        }
        return false;
    }

    /**
     * 就是恢复出厂设置
     */
    public static void dbInit(Context ctx) {
        restoreFactoryParameterGroupSettings(ctx);
        restoreFactoryRealTimeMonitor(ctx);
        restoreFactoryErrorHelp(ctx);
    }

    /**
     * 恢复ParameterGroupSettings的出厂设置
     *
     * @param ctx
     */
    public static void restoreFactoryParameterGroupSettings(Context ctx) {
        // (android:label).db
        FinalDb db = FinalDb.create(ctx,
                ctx.getString(ctx.getApplicationInfo().labelRes) + ".db",
                DEBUG);
        // 清理数据
        db.deleteAll(ParameterSettings.class);
        db.deleteAll(ParameterGroupSettings.class);

        // 读取NICE3000+_FunCode.json
        String JSON = AssetUtils.readDefaultFunCode(ctx,
                "NICE3000+_FunCode.json");

        // 解析出厂设置
        try {
            JSONArray groups = new JSONArray(JSON);
            // 遍历group
            for (int i = 0; i < groups.length(); i++) {

                JSONObject groupsJSONObject = groups.getJSONObject(i);
                ParameterGroupSettings parameterGroupSetting = new ParameterGroupSettings();
                parameterGroupSetting.setGroupText(groupsJSONObject.optString("groupText"));
                parameterGroupSetting.setGroupId(groupsJSONObject.optString("groupId"));
                parameterGroupSetting.setValid(true);
                parameterGroupSetting.setLasttime(new Date());
                // 保存groupEntity并且id设置为插入后的值
                db.saveBindId(parameterGroupSetting);

                String groupID = groupsJSONObject.optString("groupId");

                JSONArray settingJson = groupsJSONObject
                        .getJSONArray("parameterSettings");
                // 遍历settings
                for (int j = 0; j < settingJson.length(); j++) {
                    JSONObject jsonObject = settingJson.getJSONObject(j);
                    ParameterSettings parameterSetting = new ParameterSettings();
                    parameterSetting.setCode(jsonObject.optString("code"));
                    parameterSetting.setName(jsonObject.optString("name"));
                    parameterSetting.setProductId(String.valueOf(jsonObject.optInt("productId")));
                    parameterSetting.setDescription(jsonObject.optString("description"));
                    parameterSetting.setDescriptiontype(ParameterSettings
                            .ParseDescriptionToType(parameterSetting.getDescription()));
                    parameterSetting.setChildId(jsonObject.optString("childId"));
                    parameterSetting.setScope(jsonObject.optString("scope"));
                    parameterSetting.setDefaultValue(String.valueOf(jsonObject.optInt("defaultValue")));
                    parameterSetting.setScale(String.valueOf(jsonObject.optDouble("scale")));
                    parameterSetting.setUnit(jsonObject.optString("unit"));
                    parameterSetting.setType(String.valueOf(jsonObject.optInt("type")));
                    parameterSetting.setMode(String.valueOf(jsonObject.optInt("mode")));
                    parameterSetting.setParametergroupsettings(parameterGroupSetting);
                    // 保存setting
                    db.save(parameterSetting);
                }
            }
        } catch (JSONException ex) {
            // 异常处理代码
            Log.e("JSONException", ex.getMessage());
        }
    }

    public static void restoreFactoryRealTimeMonitor(Context ctx) {
        // (android:label).db
        FinalDb db = FinalDb.create(ctx,
                ctx.getString(ctx.getApplicationInfo().labelRes) + ".db",
                DEBUG);
        db.deleteAll(RealTimeMonitor.class);

        // 读取NICE3000+_State.json
        String JSON = AssetUtils
                .readDefaultFunCode(ctx, "NICE3000+_State.json");
        try {
            JSONArray monitors = new JSONArray(JSON);
            for (int i = 0; i < monitors.length(); i++) {
                JSONObject jsonObject = monitors.getJSONObject(i);
                RealTimeMonitor realTimeMonitor = new RealTimeMonitor();
                realTimeMonitor.setName(jsonObject.optString("name"));
                realTimeMonitor.setCode(jsonObject.optString("code"));
                realTimeMonitor.setChildId(jsonObject.optString("childId"));
                realTimeMonitor.setUnit(jsonObject.optString("unit"));
                realTimeMonitor.setType(jsonObject.optString("type"));
                realTimeMonitor.setDescription(jsonObject.optString("description"));
                realTimeMonitor.setDescriptionType(RealTimeMonitor
                        .ParseDescriptionToType(realTimeMonitor.getDescription()));
                realTimeMonitor.setProductId(String.valueOf(jsonObject.optInt("productId")));
                realTimeMonitor.setScale(String.valueOf(jsonObject.optDouble("scale")));
                realTimeMonitor.setScope(jsonObject.optString("scope"));
                realTimeMonitor.setShowBit(Boolean.parseBoolean(jsonObject.optString("showBit")));
                realTimeMonitor.setValid(true);
                realTimeMonitor.setLastTime(new Date());
                db.save(realTimeMonitor);
            }
        } catch (JSONException ex) {
            // 异常处理代码
            Log.e("JSONException", ex.getMessage());
        }
    }

    public static void restoreFactoryErrorHelp(Context ctx) {
        // (android:label).db
        FinalDb db = FinalDb.create(ctx,
                ctx.getString(ctx.getApplicationInfo().labelRes) + ".db",
                DEBUG);
        db.deleteAll(ErrorHelp.class);
        // 读取NICE3000+_State.json
        String JSON = AssetUtils.readDefaultFunCode(ctx,
                "NICE3000+_ErrHelp.json");
        try {
            JSONArray errHelpList = new JSONArray(JSON);
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
                db.save(errorHelp);
            }
        } catch (JSONException ex) {
            // 异常处理代码
            Log.e("JSONException", ex.getMessage());
        }

    }

}
