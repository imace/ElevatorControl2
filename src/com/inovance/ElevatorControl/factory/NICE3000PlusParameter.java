package com.inovance.ElevatorControl.factory;

import com.inovance.ElevatorControl.models.ParameterSettings;
import com.inovance.ElevatorControl.models.ParameterStatusItem;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
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
            String openStatus = "(常开)";
            if (indexValue > 31 && indexValue < 64) {
                openStatus = "(常闭)";
                indexValue -= 32;
            } else if (indexValue >= 64 && indexValue < 96) {
                openStatus = "(常开)";
                indexValue -= 32;
            } else if (indexValue >= 96) {
                openStatus = "(常闭)";
                indexValue -= 64;
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
        int state = 1;
        int indexValue = ParseSerialsUtils.getIntFromBytes(settings.getReceived());
        if (indexValue > 31 && indexValue < 64) {
            indexValue -= 32;
            state = 0;
        } else if (indexValue >= 64 && indexValue < 96) {
            indexValue -= 32;
            state = 0;
        } else if (indexValue >= 96) {
            indexValue -= 64;
            state = 0;
        }
        return new int[]{indexValue, state};
    }
}