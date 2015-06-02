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
	public String getName();
	public boolean isTestCaseAvailable(String testCaseId);
	public TestCase getTestCase(String testCaseId);
	public boolean isScriptletAvailable(String scriptletId);
	public Scriptlet getScriptlet(String scriptletId);
    public boolean isTestSuiteAvailable(String testSuiteId);
    public TestSuite getTestSuite(String testSuiteId);
    public InputStream getTestArtifact(String path);
    public boolean isTestArtifactAvailable(String path);
}
