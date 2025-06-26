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
import com.gitb.engine.processing.ProcessingHandler;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.ProcessingModule;
import com.gitb.tr.TestResultType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.ListType;
import com.gitb.types.StringType;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ProcessingHandler(name="RegExpProcessor")
public class RegExpProcessor extends AbstractProcessingHandler {

    private static final String OPERATION_CHECK = "check";
    private static final String OPERATION_COLLECT = "collect";
    private static final String INPUT_INPUT = "input";
    private static final String INPUT_EXPRESSION = "expression";
    private static final String OUTPUT_OUTPUT = "output";

    @Override
    public ProcessingModule createProcessingModule() {
        ProcessingModule module = new ProcessingModule();
        module.setId("RegExpProcessor");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION_CHECK,
            List.of(
                    createParameter(INPUT_INPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The text to run the regular expression on."),
                    createParameter(INPUT_EXPRESSION, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The regular expression to use.")
            ),
            List.of(
                createParameter(OUTPUT_OUTPUT, "boolean", UsageEnumeration.R, ConfigurationType.SIMPLE, "Whether or not the provided text matches the regular expression.")
            )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION_COLLECT,
                List.of(
                        createParameter(INPUT_INPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The text to run the regular expression on."),
                        createParameter(INPUT_EXPRESSION, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The regular expression to use.")
                ),
                List.of(
                        createParameter(OUTPUT_OUTPUT, "list[string]", UsageEnumeration.R, ConfigurationType.SIMPLE, "A list of strings that were collected as matching groups.")
                )
        ));
        return module;
    }

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        if (StringUtils.isBlank(operation)) {
            throw new IllegalArgumentException("No operation provided");
        }
        // Collect inputs
        String inputText;
        if (!input.getData().containsKey(INPUT_INPUT)) {
            throw new IllegalArgumentException("The input for the regular expression is required");
        } else {
            inputText = (String) input.getData().get(INPUT_INPUT).convertTo(DataType.STRING_DATA_TYPE).getValue();
        }
        Pattern expression;
        if (!input.getData().containsKey(INPUT_EXPRESSION)) {
            throw new IllegalArgumentException("The regular expression to apply is required");
        } else {
            expression = Pattern.compile((String) input.getData().get(INPUT_EXPRESSION).convertTo(DataType.STRING_DATA_TYPE).getValue());
        }
        // Carry out operation
        ProcessingData data = new ProcessingData();
        if (OPERATION_CHECK.equalsIgnoreCase(operation)) {
            data.getData().put(OUTPUT_OUTPUT, new BooleanType(expression.matcher(inputText).matches()));
        } else if (OPERATION_COLLECT.equalsIgnoreCase(operation)) {
            ListType groups = new ListType("string");
            Matcher matcher = expression.matcher(inputText);
            int groupCount = matcher.groupCount();
            if (groupCount == 0) {
                throw new IllegalArgumentException("The regular expression must contain at least one capturing group");
            }
            while (matcher.find()) {
                for (int i=1; i <= groupCount; i++) {
                    groups.append(new StringType(matcher.group(i)));
                }
            }
            data.getData().put(OUTPUT_OUTPUT, groups);
        } else {
            throw new IllegalArgumentException("Unknown operation [" + operation + "]");
        }
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

}
