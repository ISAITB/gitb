package com.gitb.repository;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;

/**
 * Created by serbay on 9/8/14.
 * Configuration Handler for the TestEngine
 */
public class TestCaseRepositoryConfiguration {

	public static String TEST_CASE_REPOSITORY_URL;
	public static String TEST_RESOURCE_REPOSITORY_URL;
	public static String TEST_ID_PARAMETER;
	public static String RESOURCE_ID_PARAMETER;

    /**
     * Load the configurations from the configuration files
     */
	static {
		try {
			CompositeConfiguration config = new CompositeConfiguration();
			config.addConfiguration(new SystemConfiguration());
			config.addConfiguration(new PropertiesConfiguration("remote-testcase-repository.properties"));

			TEST_CASE_REPOSITORY_URL = config.getString("remote.testcase.repository.url");
			TEST_RESOURCE_REPOSITORY_URL = config.getString("remote.testresource.repository.url");
			TEST_ID_PARAMETER = config.getString("remote.testcase.test-id.parameter");
			RESOURCE_ID_PARAMETER = config.getString("remote.testcase.resource-id.parameter");
			// TODO load configuration parameters
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}
}
