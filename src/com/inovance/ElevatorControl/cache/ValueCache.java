package com.inovance.elevatorcontrol.cache;

import com.inovance.elevatorcontrol.models.CommunicationCode;

import java.util.ArrayList;
import java.util.List;

public class ValueCache {
    private static ValueCache instance = new ValueCache();

    public byte[] getErrorData() {
        return errorData;
    }

    public void setErrorData(byte[] errorData) {
        this.errorData = errorData;
    }

    private byte[] errorData;

    public List<CommunicationCode> getCodeList() {
        return codeList;
    }

    public void setCodeList(List<CommunicationCode> codeList) {
        this.codeList = codeList;
    }

    private List<CommunicationCode> codeList = new ArrayList<CommunicationCode>();

    public static ValueCache getInstance() {
        return instance;
    }

    private ValueCache() {
    }
}
