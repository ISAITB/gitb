package com.gitb.repository;

import com.gitb.utils.HmacUtils;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serbay on 9/8/14.
 * Configuration Handler for the TestEngine
 */
public class TestCaseRepositoryConfiguration {

	public static String TEST_CASE_REPOSITORY_URL;
	public static String TEST_RESOURCE_REPOSITORY_URL;
	public static String TEST_ID_PARAMETER;
	public static String RESOURCE_ID_PARAMETER;

	private static Logger LOG = LoggerFactory.getLogger(TestCaseRepositoryConfiguration.class);

    /**
     * Load the configurations from the configuration files
     */
	static {
		try {
			CompositeConfiguration config = new CompositeConfiguration();
			config.addConfiguration(new SystemConfiguration());
			config.addConfiguration(new Configurations().properties("remote-testcase-repository.properties"));

			TEST_CASE_REPOSITORY_URL = System.getenv().getOrDefault("remote.testcase.repository.url", config.getString("remote.testcase.repository.url"));
			TEST_RESOURCE_REPOSITORY_URL = System.getenv().getOrDefault("remote.testresource.repository.url", config.getString("remote.testresource.repository.url"));
			TEST_ID_PARAMETER = System.getenv().getOrDefault("remote.testcase.test-id.parameter", config.getString("remote.testcase.test-id.parameter"));
			RESOURCE_ID_PARAMETER = System.getenv().getOrDefault("remote.testcase.resource-id.parameter", config.getString("remote.testcase.resource-id.parameter"));

			// Configure also the HMAC information used to authorize remote calls.
			String hmacKey = System.getenv().getOrDefault("HMAC_KEY", "devKey");
			String hmacKeyWindow = System.getenv().getOrDefault("HMAC_WINDOW", "10000");
			HmacUtils.configure(hmacKey, Long.valueOf(hmacKeyWindow));
		} catch (ConfigurationException e) {
			LOG.error("Error loading configuration", e);
			throw new IllegalStateException("Error loading configuration", e);
		}
	}
}
