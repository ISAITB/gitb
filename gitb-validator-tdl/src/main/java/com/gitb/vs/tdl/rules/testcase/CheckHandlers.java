package com.gitb.vs.tdl.rules.testcase;

import com.gitb.core.Configuration;
import com.gitb.tdl.*;
import com.gitb.tdl.Process;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ExternalConfiguration;
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
        if (stepObj instanceof BeginTransaction transactionStep) {
            String handler = transactionStep.getHandler();
            if (checkHandlerBeforeInputValidations(handler, context.getExternalConfiguration().getEmbeddedMessagingHandlers().keySet(), ErrorCode.INVALID_EMBEDDED_MESSAGING_HANDLER_REFERENCE)) {
                // Check deprecation.
                checkDeprecation(handler, context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler));
                // Check configs.
                checkConfigs(
                        transactionStep.getConfig(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getRequiredTxConfigs(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getOptionalTxConfigs(),
                        handler
                );
                messagingTxToHandler.put(transactionStep.getTxnId(), handler);
            }
        } else if (stepObj instanceof EndTransaction transactionStep) {
            messagingTxToHandler.remove(transactionStep.getTxnId());
        } else if (stepObj instanceof Send messagingStep) {
            String handler;
            // Checking that the handler is defined once and is correct is none in CheckTransactions.
            if (messagingStep.getTxnId() != null) {
                handler = messagingTxToHandler.get(messagingStep.getTxnId());
            } else {
                handler = messagingStep.getHandler();
            }
            if (checkHandlerBeforeInputValidations(handler, context.getExternalConfiguration().getEmbeddedMessagingHandlers().keySet(), ErrorCode.INVALID_EMBEDDED_MESSAGING_HANDLER_REFERENCE)) {
                // Check deprecation.
                checkDeprecation(handler, context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler));
                // Check inputs and configs.
                checkConfigs(
                        messagingStep.getConfig(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getRequiredSendConfigs(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getOptionalSendConfigs(),
                        handler
                );
                Set<String> requiredInputs = new HashSet<>(context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getRequiredInputs());
                requiredInputs.addAll(context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getRequiredSendInputs());
                Set<String> optionalInputs = new HashSet<>(context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getOptionalInputs());
                optionalInputs.addAll(context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getOptionalSendInputs());
                checkInputs(
                        messagingStep.getInput(),
                        requiredInputs,
                        optionalInputs,
                        handler
                );
            }
        } else if (stepObj instanceof ReceiveOrListen messagingStep) {
            String handler;
            // Checking that the handler is defined once and is correct is none in CheckTransactions.
            if (messagingStep.getTxnId() != null) {
                handler = messagingTxToHandler.get(messagingStep.getTxnId());
            } else {
                handler = messagingStep.getHandler();
            }
            if (checkHandlerBeforeInputValidations(handler, context.getExternalConfiguration().getEmbeddedMessagingHandlers().keySet(), ErrorCode.INVALID_EMBEDDED_MESSAGING_HANDLER_REFERENCE)) {
                // Check deprecation.
                checkDeprecation(handler, context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler));
                // Check inputs and configs.
                checkConfigs(
                        messagingStep.getConfig(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getRequiredReceiveConfigs(),
                        context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getOptionalReceiveConfigs(),
                        handler
                );
                Set<String> requiredInputs = new HashSet<>(context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getRequiredInputs());
                requiredInputs.addAll(context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getRequiredReceiveInputs());
                Set<String> optionalInputs = new HashSet<>(context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getOptionalInputs());
                optionalInputs.addAll(context.getExternalConfiguration().getEmbeddedMessagingHandlers().get(handler).getOptionalReceiveInputs());
                checkInputs(
                        messagingStep.getInput(),
                        requiredInputs,
                        optionalInputs,
                        handler
                );
            }
        } else if (stepObj instanceof BeginProcessingTransaction transactionStep) {
            String handler = transactionStep.getHandler();
            if (checkHandlerBeforeInputValidations(handler, context.getExternalConfiguration().getEmbeddedProcessingHandlers().keySet(), ErrorCode.INVALID_EMBEDDED_PROCESSING_HANDLER_REFERENCE)) {
                // Check deprecation.
                checkDeprecation(handler, context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler));
                // Check configs.
                checkConfigs(
                        transactionStep.getConfig(),
                        context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getRequiredConfigs(),
                        context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getOptionalConfigs(),
                        handler
                );
                processingTxToHandler.put(transactionStep.getTxnId(), handler);
            }
        } else if (stepObj instanceof Process processStep) {
            if (processStep.getOperation() != null && processStep.getOperationAttribute() != null) {
                addReportItem(ErrorCode.DOUBLE_PROCESSING_OPERATION, currentTestCase.getId(), processStep.getOperationAttribute(), processStep.getOperation());
            }
            if (!processStep.getInput().isEmpty() && processStep.getInputAttribute() != null) {
                addReportItem(ErrorCode.DOUBLE_PROCESSING_INPUTS, currentTestCase.getId(), processStep.getOperationAttribute());
            }
            String handler;
            if (processStep.getTxnId() != null && processingTxToHandler.containsKey(processStep.getTxnId())) {
                handler = processingTxToHandler.get(processStep.getTxnId());
            } else {
                handler = processStep.getHandler();
            }
            if (checkHandlerBeforeInputValidations(handler, context.getExternalConfiguration().getEmbeddedProcessingHandlers().keySet(), ErrorCode.INVALID_EMBEDDED_PROCESSING_HANDLER_REFERENCE)) {
                // Check deprecation.
                checkDeprecation(handler, context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler));
                // Check operation.
                String operation = processStep.getOperation();
                if (operation == null) {
                    operation = processStep.getOperationAttribute();
                }
                if (operation == null) {
                    if (context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getOperations().size() > 1) {
                        addReportItem(ErrorCode.MISSING_PROCESSING_OPERATION, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), handler, StringUtils.join(context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getOperations().keySet(), ','));
                    } else if (!context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getOperations().isEmpty()) {
                        var firstOperation = context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getOperations().keySet().stream().findFirst();
                        if (firstOperation.isPresent()) {
                            operation = firstOperation.get();
                        }
                    }
                }
                if (operation != null) {
                    // Check operation-specific config.
                    if (context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getOperations().containsKey(operation)) {
                        checkInputs(
                                getInputs(processStep),
                                context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getOperations().get(operation).getRequiredInputs(),
                                context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getOperations().get(operation).getOptionalInputs(),
                                handler
                        );
                    } else {
                        addReportItem(ErrorCode.INVALID_PROCESSING_HANDLER_OPERATION, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), operation, StringUtils.join(context.getExternalConfiguration().getEmbeddedProcessingHandlers().get(handler).getOperations().keySet(), ','));
                    }
                }
            }
        } else if (stepObj instanceof EndProcessingTransaction) {
            processingTxToHandler.remove(((EndProcessingTransaction)stepObj).getTxnId());
        } else if (stepObj instanceof Verify verifyStep) {
            String handler = verifyStep.getHandler();
            if (checkHandlerBeforeInputValidations(handler, context.getExternalConfiguration().getEmbeddedValidationHandlers().keySet(), ErrorCode.INVALID_EMBEDDED_VALIDATION_HANDLER_REFERENCE)) {
                // Check deprecation.
                checkDeprecation(handler, context.getExternalConfiguration().getEmbeddedValidationHandlers().get(handler));
                // Check inputs and configs.
                checkConfigs(
                        verifyStep.getConfig(),
                        context.getExternalConfiguration().getEmbeddedValidationHandlers().get(handler).getRequiredConfigs(),
                        context.getExternalConfiguration().getEmbeddedValidationHandlers().get(handler).getOptionalConfigs(),
                        handler
                );
                checkInputs(
                        verifyStep.getInput(),
                        context.getExternalConfiguration().getEmbeddedValidationHandlers().get(handler).getRequiredInputs(),
                        context.getExternalConfiguration().getEmbeddedValidationHandlers().get(handler).getOptionalInputs(),
                        handler
                );
            }
        } else if (stepObj instanceof UserInteraction interactStep) {
            String handler = interactStep.getHandler();
            if (handler != null && !Utils.isVariableExpression(handler) && !Utils.isURL(handler)) {
                addReportItem(ErrorCode.INTERACTION_WITH_NON_CUSTOM_HANDLER, currentTestCase.getId());
            }
        }
    }

    private boolean checkHandlerBeforeInputValidations(String handler, Set<String> acceptableHandlers, ErrorCode errorIfInvalid) {
        return handler != null
                && !Utils.isVariableExpression(handler)
                && !Utils.isURL(handler)
                && checkHandlerReference(handler, acceptableHandlers, errorIfInvalid);
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

    private void checkConfigs(Collection<Configuration> configs, Set<String> expectedRequiredConfigs, Set<String> expectedOptionalConfigs, String handlerName) {
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

    private void checkDeprecation(String handlerName, ExternalConfiguration.BaseHandlerConfig config) {
        if (config.isDeprecated()) {
            if (config.getReplacement() == null) {
                addReportItem(ErrorCode.DEPRECATED_HANDLER, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), handlerName);
            } else {
                addReportItem(ErrorCode.DEPRECATED_HANDLER_WITH_REPLACEMENT, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), handlerName, config.getReplacement());
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
