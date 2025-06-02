package com.gitb.repository;

import com.gitb.tdl.Scriptlet;
import com.gitb.tdl.TestCase;
import com.gitb.tdl.TestSuite;

import java.io.InputStream;
import java.util.Collection;

/**
 * Created by serbay on 9/10/14.
 */
public interface ITestCaseRepository {
	String getName();
	TestCase getTestCase(String testCaseId);
	Scriptlet getScriptlet(String from, String testCaseId, String scriptletPath);
	InputStream getTestArtifact(String from, String testCaseId, String artifactPath);
	String healthCheck(String message) throws Exception;
}
