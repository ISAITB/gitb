package com.gitb.engine;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.EnvironmentConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

/**
 * Created by serbay on 9/8/14.
 * Configuration Handler for the TestEngine
 */
public class TestEngineConfiguration {

	public static int ITERATION_LIMIT;
	public static String MESSAGING_CALLBACK_URL;

    /**
     * Load the configurations from the configuration files
     */
	public static void load() {
		try {
			CompositeConfiguration config = new CompositeConfiguration();
			config.addConfiguration(new SystemConfiguration());
			config.addConfiguration(new EnvironmentConfiguration());
			config.addConfiguration(new Configurations().properties("engine-module.properties"));

			ITERATION_LIMIT = config.getInt("gitb.engine.iteration-limit", 1000);
			MESSAGING_CALLBACK_URL = config.getString("gitb.messaging.callbackURL");
		} catch (ConfigurationException e) {
			throw new IllegalStateException("Error loading configuration", e);
		}

	}
}
