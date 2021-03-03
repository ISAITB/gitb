package com.gitb.processing;

import com.gitb.core.ConfigurationParameters;
import com.gitb.core.ConfigurationType;
import com.gitb.core.Metadata;
import com.gitb.core.UsageEnumeration;
import com.gitb.ps.ProcessingModule;
import com.gitb.tr.TestResultType;
import com.gitb.types.BooleanType;
import com.gitb.types.ListType;
import com.gitb.types.StringType;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.MetaInfServices;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@MetaInfServices(IProcessingHandler.class)
public class RegExpProcessor extends AbstractProcessingHandler {

    private static final String OPERATION__CHECK = "check";
    private static final String OPERATION__COLLECT = "collect";
    private static final String INPUT__INPUT = "input";
    private static final String INPUT__EXPRESSION = "expression";
    private static final String OUTPUT__OUTPUT = "output";

    @Override
    public ProcessingModule getModuleDefinition() {
        ProcessingModule module = new ProcessingModule();
        module.setId("RegExpProcessor");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION__CHECK,
            List.of(
                    createParameter(INPUT__INPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The text to run the regular expression on."),
                    createParameter(INPUT__EXPRESSION, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The regular expression to use.")
            ),
            List.of(
                createParameter(OUTPUT__OUTPUT, "boolean", UsageEnumeration.R, ConfigurationType.SIMPLE, "Whether or not the provided text matches the regular expression.")
            )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION__COLLECT,
                List.of(
                        createParameter(INPUT__INPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The text to run the regular expression on."),
                        createParameter(INPUT__EXPRESSION, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The regular expression to use.")
                ),
                List.of(
                        createParameter(OUTPUT__OUTPUT, "list[string]", UsageEnumeration.R, ConfigurationType.SIMPLE, "A list of strings that were collected as matching groups.")
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
        if (!input.getData().containsKey(INPUT__INPUT)) {
            throw new IllegalArgumentException("The input for the regular expression is required");
        } else {
            inputText = (String) input.getData().get(INPUT__INPUT).toStringType().getValue();
        }
        Pattern expression;
        if (!input.getData().containsKey(INPUT__EXPRESSION)) {
            throw new IllegalArgumentException("The regular expression to apply is required");
        } else {
            expression = Pattern.compile((String) input.getData().get(INPUT__EXPRESSION).toStringType().getValue());
        }
        // Carry out operation
        ProcessingData data = new ProcessingData();
        if (OPERATION__CHECK.equalsIgnoreCase(operation)) {
            data.getData().put(OUTPUT__OUTPUT, new BooleanType(expression.matcher(inputText).matches()));
        } else if (OPERATION__COLLECT.equalsIgnoreCase(operation)) {
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
            data.getData().put(OUTPUT__OUTPUT, groups);
        } else {
            throw new IllegalArgumentException("Unknown operation [" + operation + "]");
        }
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

}
