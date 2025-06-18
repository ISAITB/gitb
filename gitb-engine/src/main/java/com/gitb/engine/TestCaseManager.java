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

import com.gitb.core.ActorConfiguration;
import com.gitb.core.ErrorCode;
import com.gitb.engine.utils.TestCaseConverter;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.repository.ITestCaseRepository;
import com.gitb.tdl.TestCase;
import com.gitb.utils.ErrorUtils;

import java.util.List;

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
        TestCase testCase = repository.getTestCase(testCaseId);
        if (testCase != null) {
            return testCase;
        }
        throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.ARTIFACT_NOT_FOUND, "No test case definition found for ID [" + testCaseId + "]!"));
    }

    /**
     * Return the TestCase Description (TPL).
     *
     * @param sessionId The session ID
     * @return The test case.
     */
    public static com.gitb.tpl.TestCase getTestCasePresentationBySessionId(String sessionId, List<ActorConfiguration> configs) {
        var ctx = SessionManager.getInstance().getContext(sessionId);
        if (ctx == null) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_SESSION, "No test session could be found for ID [" + sessionId + "]!"));
        }
        return new TestCaseConverter(ctx.getTestCase(), ctx.getScriptletCache(), configs).convertTestCase(ctx.getTestCase().getId());
    }

    /**
     * Return the TestCase Description (TPL).
     *
     * @param testCaseId The test case ID.
     * @return The test case.
     */
    public static com.gitb.tpl.TestCase getTestCasePresentationByTestCaseId(String testCaseId, List<ActorConfiguration> configs) {
        TestCase testCaseDescription = getTestCaseDescription(testCaseId);
        // Ensure we replace the text ID with the internal fully unique ID
        testCaseDescription.setId(testCaseId);
        return new TestCaseConverter(testCaseDescription, configs).convertTestCase(testCaseId);
    }

}
