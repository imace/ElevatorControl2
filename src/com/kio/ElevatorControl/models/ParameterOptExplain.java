package com.kio.ElevatorControl.models;

import net.tsz.afinal.annotation.sqlite.Id;
import net.tsz.afinal.annotation.sqlite.ManyToOne;


/**
 * ����ѡ��˵��
 * @author jch
 */
public class ParameterOptExplain {
	@Id
	private int Id;
	private String key;
	private String value;
	
	@ManyToOne(column = "OptExplainId")
    private  ParameterSettings  parametersettings;
	
	
	public int getId() {
		return Id;
	}
	public void setId(int id) {
		Id = id;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
}
