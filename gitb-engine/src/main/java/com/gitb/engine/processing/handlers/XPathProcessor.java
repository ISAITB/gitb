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

package com.gitb.engine.processing.handlers;

import com.gitb.core.ConfigurationParameters;
import com.gitb.core.ConfigurationType;
import com.gitb.core.Metadata;
import com.gitb.core.UsageEnumeration;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.processing.ProcessingHandler;
import com.gitb.engine.utils.HandlerUtils;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.ProcessingModule;
import com.gitb.tr.TestResultType;
import com.gitb.types.*;

import javax.xml.xpath.XPathExpression;
import java.util.List;

@ProcessingHandler(name="XPathProcessor")
public class XPathProcessor extends AbstractProcessingHandler {

    private static final String OPERATION_PROCESS = "process";
    private static final String INPUT_INPUT = "input";
    private static final String INPUT_EXPRESSION = "expression";
    private static final String INPUT_TYPE = "type";
    private static final String OUTPUT_OUTPUT = "output";

    @Override
    public ProcessingModule createProcessingModule() {
        ProcessingModule module = new ProcessingModule();
        module.setId("XPathProcessor");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION_PROCESS,
            List.of(
                    createParameter(INPUT_INPUT, "object", UsageEnumeration.R, ConfigurationType.SIMPLE, "The XML content to evaluate the expression on."),
                    createParameter(INPUT_EXPRESSION, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The XPath expression to evaluate."),
                    createParameter(INPUT_TYPE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The expected result type.")
            ),
            List.of(createParameter(OUTPUT_OUTPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The result after evaluating the expression."))
        ));
        return module;
    }

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        // Collect inputs
        ObjectType contentToProcess;
        if (!input.getData().containsKey(INPUT_INPUT)) {
            throw new IllegalArgumentException("The XML content to evaluate the expression on is required");
        } else {
            contentToProcess = getAndConvert(input.getData(), INPUT_INPUT, DataType.OBJECT_DATA_TYPE, ObjectType.class);
        }
        StringType expression;
        if (!input.getData().containsKey(INPUT_EXPRESSION)) {
            throw new IllegalArgumentException("The XPath expression is required");
        } else {
            expression = getAndConvert(input.getData(), INPUT_EXPRESSION, DataType.STRING_DATA_TYPE, StringType.class);
        }
        String resultType;
        if (!input.getData().containsKey(INPUT_TYPE)) {
            resultType = DataType.STRING_DATA_TYPE;
        } else {
            resultType = DataTypeFactory.getInstance().create(getAndConvert(input.getData(), INPUT_TYPE, DataType.STRING_DATA_TYPE, StringType.class).getValue()).getType();
        }
        MapType namespaces = (MapType) input.getData().get(HandlerUtils.NAMESPACE_MAP_INPUT);
        // Compile expression
        XPathExpression xpath = HandlerUtils.compileXPathExpression(namespaces, expression, new VariableResolver(getScope(session)));
        // Process expression
        DataType result = contentToProcess.processXPath(xpath, resultType);
        // Return report
        ProcessingData data = new ProcessingData();
        data.getData().put(OUTPUT_OUTPUT, result);
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

}
