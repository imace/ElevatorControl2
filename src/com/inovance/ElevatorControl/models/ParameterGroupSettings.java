package com.inovance.elevatorcontrol.models;

import com.inovance.elevatorcontrol.R;
import com.mobsandgeeks.adapters.InstantText;
import net.tsz.afinal.annotation.sqlite.Id;
import net.tsz.afinal.annotation.sqlite.OneToMany;
import net.tsz.afinal.annotation.sqlite.Table;
import net.tsz.afinal.annotation.sqlite.Transient;
import net.tsz.afinal.db.sqlite.OneToManyLazyLoader;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

/**
 * 参数功能组
 *
 * @author jch
 */
@Table(name = "PARAMETER_GROUP_SETTINGS")
public class ParameterGroupSettings {
    @Id
    private int Id;

    private String groupText;//功能组

    private String groupId;//功能组Id

    @Transient
    private boolean Valid;

    @Transient
    private Date lastTime;

    private int deviceID;

    public int getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(int deviceID) {
        this.deviceID = deviceID;
    }

    private List<ParameterSettings> settingsList;

    @OneToMany(manyColumn = "FKGroupId")
    private OneToManyLazyLoader<ParameterGroupSettings, ParameterSettings> parametersettings;

    public ParameterGroupSettings() {

    }

    public ParameterGroupSettings(JSONObject object) {
        this.groupId = object.optString("groupId".toUpperCase());
        this.groupText = object.optString("groupText".toUpperCase());
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public boolean isValid() {
        return Valid;
    }

    public void setValid(boolean valid) {
        Valid = valid;
    }

    public Date getLastTime() {
        return lastTime;
    }

    public void setLastTime(Date lastTime) {
        this.lastTime = lastTime;
    }

    public OneToManyLazyLoader<ParameterGroupSettings, ParameterSettings> getParametersettings() {
        return parametersettings;
    }

    public void setParametersettings(
            OneToManyLazyLoader<ParameterGroupSettings, ParameterSettings> parametersettings) {
        this.parametersettings = parametersettings;
    }

    @InstantText(viewId = R.id.text_transaction)
    public String getGroupText() {
        return groupText;
    }

    public void setGroupText(String groupText) {
        this.groupText = groupText;
    }

    public List<ParameterSettings> getSettingsList() {
        return settingsList;
    }

    public void setSettingsList(List<ParameterSettings> settingsList) {
        this.settingsList = settingsList;
    }

}
