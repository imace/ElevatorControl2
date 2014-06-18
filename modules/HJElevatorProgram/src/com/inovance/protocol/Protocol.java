package com.inovance.protocol;

import java.util.Arrays;

import com.inovance.elevatorprogram.ParamPass;

public class Protocol {
	// error code
	public static final int PROTOCOL_BASE = 0x200;
	public static final int PROTOCOL_READ_FAIL = PROTOCOL_BASE + 1; // 读命令无回应
	public static final int PROTOCOL_WRITE_FAIL = PROTOCOL_BASE + 2; // 写命令无回应
	public static final int PROTOCOL_CRC_CHECK_ERROR = PROTOCOL_BASE + 3; // CRC校验错误
	public static final int PROTOCOL_ARM_RMRROTECTION_FAIL = PROTOCOL_BASE + 4; // arm芯片解保护失败
	public static final int PROTOCOL_ARM_UNKNOW_ACK = PROTOCOL_BASE + 5; // arm芯片未定义的应答
	public static final int PROTOCOL_ARM_RMPROTECTION_NO = PROTOCOL_BASE + 6; // arm芯片在保护状态且没有解保护权限
	public static final int PROTOCOL_RECV_CMD_ERROR   =      PROTOCOL_BASE + 7;  //返回帧命令字不同于发送帧命令字

	public static final int PROTOCOL_BOOT_BASE = 0x300;

	private AppCmd m_ModbusApp = null;
	private int m_dwErrCode = 0;

	public Protocol() {
		m_ModbusApp = new AppCmd();
	}

	public int GetErrorCode() {
		return m_dwErrCode;
	}

	public boolean Ping(BluetoothService BtCom, ParamPass u) {
		byte[] Buffer = new byte[1];
		Buffer[0] = u.uByteRet;
		// 发送
		if (!BtCom.write(Buffer)) {
			m_dwErrCode = PROTOCOL_WRITE_FAIL;
			return false;
		}
		// 延时读串口数据
		Wait(BtCom, 1, 300);
		// byte[] by= new byte[1024];
		// int n = BtCom.read(by);
		if (ReadData(BtCom, Buffer, 1) != 1) {
			m_dwErrCode = PROTOCOL_READ_FAIL;
			return false;
		}
		m_dwErrCode = 0;
		u.uByteRet = Buffer[0];
		return true;
	}

	/*
	 * 获取设备当前运行状态
	 */
	public boolean GetMachineState(BluetoothService BtCom, byte uSt,
			Integer uState) {
		boolean ret = m_ModbusApp.GetMachineState(BtCom, uSt, uState);
		m_dwErrCode = m_ModbusApp.GetErrorCode();
		return ret;
	}

	/*
	 * 检测是否可以烧录
	 */
	public boolean SafeGuard(BluetoothService BtCom, byte uSt, Integer uState) {
		// 建立重发机制
		// const int REPEAT = 3;
		// int iTimes = 0;
		// while (iTimes++ < REPEAT)
		// {
		boolean ret = m_ModbusApp.SafeGuard(BtCom, uSt, uState);
		m_dwErrCode = m_ModbusApp.GetErrorCode();
		return ret;

		// if (ret)
		// {
		// return true;
		// }
		// else
		// {
		// if (m_dwErrCode==APPCMD_READ_FAIL) // 没有回应，需要重发
		// {
		// }
		// else
		// {
		// return false;
		// }
		// }
		// }

		// return false;
	}

	/*
	 * 获取DSP官方bootloader版本号
	 */
	public boolean GetBootLoaderVersion(BluetoothService BtCom, byte uSt,
			ParamPass uVer) {
		// 建立重发机制
		// const int REPEAT = 3;
		// int iTimes = 0;
		// while (iTimes++ < REPEAT)
		// {
		boolean ret = m_ModbusApp.GetBootLoaderVersion(BtCom, uSt, uVer);
		m_dwErrCode = m_ModbusApp.GetErrorCode();
		return ret;

		// if (ret)
		// {
		// return true;
		// }
		// else
		// {
		// if (m_dwErrCode==APPCMD_READ_FAIL) // 没有回应，需要重发
		// {
		// }
		// else
		// {
		// return false;
		// }
		// }
		// }

		// return false;
	}

	/*
	 * 跳转到boot区
	 */
	public boolean JumpToBoot(BluetoothService BtCom, byte uSt, Integer uState) {
		// 建立重发机制
		// const int REPEAT = 3;
		// int iTimes = 0;
		// while (iTimes++ < REPEAT)
		// {
		boolean ret = m_ModbusApp.JumpToBoot(BtCom, uSt, uState);
		m_dwErrCode = m_ModbusApp.GetErrorCode();
		return ret;

		// if (ret)
		// {
		// return true;
		// }
		// else
		// {
		// if (m_dwErrCode==APPCMD_READ_FAIL) // 没有回应，需要重发
		// {
		// }
		// else
		// {
		// return false;
		// }
		// }
		// }

		// return false;
	}

	/*
	 * 设置升级的芯片号
	 */
	public boolean SetHost(BluetoothService BtCom, byte uSt, int uHost) {
		byte[] Para = new byte[4];
		Para[0] = 0;
		Para[1] = (byte) (uHost & 0xff);
		Para[2] = 0;
		Para[3] = 0;
		byte[] ret = new byte[4];

		if (!BootCmd(BtCom, uSt, (byte) 0x63, Para, 8, ret, 200)) {
			return false;
		}
		m_dwErrCode = 0;
		return true;
	}

	// ****************************************************************
	// 函数名称: InBootCmd
	// 函数功能: 通过读boot区电子标签命令判断是否停留在boot区
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSeriesPort & com
	// 函数参数: byte uSt
	// 作 者: 李正伟 马俊移植
	// 日 期: 2013/09/29
	// ****************************************************************
	private boolean InBootCmd(BluetoothService BtCom, byte uSt) {
		// 读boot区电子标签
		byte[] pBuf = new byte[8];
		// memset(pBuf,0,sizeof(pBuf));
		pBuf[0] = uSt;
		pBuf[1] = 0x62;
		int crc = GetCrc16(pBuf, 6);
		pBuf[6] = (byte) (crc & 0xff);
		pBuf[7] = (byte) ((crc >> 8) & 0xff);
		// 发送
		if (!BtCom.write(pBuf)) {
			m_dwErrCode = PROTOCOL_WRITE_FAIL;
			return false;
		}
		final int frmSize = 20; // 返回的boot区电子标签帧长度
		// 查询串口缓冲区数据是否接收完成
		Wait(BtCom, frmSize, 100);
		// 读数据
		byte[] recv = new byte[frmSize];
		return JudgeRecv(BtCom,pBuf[1], frmSize, recv);
//		if (ReadData(BtCom, recv, frmSize) != frmSize) {
//			m_dwErrCode = PROTOCOL_READ_FAIL;
//			return false;
//		}
//		if (GetCrc16(recv, frmSize) != 0) {
//			m_dwErrCode = PROTOCOL_CRC_CHECK_ERROR;
//			return false;
//		}
//		if ((recv[1] & 0x80) == 0x80) {
//			m_dwErrCode = PROTOCOL_BOOT_BASE + recv[2];
//			return false;
//		}
//
//		return true;
	}

	// ****************************************************************
	// 函数名称: InBoot
	// 函数功能: 判断底层程序是否停留在boot区
	// 访问属性: public
	// 返回值: boolean
	// 函数参数: CSeriesPort & com
	// 函数参数: byte uSt
	// 函数参数: unsigned int timeout
	// 作 者: 李正伟 马俊移植
	// 日 期: 2013/09/29
	// ****************************************************************
	public boolean InBoot(BluetoothService BtCom, byte uSt, int timeout) {
		long uStart = System.currentTimeMillis();
		while ((System.currentTimeMillis() - uStart) < timeout) {
			if (InBootCmd(BtCom, uSt))
				return true;
		}
		return false;
	}

	/*
	 * 修改波特率 0 9600 1 19200 2 38400 3 57600 4 115200
	 */
	public boolean ChangeBaudrate(BluetoothService BtCom, byte uSt, int uBaud,
			boolean bCheck) {
		if (bCheck) {
			if (!InBoot(BtCom, uSt, 5000)) // 检查是否在boot区
				return false;
		}

		// if (com.nBaudRate == uBaud)
		// return true;

		byte cBaud = 0;
		switch (uBaud) {
		case 9600:
			cBaud = 0;
			break;
		case 19200:
			cBaud = 1;
			break;
		case 38400:
			cBaud = 2;
			break;
		case 57600:
			cBaud = 3;
			break;
		case 115200:
			cBaud = 4;
			break;
		default:
			;
		}
		// 组合命令帧
		byte[] buf = new byte[8];
		buf[0] = uSt;
		buf[1] = 0x60;
		buf[2] = cBaud;
		buf[3] = 0;
		buf[4] = 0;
		buf[5] = 0;
		int crc = GetCrc16(buf, 6);
		buf[6] = (byte) (crc & 0xff);
		buf[7] = (byte) ((crc >> 8) & 0xff);
		// 发送
		if (!BtCom.write(buf)) {
			m_dwErrCode = PROTOCOL_WRITE_FAIL;
			return false;
		}

		try {
			Thread.sleep(15);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// 切换本地波特率
		// com.Close();
		// com.Open(com.nPortNum,uBaud,com.nDataBits,com.nParity,com.nStopBits);

		final int RepTimes = 3; // 重复次数
		int times = 0;
		while (++times < RepTimes) {
			BtCom.write(buf);
			if (GetReply(BtCom))
				break;
		}
		if (times == RepTimes) {
			return false;
		}

		m_dwErrCode = 0;
		return true;
	}

	/*
	 * 读下位机对命令帧的应答帧，检查是否正确回复。
	 */
	private boolean GetReply(BluetoothService BtCom) {
		Wait(BtCom, 8, 500);
		// 读数据
		byte[] Recv = new byte[8];
		if (ReadData(BtCom, Recv, 8) != 8) {
			m_dwErrCode = PROTOCOL_READ_FAIL;
			return false;
		}
		if (GetCrc16(Recv, 8) != 0) {
			m_dwErrCode = PROTOCOL_CRC_CHECK_ERROR;
			return false;
		}
		if ((Recv[1] & 0x80) == 0x80) {
			m_dwErrCode = PROTOCOL_BOOT_BASE + Recv[2];
			return false;
		}
		m_dwErrCode = 0;
		return true;
	}

	// *******************************************************
	// Method: GetAppID
	// Access: public
	// Returns: boolean
	// Qualifier: 获取APP区电子标签
	// Parameter: CSeriesPort & com
	// Parameter: byte uSt
	// Parameter: unsigned int & uPLine
	// Parameter: unsigned int & uPType
	// Parameter: unsigned int & uCPU
	// Author: 李正伟 马俊移植
	// Date: 2013年3月22日
	// *******************************************************
	public boolean GetAppID(BluetoothService BtCom, byte uSt, ParamPass param) {
		// 建立重发机制
		// const int REPEAT = 3;
		// int iTimes = 0;
		// while (iTimes++ < REPEAT)
		// {
		boolean ret = m_ModbusApp.GetAppID(BtCom, uSt, param);
		m_dwErrCode = m_ModbusApp.GetErrorCode();
		return ret;

		// if (ret)
		// {
		// return true;
		// }
		// else
		// {
		// if (m_dwErrCode==APPCMD_READ_FAIL) // 没有回应，需要重发
		// {
		// }
		// else
		// {
		// return false;
		// }
		// }
		// }

		// return false;
	}

	/*
	 * 读Boot电子标签
	 */
	public boolean GetBootID(BluetoothService BtCom, byte uSt, ParamPass uParam) {
		byte[] pRet = new byte[16];
		int uRetSize = 20;
		byte uCmd = 0x62;

		byte[] Para = new byte[4];
		// 读数据
		if (!BootCmd(BtCom, uSt, uCmd, Para, uRetSize, pRet, 200)) {
			return false;
		}
		uParam.uAppPLine = (pRet[3] << 8) | pRet[2];
		uParam.uAppPType = (pRet[5] << 8) | pRet[4];
		uParam.uAppIC = (pRet[13] << 8) | pRet[12];

		m_dwErrCode = 0;
		return true;
	}

	/*
	 * 读Boot版本
	 */
	public boolean GetHCBootVer(BluetoothService BtCom, byte uSt, ParamPass uParam)
	{
		byte[] pRet = new byte[16];
		int uRetSize = 20;
		byte uCmd = 0x62;

		byte[] Para = new byte[4];
		//读数据
		if (!BootCmd(BtCom,uSt,uCmd,Para,uRetSize,pRet,200))
		{
			return false;
		}
		uParam.uVer = (pRet[11]<<8) | pRet[10];

		m_dwErrCode = 0;
		return true;
	}
	/*
	 * 发送起始地址
	 */
	public boolean SendStartAddr(BluetoothService BtCom, byte uSt,
			int uStartAddr) {
		byte[] Para = new byte[4];
		Para[0] = (byte) (uStartAddr & 0xff);
		Para[1] = (byte) ((uStartAddr >> 8) & 0xff);
		Para[2] = (byte) ((uStartAddr >> 16) & 0xff);
		Para[3] = (byte) ((uStartAddr >> 24) & 0xff);
		byte[] ret = new byte[4];

		if (!BootCmd(BtCom, uSt, (byte) 0x64, Para, 8, ret, 200)) {
			return false;
		}
		m_dwErrCode = 0;
		return true;
	}

	/*
	 * 发送bin文件大小
	 */
	public boolean SendBinSize(BluetoothService BtCom, byte uSt, int uSize) {
		byte[] Para = new byte[4];
		Para[0] = (byte) (uSize & 0xff);
		Para[1] = (byte) ((uSize >> 8) & 0xff);
		Para[2] = (byte) ((uSize >> 16) & 0xff);
		Para[3] = (byte) ((uSize >> 24) & 0xff);
		byte[] ret = new byte[4];

		if (!BootCmd(BtCom, uSt, (byte) 0x65, Para, 8, ret, 200)) {
			return false;
		}
		m_dwErrCode = 0;
		return true;
	}

	/*
	 * 擦除flash
	 */
	public boolean Erase(BluetoothService BtCom, byte uSt) {
		byte[] ret = new byte[4];
		byte[] Para = new byte[4];
		// memset(Para,0,4);
		// 读数据
		if (!BootCmd(BtCom, uSt, (byte) 0x66, Para, 8, ret, 20000)) {
			return false;
		}
		m_dwErrCode = 0;
		return true;
	}

	/*
	 * 发送随机码
	 */
	public boolean SendRandomCode(BluetoothService BtCom, byte uSt, int uRand) {
		byte[] Para = new byte[4];
		Para[0] = (byte) (uRand & 0xff);
		Para[1] = (byte) ((uRand >> 8) & 0xff);
		Para[2] = (byte) ((uRand >> 16) & 0xff);
		Para[3] = (byte) ((uRand >> 24) & 0xff);
		byte[] ret = new byte[4];
		if (!BootCmd(BtCom, uSt, (byte) 0x67, Para, 8, ret, 200)) {
			return false;
		}
		m_dwErrCode = 0;
		return true;
	}

	/*
	 * 发送bin文件校验结果
	 */
	public boolean SendBinCRC32(BluetoothService BtCom, byte uSt, int CRC) {
		byte[] Para = new byte[4];
		Para[0] = (byte) (CRC & 0xff);
		Para[1] = (byte) ((CRC >> 8) & 0xff);
		Para[2] = (byte) ((CRC >> 16) & 0xff);
		Para[3] = (byte) ((CRC >> 24) & 0xff);
		byte[] ret = new byte[4];
		if (!BootCmd(BtCom, uSt, (byte) 0x68, Para, 8, ret, 200)) {
			return false;
		}
		m_dwErrCode = 0;
		return true;
	}

	/*
	 * 烧录完成后启动芯片
	 */
	public boolean Start(BluetoothService BtCom, byte uSt, int uEntryPoint,
			ParamPass param) {
		byte[] Para = new byte[4];
		Para[0] = (byte) (uEntryPoint & 0xff);
		Para[1] = (byte) ((uEntryPoint >> 8) & 0xff);
		Para[2] = (byte) ((uEntryPoint >> 16) & 0xff);
		Para[3] = (byte) ((uEntryPoint >> 24) & 0xff);
		byte[] ret = new byte[4];
		if (!BootCmd(BtCom, uSt, (byte) 0x70, Para, 8, ret, 200)) {
			return false;
		}

		if ((ret[3] & 0x1) == 0x1) // 第3字节的bit0表示是否烧录过从芯片（掉电恢复）
		{
			param.bBurn = true;
		} else {
			param.bBurn = false;
		}

		m_dwErrCode = 0;
		return true;
	}

	/*
	 * 是否有otp数据
	 */
	public boolean Otp(BluetoothService BtCom, byte uSt) {
		byte[] ret = new byte[4];
		byte[] Para = new byte[4];
		// 读数据
		if (!BootCmd(BtCom, uSt, (byte) 0x71, Para, 8, ret, 200)) {
			return false;
		}

		m_dwErrCode = 0;
		return true;
	}

	// ************************************************************************
	// 函数名称: GetSN
	// 函数功能: 获取ARM芯片的序列号
	// 参 数: CSeriesPort & com
	// 参 数: byte uSt
	// 参 数: byte * pSN
	// 返 回 值: boolean
	// 作 者: 李正伟 马俊移植
	// 时 间: 2013/02/23 13:55:24
	// ************************************************************************
	public boolean GetSN(BluetoothService BtCom, byte uSt, byte[] pSN) {
		byte[] ret = new byte[12];
		byte[] Para = new byte[4];
		// 读数据
		if (!BootCmd(BtCom, uSt, (byte) 0x72, Para, 16, ret, 500)) {
			return false;
		}
		// 移动数据
		Arrays.fill(pSN, (byte) 0);
		// memcpy(pSN,ret,12);

		m_dwErrCode = 0;
		return true;
	}

	/*
	 * 发送数据帧
	 *  2013/11/04 发送数据帧时，如果底层应答CRC16错误则再发送一次，重复次数不超过3次。原因是485组网升级时，由于受到干扰，偶尔会报CRC16错误。
	 * 2014/05/12 根据boot版本号确定是否重发，是否将超时时间设置为10s。
	 */
	public boolean SendFrame(BluetoothService BtCom, byte uSt, byte[] pData,
			int uLen, boolean bRetry, int timeout) {
		int MaxTimes = 3; // 重发次数限制
		int CurTimes = 0; // 当前重发次数
		if (!bRetry)
		{
			MaxTimes = 1;
		}

		while (CurTimes < MaxTimes) {
			if (!DataFrameCmd(BtCom, uSt, (byte) 0x69, pData, uLen, timeout)) {
				if ((m_dwErrCode == (PROTOCOL_BOOT_BASE + 1)) || // 命令帧CRC16错误
						(m_dwErrCode == PROTOCOL_READ_FAIL) || // 没有接收到数据
						(m_dwErrCode == PROTOCOL_CRC_CHECK_ERROR)) // CRC校验错误
				{
					CurTimes++;
				} else {
					return false; // 如果是其他类型的错误，则直接返回，不再重复发送。
				}
			} else {
				m_dwErrCode = 0; // 没有错误，同时将错误代码置零。
				return true;
			}
		}

		return false; // 错误次数超过限制。
	}
	
	public boolean SendFrame(BluetoothService BtCom, byte uSt, byte[] pData,
			int uLen, boolean bRetry) {
		int timeout = 1000;
		return SendFrame(BtCom, uSt, pData, uLen, bRetry, timeout);
	}
	
	public boolean SendFrame(BluetoothService BtCom, byte uSt, byte[] pData,
			int uLen) {
		boolean bRetry = true;
		int timeout = 1000;
		return SendFrame(BtCom, uSt, pData, uLen, bRetry, timeout);
	}

	/*
	 * 获取CRC16
	 */
	private int GetCrc16(byte[] pData, int uSize) {
		// CCRC16 crc;
		return CRC16.GetCrc16(pData, uSize);
	}

	/*
	 * boot区命令
	 */
	private boolean BootCmd(BluetoothService BtCom, byte uSt, byte uCmd,
			byte[] Para, int uRetFrmSize, byte[] pRet, int timeout) {
		// 组合命令帧
		byte[] pBuf = new byte[8];
		pBuf[0] = uSt;
		pBuf[1] = uCmd;
		pBuf[2] = Para[0];
		pBuf[3] = Para[1];
		pBuf[4] = Para[2];
		pBuf[5] = Para[3];
		int crc = GetCrc16(pBuf, 6);
		pBuf[6] = (byte) (crc & 0xff);
		pBuf[7] = (byte) ((crc >> 8) & 0xff);
		// 发送
		if (!BtCom.write(pBuf)) {
			m_dwErrCode = PROTOCOL_WRITE_FAIL;
			return false;
		}
		// 查询串口缓冲区数据是否接收完成
		Wait(BtCom, uRetFrmSize,/* 5000 */timeout);
		// 读数据
		byte[] pRecv = new byte[uRetFrmSize];
		if (!JudgeRecv(BtCom,uCmd,uRetFrmSize,pRecv))
		{
			return false;
		}
//		if (ReadData(BtCom, pRecv, uRetFrmSize) != uRetFrmSize) {
//			m_dwErrCode = PROTOCOL_READ_FAIL;
//			return false;
//		}
//		if (GetCrc16(pRecv, uRetFrmSize) != 0) {
//			m_dwErrCode = PROTOCOL_CRC_CHECK_ERROR;
//			return false;
//		}
//		if ((pRecv[1] & 0x80) == 0x80) {
//			m_dwErrCode = PROTOCOL_BOOT_BASE + pRecv[2];
//			return false;
//		}
		int j = 0;
		for (int i = 2; i < uRetFrmSize - 2; i++) {
			pRet[j] = pRecv[i];
			j++;
		}
		return true;
	}

	/*
	 * boot区发送数据帧命令
	 */
	private boolean DataFrameCmd(BluetoothService BtCom, byte uSt, byte uCmd,
			byte[] pData, int uLen, int timeout) {
		// 组合命令帧
		byte[] pBuf = new byte[uLen + 4];
		pBuf[0] = uSt;
		pBuf[1] = uCmd;
		for (int i = 0; i < uLen; i++) {
			pBuf[2 + i] = pData[i];
		}
		int crc = GetCrc16(pBuf, uLen + 2);
		pBuf[uLen + 2] = (byte) (crc & 0xff);
		pBuf[uLen + 3] = (byte) ((crc >> 8) & 0xff);
		// 发送
		if (!BtCom.write(pBuf)) {
			m_dwErrCode = PROTOCOL_WRITE_FAIL;
			return false;
		}
		// 延时读串口数据
		// Sleep(com.CalcDelayTime(uLen+4)); //此延时会严重影响效率（因为发送的数据帧较多，累积时间就会较长）
		Wait(BtCom, 8, timeout); // 因为编程中遇到带密码的扇区，需要先擦除扇区，2000ms是必须的。20140212
		// 读数据
		byte[] Recv = new byte[8];
		return JudgeRecv(BtCom,uCmd,8,Recv);
//		if (ReadData(BtCom, Recv, 8) != 8) {
//			m_dwErrCode = PROTOCOL_READ_FAIL;
//			return false;
//		}
//		if (GetCrc16(Recv, 8) != 0) {
//			m_dwErrCode = PROTOCOL_CRC_CHECK_ERROR;
//			return false;
//		}
//		if ((Recv[1] & 0x80) == 0x80) {
//			m_dwErrCode = PROTOCOL_BOOT_BASE + Recv[2];
//			return false;
//		}
//		return true;
	}

	/*
	 * arm读数据
	 */
	public boolean ReadMemory(BluetoothService BtCom, int uAddr, int uLen,
			byte[] pData) {
		// 发送命令，然后等待ack
		byte[] buf = new byte[256];
		buf[0] = 0x11;
		buf[1] = (byte) 0xee;
		if (!BtCom.write(buf)) {
			m_dwErrCode = PROTOCOL_WRITE_FAIL;
			return false;
		}
		// 延时读串口数据
		Wait(BtCom, 1, 1000);
		if (!GetAck(BtCom))
			return false;
		// 发送起始地址，高字节在前
		buf[0] = (byte) ((uAddr >> 24) & 0xff);
		buf[1] = (byte) ((uAddr >> 16) & 0xff);
		buf[2] = (byte) ((uAddr >> 8) & 0xff);
		buf[3] = (byte) (uAddr & 0xff);
		buf[4] = CheckSum(buf, 4);
		if (!BtCom.write(buf)) {
			m_dwErrCode = PROTOCOL_WRITE_FAIL;
			return false;
		}
		// 延时读串口数据
		Wait(BtCom, 1, 1000);
		if (!GetAck(BtCom))
			return false;
		// 发送读取的字节数
		buf[0] = (byte) (uLen - 1);
		buf[1] = CheckSum(buf, 1);
		if (!BtCom.write(buf)) {
			m_dwErrCode = PROTOCOL_WRITE_FAIL;
			return false;
		}
		// 延时读串口数据
		Wait(BtCom, 1, 1000);
		if (!GetAck(BtCom))
			return false;
		// 接收读取的数据
		// 查询串口缓冲区数据是否接收完成
		Wait(BtCom, uLen, 5000);
		if (ReadData(BtCom, buf, uLen) != uLen) {
			m_dwErrCode = PROTOCOL_READ_FAIL;
			return false;
		}

		System.arraycopy(buf, 0, pData, 0, uLen);
		// for (int i=0; i<uLen; i++)
		// {
		// pData[i]=buf[i];
		// }

		m_dwErrCode = 0;
		return true;
	}

	/*
	 * arm写数据
	 */
	public boolean WriteMemory(BluetoothService BtCom, int uAddr, int uLen,
			byte[] pData) {
		// 发送命令，然后等待ack
		byte[] buf = new byte[258]; // 最大长度是256字节数据加1字节长度和1字节校验
		buf[0] = 0x31;
		buf[1] = (byte) 0xce;
		if (!BtCom.write(buf)) {
			m_dwErrCode = PROTOCOL_WRITE_FAIL;
			return false;
		}
		// 延时读串口数据
		Wait(BtCom, 1, 1000);
		if (!GetAck(BtCom))
			return false;
		// 发送起始地址，高字节在前
		buf[0] = (byte) ((uAddr >> 24) & 0xff);
		buf[1] = (byte) ((uAddr >> 16) & 0xff);
		buf[2] = (byte) ((uAddr >> 8) & 0xff);
		buf[3] = (byte) (uAddr & 0xff);
		buf[4] = CheckSum(buf, 4);
		if (!BtCom.write(buf)) {
			m_dwErrCode = PROTOCOL_WRITE_FAIL;
			return false;
		}
		// 延时读串口数据
		Wait(BtCom, 1, 1000);
		if (!GetAck(BtCom))
			return false;
		// 写入数据长度及数据
		buf[0] = (byte) (uLen - 1);
		for (int i = 0; i < uLen; i++) {
			buf[1 + i] = pData[i];
		}
		buf[1 + uLen] = CheckSum(buf, 1 + uLen);
		if (!BtCom.write(buf)) {
			m_dwErrCode = PROTOCOL_WRITE_FAIL;
			return false;
		}
		// 查询串口缓冲区数据是否接收完成
		Wait(BtCom, 1, 1000);
		if (!GetAck(BtCom))
			return false;

		m_dwErrCode = 0;
		return true;
	}

	// ****************************************************************
	// 函数名称: WriteMemoryFirst
	// 函数功能: 往arm芯片写数据的第一帧，可能需要解保护操作。
	// 访问属性: public
	// 返回值: boolean
	// 函数参数: CSeriesPort & com
	// 函数参数: unsigned int uAddr
	// 函数参数: boolean bMater
	// 函数参数: unsigned int uLen
	// 函数参数: byte * pData
	// 作 者: 李正伟 马俊移植
	// 日 期: 2013/10/12
	// ****************************************************************
	public boolean WriteMemoryFirst(BluetoothService BtCom, int uAddr,
			boolean bMater, int uLen, byte[] pData) {
		// 发送命令，然后等待ack
		byte[] buf = new byte[258]; // 最大长度是256字节数据加1字节长度和1字节校验
		buf[0] = 0x31;
		buf[1] = (byte) 0xce;
		WM_FIRST: while (true) {
			if (!BtCom.write(buf)) {
				m_dwErrCode = PROTOCOL_WRITE_FAIL;
				return false;
			}
			// 延时读串口数据
			Wait(BtCom, 1, 1000);
			int code = 0;
			if (!GetAckFirst(BtCom, bMater, code)) //
			{
				if (code == 1) {
					continue WM_FIRST;
				}
				return false;
			}
			break;
		}
		// 发送起始地址，高字节在前
		buf[0] = (byte) ((uAddr >> 24) & 0xff);
		buf[1] = (byte) ((uAddr >> 16) & 0xff);
		buf[2] = (byte) ((uAddr >> 8) & 0xff);
		buf[3] = (byte) (uAddr & 0xff);
		buf[4] = CheckSum(buf, 4);
		if (!BtCom.write(buf)) {
			m_dwErrCode = PROTOCOL_WRITE_FAIL;
			return false;
		}
		// 延时读串口数据
		Wait(BtCom, 1, 1000);
		if (!GetAck(BtCom))
			return false;
		// 写入数据长度及数据
		buf[0] = (byte) (uLen - 1);
		System.arraycopy(pData, 0, buf, 1, uLen);
		// for (int i=0; i<uLen; i++)
		// {
		// buf[1+i]=pData[i];
		// }
		buf[1 + uLen] = CheckSum(buf, 1 + uLen);
		if (!BtCom.write(buf)) {
			m_dwErrCode = PROTOCOL_WRITE_FAIL;
			return false;
		}
		// 查询串口缓冲区数据是否接收完成
		Wait(BtCom, 1, 1000);
		if (!GetAck(BtCom))
			return false;

		m_dwErrCode = 0;
		return true;
	}

	/*
	 * arm跳转命令
	 */
	public boolean Go(BluetoothService BtCom, int uAddr) {
		// 发送命令，然后等待ack
		byte[] buf = new byte[16];
		buf[0] = 0x21;
		buf[1] = (byte) 0xDE;
		if (!BtCom.write(buf)) {
			m_dwErrCode = PROTOCOL_WRITE_FAIL;
			return false;
		}
		// 延时读串口数据
		Wait(BtCom, 1, 1000);
		if (!GetAck(BtCom))
			return false;
		// 发送起始地址，高字节在前
		buf[0] = (byte) ((uAddr >> 24) & 0xff);
		buf[1] = (byte) ((uAddr >> 16) & 0xff);
		buf[2] = (byte) ((uAddr >> 8) & 0xff);
		buf[3] = (byte) (uAddr & 0xff);
		buf[4] = CheckSum(buf, 4);
		if (!BtCom.write(buf)) {
			m_dwErrCode = PROTOCOL_WRITE_FAIL;
			return false;
		}
		// 延时读串口数据
		Wait(BtCom, 1, 1000);
		if (!GetAck(BtCom))
			return false;

		m_dwErrCode = 0;
		return true;
	}

	/*
	 * 计算校验和，ARM的方式：字节异或
	 */
	private byte CheckSum(byte[] pData, int uSize) {
		// unsigned long sum = 0;
		// while(uSize-- >= 0)
		// {
		// sum += *pData++;
		// }

		// while(sum>>8)
		// sum = (sum & 0xff) + (sum >> 8);
		// //sum = sum + (sum >> 8);
		// return byte(~sum);
		byte sum = (byte) 0xff;
		if (uSize > 1) {
			sum = 0;
		}
		for (byte bt : pData) {
			sum ^= bt;
		}
		// while(uSize-- > 0)
		// {
		// sum ^= *pData++;
		// }
		return sum;
	}

	/*
	 * 获取应答
	 */
	private boolean GetAck(BluetoothService BtCom) {
		byte[] buf = new byte[1];
		if (ReadData(BtCom, buf, 1) != 1) {
			m_dwErrCode = PROTOCOL_READ_FAIL;
			return false;
		}
		if (buf[0] == 0x79) {
			return true;
		} else {
			m_dwErrCode = PROTOCOL_ARM_UNKNOW_ACK;
			return false;
		}
	}

	/*
	 * 往芯片写第一帧数据的应答
	 */
	private boolean GetAckFirst(BluetoothService BtCom, boolean bMater,
			Integer code) {
		code = 0; // 错误码，1表示已经正确解保护。
		byte[] buf = new byte[1];
		if (ReadData(BtCom, buf, 1) != 1) {
			m_dwErrCode = PROTOCOL_READ_FAIL;
			return false;
		}
		if (buf[0] == 0x79) {
			return true;
		} else if (buf[0] == 0x1f) {
			if (bMater) {
				if (RemoveProtection(BtCom)) {
					code = 1;
				}
				return false;
			} else {
				m_dwErrCode = PROTOCOL_ARM_RMPROTECTION_NO;
				return false;
			}
		} else {
			m_dwErrCode = PROTOCOL_ARM_UNKNOW_ACK;
			return false;
		}
	}

	/*
	 * 解保护
	 */
	private boolean RemoveProtection(BluetoothService BtCom) {
		// 发送命令，然后等待ack
		byte[] buf = new byte[2];
		buf[0] = (byte) 0x92;
		buf[1] = (byte) 0x6d;
		if (!BtCom.write(buf)) {
			m_dwErrCode = PROTOCOL_WRITE_FAIL;
			return false;
		}
		// 延时读串口数据
		Wait(BtCom, 2, 30000);
		if (ReadData(BtCom, buf, 2) != 2) {
			m_dwErrCode = PROTOCOL_READ_FAIL;
			return false;
		}
		// 判断接收的数据是否正确
		if ((buf[0] == 0x79) && (buf[1] == 0x79)) {
			// 再发一次0x7F
			// Sleep(100); // 猜测的延时时间长度
			ParamPass Param = new ParamPass();
			Param.uByteRet = 0x7F;
			// byte uCh = 0x7F;
			boolean ret = Ping(BtCom, Param);
			if (ret) {
				if (Param.uByteRet == 0x79) {
					return true;
				}
			}
			return false;
		} else {
			m_dwErrCode = PROTOCOL_ARM_RMRROTECTION_FAIL;
			return false;
		}
	}

	// ************************************************************************
	// 函数名称: Wait
	// 函数功能: 等待串口缓冲区的数据达到指定的个数
	// 参 数: CSeriesPort & com 串口
	// 参 数: unsigned int size 串口缓冲区数据字节数
	// 参 数: const unsigned int timeout 超时时间
	// 返 回 值: void
	// 作 者: 李正伟 马俊移植
	// 时 间: 2013/02/04 10:10:17
	// ************************************************************************
	private void Wait(BluetoothService BtCom, int size, final int timeout) {
		long uStart = System.currentTimeMillis();
		while ((System.currentTimeMillis() - uStart) < timeout) {
			int nCount = BtCom.QueryBuffer();
			if (nCount >= size)
				break;
			// Sleep(1);
		}
	}

	// ****************************************************************
	// 函数名称: ChangStation
	// 函数功能: 设备组网升级时修改设备的站号
	// 访问属性: public
	// 返回值: boolean
	// 函数参数: CSeriesPort & com
	// 函数参数: byte uSt
	// 作 者: 李正伟 马俊移植
	// 日 期: 2013/12/19
	// ****************************************************************
	public boolean ChangStation(BluetoothService BtCom, byte uSt) {
		byte[] Para = new byte[4];
		Para[0] = uSt;
		Para[1] = 0;
		Para[2] = 0;
		Para[3] = 0;
		byte[] ret = new byte[4];
		byte ucStation = uSt; // 修改站号之前设备里的站号

		//DSP烧录时，有时会失败，由200改为300ms
		if (!BootCmd(BtCom, ucStation, (byte) 0x73, Para, 8, ret, 300)) {	
			return false;
		}
		m_dwErrCode = 0;
		return true;
	}

	/*
	 * 读数据
	 */
	private int ReadData(BluetoothService BtCom, byte[] buffer, int nReadNum) {
		byte[] bufferTemp = new byte[1024];
		// Wait(com,iBackFrmSize,timeout);
		int nRetSize = 0;
		int nReadTimes = 0;
		while (nRetSize < nReadNum) {
			int length = BtCom.read(bufferTemp);
			if (length > nReadNum) {
				System.arraycopy(bufferTemp, 0, buffer, nRetSize, nReadNum);
			} else {
				System.arraycopy(bufferTemp, 0, buffer, nRetSize, length);
			}

			nRetSize += length;
			nReadTimes++;
			if (nReadTimes > AppCmd.WM_RESETREADNUM) // 读取10次 大概25ms*15
			{
				m_dwErrCode = AppCmd.APPCMD_READ_FAIL;
				return 0;
			}
		}
		return nRetSize;
	}

	// ************************************************************************
	// 函数名称: JudgeRecv
	// 函数说明: 判断返回帧是否正确
	// 访问属性: private
	// 返 回 值: bool
	// 函数参数: CSeriesPort & com
	// 函数参数: unsigned char SendCmd
	// 函数参数: unsigned int uRecv
	// 函数参数: unsigned char * pRecv
	// 作 者: 李正伟
	// 日 期: 2014/05/19
	// ************************************************************************
	private boolean JudgeRecv(BluetoothService BtCom, byte SendCmd, int uRecv,
			byte[] pRecv) {
		if (ReadData(BtCom, pRecv, uRecv) != uRecv) {
			m_dwErrCode = PROTOCOL_READ_FAIL;
			return false;
		}
		if (GetCrc16(pRecv, uRecv) != 0) {
			m_dwErrCode = PROTOCOL_CRC_CHECK_ERROR;
			return false;
		}
		if (SendCmd != pRecv[1]) {
			if ((pRecv[1] & 0x80) == 0x80) {
				m_dwErrCode = PROTOCOL_BOOT_BASE + pRecv[2];
				return false;
			}
			else
			{
				m_dwErrCode = PROTOCOL_RECV_CMD_ERROR;
				return false;
			}
		}
		m_dwErrCode = 0;
		return true;
	}
}
