package com.gitb.engine.commands.common;

import java.io.Serializable;

/**
 * Created by serbay on 9/4/14.
 */
public class TestCaseCommand implements Serializable {
	private final String testCaseId;

	public TestCaseCommand(String testCaseId) {
		this.testCaseId = testCaseId;
	}

	public String getTestCaseId() {
		return testCaseId;
	}
}
