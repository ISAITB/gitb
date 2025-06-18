/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
