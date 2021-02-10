package com.gitb.vs.tdl.rules.testcase;

import com.gitb.core.Configuration;
import com.gitb.tdl.*;
import com.gitb.tdl.Process;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.util.Utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public class CheckUniqueSetsOfValues extends AbstractTestCaseObserver {

    @Override
    public void handleStep(Object stepObj) {
        super.handleStep(stepObj);
        if (stepObj instanceof BeginTransaction) {
            validateForUniqueValues(fromConfigs(((BeginTransaction) stepObj).getConfig()), ErrorCode.DUPLICATE_CONFIG);
        } else if (stepObj instanceof MessagingStep) {
            validateForUniqueValues(fromConfigs(((MessagingStep) stepObj).getConfig()), ErrorCode.DUPLICATE_CONFIG);
            validateForUniqueValues(fromBindings(((MessagingStep) stepObj).getInput()), ErrorCode.DUPLICATE_INPUT);
        } else if (stepObj instanceof BeginProcessingTransaction) {
            validateForUniqueValues(fromConfigs(((BeginProcessingTransaction) stepObj).getConfig()), ErrorCode.DUPLICATE_CONFIG);
        } else if (stepObj instanceof Process) {
            validateForUniqueValues(fromBindings(((Process) stepObj).getInput()), ErrorCode.DUPLICATE_INPUT);
        } else if (stepObj instanceof Verify) {
            validateForUniqueValues(fromConfigs(((Verify) stepObj).getConfig()), ErrorCode.DUPLICATE_CONFIG);
            validateForUniqueValues(fromBindings(((Verify) stepObj).getInput()), ErrorCode.DUPLICATE_INPUT);
        } else if (stepObj instanceof CallStep) {
            validateForUniqueValues(fromBindings(((CallStep) stepObj).getInput()), ErrorCode.DUPLICATE_INPUT);
            validateForUniqueValues(fromBindings(((CallStep) stepObj).getOutput()), ErrorCode.DUPLICATE_OUTPUT);
        }
    }

    private Collection<String> fromConfigs(Collection<Configuration> configs) {
        return configs.stream().map(Configuration::getName).collect(Collectors.toList());
    }

    private Collection<String> fromBindings(Collection<Binding> bindings) {
        return bindings.stream().map(Binding::getName).collect(Collectors.toList());
    }

    public void validateForUniqueValues(Collection<String> values, ErrorCode errorCode) {
        if (values != null) {
            var uniqueValues = new HashSet<String>();
            var duplicates = new LinkedHashSet<String>();
            for (var value: values) {
                if (value != null) {
                    if (uniqueValues.contains(value)) {
                        duplicates.add(value);
                    } else {
                        uniqueValues.add(value);
                    }
                }
            }
            for (var duplicate: duplicates) {
                addReportItem(errorCode, currentTestCase.getId(), Utils.getStepName(currentStep), duplicate);
            }
        }
    }

}
