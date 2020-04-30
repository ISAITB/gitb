package com.gitb.engine.utils;

import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.types.BinaryType;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gitb.engine.expr.resolvers.VariableResolver.VARIABLE_EXPRESSION__NO_DOLLAR;

/**
 * Created by serbay on 10/13/14.
 */
public class TemplateUtils {
	private static final Pattern placeholderPattern = Pattern.compile("\\$\\{("+VARIABLE_EXPRESSION__NO_DOLLAR+")\\}");

	public static DataType generateDataTypeFromTemplate(TestCaseScope scope, byte[] templateBytes, String type, String encoding, boolean forceBinaryTemplateProcessing) {
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

		StringBuffer stringBuffer = new StringBuffer();

		VariableResolver variableResolver = new VariableResolver(scope);
		if (encoding == null) {
			encoding = "UTF-8";
		}
		while(matcher.find()) {
			String variableExpression = matcher.group(1);
			DataType value = variableResolver.resolveVariable("$"+variableExpression);

			if (value == null) {
				throw new IllegalStateException("The expression ["+variableExpression+"] did not resolve an existing variable");
			}
			byte[] serializedVariable = value.serialize(encoding);
			String replacement = new String(serializedVariable);

			matcher.appendReplacement(stringBuffer, replacement);
		}
		matcher.appendTail(stringBuffer);
		return DataTypeFactory.getInstance().create(stringBuffer.toString().getBytes(), type, encoding);
	}

	public static DataType generateDataTypeFromTemplate(TestCaseScope scope, DataType templateVariable, String type) {
		return generateDataTypeFromTemplate(scope, templateVariable.serializeByDefaultEncoding(), type, null, true);
	}

	public static DataType generateDataTypeFromTemplate(TestCaseScope scope, InputStream template, String type, String encoding) throws IOException {
		return generateDataTypeFromTemplate(scope, IOUtils.toByteArray(template), type, encoding, false);
	}
}
