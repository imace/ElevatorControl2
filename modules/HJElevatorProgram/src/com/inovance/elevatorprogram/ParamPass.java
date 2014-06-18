package com.inovance.elevatorprogram;

//参数传递类
public class ParamPass {
	public byte uByteRet = 0;	//命令字
	public int uAppPLine = 0;	//产品线
	public int uAppPType = 0;	//产品线类型
	public int uAppIC = 0;		//芯片
	public boolean bBurn = false;//烧录状态
	public int uState = 0;		//状态
	public int uVer = 0;	//版本号
	
	public boolean bInBoot = false;
	public String strErr= "";
}
