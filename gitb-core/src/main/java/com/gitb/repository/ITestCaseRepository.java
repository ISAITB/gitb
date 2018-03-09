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
	boolean isTestCaseAvailable(String testCaseId);
	TestCase getTestCase(String testCaseId);
	boolean isScriptletAvailable(String testCaseId, String scriptletId);
	Scriptlet getScriptlet(String testCaseId, String scriptletId);
    InputStream getTestArtifact(String testCaseId, String path);
    boolean isTestArtifactAvailable(String testCaseId, String path);
}
