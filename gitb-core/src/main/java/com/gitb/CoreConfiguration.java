package com.gitb;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;

/**
 * Created by serbay on 9/8/14.
 * Configuration Handler for the TestEngine
 */
public class CoreConfiguration {

	public static String TEST_CASE_REPOSITORY;

    /**
     * Load the configurations from the configuration files
     */
	static {
		try {
			CompositeConfiguration config = new CompositeConfiguration();
			config.addConfiguration(new SystemConfiguration());
			config.addConfiguration(new PropertiesConfiguration("core-module.properties"));

			TEST_CASE_REPOSITORY = config.getString("gitb.test-case-repository");
			// TODO load configuration parameters
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}

	}
}
