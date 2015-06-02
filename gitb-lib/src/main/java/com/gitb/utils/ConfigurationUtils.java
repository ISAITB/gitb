package com.gitb.utils;

import com.gitb.core.Configuration;

import java.util.List;

/**
 * Created by serbay on 9/25/14.
 */
public class ConfigurationUtils {
	public static Configuration getConfiguration(List<Configuration> configurations, String name) {
		for(Configuration configuration : configurations) {
			if(configuration.getName().equals(name)) {
				return configuration;
			}
		}

		return null;
	}

	public static Configuration constructConfiguration(String name, String value) {
		Configuration configuration = new Configuration();
		configuration.setName(name);
		configuration.setValue(value);

		return configuration;
	}
}
