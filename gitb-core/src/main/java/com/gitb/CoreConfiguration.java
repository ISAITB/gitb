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

package com.gitb;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.IOException;

import static com.gitb.utils.ConfigUtils.getPropertiesConfiguration;

/**
 * Created by serbay on 9/8/14.
 * Configuration Handler for the TestEngine
 */
public class CoreConfiguration {

	public static final String TEST_ENGINE_VERSION;
	public static final String BUILD_TIMESTAMP;
    public static String MESSAGING_CALLBACK_URL;
    public static String VALIDATION_CALLBACK_URL;
    public static String PROCESSING_CALLBACK_URL;

    /*
     * Load the configurations from the configuration files
     */
	static {
		try {
			CompositeConfiguration config = new CompositeConfiguration();
			config.addConfiguration(new SystemConfiguration());
			config.addConfiguration(getPropertiesConfiguration("core-module.properties"));
			BUILD_TIMESTAMP  = config.getString("gitb.buildTimestamp");
            String version  = config.getString("gitb.version");
            TEST_ENGINE_VERSION = "%s (%s)".formatted(version, CoreConfiguration.BUILD_TIMESTAMP);
		} catch (ConfigurationException | IOException e) {
			throw new IllegalStateException("Error loading configuration", e);
		}
	}

}
