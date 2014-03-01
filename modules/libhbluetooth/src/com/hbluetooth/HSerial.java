package com.hbluetooth;

import android.annotation.SuppressLint;

/**
 * 16进制值与String/Byte之间的转换
 * */
public class HSerial {
	/**
	 * 字符串转换成十六进制字符串
	 * 
	 * @param String
	 *            str 待转换的ASCII字符串
	 * @return String 每个Byte之间空格分隔，如: [61 6C 6B]
	 */
	public static String str2HexStr(String str) {
		char[] chars = "0123456789ABCDEF".toCharArray();
		StringBuilder sb = new StringBuilder("");
		byte[] bs = str.getBytes();
		int bit;

		for (int i = 0; i < bs.length; i++) {
			bit = (bs[i] & 0x0f0) >> 4;
			sb.append(chars[bit]);
			bit = bs[i] & 0x0f;
			sb.append(chars[bit]);
			sb.append(' ');
		}
		return sb.toString().trim();
	}

	/**
	 * 十六进制转换字符串
	 * 
	 * @param String
	 *            str Byte字符串(Byte之间无分隔符 如:[616C6B])
	 * @return String 对应的字符串
	 */
	public static String hexStr2Str(String hexStr) {
		String str = "0123456789ABCDEF";
		char[] hexs = hexStr.toCharArray();
		byte[] bytes = new byte[hexStr.length() / 2];
		int n;

		for (int i = 0; i < bytes.length; i++) {
			n = str.indexOf(hexs[2 * i]) * 16;
			n += str.indexOf(hexs[2 * i + 1]);
			bytes[i] = (byte) (n & 0xff);
		}
		return new String(bytes);
	}

	/**
	 * bytes转换成十六进制字符串
	 * 
	 * @param byte[] b byte数组
	 * @return String 每个Byte值之间空格分隔
	 */
	public static String byte2HexStr(byte[] b) {
		String stmp = "";
		StringBuilder sb = new StringBuilder("");
		for (int n = 0; n < b.length; n++) {
			stmp = Integer.toHexString(b[n] & 0xFF);
			sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
			sb.append(" ");
		}
		return sb.toString().toUpperCase().trim();
	}

	@SuppressLint("DefaultLocale")
	public static String int2HexStr(int[] i) {
		String stmp = "";
		StringBuilder sb = new StringBuilder("");
		for (int n = 0; n < i.length; n++) {
			stmp = Integer.toHexString(i[n] & 0xFF);
			sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
			sb.append(" ");
		}
		return sb.toString().toUpperCase().trim();
	}

	/**
	 * bytes字符串转换为Byte值
	 * 
	 * @param String
	 *            src Byte字符串，每个Byte之间没有分隔符
	 * @return byte[]
	 */
	public static byte[] hexStr2Bytes(String src) {
		int m = 0, n = 0;
		int l = src.length() / 2;
		System.out.println(l);
		byte[] ret = new byte[l];
		for (int i = 0; i < l; i++) {
			m = i * 2 + 1;
			n = m + 1;
			ret[i] = Byte.decode("0x" + src.substring(i * 2, m)
					+ src.substring(m, n));
		}
		return ret;
	}

	public static int[] hexStr2Ints(String src) {
		src = src.replaceAll("\\s*", "");
		int m = 0, n = 0;
		int l = src.length() / 2;
		int[] ret = new int[l];
		for (int i = 0; i < l; i++) {
			m = i * 2 + 1;
			n = m + 1;
			ret[i] = Integer.decode("0x" + src.substring(i * 2, m)
					+ src.substring(m, n));
		}
		return ret;
	}

	/**
	 * String的字符串转换成unicode的String
	 * 
	 * @param String
	 *            strText 全角字符串
	 * @return String 每个unicode之间无分隔符
	 * @throws Exception
	 */
	public static String strToUnicode(String strText) throws Exception {
		char c;
		StringBuilder str = new StringBuilder();
		int intAsc;
		String strHex;
		for (int i = 0; i < strText.length(); i++) {
			c = strText.charAt(i);
			intAsc = (int) c;
			strHex = Integer.toHexString(intAsc);
			if (intAsc > 128)
				str.append("\\u" + strHex);
			else
				// 低位在前面补00
				str.append("\\u00" + strHex);
		}
		return str.toString();
	}

	/**
	 * unicode的String转换成String的字符串
	 * 
	 * @param String
	 *            hex 16进制值字符串 （一个unicode为2byte）
	 * @return String 全角字符串
	 */
	public static String unicodeToString(String hex) {
		int t = hex.length() / 6;
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < t; i++) {
			String s = hex.substring(i * 6, (i + 1) * 6);
			// 高位需要补上00再转
			String s1 = s.substring(2, 4) + "00";
			// 低位直接转
			String s2 = s.substring(4);
			// 将16进制的string转为int
			int n = Integer.valueOf(s1, 16) + Integer.valueOf(s2, 16);
			// 将int转换为字符
			char[] chars = Character.toChars(n);
			str.append(new String(chars));
		}
		return str.toString();
	}

	/**
	 * 为命令添加2位crc16校验码
	 * 
	 * @param data
	 *            纯命令
	 * @return 加入校验码之后的完整命令
	 */
	public static byte[] crc16(int[] data) {
		int[] temdata = new int[data.length + 2];
		int xda, xdapoly;
		int i, j, xdabit;
		xda = 0xFFFF;
		xdapoly = 0xA001; // (X**16 + X**15 + X**2 + 1)
		for (i = 0; i < data.length; i++) {
			xda ^= data[i];
			for (j = 0; j < 8; j++) {
				xdabit = (int) (xda & 0x01);
				xda >>= 1;
				if (xdabit == 1)
					xda ^= xdapoly;
			}
		}
		System.arraycopy(data, 0, temdata, 0, data.length);
		temdata[temdata.length - 2] = (int) (xda & 0xFF);
		temdata[temdata.length - 1] = (int) (xda >> 8);

		byte[] result = new byte[temdata.length];
		for (int x = 0; x < temdata.length; x++) {
			result[x] = (byte) (temdata[x]);
		}
		return result;
	}

	/**
	 * 获取二位crc16校验码
	 * 
	 * @param data
	 *            指令或接收的响应
	 * @return
	 */
	public static int[] getEndFrom(byte[] data) {
		int firstnotzero = 0;
		for (int i = data.length - 1; i > 0; i--) {
			if (data[i] != 0) {
				firstnotzero = i;
				break;
			}
		}
		int[] temdata = new int[2];
		int start = firstnotzero - 1 > 0 ? firstnotzero - 1 : 0;
		temdata[0] = data[start] & 0xff;
		temdata[1] = data[start + 1] & 0xff;
		return temdata;
	}

	/**
	 * 判断接受到的数据是否符合crc16
	 * 
	 * @param data
	 * @return
	 */
	public static boolean isCRC16Valid(byte[] data) {
		if (data.length <= 2)
			return false;
		byte[] trimed = trimEnd(data);
		if (trimed.length <= 2)
			return false;
		// 纯指令
		byte[] bcmd = new byte[trimed.length - 2];
		int[] icmd = new int[trimed.length - 2];
		System.arraycopy(trimed, 0, bcmd, 0, trimed.length - 2);
		for (int i = 0; i < trimed.length - 2; i++) {
			icmd[i] = bcmd[i];
		}
		// crc最后两位
		byte[] crced = crc16(icmd);
		int[] last1 = new int[] { crced[crced.length - 2] & 0xff,
				crced[crced.length - 1] & 0xff };
		// 最后两位
		int[] last2 = getEndFrom(data);
		return (last1[0] == last2[0] && last1[1] == last2[1]);
	}

	/**
	 * 截取返回值最后的00
	 * 
	 * @param data
	 *            指令或接收的响应
	 * @return 有效指令
	 */
	public static byte[] trimEnd(byte[] data) {
		int firstnotzero = 0;
		for (int i = data.length - 1; i > 0; i--) {
			if (data[i] != 0) {
				firstnotzero = i;
				break;
			}
		}
		byte[] temdata = new byte[firstnotzero + 1];
		for (int i = 0; i <= firstnotzero; i++) {
			temdata[i] = data[i];
		}
		return temdata;
	}

	/**
	 * 2个byte表示成16位0,1
	 * 
	 * @param b1
	 * @param b2
	 * @return
	 */
	public static boolean[] byte2BoolArr(byte b1, byte b2) {
		boolean[] arr = new boolean[16];
		int flag = 0x8000;
		for (int i = 0; i < 16; i++) {
			if (i < 8)
				arr[i] = ((b1 << 8) & flag) != 0;
			else
				arr[i] = (b2 & flag) != 0;
			flag >>= 1;
		}
		return arr;
	}

}