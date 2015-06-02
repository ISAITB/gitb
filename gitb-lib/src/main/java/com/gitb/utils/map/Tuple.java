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

		Tuple tuple = (Tuple) o;

		if (!Arrays.equals(contents, tuple.contents)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(contents);
	}
}
