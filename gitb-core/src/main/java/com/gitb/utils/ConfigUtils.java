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
