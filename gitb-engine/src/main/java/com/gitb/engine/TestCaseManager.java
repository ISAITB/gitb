package com.gitb.engine;

import com.gitb.ModuleManager;
import com.gitb.core.Actor;
import com.gitb.core.ErrorCode;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.repository.ITestCaseRepository;
import com.gitb.tdl.*;
import com.gitb.tdl.TestCase;
import com.gitb.utils.ErrorUtils;

/**
 * Created by serbay on 9/5/14.
 * Provide the operations to read the definitions related with the TestCase
 */
public class TestCaseManager {
    /**
     * Return the TestCase Description (TDL) given TestCase.id
     *
     * @param testCaseId
     * @return
     */
    public static TestCase getTestCaseDescription(String testCaseId) {
	    ITestCaseRepository repository = ModuleManager.getInstance().getTestCaseRepository();
        if (repository.isTestCaseAvailable(testCaseId)) {
            TestCase testCase = repository.getTestCase(testCaseId);
            if(testCase != null) {
	            return testCase;
            }
        }
        throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.ARTIFACT_NOT_FOUND, "The TestCase definition not found for [" + testCaseId + "]!"));
    }

    /**
     * Return the TestSuite Description (TDL) given TestSuite.id
     *
     * @param testSuiteId
     * @return
     */
    public static TestSuite getTestSuiteDescription(String testSuiteId) {
	    ITestCaseRepository repository = ModuleManager.getInstance().getTestCaseRepository();
        if (repository.isTestSuiteAvailable(testSuiteId)) {
            return repository.getTestSuite(testSuiteId);
        }
        throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.ARTIFACT_NOT_FOUND, "The TestSuite definition not found for [" + testSuiteId + "]!"));
    }


    /**
     * Return the TestCase Description (TPL) given TestCase.id
     *
     * @param testCaseId
     * @return
     */
    public static com.gitb.tpl.TestCase getTestCasePresentation(String testCaseId) {
        TestCase testCaseDescription = getTestCaseDescription(testCaseId);
        return TestCaseUtils.convertTestCase(testCaseDescription);
    }




    /**
     * Return the full Actor description of an Actor mentioned in a test suite
     *
     * @param testSuiteId
     * @param actorId
     * @return
     */
    public static Actor getActorDescription(String testSuiteId, String actorId) {
        TestSuite testSuite = getTestSuiteDescription(testSuiteId);
        for (Actor actor : testSuite.getActors().getActor()) {
            if (actor.getId().equals(actorId))
                return actor;
        }
        throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.ARTIFACT_NOT_FOUND, "The Actor definition not found for [" + actorId + "] in TestSuite ["+testSuiteId+"]!"));
    }

}
