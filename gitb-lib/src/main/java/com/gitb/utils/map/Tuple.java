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

package com.gitb.utils.map;

import java.util.Arrays;

/**
 * Created by serbay.
 */
public class Tuple<T> {
	private final T[] contents;

	public Tuple(T[] contents) {
		if(contents == null) {
			throw new IllegalArgumentException("Contents can not be null.");
		}
		this.contents = contents;
	}

	public T[] getContents() {
		return contents;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
        var tuple = (Tuple) o;
        return Arrays.equals(contents, tuple.contents);
    }

	@Override
	public int hashCode() {
		return Arrays.hashCode(contents);
	}
}
