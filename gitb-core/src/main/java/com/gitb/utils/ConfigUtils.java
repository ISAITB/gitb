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

package com.gitb.utils;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;

import java.io.IOException;
import java.io.InputStream;

public final class ConfigUtils {

    private ConfigUtils() {}

    public static PropertiesConfiguration getPropertiesConfiguration(String propertyPath) throws IOException, ConfigurationException {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(propertyPath)) {
            PropertiesConfiguration config = new PropertiesConfiguration();
            FileHandler handler = new FileHandler(config);
            handler.load(in);
            return config;
        }
    }

}
