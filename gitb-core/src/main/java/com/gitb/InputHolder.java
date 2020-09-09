package com.gitb;

import com.gitb.types.DataType;

/**
 * Classes that can be assigned inputs.
 */
public interface InputHolder {

    void addInput(String inputName, DataType inputData);
    boolean hasInput(String inputName);

}
