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
 * Time: 10:29.
 */
public class NICE1000PlusParameter implements ParameterFactory.Parameter {

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
    public List<ParameterStatusItem> getOutputTerminalStateList(boolean[] bitValues, List<ParameterSettings> settingsList) {
        int length = bitValues.length;
        List<ParameterStatusItem> statusList = new ArrayList<ParameterStatusItem>();
        for (ParameterSettings settings : settingsList) {
            int indexValue = ParseSerialsUtils.getIntFromBytes(settings.getReceived());
            if (indexValue >= 0 && indexValue < length) {
                try {
                    JSONArray jsonArray = new JSONArray(settings.getJSONDescription());
                    int size = jsonArray.length();
                    String[] valueStringArray = new String[size];
                    for (int i = 0; i < size; i++) {
                        JSONObject value = jsonArray.getJSONObject(i);
                        valueStringArray[i] = value.optString("id") + ":" + value.optString("value");
                    }
                    if (indexValue < valueStringArray.length) {
                        ParameterStatusItem item = new ParameterStatusItem();
                        item.setName(settings.getName().replace("功能选择", "端子   ")
                                + valueStringArray[indexValue]);
                        item.setStatus(bitValues[indexValue]);
                        item.setName(item.getName().replace("常开/常闭", ""));
                        statusList.add(item);
                    }
                } catch (Exception e) {
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
    public int getWriteInputTerminalValue(int value1, int value2, boolean alwaysOn) {
        if (!alwaysOn) {
            return value1 + 100;
        }
        return value1;
    }

    @Override
    public String getDescriptionText(ParameterSettings settings) {
        int index = ParseSerialsUtils.getIntFromBytes(settings.getReceived());
        // F6-11 ~ F6-60
        if (settings.getCode().contains("F6")) {
            int value = Integer.parseInt(settings.getCode().substring(2));
            if (value >= 11 && value <= 60) {
                try {
                    JSONArray jsonArray = new JSONArray(settings.getJSONDescription());
                    int length = jsonArray.length();
                    for (int m = 0; m < length; m++) {
                        JSONObject object = jsonArray.getJSONObject(m);
                        int itemIndex = object.optInt("id");
                        if (itemIndex == index) {
                            return itemIndex + ":" + object.optString("value");
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            int realIndex = index;
            if (index > 100) {
                index -= 100;
            }
            try {
                JSONArray jsonArray = new JSONArray(settings.getJSONDescription());
                int length = jsonArray.length();
                for (int m = 0; m < length; m++) {
                    JSONObject object = jsonArray.getJSONObject(m);
                    if (index == object.optInt("id")) {
                        return String.valueOf(realIndex) + ":" + object.optString("value");
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return "Parse value failed";
    }

    @Override
    public int getSelectedIndex(ParameterSettings settings) {
        int index = ParseSerialsUtils.getIntFromBytes(settings.getReceived());
        if (settings.getCode().contains("F6")) {
            int value = Integer.parseInt(settings.getCode().substring(2));
            if (value >= 11 && value <= 60) {
                try {
                    JSONArray jsonArray = new JSONArray(settings.getJSONDescription());
                    int length = jsonArray.length();
                    for (int m = 0; m < length; m++) {
                        JSONObject object = jsonArray.getJSONObject(m);
                        if (index == object.optInt("id")) {
                            return m;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }

    @Override
    public int getWriteValue(ParameterSettings settings, int index) {
        if (settings.getCode().contains("F6")) {
            int value = Integer.parseInt(settings.getCode().substring(2));
            if (value >= 11 && value <= 60) {
                try {
                    JSONArray jsonArray = new JSONArray(settings.getJSONDescription());
                    if (index < jsonArray.length()) {
                        JSONObject object = jsonArray.getJSONObject(index);
                        return object.optInt("id");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }

    @Override
    public int getAlwaysCloseValue(int value) {
        return value + 100;
    }

    @Override
    public List<TroubleGroup> getTroubleGroupList(Context context, List<ParameterSettings> settingsList) {
        String[] nameArray = context.getResources().getStringArray(R.array.trouble_group_name);
        List<TroubleGroup> groupList = new ArrayList<TroubleGroup>();
        if (settingsList.size() == 31) {
            for (int m = 0; m < 11; m++) {
                if (m < 10) {
                    TroubleGroup group = new TroubleGroup();
                    group.setName(nameArray[m]);
                    List<ParameterSettings> childList = new ArrayList<ParameterSettings>();
                    for (int n = 0; n < 2; n++) {
                        childList.add(settingsList.get(m * 2 + n));
                    }
                    group.setTroubleChildList(childList);
                    groupList.add(group);
                } else {
                    TroubleGroup group = new TroubleGroup();
                    group.setName(nameArray[nameArray.length - 1]);
                    List<ParameterSettings> childList = new ArrayList<ParameterSettings>();
                    for (int n = 0; n < 6; n++) {
                        childList.add(settingsList.get(n + 20));
                    }
                    group.setTroubleChildList(childList);
                    groupList.add(group);
                }
            }
        }
        return groupList;
    }
}