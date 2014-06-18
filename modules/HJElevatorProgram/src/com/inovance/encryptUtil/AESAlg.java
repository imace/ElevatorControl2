package com.inovance.encryptUtil;

public class AESAlg {

	//private static AES aesObj = new AES();

	public static final int uSegSize = 16;
	// Key密钥
	public static final byte[] g_GlobalKey = { 0x48, 0x43, 0x2E, 0x30, 0x37,
			0x39, 0x33, 0x2E, 0x32, 0x30, 0x31, 0x34, 0x30, 0x32, 0x31, 0x37 };// HC.0793.20140217
	public static final String g_strGlobalKey = "HC.0793.20140217";
	/**
	 * 密钥算法
	 */
	// ************************************************************************
	// 函数名称: Cipher
	// 函数说明: AES加密数据段
	// 访问属性: public
	// 返 回 值: const byte*
	// 函数参数: const byte * text
	// 函数参数: const byte * key
	// 函数参数: int keySize
	// 作 者: 李正伟
	// 日 期: 2014/05/09
	// ************************************************************************
	public static String Cipher(final byte[] text) throws Exception {
		return AES.Encrypt(text, g_GlobalKey);
	}

	// ************************************************************************
	// 函数名称: InvCipher
	// 函数说明: AES解密数据段
	// 访问属性: public
	// 返 回 值: const byte*
	// 函数参数: const byte * text
	// 函数参数: const byte * key
	// 函数参数: int keySize
	// 作 者: 李正伟 马俊移植
	// 日 期: 2014/05/09
	// ************************************************************************
	public static byte[] InvCipher(final byte[] text) throws Exception {
		byte[] retByte  = null; //new byte[16];
		retByte = AES.Decrypt(text, g_GlobalKey);
		return retByte;
	}
}
