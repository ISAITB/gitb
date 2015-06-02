package com.gitb.messaging;

import com.gitb.types.DataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tuncay on 9/2/14.
 */
public class Message {
    private final Map<String, DataType> fragments;

	public Message() {
		this.fragments = new ConcurrentHashMap<>();
	}

	public Map<String, DataType> getFragments() {
		return fragments;
	}
}
