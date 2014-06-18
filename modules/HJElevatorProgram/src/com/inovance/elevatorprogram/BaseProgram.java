package com.inovance.elevatorprogram;

abstract class BaseProgram {
	public static final int MESSAGE_PROGRESSINFO = 1;   //烧录信息消息
	//public static final int MESSAGE_PROGRESSBAR = 7;   	//进度条消息
	
	public static final int REALDATASIZE = 256; // 数据帧有效数据大小
	public static final int DATASIZE = 2 + REALDATASIZE + 4 + 2; // 帧数据大小

	public static final int uSeqAddr = 0x8000ff4; // 序列号起始地址
	public static final int uSeqSize = 12; // 序列号长度
	
	protected boolean m_bBootLoader = false;
	protected boolean m_bBusy = false;
	protected String m_sMsg;
	//protected String m_sLogFile;
	//protected Vector<ErrorInfoStruct> m_vErr = new Vector<ErrorInfoStruct>();
	
//	protected String GetErrorMsg(int nErrCode)
//	{
//		String smsg = "";
//		Iterator<ErrorInfoStruct> it = m_vErr.iterator();
//		while(it.hasNext())
//		{
//			ErrorInfoStruct ErrorInfo = it.next();
//			if(ErrorInfo.nNum == nErrCode)
//			{
//				smsg = ErrorInfo.sErr;
//				break;
//			}
//		}
//		return smsg;
//	}
//	
//	public String GetMessage()
//	{
//		return m_sMsg;
//	}
	
	public boolean IsBusy()
	{
		return m_bBusy;
	}
	
//	public void InitErrList(Vector<ErrorInfoStruct> verr)
//	{
////		m_vErr.clear(); //清空旧数据
////		Iterator<ErrorInfoStruct> it = m_vErr.iterator();
////		while(it.hasNext())
////		{
////			ErrorInfoStruct ErrorInfo = it.next();
////			m_vErr.add(ErrorInfo);
////		}
//		m_vErr = verr;
//	}
	
//	/**
//	 * 设置日志文件名称
//	 * @param slog
//	 */
//	public void SetLogFileName(String slog)
//	{
//		m_sLogFile = slog;
//	}
	
	/**
	 * 设定模式为烧录bootloader 
	 * @param bBootLoader
	 */
	public void SetBootLoader(boolean bBootLoader /* = true */)
	{
		m_bBootLoader = bBootLoader;
	}
	
	abstract boolean CoreProgram(SysPara sys); 

}
