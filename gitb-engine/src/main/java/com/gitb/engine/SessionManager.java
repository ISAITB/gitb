package com.gitb.engine;

import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.tdl.TestCase;
import org.apache.commons.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by serbay on 9/12/14.
 */
public class SessionManager {
    private static Logger logger = LoggerFactory.getLogger(SessionManager.class);
	private static SessionManager instance;

	// All executing TestCase Contexts; sessionId -> testcase context map
	private Map<String, TestCaseContext> contexts;

	private SessionManager() {
		contexts = new ConcurrentHashMap<>();
        logger.info("SessionManager has been initialized...");
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
	public String newSession(String testCaseId) {
        //Create a random UUID as the session id
		String sessionId = UUID.randomUUID().toString();
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
		TestCaseContext testCaseContext = contexts.get(sessionId);
		return testCaseContext;
	}
}
