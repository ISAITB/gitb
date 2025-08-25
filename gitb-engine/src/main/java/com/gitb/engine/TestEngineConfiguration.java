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

package com.gitb.engine;

import com.gitb.CoreConfiguration;
import com.gitb.engine.messaging.handlers.server.Configuration;
import com.gitb.utils.HmacUtils;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.EnvironmentConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import static com.gitb.utils.ConfigUtils.getPropertiesConfiguration;

/**
 * Created by serbay on 9/8/14.
 * Configuration Handler for the TestEngine
 */
public class TestEngineConfiguration {

	public static int ITERATION_LIMIT;
	public static String ROOT_CALLBACK_URL;
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
	public static String REPOSITORY_HEALTHCHECK_URL;
	public static String REPOSITORY_ROOT_URL;
	public static String TEST_ID_PARAMETER;
	public static String RESOURCE_ID_PARAMETER;
	public static Configuration DEFAULT_MESSAGING_CONFIGURATION;
	public static String HANDLER_API_ROOT;

	public static final String HANDLER_API_SEGMENT = "api";
	private static final String ENV_CALLBACK_ROOT_URL = "CALLBACK_ROOT_URL";
	private static final String ENV_CALLBACK_MESSAGING_URL = "CALLBACK_MESSAGING_URL";
	private static final String ENV_CALLBACK_VALIDATION_URL = "CALLBACK_VALIDATION_URL";
	private static final String ENV_CALLBACK_PROCESSING_URL = "CALLBACK_PROCESSING_URL";
	private static final String ENV_REPOSITORY_ROOT_URL = "REPOSITORY_ROOT_URL";
	private static final String ENV_REPOSITORY_TEST_CASE_URL = "remote.testcase.repository.url";
	private static final String ENV_REPOSITORY_TEST_RESOURCE_URL = "remote.testresource.repository.url";

    /**
     * Load the configurations from the configuration files
     */
	public static void load() {
		try {
			CompositeConfiguration config = new CompositeConfiguration();
			config.addConfiguration(new SystemConfiguration());
			config.addConfiguration(new EnvironmentConfiguration());
			config.addConfiguration(getPropertiesConfiguration("engine-module.properties"));
			ITERATION_LIMIT = config.getInt("gitb.engine.iteration-limit", 1000);
			// Determine callback URLs - start.
			var rootCallbackUrl = config.getString(ENV_CALLBACK_ROOT_URL, "");
			var messagingCallbackUrl = config.getString(ENV_CALLBACK_MESSAGING_URL, "");
			var validationCallbackUrl = config.getString(ENV_CALLBACK_VALIDATION_URL, "");
			var processingCallbackUrl = config.getString(ENV_CALLBACK_PROCESSING_URL, "");
			// Backwards compatibility checks.
			if (messagingCallbackUrl.isEmpty()) {
				messagingCallbackUrl = config.getString("gitb.messaging.callbackURL", "");
			}
			if (validationCallbackUrl.isEmpty()) {
				validationCallbackUrl = config.getString("gitb.validation.callbackURL", "");
			}
			if (processingCallbackUrl.isEmpty()) {
				processingCallbackUrl = config.getString("gitb.processing.callbackURL", "");
			}
			if (rootCallbackUrl.isEmpty()) {
				// Ideally the root callback URL is the one configured. However if any of the other callback URLs are defined instead we can infer from them the root.
				String referenceToUse;
				if (!messagingCallbackUrl.isEmpty()) {
					referenceToUse = messagingCallbackUrl;
				} else if (!validationCallbackUrl.isEmpty()) {
					referenceToUse = validationCallbackUrl;
				} else if (!processingCallbackUrl.isEmpty()) {
					referenceToUse = processingCallbackUrl;
				} else {
					referenceToUse = config.getString("gitb.defaultCallbackURL", "");
				}
				if (referenceToUse.isEmpty()) {
					throw new IllegalStateException("No callback addresses were configured for test engine notifications. You must configure at least one of %s, %s, %s or %s the properties.".formatted(ENV_CALLBACK_ROOT_URL, ENV_CALLBACK_MESSAGING_URL, ENV_CALLBACK_VALIDATION_URL, ENV_CALLBACK_PROCESSING_URL));
				}
				rootCallbackUrl = inferCallbackURL("", referenceToUse);
			}
			// By default, infer the messaging, processing and validation callback URLs from the root callback URL to avoid requiring their explicit definition.
			rootCallbackUrl = Objects.requireNonNull(Strings.CS.appendIfMissing(rootCallbackUrl, "/"), "No root callback address could be determined. You must configure the %s property.".formatted(ENV_CALLBACK_ROOT_URL));
			if (messagingCallbackUrl.isEmpty()) {
				messagingCallbackUrl = inferCallbackURL("MessagingClient", rootCallbackUrl);
			}
			if (validationCallbackUrl.isEmpty()) {
				validationCallbackUrl = inferCallbackURL("ValidationClient", rootCallbackUrl);
			}
			if (processingCallbackUrl.isEmpty()) {
				processingCallbackUrl = inferCallbackURL("ProcessingClient", rootCallbackUrl);
			}
			ROOT_CALLBACK_URL = rootCallbackUrl;
			CoreConfiguration.MESSAGING_CALLBACK_URL = messagingCallbackUrl;
            CoreConfiguration.VALIDATION_CALLBACK_URL = validationCallbackUrl;
            CoreConfiguration.PROCESSING_CALLBACK_URL = processingCallbackUrl;
			HANDLER_API_ROOT = rootCallbackUrl+HANDLER_API_SEGMENT+"/";
			// Determine callback URLs - end.
			// Temp storage properties - start.
			TEMP_STORAGE_ENABLED = config.getBoolean("gitb.engine.storage.enabled", Boolean.TRUE);
			TEMP_STORAGE_LOCATION = config.getString("gitb.engine.storage.location", "./temp/session/");
			TEMP_STORAGE_BINARY_ENABLED = config.getBoolean("gitb.engine.storage.binary.enabled", Boolean.TRUE);
			TEMP_STORAGE_STRING_ENABLED = config.getBoolean("gitb.engine.storage.string.enabled", Boolean.TRUE);
			TEMP_STORAGE_XML_ENABLED = config.getBoolean("gitb.engine.storage.xml.enabled", Boolean.TRUE);
			TEMP_STORAGE_BINARY_THRESHOLD_BYTES = config.getLong("gitb.engine.storage.binary.threshold", 50 * 1024L); // 50 KB
			TEMP_STORAGE_STRING_THRESHOLD_CHARS = config.getLong("gitb.engine.storage.string.threshold", 50 * 512L); // 50 KB (considering 2-byte encoding)
			TEMP_STORAGE_XML_THRESHOLD_BYTES = config.getLong("gitb.engine.storage.xml.threshold", 50 * 1024L); // 50 KB
			// Temp storage properties - end.
			// Remote repository - start.
			if (System.getenv().containsKey(ENV_REPOSITORY_TEST_CASE_URL)) {
				TEST_CASE_REPOSITORY_URL = System.getenv().get(ENV_REPOSITORY_TEST_CASE_URL);
			} else if (System.getenv().containsKey(ENV_REPOSITORY_ROOT_URL)) {
				TEST_CASE_REPOSITORY_URL = Strings.CS.removeEnd(System.getenv(ENV_REPOSITORY_ROOT_URL), "/") + "/api/repository/tests/:test_id/definition";
			} else {
				TEST_CASE_REPOSITORY_URL = config.getString(ENV_REPOSITORY_TEST_CASE_URL);
			}
			if (System.getenv().containsKey(ENV_REPOSITORY_TEST_RESOURCE_URL)) {
				TEST_RESOURCE_REPOSITORY_URL = System.getenv().get(ENV_REPOSITORY_TEST_RESOURCE_URL);
			} else if (System.getenv().containsKey(ENV_REPOSITORY_ROOT_URL)) {
				TEST_RESOURCE_REPOSITORY_URL = Strings.CS.removeEnd(System.getenv(ENV_REPOSITORY_ROOT_URL), "/") + "/api/repository/resource/:test_id/:resource_id";
			} else {
				TEST_RESOURCE_REPOSITORY_URL = config.getString(ENV_REPOSITORY_TEST_RESOURCE_URL);
			}
			REPOSITORY_HEALTHCHECK_URL = Strings.CS.removeEnd(TEST_RESOURCE_REPOSITORY_URL, "/resource/:test_id/:resource_id") + "/healthCheck";
			REPOSITORY_ROOT_URL = Strings.CS.removeEnd(Strings.CS.removeEnd(StringUtils.getCommonPrefix(REPOSITORY_HEALTHCHECK_URL, TEST_CASE_REPOSITORY_URL, TEST_RESOURCE_REPOSITORY_URL), "/"), "/api/repository");
			TEST_ID_PARAMETER = System.getenv().getOrDefault("remote.testcase.test-id.parameter", config.getString("remote.testcase.test-id.parameter"));
			RESOURCE_ID_PARAMETER = System.getenv().getOrDefault("remote.testcase.resource-id.parameter", config.getString("remote.testcase.resource-id.parameter"));
			// Configure also the HMAC information used to authorize remote calls.
			String hmacKey = getFromFileConfigOrEnvironment("HMAC_KEY", "devKey");
			String hmacKeyWindow = System.getenv().getOrDefault("HMAC_WINDOW", "10000");
			HmacUtils.configure(hmacKey, Long.valueOf(hmacKeyWindow));
			// Remote repository - end.
			// Embedded messaging handler configuration - start.
			DEFAULT_MESSAGING_CONFIGURATION = loadDefaultMessagingHandlerConfiguration(config);
			// Embedded messaging handler configuration - end.
			// JSON Path configuration
			configureJsonPath();
		} catch (ConfigurationException | IOException e) {
			throw new IllegalStateException("Error loading configuration", e);
		}
	}

	private static void configureJsonPath() {
		com.jayway.jsonpath.Configuration.setDefaults(new com.jayway.jsonpath.Configuration.Defaults() {

			private final JsonProvider jsonProvider = new JacksonJsonProvider();
			private final MappingProvider mappingProvider = new JacksonMappingProvider();

			@Override
			public JsonProvider jsonProvider() {
				return jsonProvider;
			}

			@Override
			public MappingProvider mappingProvider() {
				return mappingProvider;
			}

			@Override
			public Set<Option> options() {
				return EnumSet.noneOf(Option.class);
			}
		});

	}

	private static String getFromFileConfigOrEnvironment(String baseName, String defaultValue) {
		String filePathName = baseName+"_FILE";
		if (System.getenv().containsKey(filePathName)) {
			// Load from file.
            try {
				/*
				 * In the case of gitb-ui and file-based secrets, values are always trimmed of
				 * leading and trailing whitespace. Failing to do so in gitb-srv may lead to
				 * inconsistencies and failures (e.g. a HMAC key that doesn't match).
				 */
                return Files.readString(Path.of(System.getenv(filePathName))).trim();
            } catch (IOException e) {
                throw new IllegalStateException("Error reading file", e);
            }
        } else {
			// Load from environment variable or the default.
			return System.getenv().getOrDefault(baseName, defaultValue);
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
				config.getString("gitb.messaging.actor-name")
		);
	}

}
