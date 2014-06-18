package com.inovance.elevatorprogram;

import com.inovance.protocol.BluetoothService;
import com.inovance.protocol.Protocol;

import android.os.Handler;

public class SysPara {
	public Handler hWnd;            	//句柄
	public byte uStation; 				//站号
	public int uPort;    				//端口号
	public int uBaud;    				//波特率
	public boolean bRun;             	//烧录完成运行程序
	public boolean bMaster;          	//是否有权限烧写iap

	public BluetoothService BtCom = null;   	//蓝牙服务类
	public Protocol protocol = null;    		//协议类
	public CipherBinFile bin = null;    		//烧录文件
	
	public SysPara(BluetoothService btCom)
	{
		protocol = new Protocol();
		bin = new CipherBinFile();
		BtCom = btCom;
	}

}
