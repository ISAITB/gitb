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
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.ProcessingModule;
import com.gitb.tr.TestResultType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.StringType;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@ProcessingHandler(name="VariableUtils")
public class VariableUtils extends AbstractProcessingHandler {

    private static final String OPERATION_TYPE = "type";
    private static final String OPERATION_EXISTS = "exists";
    private static final String INPUT_NAME = "name";
    private static final String OUTPUT_OUTPUT = "output";

    @Override
    public ProcessingModule createProcessingModule() {
        ProcessingModule module = new ProcessingModule();
        module.setId("VariableUtils");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION_TYPE,
            List.of(
                createParameter(INPUT_NAME, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The name of the variable to check.")
            ),
            List.of(
                createParameter(OUTPUT_OUTPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The variable's type.")
            )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION_EXISTS,
                List.of(
                    createParameter(INPUT_NAME, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The name of the variable to check.")
                ),
                List.of(
                    createParameter(OUTPUT_OUTPUT, "boolean", UsageEnumeration.R, ConfigurationType.SIMPLE, "Whether the variable is defined or not.")
                )
        ));
        return module;
    }

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        if (StringUtils.isBlank(operation)) {
            throw new IllegalArgumentException("No operation provided");
        }
        ProcessingData data = new ProcessingData();
        if (OPERATION_EXISTS.equalsIgnoreCase(operation)) {
            var variableResolver = new VariableResolver(getScope(session));
            var variableName = getRequiredInputForName(input, INPUT_NAME, StringType.class);
            var matchedVariable = variableResolver.resolveVariable("$"+variableName, true);
            data.getData().put(OUTPUT_OUTPUT, new BooleanType(matchedVariable.isPresent()));
        } else if (OPERATION_TYPE.equalsIgnoreCase(operation)) {
            var variableResolver = new VariableResolver(getScope(session));
            var variableName = getRequiredInputForName(input, INPUT_NAME, StringType.class);
            var matchedVariable = variableResolver.resolveVariable("$"+variableName, true);
            data.getData().put(OUTPUT_OUTPUT, new StringType(matchedVariable.map(DataType::getType).orElse("")));
        } else {
            throw new IllegalArgumentException("Unknown operation [" + operation + "]");
        }
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

}
