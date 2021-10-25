package com.gitb.vs.tdl.rules.testcase;

import com.gitb.core.Configuration;
import com.gitb.tdl.Process;
import com.gitb.tdl.*;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.util.Utils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class CheckHandlers extends AbstractTestCaseObserver {

    private Map<String, String> messagingTxToHandler;
    private Map<String, String> processingTxToHandler;

    @Override
    public void initialiseTestCase(TestCase currentTestCase) {
        super.initialiseTestCase(currentTestCase);
        messagingTxToHandler = new HashMap<>();
        processingTxToHandler = new HashMap<>();
    }

    @Override
    public void handleStep(Object stepObj) {
        super.handleStep(stepObj);
        if (stepObj instanceof BeginTransaction) {
            String handler = ((BeginTransaction) stepObj).getHandler();
            if (!Utils.isVariableExpression(handler) && !Utils.isURL(handler)) {
                if (checkHandlerReference(handler, context.getExternalConfiguration().getEmbeddedMessagingHandlers().keySet(), ErrorCode.INVALID_EMBEDDED_MESSAGING_HANDLER_REFERENCE)) {
                    // Check configs.
                    checkConfigs(
                            ((BeginTransaction)stepObj).getConfig(),
                            context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getRequiredTxConfigs(),
                            context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getOptionalTxConfigs(),
                            handler
                    );
                    messagingTxToHandler.put(((BeginTransaction) stepObj).getTxnId(), handler);
                }
            }
        } else if (stepObj instanceof EndTransaction) {
            messagingTxToHandler.remove(((EndTransaction) stepObj).getTxnId());
        } else if (stepObj instanceof Send) {
            String handler = messagingTxToHandler.get(((Send) stepObj).getTxnId());
            if (handler != null) {
                // Check inputs and configs.
                checkConfigs(
                        ((Send) stepObj).getConfig(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getRequiredSendConfigs(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getOptionalSendConfigs(),
                        handler
                );
                checkInputs(
                        ((Send) stepObj).getInput(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getRequiredInputs(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getOptionalInputs(),
                        handler
                );
            }
        } else if (stepObj instanceof ReceiveOrListen) {
            String handler = messagingTxToHandler.get(((ReceiveOrListen) stepObj).getTxnId());
            if (handler != null) {
                // Check inputs and configs.
                checkConfigs(
                        ((ReceiveOrListen) stepObj).getConfig(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getRequiredReceiveConfigs(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getOptionalReceiveConfigs(),
                        handler
                );
                checkInputs(
                        ((ReceiveOrListen) stepObj).getInput(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getRequiredInputs(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getOptionalInputs(),
                        handler
                );
            }
        } else if (stepObj instanceof BeginProcessingTransaction) {
            String handler = ((BeginProcessingTransaction) stepObj).getHandler();
            if (!Utils.isVariableExpression(handler) && !Utils.isURL(handler)) {
                if (checkHandlerReference(handler, context.getExternalConfiguration().getEmbeddedProcessingHandlers().keySet(), ErrorCode.INVALID_EMBEDDED_PROCESSING_HANDLER_REFERENCE)) {
                    // Check configs.
                    checkConfigs(
                            ((BeginProcessingTransaction)stepObj).getConfig(),
                            context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getRequiredConfigs(),
                            context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getOptionalConfigs(),
                            handler
                    );
                    processingTxToHandler.put(((BeginProcessingTransaction)stepObj).getTxnId(), handler);
                }
            }
        } else if (stepObj instanceof Process) {
            String handler;
            if (((Process) stepObj).getTxnId() != null) {
                handler = processingTxToHandler.get(((Process) stepObj).getTxnId());
                if (!StringUtils.isBlank(((Process) stepObj).getHandler())) {
                    addReportItem(ErrorCode.DOUBLE_PROCESSING_HANDLER, currentTestCase.getId(), ((Process) stepObj).getTxnId(), ((Process) stepObj).getHandler());
                }
            } else {
                handler = ((Process) stepObj).getHandler();
                if (Utils.isVariableExpression(handler) || Utils.isURL(handler)) {
                    // Remote handlers (or resolved ones) should not be checked - set to null to skip.
                    handler = null;
                } else {
                    if (!checkHandlerReference(handler, context.getExternalConfiguration().getEmbeddedProcessingHandlers().keySet(), ErrorCode.INVALID_EMBEDDED_PROCESSING_HANDLER_REFERENCE)) {
                        handler = null;
                    }
                }
            }
            if (((Process) stepObj).getOperation() != null && ((Process) stepObj).getOperationAttribute() != null) {
                addReportItem(ErrorCode.DOUBLE_PROCESSING_OPERATION, currentTestCase.getId(), ((Process) stepObj).getOperationAttribute(), ((Process) stepObj).getOperation());
            }
            if (!((Process) stepObj).getInput().isEmpty() && ((Process) stepObj).getInputAttribute() != null) {
                addReportItem(ErrorCode.DOUBLE_PROCESSING_INPUTS, currentTestCase.getId(), ((Process) stepObj).getOperationAttribute());
            }
            if (handler != null) {
                String operation = ((Process) stepObj).getOperation();
                if (operation == null) {
                    operation = ((Process) stepObj).getOperationAttribute();
                }
                if (operation != null) {
                    // Check operation-specific config.
                    if (context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getOperations().containsKey(operation)) {
                        checkInputs(
                                getInputs((Process) stepObj),
                                context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getOperations().get(operation).getRequiredInputs(),
                                context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getOperations().get(operation).getOptionalInputs(),
                                handler
                        );
                    } else {
                        addReportItem(ErrorCode.INVALID_PROCESSING_HANDLER_OPERATION, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), ((Process) stepObj).getOperation());
                    }
                } else {
                    // Check default config.
                    checkInputs(
                            getInputs((Process) stepObj),
                            context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getDefaultRequiredInputs(),
                            context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getDefaultOptionalInputs(),
                            handler
                    );
                }
            }
        } else if (stepObj instanceof EndProcessingTransaction) {
            processingTxToHandler.remove(((EndProcessingTransaction)stepObj).getTxnId());
        } else if (stepObj instanceof Verify) {
            String handler = ((Verify) stepObj).getHandler();
            if (!Utils.isVariableExpression(handler) && !Utils.isURL(handler)) {
                if (checkHandlerReference(handler, context.getExternalConfiguration().getEmbeddedValidationHandlers().keySet(), ErrorCode.INVALID_EMBEDDED_VALIDATION_HANDLER_REFERENCE)) {
                    // Check inputs and configs.
                    checkConfigs(
                            ((Verify) stepObj).getConfig(),
                            context.getExternalConfiguration().getEmbeddedValidationHandlers().get(handler).getRequiredConfigs(),
                            context.getExternalConfiguration().getEmbeddedValidationHandlers().get(handler).getOptionalConfigs(),
                            handler
                    );
                    checkInputs(
                            ((Verify) stepObj).getInput(),
                            context.getExternalConfiguration().getEmbeddedValidationHandlers().get(handler).getRequiredInputs(),
                            context.getExternalConfiguration().getEmbeddedValidationHandlers().get(handler).getOptionalInputs(),
                            handler
                    );
                }
            }
        }
    }

    private List<Binding> getInputs(Process step) {
        if (!step.getInput().isEmpty()) {
            return step.getInput();
        } else if (step.getInputAttribute() != null) {
            var binding = new Binding();
            binding.setValue(step.getInputAttribute());
            return List.of(binding);
        } else {
            return null;
        }
    }

    private void checkConfigs(List<Configuration> configs, Set<String> expectedRequiredConfigs, Set<String> expectedOptionalConfigs, String handlerName) {
        if (configs != null) {
            Set<String> remainingRequiredConfigs = new HashSet<>(expectedRequiredConfigs);
            for (Configuration config: configs) {
                if (!expectedRequiredConfigs.contains(config.getName()) && !expectedOptionalConfigs.contains(config.getName())) {
                    addReportItem(ErrorCode.UNEXPECTED_HANDLER_CONFIG, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), handlerName, config.getName());
                } else {
                    remainingRequiredConfigs.remove(config.getName());
                }
            }
            for (String remainingRequiredConfig: remainingRequiredConfigs) {
                addReportItem(ErrorCode.MISSING_HANDLER_CONFIG, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), handlerName, remainingRequiredConfig);
            }
        }
    }

    private void checkInputs(List<Binding> inputs, Set<String> expectedRequiredInputs, Set<String> expectedOptionalInputs, String handlerName) {
        if (inputs != null) {
            var namedInputCount = inputs.stream().filter(input -> input.getName() != null).count();
            if (namedInputCount == 0) {
                // Inputs by sequence.
                if (inputs.size() > expectedRequiredInputs.size() + expectedOptionalInputs.size()) {
                    addReportItem(ErrorCode.UNEXPECTED_HANDLER_UNNAMED_INPUTS, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), handlerName);
                } else if (inputs.size() < expectedRequiredInputs.size()) {
                    addReportItem(ErrorCode.MISSING_HANDLER_UNNAMED_INPUTS, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), handlerName);
                }
            } else if (namedInputCount < inputs.size()) {
                addReportItem(ErrorCode.NAMED_AND_UNNAMED_HANDLER_INPUT, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), handlerName);
            } else {
                // Inputs by name.
                Set<String> remainingRequiredInputs = new HashSet<>(expectedRequiredInputs);
                for (Binding input: inputs) {
                    if (input.getName() != null) {
                        if (!expectedRequiredInputs.contains(input.getName()) && !expectedOptionalInputs.contains(input.getName())) {
                            addReportItem(ErrorCode.UNEXPECTED_HANDLER_INPUT, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), handlerName, input.getName());
                        } else {
                            remainingRequiredInputs.remove(input.getName());
                        }
                    }
                }
                for (String remainingRequiredInput: remainingRequiredInputs) {
                    addReportItem(ErrorCode.MISSING_HANDLER_INPUT, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), handlerName, remainingRequiredInput);
                }
            }
        }
    }

    private boolean checkHandlerReference(String handler, Set<String> embeddedHandlers, ErrorCode errorCode) {
        // This is a fixed string (not a URL and not a variable expression). This must be an embedded handler.
        if (!embeddedHandlers.contains(handler)) {
            addReportItem(errorCode, currentTestCase.getId(), handler);
            return false;
        }
        return true;
    }

}
