package com.inovance.protocol;

import com.inovance.elevatorprogram.ParamPass;

public class AppCmd {
	public static final int APPCMD_BASE = 0x100;
	public static final int APPCMD_READ_FAIL = APPCMD_BASE + 1;    	//读命令失败
	public static final int APPCMD_WRITE_FAIL = APPCMD_BASE + 2;    //写命令失败
	public static final int APPCMD_ERROR_0x80 = APPCMD_BASE + 3;    //返回0x80错误
	public static final int APPCMD_NOT_READY = APPCMD_BASE + 4;    	//设备未准备好
	public static final int APPCMD_RUNNING = APPCMD_BASE + 5;    	//设备正在运行
	public static final int APPCMD_PIN_NOT_USE = APPCMD_BASE + 6;   //boot引脚未使用 
	public static final int APPCMD_ERROR_CHECK =APPCMD_BASE + 7;    //返回帧校验错误
	
	public static final int WM_RESETREADNUM = 5;
	
	protected int m_dwErrCode = 0;
	
	 //查询设备运行状态
	public boolean GetMachineState(BluetoothService BtCom, byte uSt, Integer uState)
	{
		int index = 0x0004; //信息索引
		int len   = 0x1;    //读取信息长度
		byte[] data = new byte[2];        //读取的信息

	    if (!Cmd_0x45(BtCom,uSt,index,len,data,100))
		{
			return false;
		}

		uState = data[0] & 0xff;
		if (uState == 1 || uState == 2)
		{
			m_dwErrCode = APPCMD_RUNNING;
			return false;
		}

		m_dwErrCode = 0;
		return true;
	}
	
	//检测是否可以烧录
	public boolean SafeGuard(BluetoothService BtCom, byte uSt, Integer uState)
	{
		return true;
	}
	
	//获取bootloader版本号
	public boolean GetBootLoaderVersion(BluetoothService BtCom, byte uSt, ParamPass uVer)
	{
		byte index = 0x0004; //信息索引
		byte len   = 0x1;    //读取信息长度
		byte[] data = new byte[2];        //读取的信息

		if (!Cmd_0x45(BtCom,uSt,index,len,data,100))
		{
			return false;
		}

		uVer.uVer = data[1] & 0xff;

		m_dwErrCode = 0;
		return true;
	}
	 
	//跳转到boot区
	public boolean JumpToBoot(BluetoothService BtCom, byte uSt, Integer uState)
	{
		int index = 0x0005; //信息索引
		int len   = 0x1;    //读取的信息长度
		byte[] data = new byte[2];        //读取的信息

		if (!Cmd_0x45(BtCom,uSt,index,len,data, 100))
		{
			if (m_dwErrCode == APPCMD_ERROR_0x80)
			{
				if (data[0] == 4)
					m_dwErrCode=APPCMD_NOT_READY;
				else if (data[0] == 5)
					m_dwErrCode=APPCMD_PIN_NOT_USE;
			}
			return false;
		}

		m_dwErrCode = 0;
		return true;
	}
	
	//查询App电子标签
	public boolean GetAppID(BluetoothService BtCom, byte uSt, ParamPass param)
	{
		int index  = 0x0010; //信息索引
		int len    = 0x20;   //读取信息长度
		byte[] data = new byte[255];    //读取的信息

		if (!Cmd_0x45(BtCom,uSt,index,len,data,200))
		{
			return false;
		}

		//返回值
		//uPLine = 
		param.uAppPLine  = (data[2]<<8) | data[3];
		param.uAppPType  = (data[4]<<8) | data[5];
		param.uAppIC    = (data[12]<<8) | data[13];
		
		m_dwErrCode = 0;
		return true;
	}
	
	/*
	 * 获取crc
	 */
	protected int GetCrc16(byte[] pData, int uSize)
	{
		return CRC16.GetCrc16(pData,uSize);
	}
	
	public int GetErrorCode()
	{
		return m_dwErrCode;
	}
	
	 //自定义信息读取
	private boolean Cmd_0x45(BluetoothService BtCom, byte uSt, int index, int len, byte[] pData, int timeout)
	{
		// 命令帧
		byte[] buf = new byte[8];
		buf[0] = uSt;
		buf[1] = 0x45;
		buf[2] = (byte)((index >> 8) & 0xff);
		buf[3] = (byte)(index & 0xff);
		buf[4] = (byte)((len >> 8) & 0xff);
		buf[5] = (byte)(len & 0xff);
		int crc = GetCrc16(buf,6);
		buf[6] = (byte)(crc & 0xff);
		buf[7] = (byte)((crc >> 8) & 0xff);

		//发送
		if(!BtCom.write(buf))
		{
			m_dwErrCode=APPCMD_WRITE_FAIL;
			return false;
		}
		//延时读串口数据
		int iBackFrmSize = 1 + 1 + 1 + 2*len + 2; //正常返回帧的长度
		byte[] pBackFrm = new byte[iBackFrmSize];
		//byte[] bufferTemp = new byte[1024];
		Wait(BtCom,iBackFrmSize,timeout);//延时读数据
		int nReadSize = BtCom.read(pBackFrm);
//		//nReadSize = BtCom.QueryBuffer();//BtCom.read(bufferTemp);
//			if(nReadSize == iBackFrmSize || nReadSize != 0)
//				BtCom.read(pBackFrm);

		
		//int nReadSize = com.ReadData(pBackFrm,iBackFrmSize);
		if ((nReadSize == 0) || ((nReadSize != iBackFrmSize) && (nReadSize != 5)))
		{
			m_dwErrCode=APPCMD_READ_FAIL;
			return false;
		}
		if (GetCrc16(pBackFrm,nReadSize) != 0)
		{
			m_dwErrCode=APPCMD_ERROR_CHECK;
			return false;
		}
		if ((pBackFrm[1] & 0x80) == 0x80)
		{
			m_dwErrCode=APPCMD_ERROR_0x80;
			pData[0]=pBackFrm[2]; // 错误码
			return false;
		}
		//正确的数据
		for (int i=0; i<len*2; i++)
		{
			pData[i] = pBackFrm[3+i];
		}
		return true;
	}
	
	private void Wait(BluetoothService BtCom, int size, final int timeout) {
		long uStart = System.currentTimeMillis();
		while ((System.currentTimeMillis() - uStart) < timeout) {
			int nCount = BtCom.QueryBuffer();
			if (nCount >= size)
				break;
			// Sleep(1);
		}
	}
}
