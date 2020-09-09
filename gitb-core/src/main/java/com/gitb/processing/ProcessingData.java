package com.gitb.processing;

import com.gitb.InputHolder;
import com.gitb.types.DataType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ProcessingData implements InputHolder {

    private final Map<String, DataType> data = new ConcurrentHashMap<>();

    public Map<String, DataType> getData() {
        return data;
    }

    @Override
    public void addInput(String inputName, DataType inputData) {
        data.put(inputName, inputData);
    }

    @Override
    public boolean hasInput(String inputName) {
        return data.containsKey(inputName);
    }

}
