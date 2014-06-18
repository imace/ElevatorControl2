package com.inovance.elevatorprogram;

import java.util.Arrays;
import java.util.Vector;
import com.inovance.protocol.AppCmd;
import com.inovance.protocol.Protocol;

public class ARMProgram extends BaseProgram {

	private boolean m_bAlarm = false; // 是否需要警告用户，当前需要重新上电。

	public boolean CoreProgram(SysPara sys) {
		m_bBusy = true;
		//m_sMsg = ""; // 清空旧的信息
		// sys.com.TurnOnLog(m_sLogFile.c_str());
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
			//发送失败消息
			sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();
			int nCode = sys.protocol.GetErrorCode();
			// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,1,0);
			sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.ERROR_CODE, nCode).sendToTarget();
			
			// sys.hWnd.obtainMessage();
		} else {
			//String sElapse = "";
			int min = (int)fElapse / 60;
			fElapse = fElapse - min * 60;
			//sElapse = String.format("烧录用时:%d分 %.1f秒\n", min, fElapse);
			//m_sMsg += sElapse;
			// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,1,0);
			sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.PROGRAM_TIME, (int)fElapse).sendToTarget();
		}
		// sys.com.TurnOffLog();
		m_bBusy = false;

		if (m_bAlarm) {
			m_sMsg += "注意：此次烧录完成后必须重新上电！\n";
			// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,1,0);
			//sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.PROGRAM_TEXT_INFO, 0, ).sendToTarget();
			sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();
		}
		return ret;
	}

	// ****************************************************************
	// 函数名称: BootLoader
	// 函数功能: 烧录bootloader
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟 马俊移植
	// 日 期: 2013/09/22
	// ****************************************************************
	private boolean BootLoader(SysPara sys) {
		final int RepeatTimes = 2;
		int Times = 1;
		// 连接串口
		// if (!sys.com.Open(sys.uPort,57600))
		// {
		// m_sMsg += GetErrorMsg(sys.com.GetLastErrorCode());
		// m_sMsg += _T("\n");
		// return false;
		// }
		// //发送探测字符
		// m_sMsg += _T("连接中......");
		// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
		REPEAT: while (true) {
			ParamPass Param = new ParamPass();
			Param.uByteRet = 0x7F;
			//Byte uCh = 0x7F;
			// uCh = 0x7F;
			boolean ret = sys.protocol.Ping(sys.BtCom, Param);
			if (!ret) // 没有回应
			{
				if (Times++ < RepeatTimes) {
					continue REPEAT;
				} else {
					m_sMsg += "失败\n";
					return false;
				}
			}

			else if (ret && (Param.uByteRet == 0x79 || Param.uByteRet == 0x1F)) // 连接成功
			{
				m_sMsg += "成功\n初始化......";
				// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
				sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.PROGRAM_TEXT_INFO, 0,m_sMsg)
						.sendToTarget();
				//m_sMsg += "初始化......";
				// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
				//sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, 0, 0).sendToTarget();

				// 发送iap代码并启动iap程序
				if (!SendIap(sys)) {
					m_sMsg += "失败\n";
					// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
					sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,IProgram.PROGRAM_TEXT_INFO, 0,m_sMsg)
							.sendToTarget();
					return false;
				}
				try {
					Thread.sleep(100);
				} catch (Exception e) {
					e.printStackTrace();
				}
				m_sMsg += "成功\n";
				// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
				sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.PROGRAM_TEXT_INFO, 0,m_sMsg)
						.sendToTarget();

				if (!sys.protocol.InBoot(sys.BtCom,sys.uStation,200))
				{
					m_sMsg += "设备未进入Boot区\n"; // 芯片实际运行在ram
					return false;
				}
				byte[] seq = new byte[12]; // 芯片序列号
				if (!ReadSeq(sys, seq)) {
					m_sMsg += "获取芯片序列号失败\n";
					return false;
				}
				// 发送随机码(解密用)
				int urand = sys.bin.GetRand();
				if (!sys.protocol
						.SendRandomCode(sys.BtCom, sys.uStation, urand)) {
					m_sMsg += "发送随机码失败\n";
					return false;
				}
				// 发送boot文件(引导文件2)
				if (!SendSBL(sys)) {
					m_sMsg += "发送引导文件2失败\n";
					return false;
				}
				// 发送芯片序列号
				if (!SendSeq(sys, seq)) {
					m_sMsg += "发送芯片序列号失败\n";
					return false;
				}

				// 软件复位，省去重新上电步骤。
				Goto_Boot(sys);

				m_sMsg += "烧录完成\n";
				// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
				sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,IProgram.PROGRAM_TEXT_INFO, 0,m_sMsg)
						.sendToTarget();
			} else {
				m_sMsg += "失败\n";
				return false;
			}
			break;
		}

		return true;
	}

	// ****************************************************************
	// 函数名称: Program
	// 函数功能: 烧录
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟 马俊移植
	// 日 期: 2013/09/16
	// ****************************************************************
	private boolean Program(SysPara sys) {
		final int RepeatTimes = 2;
		int Times = 1;
		// 连接串口
		// if (!sys.BtCom.Open(sys.uPort,57600))
		// {
		// m_sMsg += GetErrorMsg(sys.com.GetLastErrorCode());
		// m_sMsg += _T("\n");
		// return false;
		// }
		//发送探测字符
		m_sMsg += "连接中......";
		//::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.PROGRAM_TEXT_INFO, 0,m_sMsg).sendToTarget();
		REPEAT: while (true) {
			ParamPass Param = new ParamPass();
			Param.uByteRet = 0x7F;
			//Byte uCh = 0x7F;
			boolean ret = sys.protocol.Ping(sys.BtCom, Param);
			if (!ret) // 没有回应
			{
				if (Times++ < RepeatTimes) {
					continue REPEAT;
				} else // 尝试是否运行在APP区
				{
					Param.strErr = "";
					boolean retTemp = TryUpgradeMode(sys, Param);
					if (retTemp) // 已经跳转到boot区
					{
						// 跳转到新地址需要延时
						m_sMsg += "成功\n";
						// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
						sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.PROGRAM_TEXT_INFO, 0,m_sMsg)
								.sendToTarget();

						// 将站号改为设定值
						if (!sys.protocol.ChangStation(sys.BtCom, sys.uStation)) {
							return false;
						}
					} else {
						m_sMsg += "失败\n";
						m_sMsg += Param.strErr;
						return false;
					}
				}
			} 
			else if (ret && (Param.uByteRet == 0x79 || Param.uByteRet == 0x1F)) // 连接成功
			{
				if (!BootCmds(sys))
					return false;
			} 
			else {
				m_sMsg += "失败\n";
				return false;
			}
			break;
		}

		if (!SendApp(sys)) {
			// 跳转
			Goto_Boot(sys);
			return false;
		}

		// 判断是否发送启动命令
		if (sys.bRun) {
			ParamPass param = new ParamPass();
			param.bBurn= false;
			//boolean bBurn = false;
			if (!Run(sys, param)) {
				m_sMsg += "发送启动命令失败\n";
				return false;
			}
			m_bAlarm = param.bBurn;
		}
		else
		{
			m_bAlarm = false;
		}

		return true;
	}

	// ****************************************************************
	// 函数名称: TryUpgradeMode
	// 函数功能: 判断是否处在app区并跳转到boot区
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟 马俊移植
	// 日 期: 2013/09/16
	// ****************************************************************
	private boolean TryUpgradeMode(SysPara sys, ParamPass par)
	{
		boolean bfind = false;
		boolean bInBoot = false;
		//读APP区电子标签，判断是否跟选择的文件一致。
		ParamPass param = new ParamPass();
//		Integer uAppPLine = Integer.valueOf(0);
//		Integer uAppPType = Integer.valueOf(0);
//		Integer uAppIC = Integer.valueOf(0);

		if (sys.protocol.GetAppID(sys.BtCom, sys.uStation, param))
		{
			if ((sys.bin.GetPLine() != param.uAppPLine ) ||
				(sys.bin.GetPType() != param.uAppPType)  ||
				(sys.bin.GetIC()    != param.uAppIC))
			{
				par.strErr = "原因：APP区电子标签跟烧录文件中的电子标签不一致，请选择正确的烧录文件！\n";
				return false;
			}
			bfind = true;
		}
		if(bfind)
		{
			if(JumpIntoBoot(sys))
			{
				if(sys.protocol.InBoot(sys.BtCom, sys.uStation, 500))
				{
					bInBoot = true;
				}
			}
		}
		else
		{
			//读boot区电子标签判断当前程序是否运行在boot区
			if(sys.protocol.InBoot(sys.BtCom, sys.uStation, 500))
			{
				bInBoot = true;
			}
		}
		if(!bInBoot)
		{
			par.strErr += "设备未进入Boot区\n";
			return false;
		}
		return true;

	}

	// ****************************************************************
	// 函数名称: JumpIntoBoot
	// 函数功能: 跳转到boot区
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟 马俊移植
	// 日 期: 2013/09/16
	// ****************************************************************
	private boolean JumpIntoBoot(SysPara sys) {
		// 获取设备状态
		int state = 0;
		boolean ret = sys.protocol
				.GetMachineState(sys.BtCom, sys.uStation, state);
		if (!ret) { // 如果设备在运行退出
			if (AppCmd.APPCMD_RUNNING == sys.protocol.GetErrorCode()) {
				return false;
			}
		}

		// 跳转到boot区
		int ustate = 0;
		if (!sys.protocol.JumpToBoot(sys.BtCom, sys.uStation, ustate)) {
			
			return false;
		}

		return true;
	}

	// ****************************************************************
	// 函数名称: BootCmds
	// 函数功能: 发送引导文件
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟 马俊移植
	// 日 期: 2013/09/16
	// ****************************************************************
	private boolean BootCmds(SysPara sys)
	{
		m_sMsg += "成功\n";
		//::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
		m_sMsg += "初始化......";
		//::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.PROGRAM_TEXT_INFO, 0,m_sMsg)
		.sendToTarget();
		//解保护
		//发送iap代码并启动iap程序
		if (!SendIap(sys))
		{
			m_sMsg += "失败\n";
			//::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
			sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.PROGRAM_TEXT_INFO, 0,m_sMsg)
			.sendToTarget();
			return false;
		}
		m_sMsg += "成功\n";
		//::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.PROGRAM_TEXT_INFO, 0,m_sMsg)
		.sendToTarget();

		if (!sys.protocol.InBoot(sys.BtCom,sys.uStation,500))
		{
			m_sMsg += "设备未进入Boot区\n"; // 芯片实际运行在ram
			return false;
		}
		
		if (!sys.protocol.Otp(sys.BtCom,sys.uStation)) //otp区是否有数据
		{
			if ((sys.protocol.GetErrorCode() - Protocol.PROTOCOL_BOOT_BASE) == 0x9) //otp无数据
			{
				if (sys.bMaster)
				{
					byte[] seq = new byte[12]; //芯片序列号
					if (!ReadSeq(sys,seq))
					{
						m_sMsg += "获取芯片序列号失败\n";
						//goto ERRORLABLE;
						Goto_NoBoot(sys);
						return false;
					}
					//发送随机码(解密用)
					int urand = sys.bin.GetRand();
					if (!sys.protocol.SendRandomCode(sys.BtCom,sys.uStation,urand))
					{
						m_sMsg += "发送随机码失败\n";
						return false;
					}
					//发送boot文件(引导文件2)
					if (!SendSBL(sys))
					{
						m_sMsg += "发送引导文件2失败\n";
						//goto ERRORLABLE;
						Goto_NoBoot(sys);
						return false;
					}
					//发送芯片序列号
					if (!SendSeq(sys,seq))
					{
						m_sMsg += "发送芯片序列号失败\n";
						//goto ERRORLABLE;
						Goto_NoBoot(sys);
						return false;
					}
				}
				else
				{
					m_sMsg += "boot区无数据，需要先烧录BootLoader！\n";
					//goto ERRORLABLE;
					Goto_NoBoot(sys);
					return false;
				}
			}
			else
			{
				m_sMsg += "查询boot区是否有数据失败\n";
				//goto ERRORLABLE;
				Goto_NoBoot(sys);
				return false;
			}
		}

		//跳转到固定地址
		if (!Goto_Boot(sys))
		{
			m_sMsg += "跳转失败\n";
			return false;
		}

		if (!sys.protocol.InBoot(sys.BtCom,sys.uStation,200))
		{
			m_sMsg += "设备未进入Boot区\n"; // 芯片实际运行在ram
			return false;
		}
		if (!sys.protocol.ChangStation(sys.BtCom,sys.uStation))
		{
			m_sMsg += "修改站号失败\n";
			return false;
		}

		return true;

//	ERRORLABLE:
//		Goto_NoBoot(sys);
//		return false;
	}

	// ****************************************************************
	// 函数名称: SendIap
	// 函数功能: 利用官方的指令发送iap文件并启动iap
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟 马俊移植
	// 日 期: 2013/09/16
	// ****************************************************************
	private boolean SendIap(SysPara sys) {
		Vector<Byte> vdata = new Vector<Byte>();
		sys.bin.GetFBL(vdata);
		if (vdata.isEmpty()) {
			return false;
		}
		int uaddr = CharToInt(vdata.get(0), vdata.get(1), vdata.get(2), vdata.get(3));
		final int offset = 4;
		final int frmsize = 256; // 发送的数据帧中实际数据的长度
		int size = vdata.size() - offset;
		int div = size / frmsize;
		int mod = size % frmsize;
		byte[] buf = new byte[frmsize];

		// 写第一帧数据
		for (int k = 0; k < frmsize; k++) {
			buf[k] = vdata.get(k + offset);
		}
		// 写数据，1次最多写256字节。第一帧数据包含解保护操作，如果芯片读保护，第一帧就会解保护，后面的帧就不再重复解保护。
		int ustartaddr = uaddr;
		if (!sys.protocol.WriteMemoryFirst(sys.BtCom, ustartaddr, sys.bMaster,
				frmsize, buf)) {
			return false;
		}

		int i = 1;
		for (i = 1; i < div; i++) {
			for (int j = 0; j < frmsize; j++) {
				buf[j] = vdata.get(i * frmsize + j + offset);
			}
			// 写数据，1次最多写256字节
			ustartaddr = uaddr + i * frmsize;
			if (!sys.protocol.WriteMemory(sys.BtCom, ustartaddr, frmsize, buf)) {
				return false;
			}
		}
		// 发送余下的数据
		if (mod != 0) {
			for (int j = 0; j < mod; j++) {
				buf[j] = vdata.get(i * frmsize + j + offset);
			}
			ustartaddr = uaddr + i * frmsize;
			if (!sys.protocol.WriteMemory(sys.BtCom, ustartaddr, mod, buf)) {
				return false;
			}
		}
		// 启动
		if (!sys.protocol.Go(sys.BtCom, uaddr)) {
			return false;
		}

		return true;
	}

	// ****************************************************************
	// 函数名称: CharToInt
	// 函数功能: 字符转为32位整数
	// 访问属性: private
	// 返回值: int
	// 函数参数: char c1
	// 函数参数: char c2
	// 函数参数: char c3
	// 函数参数: char c4
	// 作 者: 李正伟 马俊移植
	// 日 期: 2013/09/16
	// ****************************************************************
	private int CharToInt(byte c1, byte c2, byte c3, byte c4) {
		return (((c4 & 0xFF) << 24) | ((c3 & 0xFF) << 16) | ((c2 & 0xFF) << 8) | (c1 & 0xFF));
	}

	// ****************************************************************
	// 函数名称: SendSBL
	// 函数功能: 发送引导文件2
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟 马俊移植
	// 日 期: 2013/09/16
	// ****************************************************************
	private boolean SendSBL(SysPara sys)
	{
		//读boot文件(sbl)
		Vector<Byte> vsbl = new Vector<Byte>();
		sys.bin.GetSBL(vsbl);
		if (vsbl.isEmpty())
		{
			return false;
		}

		int uaddr = CharToInt(vsbl.get(0),vsbl.get(1),vsbl.get(2),vsbl.get(3));
		Vector<Byte> vdata = new Vector<Byte>();
		int nCount = vsbl.size();
		for(int i = 0; i < nCount; i++)
		{
			vdata.add(vsbl.get(i + 4));
		}
		//(vsbl..begin()+4,vsbl.end()
		//发送起始地址
		if (!sys.protocol.SendStartAddr(sys.BtCom,sys.uStation,uaddr))
		{
			return false;
		}

		int iRealSize = vdata.size();            //boot文件加密前的大小
		boolean bBootEncrypt = sys.bin.IsBootEncrypt();      //boot文件是否加密
		if (bBootEncrypt)
		{
			iRealSize = vdata.size() / DATASIZE * REALDATASIZE;
		}
		//计算长度，如果文件长度较小，导致没有擦除序列号所在的页面会出错。
		int endAddr  = uaddr + iRealSize;
		int endSeq   = uSeqAddr + uSeqSize;
		int size     = (Math.max(endAddr,endSeq) - uaddr)/2;
		//发送长度
		if (!sys.protocol.SendBinSize(sys.BtCom,sys.uStation,size))//长度以字为单位
		{
			return false;
		}
		//擦除
		if (!sys.protocol.Erase(sys.BtCom,sys.uStation))
		{
			return false;
		}

		//发送数据帧
		if (!bBootEncrypt)
		{
			int frmcnt = 0;
			if (vdata.size()%REALDATASIZE == 0)
				frmcnt = vdata.size()/REALDATASIZE;
			else
				frmcnt = vdata.size()/REALDATASIZE+1;
			for (int i=0; i<frmcnt; i++)
			{
				byte[] buf = new byte[DATASIZE];
				//memset(buf,0xff,DATASIZ);
				Arrays.fill(buf,(byte)0xff);
				int j = i*REALDATASIZE;
				buf[0] = (byte)(i & 0xff);
				buf[1] = (byte)((i>>8) & 0xff);
				for (int k=0; k<REALDATASIZE && (j+k)<vdata.size(); k++)
				{
					buf[2+k]=vdata.get(j+k);
				}
				//获取crc32
				byte[] bufTmp = new byte[REALDATASIZE];
				System.arraycopy(buf, 2, bufTmp, 0, REALDATASIZE);
				CRC32 crc = new CRC32();
				long ucrc = crc.GetCRC32(bufTmp,REALDATASIZE);
				buf[DATASIZE-6] = (byte)(ucrc & 0xff);
				buf[DATASIZE-5] = (byte)((ucrc>>8) & 0xff);
				buf[DATASIZE-4] = (byte)((ucrc>>16) & 0xff);
				buf[DATASIZE-3] = (byte)((ucrc>>24) & 0xff);
				buf[DATASIZE-2] = (byte)0xff; //表示下位机不需要解密此帧数据
				buf[DATASIZE-1] = (byte)0xff;

				if (!sys.protocol.SendFrame(sys.BtCom,sys.uStation,buf,DATASIZE))
				{
					return false;
				}
			}
		}
		else
		{
			int frmcnt = vdata.size() / DATASIZE;
			for (int i=0; i<frmcnt; i++)
			{
				byte[] buf = new byte[DATASIZE];
				for (int j=0; j<DATASIZE; j++)
				{
					buf[j]=vdata.get(i*DATASIZE+j);
				}
				if (!sys.protocol.SendFrame(sys.BtCom,sys.uStation,buf,DATASIZE))
				{
					return false;
				}
			}
		}

		return true;
	}

	// ****************************************************************
	// 函数名称: ReadSeq
	// 函数功能: 读取ARM芯片的序列号，并加密序列号。
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 函数参数: char * pData
	// 作 者: 李正伟 马俊移植
	// 日 期: 2013/09/16
	// ****************************************************************
	private boolean ReadSeq(SysPara sys, byte[] pData) {
		byte[] cseq = new byte[12];
		if (!sys.protocol.GetSN(sys.BtCom, sys.uStation, cseq)) {
			return false;
		}
		// 检查序列号是否为全0
		int i = 0;
		for (i = 0; i < 12; i++) {
			if (cseq[i] != 0) {
				break;
			}
		}
		if (i == 12) // 序列号为全0
		{
			return false;
		}

		// 加密序列号
		int uT1 = 0x5D195C26, uT2 = 0x7B300A41, uT3 = 0x14A003B7; // 密钥
		int uSeq1, uSeq2, uSeq3;
		uSeq1 = (cseq[0] | (cseq[1] << 8) | (cseq[2] << 16) | (cseq[3] << 24));
		uSeq2 = (cseq[4] | (cseq[5] << 8) | (cseq[6] << 16) | (cseq[7] << 24));
		uSeq3 = (cseq[8] | (cseq[9] << 8) | (cseq[10] << 16) | (cseq[11] << 24));
		uSeq1 ^= uT1;
		uSeq2 ^= uT2;
		uSeq3 ^= uT3;

		pData[0] = (byte) (uSeq1 & 0xff);
		pData[1] = (byte) ((uSeq1 >> 8) & 0xff);
		pData[2] = (byte) ((uSeq1 >> 16) & 0xff);
		pData[3] = (byte) ((uSeq1 >> 24) & 0xff);
		pData[4] = (byte) (uSeq2 & 0xff);
		pData[5] = (byte) ((uSeq2 >> 8) & 0xff);
		pData[6] = (byte) ((uSeq2 >> 16) & 0xff);
		pData[7] = (byte) ((uSeq2 >> 24) & 0xff);
		pData[8] = (byte) (uSeq3 & 0xff);
		pData[9] = (byte) ((uSeq3 >> 8) & 0xff);
		pData[10] = (byte) ((uSeq3 >> 16) & 0xff);
		pData[11] = (byte) ((uSeq3 >> 24) & 0xff);
		return true;
	}

	// ****************************************************************
	// 函数名称: SendSeq
	// 函数功能: 发送加密后的芯片序列号
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 函数参数: char * buf
	// 作 者: 李正伟 马俊移植
	// 日 期: 2013/09/16
	// ****************************************************************
	private boolean SendSeq(SysPara sys, byte[] buf)
	{
		//发送起始地址
		if (!sys.protocol.SendStartAddr(sys.BtCom,sys.uStation,uSeqAddr))
		{
			return false;
		}
		//发送长度
		if (!sys.protocol.SendBinSize(sys.BtCom,sys.uStation,uSeqSize/2))
		{
			return false;
		}
		//发送数据帧
		byte[] data = new byte[DATASIZE];
		//memset(data,0,DATASIZE);
		Arrays.fill(data,(byte)0);
		for (int i=0; i<12; i++)
		{
			data[2+i] = buf[i];
		}
		byte[] dataTmp = new byte[REALDATASIZE];
		System.arraycopy(data, 2, dataTmp, 0, REALDATASIZE);
		//获取crc32
		CRC32 crc = new CRC32();
		long ucrc=crc.GetCRC32(dataTmp,REALDATASIZE);
		data[DATASIZE-6] = (byte)(ucrc & 0xff);
		data[DATASIZE-5] = (byte)((ucrc>>8) & 0xff);
		data[DATASIZE-4] = (byte)((ucrc>>16) & 0xff);
		data[DATASIZE-3] = (byte)((ucrc>>24) & 0xff);
		data[DATASIZE-2] = (byte)0xff; //表示下位机不需要解密此帧数据
		data[DATASIZE-1] = (byte)0xff;
		if (!sys.protocol.SendFrame(sys.BtCom,sys.uStation,data,DATASIZE))
		{
			return false;
		}

		return true;
	}

	// ****************************************************************
	// 函数名称: SendApp
	// 函数功能: 发送app数据
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟 马俊移植
	// 日 期: 2013/09/16
	// ****************************************************************
	private boolean SendApp(SysPara sys) {
		// // 修改波特率到指定值
		// sys.com.Close();
		// if (!sys.com.Open(sys.uPort,9600))
		// {
		// m_sMsg += _T("重新打开串口失败\n");
		// return false;
		// }
		// //设定波特率
		// if (!sys.protocol.ChangeBaudrate(sys.com, sys.uStation, sys.uBaud))
		// {
		// m_sMsg += _T("修改波特率失败\n");
		// return false;
		// }
		// 设定芯片
		if (!sys.protocol.SetHost(sys.BtCom, sys.uStation, sys.bin.GetHost())) {
			m_sMsg += "设定主从机失败\n";
			return false;
		}
		// 读电子标签
		ParamPass Param = new ParamPass();
		//Integer uline = 0, utype = 0, ucpu = 0;
		if (!sys.protocol
				.GetBootID(sys.BtCom, sys.uStation, Param)) {
			m_sMsg += "读取电子标签失败\n";
			return false;
		}
		// 核对烧录文件里的设定是否跟读取的电子标签一致
		if (sys.bin.GetPLine() != Param.uAppPLine || sys.bin.GetPType() != Param.uAppPType
				|| sys.bin.GetIC() != Param.uAppIC) {
			m_sMsg += "烧录文件与芯片不匹配,请更换烧录文件!\n";
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
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.PROGRAM_TEXT_INFO, 0,m_sMsg).sendToTarget();
		if (!sys.protocol.Erase(sys.BtCom, sys.uStation)) {
			m_sMsg += "失败\n";
			return false;
		}
		m_sMsg += "成功\n";
		// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.PROGRAM_TEXT_INFO, 0,m_sMsg).sendToTarget();
		// 发送数据帧
		m_sMsg += "编程中......";
		// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.PROGRAM_TEXT_INFO, 0,m_sMsg).sendToTarget();
		Vector<Byte> vapp = new Vector<Byte>();
		sys.bin.GetApp(vapp);
		int frmcnt = vapp.size() / DATASIZE;
		for (int i = 0; i < frmcnt; i++) {
			byte[] buf = new byte[DATASIZE];
			for (int j = 0; j < DATASIZE; j++) {
				buf[j] = vapp.get(i * DATASIZE + j);
			}
			if (!sys.protocol.SendFrame(sys.BtCom, sys.uStation, buf, DATASIZE)) {
				m_sMsg += "失败\n";
				return false;
			}
			// ::SendMessage(sys.hWnd,UM_PROGRESSBAR,i+1,frmcnt);
			int pos = (int) ((double) (i + 1) * 100 / frmcnt);
			sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.PROGRAM_PROGRESS, pos).sendToTarget();
		}
		m_sMsg += "成功\n";
		// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,0,0);
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.PROGRAM_TEXT_INFO, 0,m_sMsg).sendToTarget();
		// 发送校验
		int ucrc = sys.bin.GetCRC32();
		if (!sys.protocol.SendBinCRC32(sys.BtCom, sys.uStation, ucrc)) {
			m_sMsg += "发送校验码失败\n";
			return false;
		}

		// 波特率改为9600
		// if (!sys.protocol.ChangeBaudrate(sys.BtCom, sys.uStation, 9600))
		// {
		// m_sMsg += _T("修改波特率到9600bps失败\n");
		// return false;
		// }

		return true;
	}

	// ****************************************************************
	// 函数名称: Goto_Boot
	// 函数功能: 跳转到固定地址
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟 马俊移植
	// 日 期: 2013/09/16
	// ****************************************************************
	private boolean Goto_Boot(SysPara sys) {
		ParamPass param = new ParamPass();
		param.bBurn = false;
		//boolean bBurn = false;
		return sys.protocol.Start(sys.BtCom, sys.uStation, 0x08000004, param);
	}

	// ****************************************************************
	// 函数名称: Goto_NoBoot
	// 函数功能: 跳转到固定地址
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟 马俊移植
	// 日 期: 2013/09/16
	// ****************************************************************
	private boolean Goto_NoBoot(SysPara sys) {
		ParamPass param = new ParamPass();
		param.bBurn = false;
		//boolean bBurn = false;
		return sys.protocol.Start(sys.BtCom, sys.uStation, 0x1FFF0004, param);
	}

	// ****************************************************************
	// 函数名称: Run
	// 函数功能: 烧录完成后启动用户程序
	// 访问属性: private
	// 返回值: boolean
	// 函数参数: CSysPara & sys
	// 作 者: 李正伟 马俊移植
	// 日 期: 2013/09/18
	// ****************************************************************
	private boolean Run(SysPara sys, ParamPass param) {
		return sys.protocol.Start(sys.BtCom, sys.uStation, 0x08000004, param);
	}

}
