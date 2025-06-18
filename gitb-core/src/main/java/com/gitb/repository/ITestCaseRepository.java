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
