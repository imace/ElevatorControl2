package com.bluetoothtool;

import android.annotation.SuppressLint;

/**
 * 16进制值与String/Byte之间的转换
 */
public class SerialUtility {

    private static final String TAG = SerialUtility.class.getSimpleName();

    /**
     * 字符串转换成十六进制字符串
     *
     * @param String str 待转换的ASCII字符串
     * @return String 每个Byte之间空格分隔，如: [61 6C 6B]
     */
    public static String str2HexStr(String str) {
        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder("");
        byte[] bs = str.getBytes();
        int bit;
        for (byte b : bs) {
            bit = (b & 0x0f0) >> 4;
            sb.append(chars[bit]);
            bit = b & 0x0f;
            sb.append(chars[bit]);
            sb.append(' ');
        }
        return sb.toString().trim();
    }

    /**
     * 十六进制转换字符串
     *
     * @param String str Byte字符串(Byte之间无分隔符 如:[616C6B])
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
        String stringTemp = "";
        StringBuilder sb = new StringBuilder("");
        for (byte aB : b) {
            stringTemp = Integer.toHexString(aB & 0xFF);
            sb.append((stringTemp.length() == 1) ? "0" + stringTemp : stringTemp);
            sb.append("");
        }
        return sb.toString().toUpperCase().trim();
    }

    @SuppressLint("DefaultLocale")
    public static String int2HexStr(int[] i) {
        String stringTemp = "";
        StringBuilder sb = new StringBuilder("");
        for (int anI : i) {
            stringTemp = Integer.toHexString(anI & 0xFF);
            sb.append((stringTemp.length() == 1) ? "0" + stringTemp : stringTemp);
            sb.append(" ");
        }
        return sb.toString().toUpperCase().trim();
    }

    /**
     * bytes字符串转换为Byte值
     *
     * @param String src Byte字符串，每个Byte之间没有分隔符
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

    public static int[] hexStringToInt(String src) {
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
     * Hex string to byte array
     *
     * @param String String
     * @return byte[]
     */
    public static byte[] hexStringToByteArray(String string) {
        int len = string.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(string.charAt(i), 16) << 4)
                    + Character.digit(string.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * String的字符串转换成unicode的String
     *
     * @param String strText 全角字符串
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
     * @param String hex 16进制值字符串 （一个unicode为2byte）
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
     * @param hexString Hex string
     * @return byte[]
     */
    public static byte[] crc16(String hexString) {
        byte[] data = hexStringToByteArray(hexString);
        int crcValue = BluetoothTool.getInstance().crcValue;
        int checkedValue;
        if (crcValue == BluetoothTool.CRCValueNone) {
            checkedValue = getNormalDeviceCRCCheck(data);
        } else {
            checkedValue = getSpecialDeviceCRCCheck(crcValue, data);
        }
        byte[] crcBytes = new byte[2];
        crcBytes[0] = (byte) (checkedValue & 0xFF);
        crcBytes[1] = (byte) ((checkedValue >> 8) & 0xFF);
        byte[] result = new byte[data.length + crcBytes.length];
        System.arraycopy(data, 0, result, 0, data.length);
        System.arraycopy(crcBytes, 0, result, data.length, crcBytes.length);
        return result;
    }

    /**
     * 标准设备 CRC 校验
     *
     * @param data Data
     * @return int
     */
    private static int getNormalDeviceCRCCheck(byte[] data) {
        int crc_value = 0xFFFF;
        for (byte aData : data) {
            crc_value ^= (aData & 0xFF);// 默认转为16进制进行异或
            for (int j = 0; j < 8; j++) {
                if ((crc_value & 0x0001) > 0) {
                    crc_value = (crc_value >> 1) ^ 0xa001;
                } else {
                    crc_value = crc_value >> 1;
                }
            }
        }
        return (crc_value);
    }

    /**
     * 专有设备 CRC 校验
     *
     * @param data   Data
     * @param length Length
     * @return int
     */
    private static int getSpecialDeviceCRCCheck(int crcValue, byte[] data) {
        int length = data.length;
        if (length < 0)
            return crcValue;
        for (byte a : data) {
            crcValue ^= (a & 0xFF);
        }
        return crcValue;
    }

    /**
     * 判断接受到的数据是否符合crc16
     *
     * @param data byte[]
     * @return boolean
     */
    public static boolean isCRC16Valid(byte[] data) {
        int length = data.length;
        if (length <= 2)
            return false;
        byte[] trim = trimEnd(data);
        if (trim.length <= 2)
            return false;
        byte[] trimBytes = new byte[length - 2];
        System.arraycopy(data, 0, trimBytes, 0, length - 2);
        int receivedCRCValue;
        int crcValue = BluetoothTool.getInstance().crcValue;
        if (crcValue == BluetoothTool.CRCValueNone) {
            receivedCRCValue = getNormalDeviceCRCCheck(trimBytes);
            int value = ((((data[length - 1] & 0xFF) << 8) | (data[length - 2] & 0xFF)));
            return receivedCRCValue == value;
        } else {
            receivedCRCValue = getSpecialDeviceCRCCheck(crcValue, trimBytes);
            return receivedCRCValue == crcValue;
        }
    }

    /**
     * 获取二位crc16校验码
     *
     * @param data 指令或接收的响应
     * @return int[]
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
     * 截取返回值最后的00
     *
     * @param data 指令或接收的响应
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

    /**
     * Convert a byte array to a boolean array
     *
     * @param bytes byte array
     * @return boolean array
     */
    public static boolean[] byteArray2BitArray(byte[] bytes) {
        boolean[] bits = new boolean[bytes.length * 8];
        for (int i = 0; i < bytes.length * 8; i++) {
            if ((bytes[i / 8] & (1 << (7 - (i % 8)))) > 0)
                bits[i] = true;
        }
        return bits;
    }

    public static byte[] int2byte(int[] src) {
        int srcLength = src.length;
        byte[] dst = new byte[srcLength << 2];

        for (int i = 0; i < srcLength; i++) {
            int x = src[i];
            int j = i << 2;
            dst[j++] = (byte) ((x) & 0xff);
            dst[j++] = (byte) ((x >>> 8) & 0xff);
            dst[j++] = (byte) ((x >>> 16) & 0xff);
            dst[j++] = (byte) ((x >>> 24) & 0xff);
        }
        return dst;
    }
}