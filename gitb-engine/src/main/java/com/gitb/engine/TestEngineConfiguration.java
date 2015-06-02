package com.gitb.engine;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;

/**
 * Created by serbay on 9/8/14.
 * Configuration Handler for the TestEngine
 */
public class TestEngineConfiguration {

	public static int ITERATION_LIMIT;

    /**
     * Load the configurations from the configuration files
     */
	public static void load() {
		try {
			CompositeConfiguration config = new CompositeConfiguration();
			config.addConfiguration(new SystemConfiguration());
			config.addConfiguration(new PropertiesConfiguration("engine-module.properties"));

			ITERATION_LIMIT = config.getInt("gitb.engine.iteration-limit", 1000);
			// TODO load configuration parameters
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}

	}
}
