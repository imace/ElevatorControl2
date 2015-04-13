package com.inovance.elevatorcontrol.cache;

public class ValueCache {
    private static ValueCache instance = new ValueCache();

    public byte[] getErrorData() {
        return errorData;
    }

    public void setErrorData(byte[] errorData) {
        this.errorData = errorData;
    }

    private byte[] errorData;

    public static ValueCache getInstance() {
        return instance;
    }

    private ValueCache() {
    }
}
