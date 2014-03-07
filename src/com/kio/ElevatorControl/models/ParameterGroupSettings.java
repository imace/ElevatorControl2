package com.kio.ElevatorControl.models;

import java.util.Date;

import com.kio.ElevatorControl.R;
import com.mobsandgeeks.adapters.InstantText;

import net.tsz.afinal.annotation.sqlite.Id;
import net.tsz.afinal.annotation.sqlite.OneToMany;
import net.tsz.afinal.db.sqlite.OneToManyLazyLoader;

/**
 * 参数功能组
 * @author jch
 *
 */
public class ParameterGroupSettings {
	@Id
	private int Id;
	
	private String groupText;//功能组
	private String groupId;//功能组Id
	
	private boolean Valid;
	private Date lasttime;
	
	
	@OneToMany(manyColumn = "FKGroupId")
    private OneToManyLazyLoader<ParameterGroupSettings ,ParameterSettings> parametersettings;


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


	public Date getLasttime() {
		return lasttime;
	}


	public void setLasttime(Date lasttime) {
		this.lasttime = lasttime;
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
	
}
