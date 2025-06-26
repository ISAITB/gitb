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

import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.types.BinaryType;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gitb.engine.expr.resolvers.VariableResolver.VARIABLE_EXPRESSION_NO_DOLLAR;

/**
 * Created by serbay on 10/13/14.
 */
public class TemplateUtils {
	private static final Pattern placeholderPattern = Pattern.compile("\\$\\{("+ VARIABLE_EXPRESSION_NO_DOLLAR +")\\}");

	private static DataType generateDataTypeFromTemplate(TestCaseScope scope, byte[] templateBytes, String type, String encoding, boolean forceBinaryTemplateProcessing) {
		if (type == null) {
			type = DataType.STRING_DATA_TYPE;
		}
		if (type.equals(DataType.BINARY_DATA_TYPE) && !forceBinaryTemplateProcessing) {
			// return immediately if dealing with binary data
			BinaryType binary = new BinaryType();
			binary.setValue(templateBytes);
			return binary;
		}

		String str = new String(templateBytes);

		Matcher matcher = placeholderPattern.matcher(str);

		StringBuilder stringBuffer = new StringBuilder();

		VariableResolver variableResolver = new VariableResolver(scope);
		if (encoding == null) {
			encoding = "UTF-8";
		}
		while(matcher.find()) {
			String variableExpression = matcher.group(1);
			DataType value = variableResolver.resolveVariable("$"+variableExpression);
			String replacement;
			if (value == null) {
				replacement = "";
			} else {
				var asStringType = value.convertTo(DataType.STRING_DATA_TYPE);
				replacement = (String) asStringType.getValue();
			}
			matcher.appendReplacement(stringBuffer, replacement);
		}
		matcher.appendTail(stringBuffer);
		return DataTypeFactory.getInstance().create(stringBuffer.toString().getBytes(), type, encoding);
	}

	public static DataType generateDataTypeFromTemplate(TestCaseScope scope, DataType templateVariable, String type, boolean forceBinaryTemplateProcessing) {
		var result = generateDataTypeFromTemplate(scope, templateVariable.serializeByDefaultEncoding(), type, null, forceBinaryTemplateProcessing);
		result.setImportPath(templateVariable.getImportPath());
		result.setImportTestSuite(templateVariable.getImportTestSuite());
		return result;
	}

	public static DataType generateDataTypeFromTemplate(TestCaseScope scope, DataType templateVariable, String type) {
		return generateDataTypeFromTemplate(scope, templateVariable, type, true);
	}

}
