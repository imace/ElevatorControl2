package com.inovance.elevatorcontrol.factory;

import android.content.Context;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.models.ParameterSettings;
import com.inovance.elevatorcontrol.models.ParameterStatusItem;
import com.inovance.elevatorcontrol.models.TroubleGroup;
import com.inovance.elevatorcontrol.utils.ParseSerialsUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-8-14.
 * Time: 10:31.
 */
public class NICE1000Parameter implements ParameterFactory.Parameter {

    @Override
    public List<ParameterStatusItem> getInputTerminalStateList(boolean[] bitValues, List<ParameterSettings> settingsList) {
        // 0保留，>100为常闭
        // BIT值为0表示常闭，1表示常开
        int length = bitValues.length;
        List<ParameterStatusItem> statusList = new ArrayList<ParameterStatusItem>();
        for (ParameterSettings settings : settingsList) {
            int indexValue = ParseSerialsUtils.getIntFromBytes(settings.getReceived());
            int tempIndex = indexValue;
            if (indexValue > 100) {
                indexValue -= 100;
            }
            if (indexValue < length && indexValue >= 0) {
                try {
                    JSONArray jsonArray = new JSONArray(settings.getJSONDescription());
                    int size = jsonArray.length();
                    String[] valueStringArray = new String[size];
                    for (int i = 0; i < size; i++) {
                        JSONObject value = jsonArray.getJSONObject(i);
                        valueStringArray[i] = tempIndex + ":" + value.optString("value");
                    }
                    if (indexValue < valueStringArray.length) {
                        ParameterStatusItem item = new ParameterStatusItem();
                        item.setName(settings.getName().replace("功能选择", "端子   ")
                                + valueStringArray[indexValue]);
                        item.setStatus(bitValues[indexValue]);
                        item.setName(item.getName().replace("常开/常闭", ""));
                        statusList.add(item);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return statusList;
    }

    @Override
    public int[] getIndexStatus(ParameterSettings settings) {
        int state = 1;
        int indexValue = ParseSerialsUtils.getIntFromBytes(settings.getReceived());
        if (indexValue > 100) {
            indexValue -= 100;
            state = 0;
        }
        return new int[]{indexValue, state};
    }

    @Override
    public String getDescriptionText(ParameterSettings settings) {
        int index = ParseSerialsUtils.getIntFromBytes(settings.getReceived());
        int realIndex = index;
        if (index > 100) {
            index -= 100;
        }
        try {
            JSONArray jsonArray = new JSONArray(settings.getJSONDescription());
            if (index < jsonArray.length()) {
                JSONObject object = jsonArray.getJSONObject(index);
                return String.valueOf(realIndex) + ":" + object.optString("value");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "Parse value failed";
    }

    @Override
    public List<TroubleGroup> getTroubleGroupList(Context context, List<ParameterSettings> settingsList) {
        String[] nameArray = context.getResources().getStringArray(R.array.trouble_group_name);
        List<TroubleGroup> groupList = new ArrayList<TroubleGroup>();
        if (settingsList.size() == 9) {
            for (int m = 0; m < 6; m++) {
                if (m < 5) {
                    TroubleGroup group = new TroubleGroup();
                    group.setName(nameArray[m]);
                    List<ParameterSettings> childList = new ArrayList<ParameterSettings>();
                    childList.add(settingsList.get(m * 2));
                    group.setTroubleChildList(childList);
                    groupList.add(group);
                } else {
                    TroubleGroup group = new TroubleGroup();
                    group.setName(nameArray[nameArray.length - 1]);
                    List<ParameterSettings> childList = new ArrayList<ParameterSettings>();
                    for (int n = 0; n < 4; n++) {
                        childList.add(settingsList.get(n + 5));
                    }
                    group.setTroubleChildList(childList);
                    groupList.add(group);
                }
            }
        }
        return groupList;
    }
}
