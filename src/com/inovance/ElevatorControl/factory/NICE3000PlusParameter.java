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
 * Time: 10:32.
 */
public class NICE3000PlusParameter implements ParameterFactory.Parameter {

    @Override
    public List<ParameterStatusItem> getInputTerminalStateList(boolean[] bitValues, List<ParameterSettings> settingsList) {
        // 32~63为常闭，96~127为常闭
        // BIT值为0表示常闭，1表示常开
        int length = bitValues.length;
        List<ParameterStatusItem> statusList = new ArrayList<ParameterStatusItem>();
        for (ParameterSettings settings : settingsList) {
            int indexValue = ParseSerialsUtils.getIntFromBytes(settings.getReceived());
            int tempIndex = indexValue;
            String openStatus = "";
            if (indexValue > 31 && indexValue < 64) {
                openStatus = "(常闭)";
                indexValue -= 32;
            } else if (indexValue >= 96 && indexValue < 128) {
                openStatus = "(常闭)";
                indexValue -= 32;
            } else {
                openStatus = "(常开)";
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
                                + valueStringArray[indexValue] + openStatus);
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
        int state;
        int indexValue = ParseSerialsUtils.getIntFromBytes(settings.getReceived());
        if (indexValue > 31 && indexValue < 64) {
            indexValue -= 32;
            state = 0;
        } else if (indexValue >= 96 && indexValue < 128) {
            indexValue -= 32;
            state = 0;
        } else {
            state = 1;
        }
        return new int[]{indexValue, state};
    }

    @Override
    public String getDescriptionText(ParameterSettings settings) {
        int index = ParseSerialsUtils.getIntFromBytes(settings.getReceived());
        int realIndex = index;
        if (index > 31 && index < 64) {
            index -= 32;
        } else if (index >= 96 && index < 128) {
            index -= 32;
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
        if (settingsList.size() == 53) {
            for (int m = 0; m < 11; m++) {
                if (m < 10) {
                    TroubleGroup group = new TroubleGroup();
                    group.setName(nameArray[m]);
                    List<ParameterSettings> childList = new ArrayList<ParameterSettings>();
                    for (int n = 0; n < 4; n++) {
                        childList.add(settingsList.get(m * 4 + n));
                    }
                    group.setTroubleChildList(childList);
                    groupList.add(group);
                } else {
                    TroubleGroup group = new TroubleGroup();
                    group.setName(nameArray[nameArray.length - 1]);
                    List<ParameterSettings> childList = new ArrayList<ParameterSettings>();
                    for (int n = 0; n < 13; n++) {
                        childList.add(settingsList.get(n + 40));
                    }
                    group.setTroubleChildList(childList);
                    groupList.add(group);
                }
            }
        }
        return groupList;
    }
}