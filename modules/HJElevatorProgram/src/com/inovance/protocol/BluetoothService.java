/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.inovance.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for incoming
 * connections, a thread for connecting with a device, and a thread for
 * performing data transmissions when connected. 蓝牙烧录服务  单例模式
 */
public class BluetoothService {
	// Debugging
	private static final String TAG = "BluetoothProgramService";
	private static final boolean D = true;

	private BluetoothSocket m_mSocket;
	private ReadDataThread mReadThread;
	private ByteBuffer mByteBuffer;

	private static BluetoothService ourInstance = new BluetoothService();

	public static BluetoothService getInstance() {
		return ourInstance;
	}

	private BluetoothService() {
	}

	public  void SetBluetoothSocket(BluetoothSocket socket) {
		if (null == socket)
			Log.d(TAG, "socket is null");
		else
			m_mSocket = socket;
		StartReadDataService();
	}

	private void StartReadDataService() {
		mReadThread = new ReadDataThread(m_mSocket);
		Log.i(TAG, "create a ReadData thread");
		mReadThread.start();
	}

	/**
	 * Write to the ConnectedThread in an unsynchronized manner
	 * 
	 * @param out
	 *            The bytes to write
	 * @see ConnectedThread
	 */
	public boolean write(byte[] out) {
		// Create temporary object
		ReadDataThread r;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			r = mReadThread;
		}
		// Perform the write unsynchronized
		r.write(out);
		return true;
	}

	/**
	 * Write to the ConnectedThread in an unsynchronized manner
	 * 
	 * @param out
	 *            The length to read
	 * @see ConnectedThread
	 */
	public int read(byte[] in) {
		// Create temporary object
		int length = mByteBuffer.position();
		if (length == 0)
			return 0;
		// 调用flip()使limit变为当前的position的值,position变为0,
		// 为接下来从ByteBuffer读取做准备
		mByteBuffer.flip();
		// mByteBuffer.position(0);
		try {
			mByteBuffer.get(in, 0, length);
			// mByteBuffer.get(in);
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}

		// 调用clear()使position变为0,limit变为capacity的值，
		// 为接下来写入数据到ByteBuffer中做准备
		mByteBuffer.clear();
		return length;
	}

	public int QueryBuffer() {
		int length = mByteBuffer.position();
		return length;
	}

	private class ReadDataThread extends Thread {
		//private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ReadDataThread(BluetoothSocket socket) {
			Log.d(TAG, "Create BluetoothReadThread");
			//mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				Log.d(TAG, "get InputStream");
				if(null == socket)
				{
					Log.d(TAG, "SOCKET IS NULL");
				}
				else 
				{
					Log.d(TAG, socket.toString());
				}
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}
			Log.d(TAG, "end  InputStream");
			mmInStream = tmpIn;
			mmOutStream = tmpOut;
			Log.d(TAG, "= inputStream");
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			// Keep listening to the InputStream while connected
			byte[] buffer = new byte[255];
			int bytes = 0;
			while (true) {
				try {
					bytes = mmInStream.read(buffer);
					// mByteBuffer.put(buffer, mByteBuffer.position(), bytes);
					mByteBuffer.put(buffer, 0, bytes);

					if (D) {
						String str = bytesToHexStringTwo(buffer, bytes);
						Log.i(TAG, "read buffer:" + str);
					}
				} catch (Exception e) {
					Log.e(TAG, "disconnected", e);
					break;
				}
			}
		}

		/** */
		/**
		 * 把字节数组转换成16进制字符串
		 * 
		 * @param bArray
		 * @return
		 */
		public String bytesToHexStringTwo(byte[] bArray, int nCount) {
			// int nCount = bArray.length;
			StringBuffer sb = new StringBuffer(bArray.length);
			String sTemp;
			for (int i = 0; i < nCount; i++) {
				sTemp = Integer.toHexString(0xFF & bArray[i]);
				if (sTemp.length() < 2)
					sb.append(0);
				sb.append(sTemp.toUpperCase());
				sb.append(" ");// 加一个空格
			}
			return sb.toString();
		}

		/**
		 * Write to the connected OutStream.
		 * 
		 * @param buffer
		 *            The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);
				if (D) {
					String str = bytesToHexStringTwo(buffer, buffer.length);
					Log.i(TAG, "write buffer:" + str);
				}
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}
	}
}
