package com.kio.ElevatorControl.daos;

import android.content.Context;
import android.util.Log;
import com.kio.ElevatorControl.models.ErrorHelp;
import com.kio.ElevatorControl.models.ParameterGroupSettings;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.models.RealtimeMonitor;
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
    private static final boolean DBDEBUG = true;

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
                DBDEBUG);
        int parametersettingssize = db.findAll(ParameterSettings.class).size();
        int parametergroupsettingssize = db.findAll(
                ParameterGroupSettings.class).size();
        int realtimemonitorsize = db.findAll(RealtimeMonitor.class).size();
        int errorhelpsize = db.findAll(ErrorHelp.class).size();
        if (parametersettingssize == 0 || parametergroupsettingssize == 0
                || realtimemonitorsize == 0 || errorhelpsize == 0) {
            return true;
        }
        return false;
    }

    /**
     * 就是恢复出厂设置
     */
    public static void dbInit(Context ctx) {
        restoreFactoryParameterGroupSettings(ctx);
        restoreFactoryRealtimeMonitor(ctx);
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
                DBDEBUG);
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

                JSONObject groupjson = groups.getJSONObject(i);
                ParameterGroupSettings groupentity = new ParameterGroupSettings();
                groupentity.setGroupText(groupjson.optString("groupText"));
                groupentity.setGroupId(groupjson.optString("groupId"));
                groupentity.setValid(true);
                groupentity.setLasttime(new Date());
                // 保存groupentity并且id设置为插入后的值
                db.saveBindId(groupentity);

                JSONArray settingjson = groupjson
                        .getJSONArray("parametersettings");
                // 遍历settings
                for (int j = 0; j < settingjson.length(); j++) {
                    JSONObject stjsn = settingjson.getJSONObject(j);
                    ParameterSettings setting = new ParameterSettings();
                    setting.setCode(stjsn.optString("code"));
                    setting.setName(stjsn.optString("name"));
                    setting.setProductId(String.valueOf(stjsn
                            .optInt("productId")));
                    setting.setDescription(stjsn.optString("description"));
                    setting.setDescriptiontype(ParameterSettings
                            .ParseDescriptionToType(setting.getDescription()));
                    setting.setChildId(stjsn.optString("childId"));
                    setting.setScope(stjsn.optString("scope"));
                    setting.setDefaultValue(String.valueOf(stjsn
                            .optInt("defaultValue")));
                    setting.setScale(String.valueOf(stjsn.optDouble("scale")));
                    setting.setUnit(stjsn.optString("unit"));
                    setting.setType(String.valueOf(stjsn.optInt("type")));
                    setting.setMode(String.valueOf(stjsn.optInt("mode")));
                    setting.setParametergroupsettings(groupentity);
                    // 保存setting
                    db.save(setting);
                }
            }
        } catch (JSONException ex) {
            // 异常处理代码
            Log.e("JSONEXP", ex.getMessage());
        }
    }

    public static void restoreFactoryRealtimeMonitor(Context ctx) {
        // (android:label).db
        FinalDb db = FinalDb.create(ctx,
                ctx.getString(ctx.getApplicationInfo().labelRes) + ".db",
                DBDEBUG);
        db.deleteAll(RealtimeMonitor.class);

        // 读取NICE3000+_State.json
        String JSON = AssetUtils
                .readDefaultFunCode(ctx, "NICE3000+_State.json");
        try {
            JSONArray monitors = new JSONArray(JSON);
            for (int i = 0; i < monitors.length(); i++) {
                JSONObject mtor = monitors.getJSONObject(i);
                RealtimeMonitor mtentity = new RealtimeMonitor();
                mtentity.setName(mtor.optString("name"));
                mtentity.setCode(mtor.optString("code"));
                mtentity.setChildId(mtor.optString("childId"));
                mtentity.setUnit(mtor.optString("unit"));
                mtentity.setDescription(mtor.optString("description"));
                mtentity.setDescriptiontype(RealtimeMonitor
                        .ParseDescriptionToType(mtentity.getDescription()));
                mtentity.setProductId(String.valueOf(mtor.optInt("productId")));
                mtentity.setScale(String.valueOf(mtor.optDouble("scale")));
                mtentity.setScope(mtor.optString("scope"));
                mtentity.setShowBit(Boolean.parseBoolean(mtor
                        .optString("showBit")));
                mtentity.setValid(true);
                mtentity.setLasttime(new Date());
                db.save(mtentity);
            }
        } catch (JSONException ex) {
            // 异常处理代码
            Log.e("JSONEXP", ex.getMessage());
        }
    }

    public static void restoreFactoryErrorHelp(Context ctx) {
        // (android:label).db
        FinalDb db = FinalDb.create(ctx,
                ctx.getString(ctx.getApplicationInfo().labelRes) + ".db",
                DBDEBUG);
        db.deleteAll(ErrorHelp.class);
        // 读取NICE3000+_State.json
        String JSON = AssetUtils.readDefaultFunCode(ctx,
                "NICE3000+_ErrHelp.json");
        try {
            JSONArray errhelplist = new JSONArray(JSON);
            for (int i = 0; i < errhelplist.length(); i++) {
                JSONObject errhp = errhelplist.getJSONObject(i);
                ErrorHelp epobj = new ErrorHelp();
                epobj.setDisplay(errhp.optString("display"));
                epobj.setName(errhp.optString("name"));
                epobj.setChildIda(errhp.optString("childIda"));
                epobj.setChildIdb(errhp.optString("childIdb"));
                epobj.setLevel(errhp.optString("level"));
                epobj.setProductId(String.valueOf(errhp.optInt("productId")));
                epobj.setReason(errhp.optString("reason"));
                epobj.setSolution(errhp.optString("solution"));
                epobj.setValid(true);
                epobj.setLasttime(new Date());
                db.save(epobj);
            }
        } catch (JSONException ex) {
            // 异常处理代码
            Log.e("JSONEXP", ex.getMessage());
        }

    }

}
