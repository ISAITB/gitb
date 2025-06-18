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

package com.gitb.engine.utils;

import com.gitb.engine.ModuleManager;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.repository.ITestCaseRepository;
import com.gitb.tdl.TestArtifact;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by serbay on 10/10/14.
 */
public class ArtifactUtils {

	public static Pair<String, DataType> resolveArtifact(TestCaseContext context, TestCaseScope scope, TestArtifact artifact) throws IOException {
		if (artifact == null) {
			return null;
		}
		String fromToConsider = artifact.getFrom();
		if (fromToConsider == null) {
			fromToConsider = scope.getTestSuiteContext();
		}
		String pathToLookup = artifact.getValue();
		VariableResolver variableResolver = new VariableResolver(scope);
		if (VariableResolver.isVariableReference(pathToLookup)) {
			DataType resolvedType = variableResolver.resolveVariable(pathToLookup);
			pathToLookup = (String)(resolvedType.convertTo(DataType.STRING_DATA_TYPE).getValue());
		}
		ITestCaseRepository testCaseRepository = ModuleManager.getInstance().getTestCaseRepository();
		DataType data = null;
		if (testCaseRepository != null) {
			InputStream inputStream = testCaseRepository.getTestArtifact(fromToConsider, context.getTestCase().getId(), pathToLookup);
			if (inputStream != null) {
				// Create data type from artifact.
				data = DataTypeFactory.getInstance().create(
						IOUtils.toByteArray(inputStream),
						(artifact.getType() == null)?DataType.STRING_DATA_TYPE:artifact.getType(),
						(artifact.getEncoding() == null)?"UTF-8":artifact.getEncoding());
				// Set the location of the artifact if it is a schema type in order to resolve
				// the location of other artifacts imported by this one.
				data.setImportPath(pathToLookup);
				data.setImportTestSuite(fromToConsider);
			}
		}
		return Pair.of(pathToLookup, data);
	}

}
