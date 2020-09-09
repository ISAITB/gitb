package com.gitb.messaging;

import com.gitb.InputHolder;
import com.gitb.types.DataType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tuncay on 9/2/14.
 */
public class Message implements InputHolder {

    private final Map<String, DataType> fragments;

	public Message() {
		this.fragments = new ConcurrentHashMap<>();
	}

	public Map<String, DataType> getFragments() {
		return fragments;
	}

	@Override
	public void addInput(String inputName, DataType inputData) {
		fragments.put(inputName, inputData);
	}

	@Override
	public boolean hasInput(String inputName) {
		return fragments.containsKey(inputName);
	}
}
