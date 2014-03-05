package com.hbluetooth;

/**
 * 用来封装发送接收过程中(之前、之后)的不同的操作
 * 注意：不要用这些方法来更新ui
 *
 * @author jch
 */
public abstract class HCommunication {

    private Object item;

    private byte[] sendBuffer = null;
    private byte[] receivedBuffer = null;

    public abstract void beforeSend();

    public abstract void afterSend();

    public abstract void beforeReceive();

    public abstract void afterReceive();

    public abstract Object onParse();

    public HCommunication() {

    }

    public HCommunication(Object obj) {
        setItem(obj);
    }

    public byte[] getSendBuffer() {
        return sendBuffer;
    }

    public void setSendBuffer(byte[] sendBuffer) {
        this.sendBuffer = sendBuffer;
    }

    public byte[] getReceivedBuffer() {
        return receivedBuffer;
    }

    public void setReceivedBuffer(byte[] receivedBuffer) {
        this.receivedBuffer = receivedBuffer;
    }

    public Object getItem() {
        return item;
    }

    public void setItem(Object item) {
        this.item = item;
    }
}
