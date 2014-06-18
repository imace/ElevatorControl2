package com.inovance.elevatorprogram;

import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class StringConvert {
	private static String hexString = "0123456789ABCDEF";

	/** */
	/**
	 * 将16进制字符串转成字节数
	 */
	public static byte[] HexStrToByte(String str) {
		// TODO Auto-generated method stub
		String strTempString = StringFilter(str);
		ByteArrayOutputStream baos = new ByteArrayOutputStream(
				strTempString.length() / 2);
		// 将每2位16进制字符组装成一个字节
		for (int i = 0; i < strTempString.length(); i += 2)
			baos.write((hexString.indexOf(strTempString.charAt(i)) << 4 | hexString
					.indexOf(strTempString.charAt(i + 1))));
		return baos.toByteArray();
	}

	/** */
	/**
	 * 把字节数组转换成16进制字符串
	 * 
	 * @param bArray
	 * @return
	 */
	public static final String bytesToHexStringTwo(byte[] bArray, int nCount) {
		//int nCount = bArray.length;
		StringBuffer sb = new StringBuffer(bArray.length);
		String sTemp;
		for (int i = 0; i < nCount; i++) {
			sTemp = Integer.toHexString(0xFF & bArray[i]);
			if (sTemp.length() < 2)
				sb.append(0);
			sb.append(sTemp.toUpperCase());
			sb.append(" ");//加一个空格
		}
		return sb.toString();
	}
	
	/*
	 * 过滤字符串
	 */
	public static String StringFilter(String str) throws PatternSyntaxException {
		// 只允许字母和数字
		// String regEx = "[^a-zA-Z0-9]";
		// 清除掉所有特殊字符
		String regEx = "[`~!@#$%^&*()+=|{}':;',//[//].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？-]";
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(str);
		String tmpstr = m.replaceAll("").trim();
		//去除所有空格
		 tmpstr = tmpstr.replace(" ","");  
		 return tmpstr;
	}
}
