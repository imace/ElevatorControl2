package com.inovance.elevatorprogram;

import java.util.Arrays;
import java.util.Vector;

import com.inovance.protocol.AppCmd;
import com.inovance.protocol.BluetoothService;
import com.inovance.protocol.Protocol;

public class DSPProgram extends BaseProgram {

	private int m_uBLVer = 0; // 官方bootloader版本号
	private int m_uHCBootVer; // boot版本
	private int m_uTimeOut = 1000; // 数据帧超时时间
	private boolean m_bRetry = false; // 是否重发数据

	public boolean CoreProgram(SysPara sys) {
		m_bBusy = true;
		m_sMsg = ""; // 清空旧的信息
		// sys.BtCom.TurnOnLog(m_sLogFile);
		long uStart = System.currentTimeMillis();
		boolean ret;
		if (m_bBootLoader) {
			ret = BootLoader(sys);
		} else {
			ret = Program(sys);
		}
		long uElapse = System.currentTimeMillis() - uStart;
		float fElapse = (float) uElapse / 1000;
		if (!ret) {
			// 发送失败消息
			sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
					IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();
			int nCode = sys.protocol.GetErrorCode();
			sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.ERROR_CODE,
					nCode).sendToTarget();
		} else {
			String sElapse = "";
			sElapse = String.format("烧录用时:%.1f秒\n", fElapse);
			m_sMsg += sElapse;
			// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,1,0);
			sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.PROGRAM_TIME,
					(int) fElapse).sendToTarget();
		}
		// sys.com.TurnOffLog();
		m_bBusy = false;

		return ret;
	}

	private boolean BootLoader(SysPara sys) {
		// 以9600的波特率跟官方bootloader通讯
		// if (!sys.com.Open(sys.uPort, 9600))
		// {
		// m_sMsg += GetErrorMsg(sys.com.GetLastErrorCode());
		// m_sMsg += _T("\r\n");
		// return false;
		// }
		m_sMsg += "连接中......";
		// ::SendMessage(sys.hWnd, UM_PROGRESSINFO, 0, 0);
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
				IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();

		ParamPass param = new ParamPass();
		param.uByteRet = 'A';
		// Byte uCh = 'A';
		boolean ret = sys.protocol.Ping(sys.BtCom, param);
		if (!ret) // 没有应答
		{
			m_sMsg += "失败\n";
			return false;
		} else if (ret && param.uByteRet != 'A') // 探测失败
		{
			m_sMsg += "失败\n";
			return false;
		} else // 掉电升级
		{
			m_sMsg += "成功\n";
			// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
			m_sMsg += "初始化......";
			// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
			sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
					IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();
			// 下发引导文件1
			Vector<Byte> vData = new Vector<Byte>();
			sys.bin.GetFBL(vData);

			if (!SendIapTest(sys, vData)) {
				m_sMsg += "失败\n请重新上电！\n";
				return false;
			}
			if (!ReceiveCmd(sys)) {
				m_sMsg += "失败\n芯片解密失败！\n原因：烧录文件错误或者芯片损坏！请重新上电再试一次！\n";
				return false;
			}
			// Sleep(100); //延时
			if (!SendSBL(sys)) {
				m_sMsg += "失败\n请重新上电r\n";
				return false;
			}
			m_sMsg += "成功\n";
			// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
			sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
					IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();
			// Sleep(100); //延时

			if (!sys.protocol.Otp(sys.BtCom, sys.uStation)) // otp区是否有数据
			{
				int errcode = sys.protocol.GetErrorCode();
				if (0x9 != (errcode - Protocol.PROTOCOL_BOOT_BASE)) {
					m_sMsg += "查询OTP区有无数据失败\n";
					return false;
				}
				if (!SendOtp(sys)) // 发送otp
				{
					m_sMsg += "发送otp数据失败\n";
					return false;
				}
			}

			// 软件复位，省去重新上电步骤。
			int addr = DSPErrJumpAddr(sys, 2);

			param.bBurn = false;
			// boolean bBurn = false;
			sys.protocol.Start(sys.BtCom, sys.uStation, addr, param);

			m_sMsg += "烧录完成\n";
			// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
			sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
					IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();

		}

		return true;
	}

	// ****************************************************************
	// 函数名称: Program
	// 函数功能: 烧录主函数
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟
	// 日 期: 2013/09/13
	// ****************************************************************
	private boolean Program(SysPara sys) {
		boolean bFirstTime = true; //
		m_sMsg += "连接中......";
		// ::SendMessage(sys.hWnd, UM_PROGRESSINFO, 0, 0);

		// START:
		ParamPass param = new ParamPass();
		param.uByteRet = 'A';
		// Byte uCh = 'A';
		START: while (true) {
			boolean ret = sys.protocol.Ping(sys.BtCom, param);
			if (!ret) // 没有应答
			{
				if (bFirstTime) {
					bFirstTime = false;
					ParamPass par = new ParamPass();// 返回bInBoot
					boolean retOK = TryUpgradeMode(sys, par);
					if (retOK) // 成功跳转到boot区
					{
						continue START;
					} else {
						if (par.bInBoot) {
							if (Start(sys, true))
								return true;
							else
								return false;
						} else {
							m_sMsg += "失败\n";
							m_sMsg += par.strErr;
							return false;
						}
					}
				} else {
					m_sMsg += "失败\n";
					return false;
				}
			} else if (ret && param.uByteRet != 'A') // 探测失败
			{
				m_sMsg += "失败\n";
				return false;
			} else // 掉电升级
			{
				if (!Start(sys, false))
					return false;
			}
			break;
		}
		return true;
	}

	// ****************************************************************
	// 函数名称: TryUpgradeMode
	// 函数功能: 尝试APP区指令，如果当前程序停留在APP区则跳转到boot区。
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟
	// 日 期: 2013/09/13
	// ****************************************************************
	private boolean TryUpgradeMode(SysPara sys, ParamPass par) {
		boolean bfind = false;
		// 读APP区电子标签，判断是否跟选择的文件一致。
		ParamPass param = new ParamPass();
		if (sys.protocol.GetAppID(sys.BtCom, sys.uStation, param)) {
			if ((sys.bin.GetPLine() != param.uAppPLine)
					|| (sys.bin.GetPType() != param.uAppPType)
					|| (sys.bin.GetIC() != param.uAppIC)) {
				par.strErr = "原因：APP区电子标签跟烧录文件中的电子标签不一致，请选择正确的烧录文件！\n";
				return false;
			}
			bfind = true;
		}
		if (bfind) {
			boolean ret = JumpIntoBoot(sys);
			return ret;
		} else {
			// 是否已经在Boot区
			if (sys.protocol.InBoot(sys.BtCom, sys.uStation, 300)) {
				par.bInBoot = true;
			}
			return false;
		}
	}

	// ****************************************************************
	// 函数名称: JumpIntoBoot
	// 函数功能: 从APP区跳转到boot区
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟
	// 日 期: 2013/09/13
	// ****************************************************************
	private boolean JumpIntoBoot(SysPara sys) {
		// 获取烧录权限
		final int TIMELIMIT = 5; // 可重复的次数
		int times = 0;
		boolean ready = false;
		while (times++ < TIMELIMIT) {
			int ustate = 0;
			boolean ret = sys.protocol.SafeGuard(sys.BtCom, sys.uStation,
					ustate);
			if (!ret) {
				if (sys.protocol.GetErrorCode() == AppCmd.APPCMD_NOT_READY) // 只在提示未就绪时重复
				{
					try {
						Thread.sleep(1000);
					} catch (Exception E) {
						E.printStackTrace();
					}
				} else {
					break;
				}
			} else {
				ready = true;
				break;
			}
		}

		if (!ready) {
			return false;
		}

		// 获取官方bootloader版本号
		ParamPass par = new ParamPass();
		sys.protocol.GetBootLoaderVersion(sys.BtCom, sys.uStation, par);
		m_uBLVer = par.uVer;

		// 读设备状态
		int state = 0;
		if (!sys.protocol.GetMachineState(sys.BtCom, sys.uStation, state)) {
			return false;
		}
		// Sleep(10);
		// 跳转到boot区
		int ustate = 0;
		if (!sys.protocol.JumpToBoot(sys.BtCom, sys.uStation, ustate)) {
			return false;
		}
		// DSP从用户程序跳转到官方bootloader成功后需要延时
		try {
			Thread.sleep(100);
		} catch (Exception E) {
			E.printStackTrace();
		}
		// Sleep(100);

		return true;
	}

	// ************************************************************************
	// 函数名称: Start
	// 函数说明: 判断当前状态，然后开始烧录。
	// 访问属性: private
	// 返 回 值: bool
	// 函数参数: CSysPara & sys
	// 函数参数: bool bInBoot
	// 作 者: 李正伟
	// 日 期: 2014/05/16
	// ************************************************************************
	private boolean Start(SysPara sys, boolean bInBoot) {
		if (bInBoot) {
			m_sMsg += "成功\n";
			// 获取boot版本，以版本来判断数据帧超时时间是10s还是1s，数据帧重发与否。
			ParamPass par = new ParamPass();
			if (!sys.protocol.GetHCBootVer(sys.BtCom, sys.uStation, par)) {
				m_uHCBootVer = par.uVer;
				m_sMsg += "读取boot版本失败!\n";
				// sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, 0, 0)
				// .sendToTarget();
				// // ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
				return false;
			}
			GetTimeOut(m_uHCBootVer);
			GetRetry(m_uHCBootVer);
		} else {
			if (!BootCmds(sys)) {
				return false;
			}
		}

		// 开始编程
		if (!SendApp(sys)) {
			int addr = DSPErrJumpAddr(sys, 2);
			ParamPass par = new ParamPass();
			par.bBurn = false;
			if (!sys.protocol.Start(sys.BtCom, sys.uStation, addr, par)) {
				return false; // 回到初始状态失败
			}
			return false;
		}

		return true;
	}

	// ****************************************************************
	// 函数名称: BootCmds
	// 函数功能: 在boot区编程
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟
	// 日 期: 2013/09/16
	// ****************************************************************
	private boolean BootCmds(SysPara sys) {
		m_sMsg += "成功\n";
		// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
				IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();
		m_sMsg += "初始化......";
		// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
				IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();
		// 下发引导文件1
		Vector<Byte> vData = new Vector<Byte>();
		sys.bin.GetFBL(vData);

		if (!SendIapTest(sys, vData)) {
			m_sMsg += "失败\n请重新上电！\n";
			return false;
		}
		if (!ReceiveCmd(sys)) {
			m_sMsg += "失败\n芯片解密失败！\n原因：烧录文件错误或者芯片损坏！请重新上电再试一次！\n";
			return false;
		}
		try {
			Thread.sleep(100);
		} catch (Exception E) {
			E.printStackTrace();
		}
		// Sleep(100); //延时
		if (!SendSBL(sys)) {
			m_sMsg += "失败\n请重新上电！\n";
			return false;
		}
		m_sMsg += "成功\n";
		// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
				IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();

		try {
			Thread.sleep(100);
		} catch (Exception E) {
			E.printStackTrace();
		}

		ParamPass param = new ParamPass();
		// Sleep(100); //延时
		// 获取boot版本，以版本来判断数据帧超时时间是10s还是1s，数据帧重发与否。
		if (!sys.protocol.GetHCBootVer(sys.BtCom, sys.uStation, param)) {
			m_sMsg += "读取boot版本失败!\n";
			sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, 0, 0).sendToTarget();
			return false;
		}
		m_uHCBootVer = param.uVer;
		GetTimeOut(m_uHCBootVer);
		GetRetry(m_uHCBootVer);

		if (!sys.protocol.Otp(sys.BtCom, sys.uStation)) // otp区是否有数据
		{
			boolean tag = false; // 局部变量，用于标记是否跳转到特定地址。
			int errcode = sys.protocol.GetErrorCode();
			if (0x9 == (errcode - Protocol.PROTOCOL_BOOT_BASE)) {
				if (sys.bMaster) {
					if (!SendOtp(sys)) // 发送otp
					{
						m_sMsg += "发送otp数据失败\n";
					} else {
						tag = true;
					}
				} else {
					m_sMsg += "OTP区无数据，需要先烧录BootLoader！\n";
				}
			} else {
				m_sMsg += "查询OTP区有无数据失败\n";
			}
			// 跳转到地址1
			if (!tag) {
				int addr = DSPErrJumpAddr(sys, 1);
				param.bBurn = false;
				sys.protocol.Start(sys.BtCom, sys.uStation, addr, param);
				return false;
			}
		}
		return true;
	}

	// ****************************************************************
	// 函数名称: SendIap
	// 函数功能: 发送引导文件
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 函数参数: Vector<Byte> & vdata
	// 作 者: 李正伟
	// 日 期: 2013/09/16
	// ****************************************************************
	//private boolean SendIap(SysPara sys, Vector<Byte> vdata) {
		// if (vdata.isEmpty())
		// return false;
		// //发送
		// int ulen = vdata.size()-2;
		// byte[] uc = new byte[1];
		// for (int i=0; i < ulen; i++)
		// {
		// uc[0] = vdata.get(i);
		// sys.BtCom.write(uc);
		// //.WriteData(&uc,1,false);
		// }
		// //接收
		// byte[] uRecv = new byte[ulen];
		// unsigned int uBegin = GetTickCount();
		// const unsigned int TIMEOUT = 5000;
		// while (1)
		// {
		// if ((unsigned int)sys.com.QueryBuffer() >= ulen)
		// {
		// sys.com.ReadData(uRecv,ulen);
		// break;
		// }
		// if ((GetTickCount()-uBegin) > TIMEOUT)
		// {
		// delete[] uRecv;
		// return false;
		// }
		// Sleep(1);
		// }
		// //对比
		// for (unsigned int i=0; i<ulen; i++)
		// {
		// if (vdata.at(i) != uRecv[i])
		// {
		// delete[] uRecv;
		// return false;
		// }
		// }
		// //发送最后的2字节数据
		// unsigned char uS[2],uR[2];
		// uS[0]=vdata.at(ulen);
		// uS[1]=vdata.at(ulen+1);
		// sys.com.WriteData(uS,2,false);
		// //接收最后的2字节
		// uBegin=GetTickCount();
		// while (1)
		// {
		// if (sys.com.QueryBuffer() >= 2)
		// {
		// sys.com.ReadData(uR,2);
		// break;
		// }
		// if ((GetTickCount()-uBegin) > TIMEOUT)
		// {
		// delete[] uRecv;
		// return false;
		// }
		// Sleep(1);
		// }
		// //比对最后2字节数据
		// if ((uS[0] != uR[0]) || (uS[1] != uR[1]))
		// {
		// delete[] uRecv;
		// return false;
		// }
		//
		// delete[] uRecv;
	//	return true;
	//}

	// ****************************************************************
	// 函数名称: SendIapOneByOne
	// 函数功能: 发送引导文件
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 函数参数: Vector<Byte> & vdata
	// 作 者: 李正伟
	// 日 期: 2013/09/16
	// ****************************************************************
//	private boolean SendIapOneByOne(SysPara sys, Vector<Byte> vdata) {
//		if (vdata.isEmpty())
//			return false;
//		final int TIMEOUT = 1000;
//		// 发送
//		int ulen = vdata.size();
//		byte[] uc = new byte[1];
//		for (int i = 0; i < ulen; i++) {
//			uc[0] = vdata.get(i);
//			sys.BtCom.write(uc);
//			// 延时接收数据
//			byte[] ur = new byte[1];
//			long uBegin = System.currentTimeMillis();
//			while (true) {
//				// if (sys.BtCom.QueryBuffer() >= 1)
//				// {
//				// sys.BtCom.read(ur);
//				// break;
//				// }
//				if (ReadData(sys.BtCom, ur, 1) >= 1) {
//					break;
//				}
//				if ((System.currentTimeMillis() - uBegin) > TIMEOUT) {
//					return false;
//				}
//				// Sleep(0);
//			}
//			if (uc[0] != ur[0]) {
//				return false;
//			}
//		}
//
//		return true;
//	}

	// ****************************************************************
	// 函数名称: SendIapTest
	// 函数功能: 发送第二个字符后接收第一个字符
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 函数参数: Vector<Byte> & vdata
	// 作 者: 李正伟
	// 日 期: 2013/09/16
	// ****************************************************************
	private boolean SendIapTest(SysPara sys, Vector<Byte> vdata) {
		if (vdata.isEmpty())
			return false;
		final int TIMEOUT = 1000;
		int ulen = vdata.size();
		// 发送
		byte[] ur = new byte[1]; // 接收字符
		byte[] uc = new byte[1];
		uc[0] = vdata.get(0);
		sys.BtCom.write(uc);
		for (int i = 1; i < ulen; i++) {
			uc[0] = vdata.get(i);
			sys.BtCom.write(uc);
			// 延时接收数据
			long uBegin = System.currentTimeMillis();
			while (true) {
				if (ReadData(sys.BtCom, ur, 1) >= 1) {
					break;
				}
				if ((System.currentTimeMillis() - uBegin) > TIMEOUT) {
					return false;
				}
				// Sleep(0);
			}
			uc[0] = vdata.get(i - 1);
			if (uc[0] != ur[0]) {
				return false;
			}
		}
		// 对比最后一个字节
		long uBegin = System.currentTimeMillis();
		while (true) {
			if (ReadData(sys.BtCom, ur, 1) >= 1) {
				// sys.com.ReadData(&ur,1);
				break;
			}
			if ((System.currentTimeMillis() - uBegin) > TIMEOUT) {
				return false;
			}
			// Sleep(0);
		}
		uc[0] = vdata.get(ulen - 1);
		if (uc[0] != ur[0]) {
			return false;
		}

		return true;
	}

	// ****************************************************************
	// 函数名称: ReceiveCmd
	// 函数功能: iap文件发送完之后的回应数据检查
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟
	// 日 期: 2013/09/16
	// ****************************************************************
	private boolean ReceiveCmd(SysPara sys) {
		final int TIMEOUT = 1000;
		byte[] ur = new byte[1];
		long uBegin = System.currentTimeMillis();

		while (true) {
			// if (sys.com.QueryBuffer() >= 1)
			// {
			// sys.com.ReadData(&ur,1);
			// break;
			// }
			if (ReadData(sys.BtCom, ur, 1) >= 1) {
				break;
			}
			if ((System.currentTimeMillis() - uBegin) > TIMEOUT) {
				return false;
			}
			// Sleep(1);
		}

		if (ur[0] != 0x30) {
			return false;
		}

		return true;
	}

	// ****************************************************************
	// 函数名称: SendSBL
	// 函数功能: 发送引导文件2
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟
	// 日 期: 2013/09/16
	// ****************************************************************
	private boolean SendSBL(SysPara sys) {
		ParamPass param = new ParamPass();
		param.uByteRet = 'A';
		// Byte uCh = 'A';
		boolean ret = sys.protocol.Ping(sys.BtCom, param);

		if (!ret) {
			return false;
		} else if (ret && param.uByteRet != 'A') {
			return false;
		}

		Vector<Byte> vData = new Vector<Byte>();
		sys.bin.GetSBL(vData);

		if (!SendIapTest(sys, vData)) {
			return false;
		}

		return true;
	}

	// ****************************************************************
	// 函数名称: SendOtp
	// 函数功能: 发送otp数据
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟
	// 日 期: 2013/09/16
	// ****************************************************************
	private boolean SendOtp(SysPara sys) {
		Vector<Byte> votp = new Vector<Byte>();
		Vector<DataBlock> vblock = new Vector<DataBlock>();
		sys.bin.GetTBL(votp);

		if (votp.isEmpty()) {
			return false;
		}
		ConvertOtp(votp, vblock);

		int nCount = vblock.size();
		for (int m = 0; m < nCount; m++)
		// vector<DataBlock>::iterator it=vblock.begin(); it!=vblock.end();
		// it++)
		{
			// 设置烧录起始地址
			if (!sys.protocol.SendStartAddr(sys.BtCom, sys.uStation,
					vblock.get(m).uAddr)) {
				return false;
			}
			// 设置长度
			if (!sys.protocol.SendBinSize(sys.BtCom, sys.uStation,
					vblock.get(m).uLen)) {
				return false;
			}
			// 发送数据帧
			int frmcnt = 0;
			if (vblock.get(m).vData.size() % REALDATASIZE == 0)
				frmcnt = vblock.get(m).vData.size() / REALDATASIZE;
			else
				frmcnt = vblock.get(m).vData.size() / REALDATASIZE + 1;

			for (int i = 0; i < frmcnt; i++) {
				byte[] buf = new byte[DATASIZE];
				Arrays.fill(buf, (byte) 0xff);
				// memset(buf,0xff,DATASIZE); //填充0xff

				// 封装数据
				int j = i * REALDATASIZE;
				buf[0] = (byte) (i & 0xff);
				buf[1] = (byte) ((i >> 8) & 0xff);
				for (int k = 0; k < REALDATASIZE
						&& (j + k) < (vblock.get(m).vData).size(); k++) {
					buf[2 + k] = (vblock.get(m).vData).get(j + k);
				}
				// 获取crc32
				byte[] bufTmp = new byte[REALDATASIZE];
				System.arraycopy(buf, 2, bufTmp, 0, REALDATASIZE);
				CRC32 crc = new CRC32();
				long ucrc = crc.GetCRC32(bufTmp, REALDATASIZE);
				// int ucrc = crc.GetCRC32(buf+2,REALDATASIZE);
				buf[DATASIZE - 6] = (byte) (ucrc & 0xff);
				buf[DATASIZE - 5] = (byte) ((ucrc >> 8) & 0xff);
				buf[DATASIZE - 4] = (byte) ((ucrc >> 16) & 0xff);
				buf[DATASIZE - 3] = (byte) ((ucrc >> 24) & 0xff);
				buf[DATASIZE - 2] = (byte) 0xff; // 表示下位机不需要解密此帧数据
				buf[DATASIZE - 1] = (byte) 0xff;

				if (!sys.protocol.SendFrame(sys.BtCom, sys.uStation, buf,
						DATASIZE, m_bRetry,m_uTimeOut)) {
					return false;
				}
			}
		}

		return true;
	}

	// ****************************************************************
	// 函数名称: ConvertOtp
	// 函数功能: otp数据转换
	// 访问属性: private
	// 返回值: void
	// 函数参数: Vector<Byte> & vSrc
	// 函数参数: vector<DataBlock> & vDes
	// 作 者: 李正伟
	// 日 期: 2013/09/16
	// ****************************************************************
	private void ConvertOtp(Vector<Byte> vSrc, Vector<DataBlock> vDes) {
		int i = 0;
		while (i < vSrc.size()) {
			DataBlock block = new DataBlock();
			int ulen = CharToInt(vSrc.get(i), vSrc.get(i + 1), vSrc.get(i + 2),
					vSrc.get(i + 3));
			i += 4;
			int uaddr = CharToInt(vSrc.get(i), vSrc.get(i + 1),
					vSrc.get(i + 2), vSrc.get(i + 3));
			i += 4;
			block.uLen = ulen;
			block.uAddr = uaddr;
			for (int j = 0; j < ulen * 2; j++) {
				block.vData.add(vSrc.get(i + j));
			}
			vDes.add(block);
			i += ulen * 2;
		}
	}

	// ****************************************************************
	// 函数名称: CharToInt
	// 函数功能: 字符转为32位整形数
	// 访问属性: private
	// 返回值: unsigned int
	// 函数参数: unsigned char c1
	// 函数参数: unsigned char c2
	// 函数参数: unsigned char c3
	// 函数参数: unsigned char c4
	// 作 者: 李正伟
	// 日 期: 2013/09/16
	// ****************************************************************
	private int CharToInt(byte c1, byte c2, byte c3, byte c4) {
		return (((c4 & 0xFF) << 24) | ((c3 & 0xFF) << 16) | ((c2 & 0xFF) << 8) | (c1 & 0xFF));
	}

	// ****************************************************************
	// 函数名称: SendApp
	// 函数功能: 烧录用户程序
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟
	// 日 期: 2013/09/16
	// ****************************************************************
	private boolean SendApp(SysPara sys) {
		// 设定波特率
		// if (!sys.protocol.ChangeBaudrate(sys.BtCom, sys.uStation, sys.uBaud))
		// {
		// m_sMsg += _T("修改波特率失败\r\n");
		// return false;
		// }
		// 设定芯片
		if (!sys.protocol.SetHost(sys.BtCom, sys.uStation, sys.bin.GetHost())) {
			m_sMsg += "设定主从机失败\n";
			return false;
		}
		// 读电子标签
		ParamPass param = new ParamPass();

		// Integer uline = 0,utype = 0,ucpu = 0;
		if (!sys.protocol.GetBootID(sys.BtCom, sys.uStation, param)) {
			m_sMsg += "读取电子标签失败\n";
			return false;
		}
		// 核对烧录文件里的设定是否跟读取的电子标签一致
		if (sys.bin.GetPLine() != param.uAppPLine
				|| sys.bin.GetPType() != param.uAppPType
				|| sys.bin.GetIC() != param.uAppIC) {
			m_sMsg += "烧录文件与芯片不匹配，请更换烧录文件!\n";
			return false;
		}
		// 发送随机码
		int urand = sys.bin.GetRand();
		if (!sys.protocol.SendRandomCode(sys.BtCom, sys.uStation, urand)) {
			m_sMsg += "发送随机码失败\n";
			return false;
		}
		// 设置烧录起始地址
		int ustartaddr = sys.bin.GetStartAddress();
		if (!sys.protocol.SendStartAddr(sys.BtCom, sys.uStation, ustartaddr)) {
			m_sMsg += "设定烧录起始地址失败\n";
			return false;
		}
		// 设置bin文件长度
		int usize = sys.bin.GetSize();
		if (!sys.protocol.SendBinSize(sys.BtCom, sys.uStation, usize)) {
			m_sMsg += "发送Bin文件长度失败\n";
			return false;
		}
		// 擦除芯片，耗时较长 5~7s，需要增加进度条。
		m_sMsg += "擦除中......";
		// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
				IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();
		if (!sys.protocol.Erase(sys.BtCom, sys.uStation)) {
			m_sMsg += "失败\n";
			return false;
		}
		m_sMsg += "成功\n";
		// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
				IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();
		// 发送数据帧
		m_sMsg += "编程中......";
		// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
				IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();
		Vector<Byte> vapp = new Vector<Byte>();
		sys.bin.GetApp(vapp);
		int frmcnt = vapp.size() / DATASIZE;
		for (int i = 0; i < frmcnt; i++) {
			byte[] buf = new byte[DATASIZE];
			for (int j = 0; j < DATASIZE; j++) {
				buf[j] = vapp.get(i * DATASIZE + j);
			}
			if (!sys.protocol.SendFrame(sys.BtCom, sys.uStation, buf, DATASIZE,
					m_bRetry,m_uTimeOut)) {
				m_sMsg += "失败\n";
				return false;
			}
			// ::SendMessage(sys.hWnd,UM_PROGRESSBAR,i+1,frmcnt);
			int pos = (int) ((double) (i + 1) * 100 / frmcnt);
			sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
					IProgram.PROGRAM_PROGRESS, pos).sendToTarget();
		}
		m_sMsg += "成功\n";
		// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
				IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();
		// 发送校验
		int ucrc = sys.bin.GetCRC32();
		if (!sys.protocol.SendBinCRC32(sys.BtCom, sys.uStation, ucrc)) {
			m_sMsg += "发送校验码失败\n";
			return false;
		}
		// 判断是否发送启动命令
		if (sys.bRun) {
			int entrypoint = DSPSuccessJumpAddr(sys);
			param.bBurn = false;
			// boolean bBurn = false;
			if (!sys.protocol.Start(sys.BtCom, sys.uStation, entrypoint, param)) {
				m_sMsg += "发送启动命令失败\n";
				return false;
			}
		}

		return true;
	}

	// ************************************************************************
	// 函数名称: GetTimeOut
	// 函数说明: 获取超时时间
	// 访问属性: private
	// 返 回 值: void
	// 函数参数: unsigned int uVer
	// 作 者: 李正伟
	// 日 期: 2014/05/12
	// ************************************************************************
	private void GetTimeOut(int uVer) {
		if (uVer == 0xffff) {
			m_uTimeOut = 10000;
		} else {
			m_uTimeOut = 1000;
		}
	}

	// ************************************************************************
	// 函数名称: GetRetry
	// 函数说明: 由版本确定是否重发
	// 访问属性: private
	// 返 回 值: void
	// 函数参数: unsigned int uVer
	// 作 者: 李正伟
	// 日 期: 2014/05/12
	// ************************************************************************
	private void GetRetry(int uVer) {
		if (uVer == 0xffff) {
			m_bRetry = false;
		} else {
			m_bRetry = true;
		}
	}

	// ****************************************************************
	// 函数名称: DSPSuccessJumpAddr
	// 函数功能: 获取APP发送成功后的跳转地址
	// 访问属性: private
	// 返回值: unsigned int
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟
	// 日 期: 2013/09/16
	// ****************************************************************
	private int DSPSuccessJumpAddr(SysPara sys) {
		int addr = 0;
		int ic = sys.bin.GetIC();
		if (ic == 0x10 || ic == 0x11 || ic == 0x12 || ic == 0x14 || ic == 0x15
				|| ic == 0x16) {
			addr = 0x3f7ff6;
		} else if (ic == 0x17 || ic == 0x18) {
			addr = 0x33fff6;
		}

		return addr;
	}

	// ****************************************************************
	// 函数名称: DSPErrJumpAddr
	// 函数功能: 获取APP发送失败后的跳转地址
	// 访问属性: private
	// 返回值: unsigned int
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟
	// 日 期: 2014/03/26
	// ****************************************************************
	private int DSPErrJumpAddr(SysPara sys, int type) {
		int addr = 0;
		int PLine = sys.bin.GetPLine();
		int ic = sys.bin.GetIC();
		if (1 == type) // 错误地址1
		{
			if (0x16 == ic || ((0x15 == ic) && (PLine != 21))) {
				addr = 0x3ffb50;
			} else if ((0x15 == ic) && (21 == PLine)) {
				addr = 0x3ff7dd;
			} else if (0x10 == ic || 0x11 == ic || 0x12 == ic) {
				if (m_uBLVer == 1)
					addr = 0x3ffd4e;
				else if (m_uBLVer == 2 || m_uBLVer == 3 || m_uBLVer == 4)
					addr = 0x3ffd63;
			} else if (0x17 == ic || 0x18 == ic) {
				if (m_uBLVer == 1)
					addr = 0x3ff633;
				else if (m_uBLVer == 2)
					addr = 0x3ff63c;
			}
		} else if (2 == type) {
			if (0x10 == ic || 0x11 == ic || 0x12 == ic || 0x14 == ic
					|| 0x15 == ic || 0x16 == ic) {
				addr = 0x3f7ff6;
			} else if (0x17 == ic || 0x18 == ic) {
				addr = 0x33fff6;
			}
		}

		return addr;
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
			System.arraycopy(bufferTemp, 0, buffer, nRetSize, length);
			nRetSize += length;
			nReadTimes++;
			if (nReadTimes > AppCmd.WM_RESETREADNUM) // 读取30次 大概25ms*15
			{
				// Protocol.m_dwErrCode = AppCmd.APPCMD_READ_FAIL;
				return 0;
			}
		}
		return nRetSize;
	}
}