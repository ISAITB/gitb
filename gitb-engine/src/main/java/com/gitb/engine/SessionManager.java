package com.gitb.engine;

import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.tdl.TestCase;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by serbay on 9/12/14.
 */
public class SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
	private static SessionManager instance;

	// All executing TestCase Contexts; sessionId -> testcase context map
	private final Map<String, TestCaseContext> contexts;
	private final Map<String, String> messagingSessionToTestSessionMap;
	private final Map<String, String> processingSessionToTestSessionMap;

	private SessionManager() {
		contexts = new ConcurrentHashMap<>();
		messagingSessionToTestSessionMap = new ConcurrentHashMap<>();
		processingSessionToTestSessionMap = new ConcurrentHashMap<>();
        logger.info("SessionManager has been initialized...");
		try {
			var sessionDataRoot = Path.of(TestEngineConfiguration.TEMP_STORAGE_LOCATION);
			if (Files.exists(sessionDataRoot)) {
				FileUtils.cleanDirectory(sessionDataRoot.toFile());
			}
		} catch (IOException e) {
			throw new IllegalStateException("Unable to clean temporary session storage", e);
		}
	}

	public synchronized static SessionManager getInstance() {
		if(instance == null)  {
			instance = new SessionManager();
		}
		return instance;
	}

	public boolean exists(String sessionId) {
		return contexts.containsKey(sessionId);
	}

	public boolean notExists(String sessionId) {
		return !exists(sessionId);
	}

    /**
     * Create a new testcase execution session
     * @param testCaseId
     * @return
     */
	public String newSession(String testCaseId, String sessionIdToAssign) {
		String sessionId = sessionIdToAssign;
		if (sessionId == null || sessionId.isBlank()) {
			// Create a random UUID as the session id
			sessionId = UUID.randomUUID().toString();
		} else if (exists(sessionId)) {
			sessionId = UUID.randomUUID().toString();
			logger.warn("Ignoring requested session ID ["+sessionIdToAssign+"] as it already exists. Using ["+sessionId+"] instead.");
		}
		//Load the tdl:TestCase definition
        TestCase testCase = TestCaseManager.getTestCaseDescription(testCaseId);
        // Ensure we replace the text ID with the internal fully unique ID
		testCase.setId(testCaseId);
        //Create the test case context
		TestCaseContext testCaseContext = new TestCaseContext(testCase, sessionId);
        //Put the context into the map
		contexts.put(sessionId, testCaseContext);
        //Return the session id
		return sessionId;
	}

	/**
	 * Soft-copies the test case context for the source session id to the destination session id
	 * @param srcSessionId source session id
	 */
	public String duplicateSession(String srcSessionId) {
		String dstSessionId = UUID.randomUUID().toString();

		TestCaseContext testCaseContext = contexts.get(srcSessionId);
		contexts.put(dstSessionId, testCaseContext);

		return dstSessionId;
	}

	public void endSession(String sessionId) {
		TestCaseContext testCaseContext = contexts.remove(sessionId);

		if (testCaseContext != null) {
			testCaseContext.destroy();
		}
	}

	public void destroy() {
		Collection<String> sessionIds = contexts.keySet();

		for(String sessionId : sessionIds) {
			endSession(sessionId);
		}
	}

    /**
     * Returns the TestCaseContext object for the given test execution session
     * @param sessionId
     * @return
     */
	public TestCaseContext getContext(String sessionId) {
		return contexts.get(sessionId);
	}

	public String getTestSessionForMessagingSession(String messagingSessionId) {
		return messagingSessionToTestSessionMap.get(messagingSessionId);
	}

	public String getTestSessionForProcessingSession(String processingSessionId) {
		if (processingSessionToTestSessionMap.containsKey(processingSessionId)) {
			return processingSessionToTestSessionMap.get(processingSessionId);
		} else if (exists(processingSessionId)) {
			// The processing session ID for a non-transactional process step is the test session ID itself.
			return processingSessionId;
		}
		return null;
	}

	public void removeMessagingSession(String messagingSessionId) {
		messagingSessionToTestSessionMap.remove(messagingSessionId);
	}

	public void removeProcessingSession(String processingSessionId) {
		processingSessionToTestSessionMap.remove(processingSessionId);
	}

	public void mapMessagingSessionToTestSession(String messagingSessionId, String testSessionId) {
		messagingSessionToTestSessionMap.put(messagingSessionId, testSessionId);
	}

	public void mapProcessingSessionToTestSession(String processingSessionId, String testSessionId) {
		processingSessionToTestSessionMap.put(processingSessionId, testSessionId);
	}

}
