package com.inovance.elevatorprogram;

import java.util.Vector;
//import android.util.Log;
import com.inovance.protocol.AppCmd;
import com.inovance.protocol.BluetoothService;

public class DSPExProgram extends BaseProgram {
	private int m_uBLVer = 1; // 官方bootloader版本号

	public boolean CoreProgram(SysPara sys) {
		m_bBusy = true;
		m_sMsg = ""; // 清空旧的信息
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
			sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
					IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();
			int nCode = sys.protocol.GetErrorCode();
			// ::SendMessage(sys.hWnd,UM_PROGRESSINFO,1,0);
			sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO, IProgram.ERROR_CODE, nCode).sendToTarget();
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

		m_sMsg += "连接中......";
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
			m_sMsg += "成功\n初始化......";
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
			sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
					IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();

			// Sleep(100); //延时
			if (!sys.protocol.InBoot(sys.BtCom, sys.uStation, 200)) {
				m_sMsg += "设备未进入Boot区\n"; // 芯片实际运行在ram
				return false;
			}
			if (!SendBoot(sys)) // 发送boot
			{
				m_sMsg += "发送boot数据失败\n";
				return false;
			}

			// 软件复位，省去重新上电步骤。
			int addr = DSPErrJumpAddr(sys, 2);
			param.bBurn = false;
			// boolean bBurn = false;
			sys.protocol.Start(sys.BtCom, sys.uStation, addr, param);

			m_sMsg += "烧录完成\n";
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
		// boolean bFirstTime = true; //
		m_sMsg += "连接中......";
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
				IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();

		// START:
		ParamPass param = new ParamPass();
		param.uByteRet = 'A';
		boolean ret = sys.protocol.Ping(sys.BtCom, param);
		if (!ret) // 没有应答
		{
			String serr = "";
			ret = TryUpgradeMode(sys, serr);
			if (ret)// 成功跳转到汇川boot区
			{
				m_sMsg += "成功\n";
				sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
						IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();
				// 将站号改为设定值（其实应该在进入boot后就改站号。改站号的原因：进入boot后底层的站号是0xff，表示万能地址。
				// 如果因为意外，多个站点停留在boot区且站号都是0xff，再次烧录时会出现多个应答。）
				if (!sys.protocol.ChangStation(sys.BtCom, sys.uStation)) {
					m_sMsg += "修改站号失败\n";
					return false;
				}
			} 
			else 
			{
				m_sMsg += "失败\n";
				m_sMsg += serr;
				return false;
			}
		} 
		else if (ret && param.uByteRet != 'A') // 探测失败
		{
			m_sMsg += "失败\n";
			return false;
		} 
		else // 掉电升级
		{
			if (sys.bMaster) {
				if (!SendFSBL(sys)) 
				{
					return false;
				}
			} 
			else 
			{
				m_sMsg += "升级程序不允许跳线，请去掉跳线再上电！\n";
				return false;
			}
		}
		
		// 开始编程
		if (!SendApp(sys)) {
			int addr = DSPErrJumpAddr(sys,2);
			param.bBurn = false;
			if (!sys.protocol.Start(sys.BtCom, sys.uStation, addr, param)) {
				return false;
			}
			return false;
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
	private boolean TryUpgradeMode(SysPara sys, String serr) {

		boolean bfind = false;
		// 读APP区电子标签，判断是否跟选择的文件一致。
		ParamPass param = new ParamPass();
		if (sys.protocol.GetAppID(sys.BtCom, sys.uStation, param)) {
			if ((sys.bin.GetPLine() != param.uAppPLine)
					|| (sys.bin.GetPType() != param.uAppPType)
					|| (sys.bin.GetIC() != param.uAppIC)) {
				serr = "原因：APP区电子标签跟烧录文件中的电子标签不一致，请选择正确的烧录文件！\n";
				return false;
			}
			bfind = true;
		}

		boolean bInBoot = false;
		if (bfind) {
			// 获取官方bootloader版本号
			sys.protocol.GetBootLoaderVersion(sys.BtCom, sys.uStation, param);
			m_uBLVer = param.uVer;
			if (JumpIntoBoot(sys)) {
				if (sys.protocol.InBoot(sys.BtCom, sys.uStation, 500)) {
					bInBoot = true;
				}
			}
		} else {
			// 是否已经在Boot区
			if (sys.protocol.InBoot(sys.BtCom, sys.uStation, 500)) {
				bInBoot = true;
			}
		}
		if (!bInBoot) {
			serr += "设备未进入Boot区\n";
			return false;
		}
		return true;
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
		// 读设备状态
		int state = 0;
		if (!sys.protocol.GetMachineState(sys.BtCom, sys.uStation, state)) {
			return false;
		}

		// 跳转到boot区
		int ustate = 0;
		if (!sys.protocol.JumpToBoot(sys.BtCom, sys.uStation, ustate)) {
			return false;
		}
		return true;
	}

	//发送第一段和第二段引导文件
	private boolean SendFSBL(SysPara sys)
	{
		m_sMsg += "成功\n";
		m_sMsg += "初始化......";
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
				IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();
		
		//下发引导文件1
		Vector<Byte> vData = new Vector<Byte>();
		sys.bin.GetFBL(vData);

		if (!SendIapTest(sys,vData))
		{
			m_sMsg += "失败\n请重新上电！\n";
			return false;
		}
		if (!ReceiveCmd(sys))
		{
			m_sMsg += "失败\n芯片解密失败！\n原因：烧录文件错误或者芯片损坏！请重新上电再试一次！\n";
			return false;
		}
		//Sleep(100); //延时
		if (!SendSBL(sys))
		{
			m_sMsg += "失败\n请重新上电！\n";
			return false;
		}
		m_sMsg += "成功\n";
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
				IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();

		//Sleep(100);
		//unsigned int baud[5] = {9600, 38400, 115200, 57600, 19200};
		if (!sys.protocol.InBoot(sys.BtCom,sys.uStation,200))
		{
			m_sMsg += "设备未进入Boot区\n"; // 芯片实际运行在ram
			return false;
		}

		if (!SendBoot(sys)) //发送固化的boot
		{
			m_sMsg += "发送boot数据失败\n";
			int addr = DSPErrJumpAddr(sys,1);
			ParamPass par = new ParamPass();
			par.bBurn = false;
			sys.protocol.Start(sys.BtCom, sys.uStation, addr, par);
			return false;
		}

		//跳转到固定地址（汇川boot）
		if (!Goto_Flash(sys))
		{
			m_sMsg += "跳转失败\n";
			return false;
		}

		//Sleep(100); //对整个FLASH进行CRC32校验，需要等待一段时间。
		// 修改波特率到指定值
		//unsigned int ubaud[5] = {9600, 38400, 115200, 57600, 19200};
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
	}
	
	//跳转到固定地址(有boot数据)
	private boolean Goto_Flash(SysPara sys)
	{
		//bool bBurn = false;
		int addr = DSPSuccessJumpAddr(sys);
		return sys.protocol.Start(sys.BtCom,sys.uStation,addr,new ParamPass());
	}

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
		//return ((c4 << 24) | (c3 << 16) | (c2 << 8) | c1);
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
			//Log.i("AAAA", m_sMsg);
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
		m_sMsg += "成功\n编程中......";
		sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
				IProgram.PROGRAM_TEXT_INFO, 0, m_sMsg).sendToTarget();
		
		// 发送数据帧
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
//			sys.hWnd.obtainMessage(MESSAGE_PROGRESSBAR, i + 1, frmcnt)
//					.sendToTarget();
			int pos = (int) ((double) (i + 1) * 100 / frmcnt);
			sys.hWnd.obtainMessage(MESSAGE_PROGRESSINFO,
					IProgram.PROGRAM_PROGRESS, pos).sendToTarget();
		}
		m_sMsg += "成功\n";
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

	//************************************************************************
	// 函数名称:    SendBoot
	// 函数说明:    发送DSP的固化boot文件
	// 访问属性:    private 
	// 返 回 值:    bool
	// 函数参数:    CSysPara & sys
	// 作    者:    李正伟
	// 日    期:    2014/05/07
	//************************************************************************
	private boolean SendBoot(SysPara sys)
	{
		//读DSP的boot文件
		Vector<Byte> vtbl = new Vector<Byte>();
		sys.bin.GetTBL(vtbl);
		if (vtbl.isEmpty())
		{
			return false;
		}

		//int uaddr = sys.bin.CharToInt(vtbl.get(0),vtbl.get(1),vtbl.get(2),vtbl.get(3));
		int uaddr = CharToInt(vtbl.get(0),vtbl.get(1),vtbl.get(2),vtbl.get(3));
		int nCount  = vtbl.size() - 4;
		byte[] buf = new byte[nCount];
		for(int i = 0; i< nCount; i ++)
		{
			buf[i] = vtbl.get(i + 4);
		}
		//vector<unsigned char> vdata(vtbl.begin()+4,vtbl.end());
		int iRealSize = nCount / DATASIZE * REALDATASIZE / 2;  //长度以字为单位
		//发送随机码
		int urand = sys.bin.GetRand();
		if (!sys.protocol.SendRandomCode(sys.BtCom,sys.uStation,urand))
		{
			return false;
		}
		//发送起始地址
		if (!sys.protocol.SendStartAddr(sys.BtCom,sys.uStation,uaddr))
		{
			return false;
		}
		//发送长度
		if (!sys.protocol.SendBinSize(sys.BtCom,sys.uStation,iRealSize)) //长度以字为单位
		{
			return false;
		}
		//擦除
		if (!sys.protocol.Erase(sys.BtCom,sys.uStation))
		{
			return false;
		}
		//发送数据帧
		int frmcnt = nCount / DATASIZE;
		for (int i=0; i<frmcnt; i++)
		{
			byte[] bufFrame = new byte[DATASIZE];
			for (int j=0; j<DATASIZE; j++)
			{
				bufFrame[j]=buf[i*DATASIZE+j];
			}
			if (!sys.protocol.SendFrame(sys.BtCom,sys.uStation,bufFrame,DATASIZE))
			{
				return false;
			}
		}
		return true;
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
