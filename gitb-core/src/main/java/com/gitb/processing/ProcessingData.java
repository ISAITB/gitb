package com.gitb.processing;

import com.gitb.types.DataType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ProcessingData {

    private final Map<String, DataType> data = new ConcurrentHashMap<>();

    public Map<String, DataType> getData() {
        return data;
    }

}
