package com.gitb.engine.utils;

import com.gitb.ModuleManager;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.repository.ITestCaseRepository;
import com.gitb.tdl.TestArtifact;
import com.gitb.types.DataType;
import com.gitb.types.SchemaType;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by serbay on 10/10/14.
 */
public class ArtifactUtils {

	public static DataType resolveArtifact(TestCaseContext context, TestCaseScope scope, TestArtifact artifact) throws IOException {
		if (artifact == null) {
			return null;
		}
		String fromToConsider = artifact.getFrom();
		if (fromToConsider == null) {
			fromToConsider = scope.getTestSuiteContext();
		}
		String pathToLookup = artifact.getValue();
		VariableResolver variableResolver = new VariableResolver(scope);
		if (variableResolver.isVariableReference(pathToLookup)) {
			DataType resolvedType = variableResolver.resolveVariable(pathToLookup);
			pathToLookup = (String)resolvedType.toStringType().getValue();
		}
		ITestCaseRepository testCaseRepository = ModuleManager.getInstance().getTestCaseRepository();
		DataType data = null;
		if (testCaseRepository != null) {
			InputStream inputStream = testCaseRepository.getTestArtifact(fromToConsider, context.getTestCase().getId(), pathToLookup);
			if (inputStream != null) {
				data = TemplateUtils.generateDataTypeFromTemplate(scope, inputStream, artifact.getType(), artifact.getEncoding());
			}
			// Set the location of the artifact if it is a schema type in order to resolve
			// the location of other artifacts imported by this one.
			if (data instanceof SchemaType) {
				((SchemaType) data).setSchemaLocation(pathToLookup);
				((SchemaType) data).setTestSuiteId(fromToConsider);
			}
		}
		return data;
	}

}
