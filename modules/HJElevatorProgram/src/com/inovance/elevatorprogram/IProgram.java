package com.inovance.elevatorprogram;

import java.io.FileInputStream;
import java.util.ArrayList;

import com.inovance.protocol.BluetoothService;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

/*
 * 烧录接口
 */
public class IProgram {
	private static final String TAG = "IProgram";
	private SysPara mSysParam = null;
	// 烧录ARM或DSP
	private static DSPProgram gs_dsp = new DSPProgram();
	private static ARMProgram gs_arm = new ARMProgram();
	private static BaseProgram gs_pProg = gs_dsp; // 默认值
	private BurnThread mBurnThread; // 烧录线程

	private static IProgram ourInstance = new IProgram();
	public static final int ERROR_CODE = 1; // 故障码
	public static final int PROGRAM_TIME = 2; // 烧录时间
	public static final int PROGRAM_PROGRESS = 3; // 烧录进程
	public static final int PROGRAM_TEXT_INFO = 4; // 烧录文字信息

	public static IProgram getInstance() {
		return ourInstance;
	}

	private IProgram() {
	}

	public void SetProgramPara(BluetoothSocket socket,
			FileInputStream fileInStream, Handler handle) {
		if(null == socket)
			Log.d(TAG, "socket is null");
		BluetoothService ser = BluetoothService.getInstance();
		ser.SetBluetoothSocket(socket);
		mSysParam = new SysPara(BluetoothService.getInstance());
		int nFileRet = 1;
		if (null != mSysParam) {
			nFileRet = mSysParam.bin.ReadBinFile(fileInStream);
			mSysParam.hWnd = handle;
			mSysParam.uStation = 1;
			mSysParam.bRun = true;
		}
		String strMsg;
		if (0 != nFileRet) {
			strMsg = "烧录文件读取失败!";
			handle.obtainMessage(BaseProgram.MESSAGE_PROGRESSINFO,
					PROGRAM_TEXT_INFO, 0, strMsg);
		}
		else if (2 == nFileRet) {
			strMsg = "烧录文件解密失败!";
			handle.obtainMessage(BaseProgram.MESSAGE_PROGRESSINFO,
					PROGRAM_TEXT_INFO, 0, strMsg);
		}
	}

	public String GetBinFileInfo() {
		// 读bin文件获取文件信息
		if (mSysParam != null && !mSysParam.bin.IsEmpty()) {
			String sinfo = "烧录文件信息:\n";
			String sfmt = "";
			sinfo += "产品系列: " + mSysParam.bin.GetPLineName() + "\n";
			// sfmt.Format("产品型号: %s\r\n",mSysParm.bin.GetPTypeName().c_str());
			sinfo += "产品型号: " + mSysParam.bin.GetPTypeName() + "\n";
			// sfmt.Format(_T("芯片型号: 0x%X\r\n"),theApp.m_sys.bin.GetIC());
			sfmt = String.format("芯片型号: 0x%X\n", mSysParam.bin.GetIC());
			sinfo += sfmt;
			// sfmt.Format(_T("主从芯片: %s\r\n"),GetHostInfo(theApp.m_sys.bin.GetPLine(),theApp.m_sys.bin.GetHost()));
			sinfo += "主从芯片:"
					+ GetHostInfo(mSysParam.bin.GetPLine(),
							mSysParam.bin.GetHost()) + "\n";
			// sfmt.Format(_T("文件大小: %d字节\r\n"),theApp.m_sys.bin.GetSize()*2);
			sfmt = String.format("文件大小: %d字节\n", (mSysParam.bin.GetSize() * 2));
			sinfo += sfmt;
			return sinfo;
		}
		return "";
	}

	private String GetHostInfo(int uPLine, int uHost) {
		ArrayList<String> arr = new ArrayList<String>();
		if (uPLine == 11) {
			arr.add("控制板");
			arr.add("驱动板");
		} else if (uPLine == 13) {
			arr.add("MCU");
			arr.add("FPGA");
		} else {
			arr.add("主芯片");
			arr.add("辅芯片");
		}
		return arr.get(uHost);
	}

	public void StartProgram() {
		if (null != mSysParam && mSysParam.bin.IsEmpty()) {
			// Toast.makeText(getApplicationContext(), "请选择烧录文件!",
			// Toast.LENGTH_SHORT).show();
			String strMsg = "烧录文件读取失败，无法烧录!";
			mSysParam.hWnd.obtainMessage(BaseProgram.MESSAGE_PROGRESSINFO,
					PROGRAM_TEXT_INFO, 0, strMsg);
			return;
		}
		InitProgram();
		mBurnThread = new BurnThread(gs_pProg, mSysParam);
		mBurnThread.start();
	}

	private void InitProgram() {
		// 获取芯片编号
		int icid = mSysParam.bin.GetIC();
		if (icid >= 0x0010 && icid < 0x0100) // DSP 28系列
		{
			gs_pProg = gs_dsp;
		} else {
			gs_pProg = gs_arm;
		}
	}

	/**
	 * 烧录线程
	 */
	private class BurnThread extends Thread {
		private final BaseProgram threadProg;
		private final SysPara threadSysParam;

		public BurnThread(BaseProgram Prog, SysPara sysParam) {
			threadProg = Prog;
			threadSysParam = sysParam;
		}

		public void run() {
			// Log.i(TAG, "BEGIN Burn Firmware Thread:");
			setName("BurnThread");
			threadProg.CoreProgram(threadSysParam);
		}
	}
}
