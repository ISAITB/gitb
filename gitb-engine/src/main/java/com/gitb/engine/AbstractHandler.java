package com.gitb.engine;

import com.gitb.types.DataType;

import java.util.Map;

public abstract class AbstractHandler {

    protected <T extends DataType> T getAndConvert(Map<String, DataType> inputs, String inputName, String dataType, Class<T> dataTypeClass) {
        var input = inputs.get(inputName);
        if (input != null) {
            return dataTypeClass.cast(input.convertTo(dataType));
        } else {
            return null;
        }
    }

}
