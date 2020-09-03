package com.gitb.vs.tdl.rules;

import com.gitb.core.Actor;
import com.gitb.core.Endpoint;
import com.gitb.core.EndpointParameter;
import com.gitb.tdl.TestSuite;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CheckTestSuiteEndpointParameters extends AbstractCheck {

    @Override
    public void doCheck(Context context, ValidationReport report) {
        TestSuite testSuite = context.getTestSuite();
        if (testSuite.getActors() != null && testSuite.getActors().getActor() != null) {
            for (Actor actor: testSuite.getActors().getActor()) {
                if (actor.getEndpoint() != null) {
                    Set<String> endpointNames = new HashSet<>();
                    for (Endpoint endpoint: actor.getEndpoint()) {
                        if (endpointNames.contains(endpoint.getName())) {
                            report.addItem(ErrorCode.DUPLICATE_ENDPOINT_NAME, getTestSuiteLocation(context), actor.getId(), endpoint.getName());
                        } else {
                            endpointNames.add(endpoint.getName());
                        }
                        if (endpoint.getConfig() != null) {
                            Set<String> parameterNames = new HashSet<>();
                            Map<String, Map<String, String>> parameterValueMap = new HashMap<>();
                            // First make a pass to check record the information on the parameters.
                            for (EndpointParameter parameter: endpoint.getConfig()) {
                                if (parameterNames.contains(parameter.getName())) {
                                    report.addItem(ErrorCode.DUPLICATE_PARAMETER_NAME, getTestSuiteLocation(context), actor.getId(), endpoint.getName(), parameter.getName());
                                } else {
                                    parameterNames.add(parameter.getName());
                                }
                                Map<String, String> allowedValues = validateAndGetAllowedValues(parameter, context, report);
                                if (allowedValues != null && !allowedValues.isEmpty()) {
                                    parameterValueMap.put(parameter.getName(), allowedValues);
                                }
                            }
                            for (EndpointParameter parameter: endpoint.getConfig()) {
                                if (parameter.getDependsOn() != null) {
                                    if (parameter.getDependsOn().equals(parameter.getName())) {
                                        report.addItem(ErrorCode.INVALID_PARAMETER_PREREQUISITE_SELF, getTestSuiteLocation(context), parameter.getName());
                                    } else {
                                        if (parameterNames.contains(parameter.getDependsOn())) {
                                            Map<String, String> otherValues = parameterValueMap.get(parameter.getDependsOn());
                                            if (otherValues != null && parameter.getDependsOnValue() != null && !otherValues.containsKey(parameter.getDependsOnValue())) {
                                                report.addItem(ErrorCode.PARAMETER_PREREQUISITE_VALUE_NOT_ALLOWED, getTestSuiteLocation(context), parameter.getName(), parameter.getDependsOn(), parameter.getDependsOnValue());
                                            }
                                        } else {
                                            report.addItem(ErrorCode.INVALID_PARAMETER_PREREQUISITE, getTestSuiteLocation(context), parameter.getName(), parameter.getDependsOn());
                                        }
                                        if (parameter.getDependsOnValue() == null || parameter.getDependsOnValue().isBlank()) {
                                            report.addItem(ErrorCode.PARAMETER_PREREQUISITE_WITHOUT_EXPECTED_VALUE, getTestSuiteLocation(context), parameter.getName(), parameter.getDependsOn());
                                        }
                                    }
                                } else if (parameter.getDependsOnValue() != null) {
                                    report.addItem(ErrorCode.PARAMETER_PREREQUISITE_VALUE_WITHOUT_PREREQUISITE, getTestSuiteLocation(context), parameter.getName());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Map<String, String> validateAndGetAllowedValues(EndpointParameter parameter, Context context, ValidationReport report) {
        Map<String, String> validatedValues = null;
        if (parameter.getAllowedValues() != null) {
            String[] values = StringUtils.split(parameter.getAllowedValues(), ',');
            String[] labels;
            if (parameter.getAllowedValueLabels() == null || parameter.getAllowedValueLabels().isBlank()) {
                labels = values;
            } else {
                labels = StringUtils.split(parameter.getAllowedValueLabels(), ',');
            }
            if (values.length != labels.length) {
                report.addItem(ErrorCode.PARAMETER_ALLOWED_VALUES_AND_LABELS_MISMATCH, getTestSuiteLocation(context), parameter.getName(), String.valueOf(values.length), String.valueOf(labels.length));
            } else {
                validatedValues = new HashMap<>();
                for (int i=0; i < values.length; i++) {
                    String value = values[i].trim();
                    String label = labels[i].trim();
                    if (validatedValues.containsKey(value)) {
                        report.addItem(ErrorCode.PARAMETER_ALLOWED_VALUES_DUPLICATE_VALUE, getTestSuiteLocation(context), parameter.getName(), value);
                    } else {
                        if (validatedValues.containsValue(label)) {
                            report.addItem(ErrorCode.PARAMETER_ALLOWED_VALUES_DUPLICATE_VALUE_LABEL, getTestSuiteLocation(context), parameter.getName(), label);
                        }
                        validatedValues.put(value, label);
                    }
                }
            }
        } else if (parameter.getAllowedValueLabels() != null) {
            report.addItem(ErrorCode.PARAMETER_ALLOWED_VALUE_LABELS_WITHOUT_VALUES, getTestSuiteLocation(context), parameter.getName());
        }
        return validatedValues;
    }

}
