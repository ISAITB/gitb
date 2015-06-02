package com.gitb.engine.utils;

import com.gitb.ModuleManager;
import com.gitb.engine.TestEngine;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.repository.ITestCaseRepository;
import com.gitb.tdl.TestArtifact;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.SchemaType;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by serbay on 10/10/14.
 */
public class ArtifactUtils {
	public static DataType resolveArtifact(TestCaseContext context, TestCaseScope scope, TestArtifact artifact) throws IOException {
		if(artifact == null) {
			return null;
		}
		ITestCaseRepository testCaseRepository = ModuleManager.getInstance().getTestCaseRepository();

		if(testCaseRepository == null || !testCaseRepository.isTestArtifactAvailable(artifact.getValue())) {
			return null;
		}

		InputStream inputStream = testCaseRepository.getTestArtifact(artifact.getValue());

		DataType data = TemplateUtils.generateDataTypeFromTemplate(scope, inputStream, artifact.getType(), artifact.getEncoding());

        //set the location of the artifact if it is a schema type in order to resolve
        //the location of other artifacts imported by this one.
        if(data instanceof SchemaType) {
            ((SchemaType) data).setSchemaLocation(artifact.getValue());
        }

		return data;
	}
}
