package com.inovance.elevatorprogram;

import java.util.Iterator;
import java.util.Vector;

/**
 * CRC校验类
 * @author inovance
 *
 */
public class CRC32 {

	private long[] crc_table = new long[256];
	
	public CRC32()
	{
		InitCRC32Table();
	}
	
	public void InitCRC32Table()
	{
		long c;  
		int i, j;  

		for (i = 0; i < 256; i++) 
		{  
			c = (int)i;  
			for (j = 0; j < 8; j++) 
			{ 
				if ((c & 1) > 0)  
					c = 0xedb88320L ^ (c >> 1);  
				else  
					c = c >> 1;  
			}  
			crc_table[i] = c;  
		}
	}
	
	public long GetCRC32(byte[] buffer, long size)
	{
		long crc=0xffffffff;
		int i;  
		for (i = 0; i < size; i++) 
		{ 
			crc = crc_table[(int)(crc ^ buffer[i]) & 0xff] ^ (crc >> 8);
		}  
		return (crc^0xffffffff);  
	}

	public long GetCRC32(Vector<Byte> vData)
	{
		long crc = 0xffffffff;
		Iterator<Byte> it = vData.iterator();
		while(it.hasNext())
		{
			byte bt = it.next();
			crc = crc_table[(int)(crc ^ bt) & 0xff] ^ (crc >> 8);
		}
//		for (vector<unsigned char>::iterator it=vData.begin(); it!=vData.end(); it++)
//		{
//			crc = crc_table[(crc ^ (*it)) & 0xff] ^ (crc >> 8);
//		}
		return (crc^0xffffffff); 
	}
	
}
