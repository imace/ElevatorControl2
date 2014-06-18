package com.inovance.elevatorprogram;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.inovance.encryptUtil.AESAlg;

/**
 * 烧录文件解析类
 * 
 * @author inovance
 * 
 */
public class CipherBinFile {

	//private String m_sfileName; // 要加载的文件名
	// private Vector<Byte> m_vtBin = new Vector<Byte>();
	private byte[] m_buffBin = null;

	/**
	 * 设置文件路径
	 * 
	 * @param fileName
	 */
//	public void SetFileName(String fileName) {
//		m_sfileName = fileName;
//	}

	/**
	 * 读取二进制烧录文件
	 * 
	 * @return
	 */
	public int ReadBinFile(FileInputStream fileInStream) {
		// m_vtBin.clear();
		try {
			//FileInputStream m_fileInput = new FileInputStream(m_sfileName);
			int length = fileInStream.available();
			//byte[] buffer = new byte[length];
			m_buffBin = new byte[length];
			fileInStream.read(m_buffBin);
			fileInStream.close();

			// 解密数据
			if (!Decrypt()) {
				return 2;
			}
			if (!CheckFile(m_buffBin)) {
				return 3; // 文件头校验失败
			}
		} catch (Exception e) {
			e.printStackTrace();
			return 1; // 读取失败
		}
		return 0; // 读取成功
	}

	/**
	 * 检测文件是否正确
	 * 
	 * @return
	 */
	public boolean CheckFile(byte[] buf) {
		// if (m_vtBin.get(0) == 'I' && m_vtBin.get(1) == 'n'
		// && m_vtBin.get(2) == 'o' && m_vtBin.get(3) == 'v'
		// && m_vtBin.get(4) == 'a' && m_vtBin.get(5) == 'n'
		// && m_vtBin.get(6) == 'c' && m_vtBin.get(7) == 'e')
		char c1 = (char) buf[0];
		char c2 = (char) buf[1];
		char c3 = (char) buf[2];
		char c4 = (char) buf[3];
		char c5 = (char) buf[4];
		char c6 = (char) buf[5];
		char c7 = (char) buf[6];
		char c8 = (char) buf[7];

		if (c1 == 'I' && c2 == 'n' && c3 == 'o' && c4 == 'v' && c5 == 'a'
				&& c6 == 'n' && c7 == 'c' && c8 == 'e') {
			return true;
		} else {
			return false;
		}
	}

	public int CharToInt(byte c1, byte c2, byte c3, byte c4) {
		return (((c4 & 0xFF) << 24) | ((c3 & 0xFF) << 16) | ((c2 & 0xFF) << 8) | (c1 & 0xFF));
	}

	public int GetFBLSize() {
		int i = 8;
		return CharToInt(m_buffBin[i], m_buffBin[i + 1], m_buffBin[i + 2],
				m_buffBin[i + 3]);
	}

	public int GetSBLSize() {
		int i = 12;
		return CharToInt(m_buffBin[i], m_buffBin[i + 1], m_buffBin[i + 2],
				m_buffBin[i + 3]);
	}

	public int GetTBLSize() {
		int i = 16;
		return CharToInt(m_buffBin[i], m_buffBin[i + 1], m_buffBin[i + 2],
				m_buffBin[i + 3]);
	}

	public int GetAppSize() {
		int i = 20;
		return CharToInt(m_buffBin[i], m_buffBin[i + 1], m_buffBin[i + 2],
				m_buffBin[i + 3]);
	}

	public int GetStartAddress() {
		int i = 24;
		return CharToInt(m_buffBin[i], m_buffBin[i + 1], m_buffBin[i + 2],
				m_buffBin[i + 3]);
	}

	public int GetPLine() {
		int i = 28;
		return CharToInt(m_buffBin[i], m_buffBin[i + 1], m_buffBin[i + 2],
				m_buffBin[i + 3]);
	}

	/*
	 * 获取产品线名称
	 */
	public String GetPLineName() {
		return byteToString(32, 96);
		// int i = 32;
		// byte[] btStr = new byte[96];
		// for (int j=0 ; j < 96; j++)
		// {
		// btStr[j] = m_vtBin.get(i+j);
		// }
		// String str = new String(btStr);
		// return str;
	}

	public int GetPType() {
		int i = 128;
		return CharToInt(m_buffBin[i], m_buffBin[i + 1], m_buffBin[i + 2],
				m_buffBin[i + 3]);
	}

	public String GetPTypeName() {
		return byteToString(132, 96);
		// int i = 132;
		// byte[] btStr = new byte[96];
		// for (int j=0 ; j < 96; j++)
		// {
		// btStr[j] = m_vtBin.get(i+j);
		// }
		// String str = new String(btStr);
		// return str;
	}

	public int GetIC() {
		int i = 228;
		return CharToInt(m_buffBin[i], m_buffBin[i + 1], m_buffBin[i + 2],
				m_buffBin[i + 3]);
	}

	public String GetICName() {
		return byteToString(232, 96);
		// int i = 232;
		// byte[] btStr = new byte[96];
		// for (int j=0 ; j < 96; j++)
		// {
		// btStr[j] = m_vtBin.get(i+j);
		// }
		// String str = new String(btStr);
		// return str;
	}

	public int GetRand() {
		int i = 328;
		return CharToInt(m_buffBin[i], m_buffBin[i + 1], m_buffBin[i + 2],
				m_buffBin[i + 3]);
	}

	public int GetCRC32() {
		int i = 332;
		return CharToInt(m_buffBin[i], m_buffBin[i + 1], m_buffBin[i + 2],
				m_buffBin[i + 3]);
	}

	public int GetSize() {
		int i = 336;
		return CharToInt(m_buffBin[i], m_buffBin[i + 1], m_buffBin[i + 2],
				m_buffBin[i + 3]);
	}

	public int GetHost() {
		int i = 340;
		return CharToInt(m_buffBin[i], m_buffBin[i + 1], m_buffBin[i + 2],
				m_buffBin[i + 3]);
	}

	public boolean IsBootEncrypt() {
		int i = 344;
		if (m_buffBin[i] == 1) {
			return true;
		} else {
			return false;
		}
	}

	// public Vector<Byte> GetFBL()
	public void GetFBL(Vector<Byte> vData) {
		vData.clear();
		final int uHeadSize = 400; // 文件头长度
		int uFBLSize = GetFBLSize(); // FBL部分长度
		// Vector<Byte> btData = new Vector<Byte>();
		for (int i = 0; i < uFBLSize; i++) {
			vData.add(m_buffBin[uHeadSize + i]);
		}
		// return btData;
	}

	public void GetSBL(Vector<Byte> vData) {
		// vdata.clear();
		// const unsigned int uHeadSize=400; //文件头长度
		// unsigned int uFBLSize=GetFBLSize(); //FBL部分长度
		// unsigned int uSBLSize=GetSBLSize(); //SBL部分长度
		// for (unsigned int i=0; i<uSBLSize; i++)
		// {
		// vdata.push_back(m_vbin.at(uHeadSize+uFBLSize+i));
		// }
		vData.clear();
		final int uHeadSize = 400; // 文件头长度
		int uFBLSize = GetFBLSize(); // FBL部分长度
		int uSBLSize = GetSBLSize(); // SBL部分长度
		// Vector<Byte> btData = new Vector<Byte>();
		for (int i = 0; i < uSBLSize; i++) {
			vData.add(m_buffBin[uHeadSize + uFBLSize + i]);
		}
	}

	public void GetTBL(Vector<Byte> vData) {
		// vdata.clear();
		// const unsigned int uHeadSize=400; //文件头长度
		// unsigned int uFBLSize=GetFBLSize(); //FBL部分长度
		// unsigned int uSBLSize=GetSBLSize(); //SBL部分长度
		// unsigned int uTBLSize=GetTBLSize(); //TBL部分长度
		// for (unsigned int i=0; i<uTBLSize; i++)
		// {
		// vdata.push_back(m_vbin.at(uHeadSize+uFBLSize+uSBLSize+i));
		// }
		vData.clear();

		final int uHeadSize = 400; // 文件头长度
		int uFBLSize = GetFBLSize(); // FBL部分长度
		int uSBLSize = GetSBLSize(); // SBL部分长度
		int uTBLSize = GetTBLSize(); // TBL部分长度
		// Vector<Byte> btData = new Vector<Byte>();
		for (int i = 0; i < uTBLSize; i++) {
			vData.add(m_buffBin[uHeadSize + uFBLSize + uSBLSize + i]);
		}
		// return btData;
	}

	public void GetApp(Vector<Byte> vData) {
		// vdata.clear();
		// const unsigned int uHeadSize=400; //文件头长度
		// unsigned int uFBLSize=GetFBLSize(); //FBL部分长度
		// unsigned int uSBLSize=GetSBLSize(); //SBL部分长度
		// unsigned int uTBLSize=GetTBLSize(); //TBL部分长度
		// unsigned int uAppSize=GetAppSize(); //用户文件长度
		// for (unsigned int i=0; i<uAppSize; i++)
		// {
		// vdata.push_back(m_vbin.at(uHeadSize+uFBLSize+uSBLSize+uTBLSize+i));
		// }
		vData.clear();

		final int uHeadSize = 400; // 文件头长度
		int uFBLSize = GetFBLSize(); // FBL部分长度
		int uSBLSize = GetSBLSize(); // SBL部分长度
		int uTBLSize = GetTBLSize(); // TBL部分长度
		int uAppSize = GetAppSize(); // 用户文件长度
		// Vector<Byte> btData = new Vector<Byte>();
		for (int i = 0; i < uAppSize; i++) {
			vData.add(m_buffBin[uHeadSize + uFBLSize + uSBLSize + uTBLSize + i]);
		}
		// return btData;
	}

	public String byteToString(int nStart, int nCount) {

		byte[] btStr = new byte[nCount];
		for (int j = 0; j < nCount; j++) {
			btStr[j] = m_buffBin[nStart + j];
			// strbuff.append((char)(m_buffBin[nStart + j] & 0xff));

		}
		StringBuffer strbuff = new StringBuffer();
		for (int i = 0; i < nCount; i += 2) {

			strbuff.append('\\');
			strbuff.append('u');
			byte l = btStr[i];
			byte h = btStr[i + 1];
			int ch = (h & 0xff) << 8 | (l & 0xff);
			// String strTmp = String.format("%04x", ch);
			strbuff.append(String.format("%04x", ch));
			// strbuff.append(Integer.toHexString(ch & 0xffff));
		}
		String str = unicodeToString(strbuff.toString());
		return str.trim();
	}

	private String unicodeToString(String str) {
		Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
		Matcher matcher = pattern.matcher(str);
		char ch;
		while (matcher.find()) {
			ch = (char) Integer.parseInt(matcher.group(2), 16);
			str = str.replace(matcher.group(1), ch + "");
		}
		return str;
	}

	public boolean IsEmpty() {
		// return m_vtBin.isEmpty();
		if (m_buffBin != null) {
			int nCount = m_buffBin.length;
			if (nCount > 0)
				return false;
		}
		return true;
	}

	public boolean IsSolidBoot() {
		int i = 345;
		if (m_buffBin[i] == 1) {
			return true;
		} else {
			return false;
		}
	}

	// ************************************************************************
	// 函数名称: IsEncrypt
	// 函数说明: 数据是否加密
	// 访问属性: private
	// 返 回 值: bool
	// 函数参数: vector<unsigned char> & vSrc
	// 作 者: 李正伟
	// 日 期: 2014/05/12
	// ************************************************************************
	private boolean IsEncrypt(final byte[] vSrc) {
		if (vSrc[0] == 'E' && vSrc[1] == 'n' && vSrc[2] == 'c'
				&& vSrc[3] == 'r' && vSrc[4] == 'y' && vSrc[5] == 'p'
				&& vSrc[6] == 't') {
			return true;
		} else {
			return false;
		}
	}

	// ************************************************************************
	// 函数名称: Decrypt
	// 函数说明: 解密数据
	// 访问属性: private
	// 返 回 值: bool
	// 函数参数: vector<unsigned char> & vSrc
	// 函数参数: vector<unsigned char> & vDes
	// 作 者: 李正伟
	// 日 期: 2014/05/13
	// ************************************************************************
	private boolean Decrypt(final byte[] vSrc, ArrayList<Byte> vDes) {
		int index = 7; // 标记长度
		// 获取明文的大小
		//int len = vSrc[index] | (vSrc[index + 1] << 8)| (vSrc[index + 2] << 16) | (vSrc[index + 3] << 24);
		int len = CharToInt(vSrc[index], vSrc[index + 1], vSrc[index + 2], vSrc[index + 3]);
		
		// 密文部分
		//byte[] NewBuf = new byte[vSrc.length - index - 4];
		ArrayList<Byte> NewSrc = new ArrayList<Byte>();
		for(int i = 4 + index; i < vSrc.length; i++)
		{
			NewSrc.add(vSrc[i]);
		}
		//System.arraycopy(arg0, arg1, arg2, arg3, arg4);
		//System.arraycopy(vSrc, index + 4, NewSrc, 0, vSrc.length - index - 4);
		// vector<unsigned char> vNewSrc(vSrc.begin()+index+4,vSrc.end());

		// 清空
		// vDes.clear();

		int count = NewSrc.size() / AESAlg.uSegSize; // 分段数目
		int mod = NewSrc.size() % AESAlg.uSegSize;
		if (mod != 0) {
			return false;
		}
		for (int i = 0; i < count; i++) {
			byte[] cipherText = new byte[AESAlg.uSegSize];
			byte[] plainText = null ; //new byte[AESAlg.uSegSize];
			for (int j = 0; j < AESAlg.uSegSize; j++) {
				cipherText[j] = NewSrc.get(i * AESAlg.uSegSize + j);
			}
			try {
				plainText = AESAlg.InvCipher(cipherText);
			} catch (Exception e) {
				return false;
			}
			for (int k = 0; k < AESAlg.uSegSize; k++) {
				if(plainText.length <= k)
					vDes.add((byte)0);
				else
					vDes.add(plainText[k]);
					
			}
		}
		// 去掉填充字符
		for (int i = len; i < vDes.size();) {
			vDes.remove(i);
		}

		return true;
	}

	// ************************************************************************
	// 函数名称: Decrypt
	// 函数说明: 解密数据
	// 访问属性: private
	// 返 回 值: bool
	// 函数参数: void
	// 作 者: 李正伟
	// 日 期: 2014/05/13
	// ************************************************************************
	private boolean Decrypt() {
		if (IsEncrypt(m_buffBin)) {
			ArrayList<Byte> vDes = new ArrayList<Byte>();
			if (!Decrypt(m_buffBin, vDes)) {
				return false;
			}
			for (int i = 0; i < vDes.size(); i++) {
				m_buffBin[i] = vDes.get(i);
			}
		}

		return true;
	}
}
