/*
 * Copyright (C) 2026 European Union
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The limits applied to the remote resources that are loaded while a test step executes (see {@link RemoteResourceBudget}).
 * <p>
 * The limits are set by the operator of the engine by means of environment variables (or, as a fallback, system properties):
 * <ul>
 *     <li>{@value #ENV_MAX_RESOURCE_SIZE}: the maximum size of a single remote resource (e.g. 50MB).</li>
 *     <li>{@value #ENV_MAX_REQUESTS}: the maximum number of remote requests (including redirects).</li>
 *     <li>{@value #ENV_MAX_TOTAL_SIZE}: the maximum total size of the remote resources (e.g. 250MB).</li>
 *     <li>{@value #ENV_REQUEST_TIMEOUT}: the maximum time (in milliseconds) allowed for a single remote request, including
 *     the reading of its response.</li>
 * </ul>
 *
 * @param maxResourceSize The maximum size (in bytes) of a single remote resource.
 * @param maxRequests The maximum number of remote requests (including redirects).
 * @param maxTotalSize The maximum total size (in bytes) of all remote resources.
 * @param requestTimeoutMillis The maximum time (in milliseconds) for a single request, including the reading of its response.
 */
public record RemoteResourceLimits(long maxResourceSize, int maxRequests, long maxTotalSize, long requestTimeoutMillis) {

    public static final String ENV_MAX_RESOURCE_SIZE = "REMOTE_LIMITS_MAX_RESOURCE_SIZE";
    public static final String ENV_MAX_REQUESTS = "REMOTE_LIMITS_MAX_REQUESTS";
    public static final String ENV_MAX_TOTAL_SIZE = "REMOTE_LIMITS_MAX_TOTAL_SIZE";
    public static final String ENV_REQUEST_TIMEOUT = "REMOTE_LIMITS_REQUEST_TIMEOUT";
    public static final String PROPERTY_MAX_RESOURCE_SIZE = "gitb.remote-limits.max-resource-size";
    public static final String PROPERTY_MAX_REQUESTS = "gitb.remote-limits.max-requests";
    public static final String PROPERTY_MAX_TOTAL_SIZE = "gitb.remote-limits.max-total-size";
    public static final String PROPERTY_REQUEST_TIMEOUT = "gitb.remote-limits.request-timeout";

    private static final Logger LOG = LoggerFactory.getLogger(RemoteResourceLimits.class);
    private static final Pattern SIZE_PATTERN = Pattern.compile("^(\\d+)\\s*(B|KB|MB|GB)?$");
    private static final long KB = 1024L;

    private static volatile RemoteResourceLimits global;

    /**
     * Constructor.
     */
    public RemoteResourceLimits {
        if (maxResourceSize <= 0 || maxRequests <= 0 || maxTotalSize <= 0 || requestTimeoutMillis <= 0) {
            throw new IllegalArgumentException("All remote resource limits must be greater than zero");
        }
    }

    /**
     * @return The limits that apply if nothing is configured: 50 MB per resource, 200 requests, 250 MB in total and 60 seconds per request.
     */
    public static RemoteResourceLimits defaults() {
        return new RemoteResourceLimits(50 * KB * KB, 200, 250 * KB * KB, 60000L);
    }

    /**
     * Get the limits to apply to the remote resources loaded by the engine, as configured by the operator.
     * <p>
     * Unless set explicitly (see {@link #setGlobal(RemoteResourceLimits)}) these are determined from the environment.
     *
     * @return The limits.
     * @throws IllegalArgumentException If a configured value is invalid.
     */
    public static RemoteResourceLimits global() {
        var limits = global;
        if (limits == null) {
            synchronized (RemoteResourceLimits.class) {
                limits = global;
                if (limits == null) {
                    limits = fromSettings(RemoteResourceLimits::environmentOrProperty);
                    LOG.info("Remote resource limits - maximum resource size: {} bytes, maximum requests: {}, maximum total size: {} bytes, request timeout: {} ms",
                            limits.maxResourceSize, limits.maxRequests, limits.maxTotalSize, limits.requestTimeoutMillis);
                    global = limits;
                }
            }
        }
        return limits;
    }

    /**
     * Explicitly set the limits to apply to the remote resources loaded by the engine.
     *
     * @param limits The limits (null to determine them again from the environment).
     */
    public static void setGlobal(RemoteResourceLimits limits) {
        synchronized (RemoteResourceLimits.class) {
            global = limits;
        }
    }

    /**
     * Determine the limits from the provided settings, using defaults for the ones not set.
     *
     * @param settingLookup Function that returns the value of a setting given its environment variable name (null if not set).
     * @return The limits.
     * @throws IllegalArgumentException If a configured value is invalid.
     */
    static RemoteResourceLimits fromSettings(Function<String, String> settingLookup) {
        var defaults = defaults();
        return new RemoteResourceLimits(
                parseSize(ENV_MAX_RESOURCE_SIZE, settingLookup.apply(ENV_MAX_RESOURCE_SIZE), defaults.maxResourceSize),
                (int) parseNumber(ENV_MAX_REQUESTS, settingLookup.apply(ENV_MAX_REQUESTS), defaults.maxRequests, Integer.MAX_VALUE),
                parseSize(ENV_MAX_TOTAL_SIZE, settingLookup.apply(ENV_MAX_TOTAL_SIZE), defaults.maxTotalSize),
                parseNumber(ENV_REQUEST_TIMEOUT, settingLookup.apply(ENV_REQUEST_TIMEOUT), defaults.requestTimeoutMillis, Long.MAX_VALUE)
        );
    }

    private static String environmentOrProperty(String envName) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            value = System.getProperty(switch (envName) {
                case ENV_MAX_RESOURCE_SIZE -> PROPERTY_MAX_RESOURCE_SIZE;
                case ENV_MAX_REQUESTS -> PROPERTY_MAX_REQUESTS;
                case ENV_MAX_TOTAL_SIZE -> PROPERTY_MAX_TOTAL_SIZE;
                default -> PROPERTY_REQUEST_TIMEOUT;
            });
        }
        return value;
    }

    private static long parseNumber(String name, String value, long defaultValue, long max) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            long number = Long.parseLong(value.trim());
            if (number <= 0 || number > max) {
                throw new NumberFormatException();
            }
            return number;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value [%s] for %s: a number greater than zero is expected".formatted(value, name));
        }
    }

    private static long parseSize(String name, String value, long defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        Matcher matcher = SIZE_PATTERN.matcher(value.trim().toUpperCase(Locale.ROOT));
        if (matcher.matches()) {
            long amount = Long.parseLong(matcher.group(1));
            long multiplier = switch (matcher.group(2) == null ? "B" : matcher.group(2)) {
                case "KB" -> KB;
                case "MB" -> KB * KB;
                case "GB" -> KB * KB * KB;
                default -> 1L;
            };
            if (amount > 0 && amount <= Long.MAX_VALUE / multiplier) {
                return amount * multiplier;
            }
        }
        throw new IllegalArgumentException("Invalid value [%s] for %s: a size greater than zero is expected (e.g. 512KB, 50MB or 1GB)".formatted(value, name));
    }

}
