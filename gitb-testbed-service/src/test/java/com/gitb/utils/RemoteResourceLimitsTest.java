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

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RemoteResourceLimitsTest {

    @Test
    void defaultsApplyWhenNothingIsConfigured() {
        var limits = RemoteResourceLimits.fromSettings(name -> null);
        assertEquals(50L * 1024 * 1024, limits.maxResourceSize());
        assertEquals(200, limits.maxRequests());
        assertEquals(250L * 1024 * 1024, limits.maxTotalSize());
        assertEquals(60000L, limits.requestTimeoutMillis());
        assertEquals(RemoteResourceLimits.defaults(), limits);
    }

    @Test
    void settingsAreParsed() {
        var limits = RemoteResourceLimits.fromSettings(Map.of(
                RemoteResourceLimits.ENV_MAX_RESOURCE_SIZE, "10MB",
                RemoteResourceLimits.ENV_MAX_REQUESTS, "20",
                RemoteResourceLimits.ENV_MAX_TOTAL_SIZE, " 1gb ",
                RemoteResourceLimits.ENV_REQUEST_TIMEOUT, "5000"
        )::get);
        assertEquals(10L * 1024 * 1024, limits.maxResourceSize());
        assertEquals(20, limits.maxRequests());
        assertEquals(1024L * 1024 * 1024, limits.maxTotalSize());
        assertEquals(5000L, limits.requestTimeoutMillis());
    }

    @Test
    void sizesCanBeGivenInKilobytesOrBytes() {
        assertEquals(512L * 1024, RemoteResourceLimits.fromSettings(Map.of(RemoteResourceLimits.ENV_MAX_RESOURCE_SIZE, "512KB")::get).maxResourceSize());
        assertEquals(1000L, RemoteResourceLimits.fromSettings(Map.of(RemoteResourceLimits.ENV_MAX_RESOURCE_SIZE, "1000")::get).maxResourceSize());
        assertEquals(1000L, RemoteResourceLimits.fromSettings(Map.of(RemoteResourceLimits.ENV_MAX_RESOURCE_SIZE, "1000B")::get).maxResourceSize());
    }

    @Test
    void invalidSettingsAreRejectedNamingTheSetting() {
        for (var setting : Map.of(
                RemoteResourceLimits.ENV_MAX_RESOURCE_SIZE, "lots",
                RemoteResourceLimits.ENV_MAX_TOTAL_SIZE, "0MB",
                RemoteResourceLimits.ENV_MAX_REQUESTS, "-1",
                RemoteResourceLimits.ENV_REQUEST_TIMEOUT, "soon"
        ).entrySet()) {
            var error = assertThrows(IllegalArgumentException.class, () -> RemoteResourceLimits.fromSettings(Map.of(setting.getKey(), setting.getValue())::get));
            assertTrue(error.getMessage().contains(setting.getKey()), error.getMessage());
        }
    }

    @Test
    void limitsMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new RemoteResourceLimits(0, 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new RemoteResourceLimits(1, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new RemoteResourceLimits(1, 1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new RemoteResourceLimits(1, 1, 1, 0));
    }

    @Test
    void globalLimitsCanBeSetAndReset() {
        var custom = new RemoteResourceLimits(1024, 5, 2048, 1000);
        try {
            RemoteResourceLimits.setGlobal(custom);
            assertSame(custom, RemoteResourceLimits.global());
        } finally {
            RemoteResourceLimits.setGlobal(null);
        }
    }

}
