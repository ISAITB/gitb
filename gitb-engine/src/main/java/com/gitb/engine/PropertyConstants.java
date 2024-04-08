package com.gitb.engine;

import java.util.Set;

public class PropertyConstants {

    public static String LOG_EVENT_STEP_ID = "-999";

    public static final String AUTH_USERNAMETOKEN_USERNAME = "auth.token.username";
    public static final String AUTH_USERNAMETOKEN_PASSWORD = "auth.token.password";
    public static final String AUTH_USERNAMETOKEN_PASSWORDTYPE = "auth.token.password.type";
    public static final String AUTH_USERNAMETOKEN_PASSWORDTYPE__VALUE_TEXT = "text";
    public static final String AUTH_USERNAMETOKEN_PASSWORDTYPE__VALUE_DIGEST = "digest";

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

    public static final String SESSION_MAP__TEST_SESSION_ID = "sessionId";
    public static final String SESSION_MAP__TEST_CASE_ID = "testCaseId";
    public static final String SESSION_MAP__TEST_ENGINE_VERSION = "testEngineVersion";

    private static final Set<String> BUILT_IN_PROPERTIES = Set.of(DOMAIN_MAP, ORGANISATION_MAP, SYSTEM_MAP, SESSION_MAP, STEP_STATUS_MAP, STEP_SUCCESS_MAP, TEST_SUCCESS);

    public static boolean isBuiltInProperty(String name) {
        return name != null && BUILT_IN_PROPERTIES.contains(name);
    }
}
