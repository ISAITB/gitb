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
                            context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getOptionalTxConfigs()
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
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getOptionalSendConfigs()
                );
                checkInputs(
                        ((Send) stepObj).getInput(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getRequiredInputs(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getOptionalInputs()
                );
            }
        } else if (stepObj instanceof ReceiveOrListen) {
            String handler = messagingTxToHandler.get(((ReceiveOrListen) stepObj).getTxnId());
            if (handler != null) {
                // Check inputs and configs.
                checkConfigs(
                        ((ReceiveOrListen) stepObj).getConfig(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getRequiredReceiveConfigs(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getOptionalReceiveConfigs()
                );
                checkInputs(
                        ((ReceiveOrListen) stepObj).getInput(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getRequiredInputs(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getOptionalInputs()
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
                            context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getOptionalConfigs()
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
            if (handler != null) {
                String operation = ((Process) stepObj).getOperation();
                if (operation != null) {
                    // Check operation-specific config.
                    if (context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getOperations().containsKey(operation)) {
                        checkInputs(
                                ((Process) stepObj).getInput(),
                                context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getOperations().get(operation).getRequiredInputs(),
                                context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getOperations().get(operation).getOptionalInputs()
                        );
                    } else {
                        addReportItem(ErrorCode.INVALID_PROCESSING_HANDLER_OPERATION, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), ((Process) stepObj).getOperation());
                    }
                } else {
                    // Check default config.
                    checkInputs(
                            ((Process) stepObj).getInput(),
                            context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getDefaultRequiredInputs(),
                            context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getDefaultOptionalInputs()
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
                            context.getExternalConfiguration().getEmbeddedValidationHandlers().get(handler).getOptionalConfigs()
                    );
                    checkInputs(
                            ((Verify) stepObj).getInput(),
                            context.getExternalConfiguration().getEmbeddedValidationHandlers().get(handler).getRequiredInputs(),
                            context.getExternalConfiguration().getEmbeddedValidationHandlers().get(handler).getOptionalInputs()
                    );
                }
            }
        }
    }

    private void checkConfigs(List<Configuration> configs, Set<String> expectedRequiredConfigs, Set<String> expectedOptionalConfigs) {
        if (configs != null) {
            Set<String> remainingRequiredConfigs = new HashSet<>(expectedRequiredConfigs);
            for (Configuration config: configs) {
                if (!expectedRequiredConfigs.contains(config.getName()) && !expectedOptionalConfigs.contains(config.getName())) {
                    addReportItem(ErrorCode.UNEXPECTED_HANDLER_CONFIG, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), config.getName());
                } else {
                    remainingRequiredConfigs.remove(config.getName());
                }
            }
            for (String remainingRequiredConfig: remainingRequiredConfigs) {
                addReportItem(ErrorCode.MISSING_HANDLER_CONFIG, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), remainingRequiredConfig);
            }
        }
    }

    private void checkInputs(List<Binding> inputs, Set<String> expectedRequiredInputs, Set<String> expectedOptionalInputs) {
        if (inputs != null) {
            Set<String> remainingRequiredInputs = new HashSet<>(expectedRequiredInputs);
            for (Binding input: inputs) {
                if (!expectedRequiredInputs.contains(input.getName()) && !expectedOptionalInputs.contains(input.getName())) {
                    addReportItem(ErrorCode.UNEXPECTED_HANDLER_INPUT, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), input.getName());
                } else {
                    remainingRequiredInputs.remove(input.getName());
                }
            }
            for (String remainingRequiredInput: remainingRequiredInputs) {
                addReportItem(ErrorCode.MISSING_HANDLER_INPUT, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), remainingRequiredInput);
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
