package com.gitb.engine;

import com.gitb.engine.messaging.handlers.server.Configuration;
import com.gitb.utils.HmacUtils;
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
	public static String VALIDATION_CALLBACK_URL;
	public static String PROCESSING_CALLBACK_URL;
	public static Boolean TEMP_STORAGE_ENABLED;
	public static String TEMP_STORAGE_LOCATION;
	public static Boolean TEMP_STORAGE_BINARY_ENABLED;
	public static Boolean TEMP_STORAGE_STRING_ENABLED;
	public static Boolean TEMP_STORAGE_XML_ENABLED;
	public static Long TEMP_STORAGE_BINARY_THRESHOLD_BYTES;
	public static Long TEMP_STORAGE_STRING_THRESHOLD_CHARS;
	public static Long TEMP_STORAGE_XML_THRESHOLD_BYTES;
	public static String TEST_CASE_REPOSITORY_URL;
	public static String TEST_RESOURCE_REPOSITORY_URL;
	public static String TEST_ID_PARAMETER;
	public static String RESOURCE_ID_PARAMETER;
	public static Configuration DEFAULT_MESSAGING_CONFIGURATION;

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
			// By default, infer the processing and validation callback URLs from the messaging callback to avoid requiring their explicit definition.
			VALIDATION_CALLBACK_URL = config.getString("gitb.validation.callbackURL", inferCallbackURL("ValidationClient", MESSAGING_CALLBACK_URL));
			PROCESSING_CALLBACK_URL = config.getString("gitb.processing.callbackURL", inferCallbackURL("ProcessingClient", MESSAGING_CALLBACK_URL));
			// Temp storage properties - start.
			TEMP_STORAGE_ENABLED = config.getBoolean("gitb.engine.storage.enabled", Boolean.TRUE);
			TEMP_STORAGE_LOCATION = config.getString("gitb.engine.storage.location", "./temp/session/");
			TEMP_STORAGE_BINARY_ENABLED = config.getBoolean("gitb.engine.storage.binary.enabled", Boolean.TRUE);
			TEMP_STORAGE_STRING_ENABLED = config.getBoolean("gitb.engine.storage.string.enabled", Boolean.TRUE);
			TEMP_STORAGE_XML_ENABLED = config.getBoolean("gitb.engine.storage.xml.enabled", Boolean.TRUE);
			TEMP_STORAGE_BINARY_THRESHOLD_BYTES = config.getLong("gitb.engine.storage.binary.threshold", 1024L * 1024L); // 1 MB
			TEMP_STORAGE_STRING_THRESHOLD_CHARS = config.getLong("gitb.engine.storage.string.threshold", 512L * 1024L); // 1 MB (considering 2-byte encoding)
			TEMP_STORAGE_XML_THRESHOLD_BYTES = config.getLong("gitb.engine.storage.xml.threshold", 1024L * 1024L); // 1 MB
			// Temp storage properties - end.
			// Remote test case repository - start.
			TEST_CASE_REPOSITORY_URL = System.getenv().getOrDefault("remote.testcase.repository.url", config.getString("remote.testcase.repository.url"));
			TEST_RESOURCE_REPOSITORY_URL = System.getenv().getOrDefault("remote.testresource.repository.url", config.getString("remote.testresource.repository.url"));
			TEST_ID_PARAMETER = System.getenv().getOrDefault("remote.testcase.test-id.parameter", config.getString("remote.testcase.test-id.parameter"));
			RESOURCE_ID_PARAMETER = System.getenv().getOrDefault("remote.testcase.resource-id.parameter", config.getString("remote.testcase.resource-id.parameter"));
			// Configure also the HMAC information used to authorize remote calls.
			String hmacKey = System.getenv().getOrDefault("HMAC_KEY", "devKey");
			String hmacKeyWindow = System.getenv().getOrDefault("HMAC_WINDOW", "10000");
			HmacUtils.configure(hmacKey, Long.valueOf(hmacKeyWindow));
			// Remote test case repository - end.
			// Embedded messaging handler configuration - start.
			DEFAULT_MESSAGING_CONFIGURATION = loadDefaultMessagingHandlerConfiguration(config);
			// Embedded messaging handler configuration - end.
		} catch (ConfigurationException e) {
			throw new IllegalStateException("Error loading configuration", e);
		}
	}

	private static String inferCallbackURL(String endpointName, String referenceCallbackURL) {
		int index = referenceCallbackURL.lastIndexOf('/');
		if (index >= 0) {
			return referenceCallbackURL.substring(0, index) + "/" + endpointName;
		}
		return null;
	}

	private static Configuration loadDefaultMessagingHandlerConfiguration(CompositeConfiguration config) {
		return new Configuration(
				config.getInt("gitb.messaging.start-port", 8080),
				config.getInt("gitb.messaging.end-port", 9000),
				config.getString("gitb.messaging.server-ip-address"),
				config.getString("gitb.messaging.actor-name"),
				config.getString("gitb.messaging.keystore.location"),
				config.getString("gitb.messaging.keystore.password"),
				config.getString("gitb.messaging.default.alias")
		);
	}

}
