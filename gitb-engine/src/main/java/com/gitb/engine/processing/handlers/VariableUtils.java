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

    private static final String OPERATION__TYPE = "type";
    private static final String OPERATION__EXISTS = "exists";
    private static final String INPUT__NAME = "name";
    private static final String OUTPUT__OUTPUT = "output";

    @Override
    public ProcessingModule getModuleDefinition() {
        ProcessingModule module = new ProcessingModule();
        module.setId("VariableUtils");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION__TYPE,
            List.of(
                createParameter(INPUT__NAME, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The name of the variable to check.")
            ),
            List.of(
                createParameter(OUTPUT__OUTPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The variable's type.")
            )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION__EXISTS,
                List.of(
                    createParameter(INPUT__NAME, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The name of the variable to check.")
                ),
                List.of(
                    createParameter(OUTPUT__OUTPUT, "boolean", UsageEnumeration.R, ConfigurationType.SIMPLE, "Whether the variable is defined or not.")
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
        if (OPERATION__EXISTS.equalsIgnoreCase(operation)) {
            var variableResolver = new VariableResolver(scope);
            var variableName = getRequiredInputForName(input, INPUT__NAME, StringType.class);
            var matchedVariable = variableResolver.resolveVariable("$"+variableName, true);
            data.getData().put(OUTPUT__OUTPUT, new BooleanType(matchedVariable.isPresent()));
        } else if (OPERATION__TYPE.equalsIgnoreCase(operation)) {
            var variableResolver = new VariableResolver(scope);
            var variableName = getRequiredInputForName(input, INPUT__NAME, StringType.class);
            var matchedVariable = variableResolver.resolveVariable("$"+variableName, true);
            data.getData().put(OUTPUT__OUTPUT, new StringType(matchedVariable.map(DataType::getType).orElse("")));
        } else {
            throw new IllegalArgumentException("Unknown operation [" + operation + "]");
        }
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

}
