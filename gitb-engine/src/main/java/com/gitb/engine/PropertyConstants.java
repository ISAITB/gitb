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

import java.util.Set;

public class PropertyConstants {

    public static String LOG_EVENT_STEP_ID = "-999";

    public static final String AUTH_USERNAMETOKEN_USERNAME = "auth.token.username";
    public static final String AUTH_USERNAMETOKEN_PASSWORD = "auth.token.password";
    public static final String AUTH_USERNAMETOKEN_PASSWORDTYPE = "auth.token.password.type";
    public static final String AUTH_USERNAMETOKEN_PASSWORDTYPE_VALUE_TEXT = "text";
    public static final String AUTH_USERNAMETOKEN_PASSWORDTYPE_VALUE_DIGEST = "digest";

    public static final String AUTH_BASIC_USERNAME = "auth.basic.username";
    public static final String AUTH_BASIC_PASSWORD = "auth.basic.password";

    public static final String TEST_SESSION_ID = "TEST_SESSION_ID";
    public static final String TEST_CASE_ID = "TEST_CASE_ID";
    public static final String TEST_STEP_ID = "TEST_STEP_ID";

    public static final String DOMAIN_MAP = "DOMAIN";
    public static final String ORGANISATION_MAP = "ORGANISATION";
    public static final String SYSTEM_MAP = "SYSTEM";
    public static final String SESSION_MAP = "SESSION";
    /**
     * The map containing the success flags for each executed step.
     */
    public static final String STEP_SUCCESS_MAP = "STEP_SUCCESS";

    /**
     * The map containing the status values for all steps.
     */
    public static final String STEP_STATUS_MAP = "STEP_STATUS";

    /**
     * The scope variable holding the overall result of the test session.
     */
    public static final String TEST_SUCCESS = "TEST_SUCCESS";

    public static final String SESSION_MAP_TEST_SESSION_ID = "sessionId";
    public static final String SESSION_MAP_TEST_CASE_ID = "testCaseId";
    public static final String SESSION_MAP_TEST_ENGINE_VERSION = "testEngineVersion";

    public static final String SYSTEM_MAP_API_KEY = "apiKey";

    private static final Set<String> BUILT_IN_PROPERTIES = Set.of(DOMAIN_MAP, ORGANISATION_MAP, SYSTEM_MAP, SESSION_MAP, STEP_STATUS_MAP, STEP_SUCCESS_MAP, TEST_SUCCESS);

    public static boolean isBuiltInProperty(String name) {
        return name != null && BUILT_IN_PROPERTIES.contains(name);
    }
}
