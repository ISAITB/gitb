package com.gitb.utils;

import com.gitb.tdl.Binding;

import java.util.List;

/**
 * Created by serbay on 9/30/14.
 */
public class BindingUtils {
	public static boolean isNameBinding(List<Binding> bindings){
		for(Binding input:bindings)
			if(input.getName() == null)
				return false;
		return true;
	}
}
