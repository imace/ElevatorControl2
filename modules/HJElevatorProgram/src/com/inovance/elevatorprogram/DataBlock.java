package com.inovance.elevatorprogram;

import java.util.Vector;

public class DataBlock {
	int uLen;            //数据块长度，以字为单位
	int uAddr;           //数据块起始地址
	Vector<Byte> vData = new Vector<Byte>();  //数据

}
