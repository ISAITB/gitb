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

	public static DataType generateDataTypeFromTemplate(TestCaseScope scope, InputStream template, String type, String encoding) throws IOException {
		byte[] content = IOUtils.toByteArray(template);

        //return immediately if dealing with binary data
        if(type.equals(DataType.BINARY_DATA_TYPE)) {
            BinaryType binary = new BinaryType();
            binary.setValue(content);
            return binary;
        }

		String str = new String(content);

		Matcher matcher = placeholderPattern.matcher(str);

		StringBuffer stringBuffer = new StringBuffer();

		VariableResolver variableResolver = new VariableResolver(scope);

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

		DataType data = DataTypeFactory.getInstance().create(stringBuffer.toString().getBytes(), type, encoding);

		return data;
	}
}
