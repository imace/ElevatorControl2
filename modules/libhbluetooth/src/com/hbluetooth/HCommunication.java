package com.hbluetooth;

/**
 * 用来封装发送接收过程中(之前、之后)的不同的操作
 * 注意：不要用这些方法来更新ui
 * @author jch
 *
 */
public abstract class HCommunication {
	
	private Object item;
	
	private byte[] sendbuffer = null;
	private byte[] receivebuffer = null;
	
	public abstract void beforeSend();
	public abstract void afterSend();
	
	public abstract void beforeReceive();
	public abstract void afterReceive();
	
	public abstract Object onParse();
	
	public HCommunication(){
		
	}
	
	public HCommunication(Object obj){
		setItem(obj);
	}
	
	public byte[] getSendbuffer() {
		return sendbuffer;
	}
	public void setSendbuffer(byte[] sendbuffer) {
		this.sendbuffer = sendbuffer;
	}
	public byte[] getReceivebuffer() {
		return receivebuffer;
	}
	public void setReceivebuffer(byte[] receivebuffer) {
		this.receivebuffer = receivebuffer;
	}
	public Object getItem() {
		return item;
	}
	public void setItem(Object item) {
		this.item = item;
	}
}
