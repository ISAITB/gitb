/*
 * Copyright (C) 2026 European Union
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

package com.gitb.engine.actors.processors;

import com.gitb.PropertyConstants;
import com.gitb.core.*;
import com.gitb.engine.CallbackManager;
import com.gitb.engine.TestbedService;
import com.gitb.engine.commands.messaging.NotificationReceived;
import com.gitb.engine.commands.messaging.TimeoutExpired;
import com.gitb.engine.events.TestStepInputEventBus;
import com.gitb.engine.events.model.InputEvent;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.HandlerUtils;
import com.gitb.engine.utils.StepContext;
import com.gitb.engine.utils.TemplateUtils;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.DeferredMessagingReport;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.messaging.callback.SessionCallbackData;
import com.gitb.remote.HandlerTimeoutException;
import com.gitb.tbs.InputRequest;
import com.gitb.tbs.Instruction;
import com.gitb.tbs.UserInput;
import com.gitb.tbs.UserInteractionRequest;
import com.gitb.tdl.HandlerConfiguration;
import com.gitb.tdl.InstructionOrRequest;
import com.gitb.tdl.UserInteraction;
import com.gitb.tdl.UserRequest;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.*;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.ErrorUtils;
import com.gitb.utils.XMLDateTimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.springframework.util.MimeType;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.runtime.BoxedUnit;

import javax.xml.datatype.DatatypeConfigurationException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by tuncay on 9/24/14.
 * <p/>
 * User interaction step executor actor
 */
public class InteractionStepProcessorActor extends AbstractTestStepActor<UserInteraction> {

    public static final String NAME = "interaction-p";

    private static final Logger logger = LoggerFactory.getLogger(InteractionStepProcessorActor.class);
    private Promise<TestStepReportType> promise;

    public InteractionStepProcessorActor(UserInteraction step, TestCaseScope scope, String stepId, StepContext stepContext) {
        super(step, scope, stepId, stepContext);
    }

    @Override
    public void onReceive(Object message) {
        if (message instanceof NotificationReceived notificationMessage) {
            /*
             * This case occurs if we have an asynchronous response delivered by a messaging handler in case the
             * interaction step was set to be handled by a handler (rather than via the UI).
             */
            if (promise != null && !promise.isCompleted()) {
                if (notificationMessage.getError() != null) {
                    promise.tryFailure(notificationMessage.getError());
                } else {
                    logger.debug(addMarker(), "Received notification");
                    this.handleInputEvent(convertToInputEvent(notificationMessage.getReport()));
                }
            }
        } else if (message instanceof TimeoutExpired) {
            if (!promise.isCompleted()) {
                logger.debug(addMarker(), "Timeout expired while waiting to receive input");
                var inputEvent = new InputEvent(scope.getContext().getSessionId(), step.getId(), Collections.emptyList(), step.isAdmin());
                this.handleInputEvent(inputEvent);
            }
        } else {
            super.onReceive(message);
        }
    }

    @Override
    protected void init() {
        String classifier = TestStepInputEventBus.getClassifier(scope.getContext().getSessionId(), stepId);
        TestStepInputEventBus
                .getInstance()
                .subscribe(self(), classifier);

        promise = Futures.promise();

        promise.future().onComplete(result -> {
            if (result.isSuccess()) {
                completed(result.get());
            } else {
                handleFutureFailure(result.failed().get());
            }
            return BoxedUnit.UNIT;
        }, getContext().dispatcher());
    }

    @Override
    protected void completed(TestStepReportType testStepReport) {
        HandlerUtils.recordHandlerTimeout(step.getHandlerTimeoutFlag(), scope, false);
        super.completed(testStepReport);
    }

    @Override
    protected void handleFutureFailure(Throwable failure) {
        if (failure instanceof HandlerTimeoutException) {
            HandlerUtils.recordHandlerTimeout(step.getHandlerTimeoutFlag(), scope, true);
        }
        super.handleFutureFailure(failure);
    }

    private String fixedValueOrVariable(String value, VariableResolver variableResolver, String defaultValue) {
        if (VariableResolver.isVariableReference(value)) {
            value = variableResolver.resolveVariableAsString(value).getValue();
        }
        if (StringUtils.isBlank(value) && StringUtils.isNotBlank(defaultValue)) {
            value = defaultValue;
        }
        return value;
    }

    private void scheduleTimeout(ActorContext context, long timeoutMs) {
        context.system().scheduler().scheduleOnce(
                scala.concurrent.duration.Duration.apply(timeoutMs, TimeUnit.MILLISECONDS), () -> {
                    if (!self().isTerminated()) {
                        self().tell(new TimeoutExpired(), self());
                    }
                },
                context.dispatcher()
        );
    }

    @Override
    protected void start() {
        processing();
        //Process the instructions and request the interaction from TestbedClient
        // Set the type in case this is missing.
        // if the contentTYpe is set to BASE64 this will be a file.
        // If a target variable is referenced we can use this to determine the type.
        // Set "string" if no other type can be determined.
        // Set the default content type based on the type.
        //If it is an instruction
        // If no expression is specified consider it an empty expression.
        ExpressionHandler expressionHandler = new ExpressionHandler(scope);
        final ActorContext context = getContext();
        //If it is a request
        Future<TestStepReportType> future = Futures.future(() -> {
            // Add a timeout if this is configured for the step.
            long timeout = 0;
            if (!StringUtils.isBlank(step.getTimeout())) {
                if (VariableResolver.isVariableReference(step.getTimeout())) {
                    timeout = expressionHandler.getVariableResolver().resolveVariableAsNumber(step.getTimeout()).longValue();
                } else {
                    timeout = Double.valueOf(step.getTimeout()).longValue();
                }
                if (timeout > 0) {
                    scheduleTimeout(context, timeout);
                }
            }
            // Process the instructions and request the interaction from TestbedClient
            try {
                if (isHandlerEnabled(expressionHandler.getVariableResolver())) {
                    processAsHandlerInteraction(expressionHandler);
                } else {
                    processAsUserInterfaceInteraction(timeout, expressionHandler);
                }
                return null;
            } catch (Exception e) {
                logger.error(addMarker(), "Error in interaction step", e);
                throw new GITBEngineInternalError(e);
            }
        }, context.dispatcher());

        future.onComplete(result -> {
            if (result.isFailure()) {
                promise.tryFailure(result.failed().get());
            }
            return BoxedUnit.UNIT;
        }, context.dispatcher());
        waiting();
    }

    private void processAsHandlerInteraction(ExpressionHandler expressionHandler) {
        String handler = VariableResolver.isVariableReference(step.getHandler())?expressionHandler.getVariableResolver().resolveVariableAsString(step.getHandler()).toString():step.getHandler();
        MessagingContext messagingContext = scope.getContext().getMessagingContexts().stream()
                .filter(ctx -> handler.equals(ctx.getHandlerIdentifier()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to determine the handler messaging context for an interact step"));
        IMessagingHandler messagingHandler = messagingContext.getHandler();
        if (messagingHandler.isRemote()) {
            Message inputMessage = MessagingHandlerUtils.getMessageFromBindings(
                    messagingHandler,
                    Optional.ofNullable(step.getHandlerConfig()).map(HandlerConfiguration::getInput).orElseGet(Collections::emptyList),
                    new ExpressionHandler(scope)
            );
            String callId = UUID.randomUUID().toString();
            CallbackManager.getInstance().registerForNotification(self(), messagingContext.getSessionId(), callId);
            MessagingReport report = messagingHandler
                    .receiveMessage(
                            messagingContext.getSessionId(),
                            null, // No need for a transaction ID - it's never used by remote handlers.
                            callId,
                            createDummyReceiveStep(),
                            inputMessage,
                            messagingContext.getMessagingThreads()
                    );
            if (report instanceof DeferredMessagingReport deferredReport) {
                // This means that we should not resolve this step but rather wait for a message to be delivered to the actor.
                if (deferredReport.getCallbackData() != null) {
                    // Register the data needed to respond when receiving a call.
                    CallbackManager.getInstance().registerCallbackData(new SessionCallbackData(
                            messagingContext.getSessionId(),
                            callId,
                            ((MapType) scope.getVariable(PropertyConstants.SYSTEM_MAP).getValue()).getItem(PropertyConstants.SYSTEM_MAP_API_KEY).toString(),
                            deferredReport.getCallbackData())
                    );
                }
            } else {
                throw new IllegalStateException("Only custom messaging handlers can be used for interact steps");
            }
        } else {
            throw new IllegalStateException("Only custom messaging handlers can be used for interact steps");
        }
    }

    private boolean isRequired(UserRequest request, VariableResolver variableResolver) {
        return TestCaseUtils.resolveBooleanFlag(request.getRequired(), false, () -> variableResolver);
    }

    private InputEvent convertToInputEvent(MessagingReport report) {
        List<UserInput> userInputs = new ArrayList<>();
        if (report != null && report.getReport() != null && report.getReport().getContext() != null) {
            List<AnyContent> items = report.getReport().getContext().getItem();
            if (!items.isEmpty()) {
                List<Pair<String, UserRequest>> requestElementsToProcess = new ArrayList<>();
                int idIndex = 0;
                for (var interaction: step.getInstructOrRequest()) {
                    idIndex += 1;
                    if (interaction instanceof UserRequest request) {
                        if (request.getName() != null) {
                            var matchingItem = findFirstByName(request.getName(), items);
                            if (matchingItem != null) {
                                userInputs.add(toUserInput(matchingItem.getValue(), String.valueOf(idIndex)));
                                items.remove(matchingItem.getKey().intValue());
                            } else {
                                requestElementsToProcess.add(Pair.of(String.valueOf(idIndex), request));
                            }
                        } else {
                            requestElementsToProcess.add(Pair.of(String.valueOf(idIndex), request));
                        }
                    }
                }
                // At this point we have processed all named elements that were matched. Process the remaining ones by simple position matching.
                for (var requestInfo: requestElementsToProcess) {
                    if (!items.isEmpty()) {
                        userInputs.add(toUserInput(items.removeFirst(), requestInfo.getKey()));
                    }
                }
            }
        }
        return new InputEvent(scope.getContext().getSessionId(), step.getId(), userInputs, step.isAdmin());
    }

    private static UserInput toUserInput(AnyContent item, String idValue) {
        var userInput = new UserInput();
        userInput.setId(idValue);
        userInput.setEncoding(item.getEncoding());
        userInput.setName(item.getName());
        userInput.setValue(item.getValue());
        userInput.setType(item.getType());
        userInput.setEmbeddingMethod(item.getEmbeddingMethod());
        userInput.setForContext(item.isForContext());
        userInput.setForDisplay(item.isForDisplay());
        userInput.setMimeType(item.getMimeType());
        return userInput;
    }

    private Pair<Integer, AnyContent> findFirstByName(String name, List<AnyContent> items) {
        int i = -1;
        for (var item: items) {
            i += 1;
            if (name.equals(item.getName())) {
                return Pair.of(i, item);
            }
        }
        return null;
    }

    private com.gitb.tdl.Receive createDummyReceiveStep() {
        com.gitb.tdl.Receive receiveStep = new com.gitb.tdl.Receive();
        receiveStep.setId(step.getId());
        return receiveStep;
    }

    private boolean isHandlerEnabled(VariableResolver variableResolver) {
        if (StringUtils.isNotBlank(step.getHandler())) {
            return TestCaseUtils.resolveBooleanFlag(step.getHandlerEnabled(), false, () -> variableResolver);
        } else {
            return false;
        }
    }

    /**
     * An interaction element that is included in the interaction (see {@link #isIncluded}), paired with the ID
     * assigned to it on the wire (matching the existing, pre-dependencies, sequential numbering that skips
     * excluded elements). This ID is also what {@code dependsOn} references resolve to, and what {@link InputEvent}
     * answers are correlated against (see {@link #handleInputEvent}).
     */
    private record IncludedInteractionItem(InstructionOrRequest item, String id) {}

    /**
     * Determines the included elements of the interaction and assigns them their wire ID, preserving the existing
     * numbering scheme (sequential, 1-based, skipping excluded elements) so that this continues to line up with how
     * {@link InputEvent} answers are matched back to their originating element.
     */
    private List<IncludedInteractionItem> collectIncludedItems(List<InstructionOrRequest> instructionAndRequests, ExpressionHandler expressionHandler, Map<String, IncludedInteractionItem> includedItemsByName) {
        List<IncludedInteractionItem> includedItems = new ArrayList<>();
        int childStepId = 1;
        for (InstructionOrRequest instructionOrRequest : instructionAndRequests) {
            if (isIncluded(instructionOrRequest, expressionHandler)) {
                var includedItem = new IncludedInteractionItem(instructionOrRequest, String.valueOf(childStepId));
                includedItems.add(includedItem);
                if (instructionOrRequest instanceof UserRequest request && request.getName() != null) {
                    includedItemsByName.put(request.getName(), includedItem);
                }
                childStepId++;
            }
        }
        return includedItems;
    }

    private void normaliseTypeInformation(InstructionOrRequest instructionOrRequest, ExpressionHandler expressionHandler) {
        // Set the type in case this is missing.
        if (StringUtils.isBlank(instructionOrRequest.getType())) {
            if (instructionOrRequest.getContentType() == ValueEmbeddingEnumeration.BASE_64 || (instructionOrRequest instanceof UserRequest && ((UserRequest)instructionOrRequest).getInputType() == InputRequestInputType.UPLOAD)) {
                // if the contentType is set to BASE64 or the inputType is UPLOAD this will be a file.
                instructionOrRequest.setType(DataType.BINARY_DATA_TYPE);
            } else {
                if (VariableResolver.isVariableReference(instructionOrRequest.getValue())) {
                    // If a target variable is referenced we can use this to determine the type.
                    DataType targetVariable = expressionHandler.getVariableResolver().resolveVariable(instructionOrRequest.getValue());
                    if (targetVariable == null) {
                        throw new GITBEngineInternalError("No variable could be found based on expression [" + instructionOrRequest.getValue() + "]");
                    }
                    instructionOrRequest.setType(targetVariable.getType());
                } else {
                    // Set "string" if no other type can be determined.
                    instructionOrRequest.setType(DataType.STRING_DATA_TYPE);
                }
            }
        }
        // Ensure consistency and complete information for contentType and inputType.
        if (DataType.isFileType(instructionOrRequest.getType())) {
            instructionOrRequest.setContentType(ValueEmbeddingEnumeration.BASE_64);
            if (instructionOrRequest instanceof UserRequest) {
                ((UserRequest) instructionOrRequest).setInputType(InputRequestInputType.UPLOAD);
            }
        } else {
            instructionOrRequest.setContentType(ValueEmbeddingEnumeration.STRING);
            if (instructionOrRequest instanceof UserRequest request) {
                if (request.getInputType() == null || request.getInputType() == InputRequestInputType.UPLOAD) {
                    if (request.getOptions() != null) {
                        request.setInputType(InputRequestInputType.SELECT_SINGLE);
                    } else {
                        request.setInputType(InputRequestInputType.TEXT);
                    }
                }
            }
        }
    }

    /**
     * Resolves a {@code dependsOn}/{@code dependsOnValue} pair against the included sibling requests of the same
     * interaction, and applies the result (the sibling's wire ID and the resolved dependency value) via the given
     * setters. Dependencies on file uploads, or that reference a request that isn't included, are ignored (the
     * element is treated as unconditional) rather than failing the step.
     */
    private void applyDependency(Consumer<String> idSetter, Consumer<String> valueSetter, InstructionOrRequest source,
                                  Map<String, IncludedInteractionItem> includedItemsByName, ExpressionHandler expressionHandler) {
        if (source.getDependsOn() != null) {
            var target = includedItemsByName.get(source.getDependsOn());
            if (target == null) {
                logger.warn(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), "Ignoring dependency on interaction step element as the referenced request [{}] could not be resolved", source.getDependsOn());
            } else if (!(target.item() instanceof UserRequest targetRequest) || targetRequest.getInputType() != InputRequestInputType.UPLOAD) {
                idSetter.accept(target.id());
                valueSetter.accept(fixedValueOrVariable(source.getDependsOnValue(), expressionHandler.getVariableResolver(), null));
            }
        }
    }

    private void processAsUserInterfaceInteraction(long timeout, ExpressionHandler expressionHandler) {
        boolean hasInstructions = false;
        boolean hasRequests = false;
        List<InstructionOrRequest> instructionAndRequests = step.getInstructOrRequest();
        var withValue = fixedValueOrVariable(step.getWith(), expressionHandler.getVariableResolver(), getSUTActor().getId());
        // Prepare the message to send to the frontend.
        UserInteractionRequest userInteractionRequest = new UserInteractionRequest();
        userInteractionRequest.setInputTitle(fixedValueOrVariable(step.getInputTitle(), expressionHandler.getVariableResolver(), "User interaction"));
        userInteractionRequest.setWith(withValue);
        userInteractionRequest.setAdmin(step.isAdmin());
        userInteractionRequest.setDesc(step.getDesc());
        userInteractionRequest.setHasTimeout(timeout > 0);
        // First pass: determine the included elements and their wire IDs, and normalise their type information.
        // This needs to happen in full before dependency resolution below, since a dependency may reference a
        // sibling that appears later in the TDL declaration order.
        Map<String, IncludedInteractionItem> includedItemsByName = new HashMap<>();
        List<IncludedInteractionItem> includedItems = collectIncludedItems(instructionAndRequests, expressionHandler, includedItemsByName);
        includedItems.forEach(includedItem -> normaliseTypeInformation(includedItem.item(), expressionHandler));
        // Second pass: build the wire objects, resolving any input dependencies against the now fully-typed items.
        for (IncludedInteractionItem includedItem : includedItems) {
            InstructionOrRequest instructionOrRequest = includedItem.item();
            //If it is an instruction
            if (instructionOrRequest instanceof com.gitb.tdl.Instruction instruction) {
                hasInstructions = true;
                // If no expression is specified consider it an empty expression.
                if (StringUtils.isBlank(instruction.getValue())) {
                    instructionOrRequest.setValue("''");
                }
                Instruction wireInstruction = processInstruction(instruction, includedItem.id(), withValue, expressionHandler);
                applyDependency(wireInstruction::setDependsOn, wireInstruction::setDependsOnValue, instructionOrRequest, includedItemsByName, expressionHandler);
                userInteractionRequest.getInstructionOrRequest().add(wireInstruction);
            } else if (instructionOrRequest instanceof UserRequest request) { // If it is a request
                hasRequests = true;
                InputRequest wireRequest = processRequest(request, includedItem.id(), withValue, expressionHandler.getVariableResolver());
                applyDependency(wireRequest::setDependsOn, wireRequest::setDependsOnValue, instructionOrRequest, includedItemsByName, expressionHandler);
                userInteractionRequest.getInstructionOrRequest().add(wireRequest);
            } else {
                throw new IllegalStateException("Unsupported interaction type ["+instructionOrRequest+"]");
            }
        }
        logger.debug(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), String.format("Triggering user interaction - step [%s] - ID [%s]", TestCaseUtils.extractStepDescription(step, scope), stepId));
        TestbedService.interactWithUsers(scope.getContext().getSessionId(), stepId, userInteractionRequest);

        if (hasInstructions && !hasRequests && isNonBlocking(expressionHandler.getVariableResolver())) {
            // The step is a non-blocking interaction containing only instructions. Notify immediately for its completion.
            self().tell(new InputEvent(scope.getContext().getSessionId(), stepId, Collections.emptyList(), step.isAdmin()), self());
        }
    }

    private boolean isIncluded(InstructionOrRequest instructionOrRequest, ExpressionHandler expressionHandler) {
        if (instructionOrRequest != null && instructionOrRequest.getIncluded() != null) {
            if (VariableResolver.isVariableReference(instructionOrRequest.getIncluded())) {
                return expressionHandler.getVariableResolver().resolveVariableAsBoolean(instructionOrRequest.getIncluded()).getValue();
            } else {
                return Boolean.parseBoolean(instructionOrRequest.getIncluded());
            }
        }
        return true;
    }

    private boolean isNonBlocking(VariableResolver resolver) {
        boolean blocking;
        if (step.getBlocking() == null) {
          blocking = true;
        } else {
            if (VariableResolver.isVariableReference(step.getBlocking())) {
                blocking = resolver.resolveVariableAsBoolean(step.getBlocking()).getValue();
            } else {
                blocking = step.getBlocking() != null && Boolean.parseBoolean(step.getBlocking());
            }
        }
        return !blocking;
    }

    /**
     * Process TDL Instruction command and convert it to Instruction TBS request object
     *
     * @param instructionCommand command
     * @param stepId step id
     * @return instruction
     */
    private Instruction processInstruction(com.gitb.tdl.Instruction instructionCommand, String stepId, String withValue, ExpressionHandler expressionHandler) {
        Instruction instruction = new Instruction();
        instruction.setWith(withValue);
        instruction.setDesc(fixedValueOrVariable(instructionCommand.getDesc(), expressionHandler.getVariableResolver(), null));
        instruction.setId(stepId);
        instruction.setName(instructionCommand.getName());
        instruction.setEncoding(instructionCommand.getEncoding());
        instruction.setMimeType(fixedValueOrVariable(instructionCommand.getMimeType(), expressionHandler.getVariableResolver(), null));
        instruction.setForceDisplay(instructionCommand.isForceDisplay());
        instruction.setShowControls(instructionCommand.isShowControls());
        instruction.setLevel(getInstructionLevel(instructionCommand, expressionHandler));
        setInstructionValue(instruction, instructionCommand, expressionHandler);
        return instruction;
    }

    private InstructionLevel getInstructionLevel(com.gitb.tdl.Instruction instructionCommand, ExpressionHandler expressionHandler) {
        InstructionLevel result = null;
        if (instructionCommand.getLevel() != null) {
            String level;
            if (VariableResolver.isVariableReference(instructionCommand.getLevel())) {
                level = expressionHandler.getVariableResolver().resolveVariableAsString(instructionCommand.getLevel()).getValue();
            } else {
                level = instructionCommand.getLevel();
            }
            InstructionLevel levelToSet = null;
            try {
                levelToSet = InstructionLevel.fromValue(level);
            } catch (Exception e) {
                logger.warn(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), "Ignoring 'level' on interaction step instruction as it was invalid");
            }
            if (levelToSet != null && levelToSet != InstructionLevel.NONE) {
                result = levelToSet;
            }
        }
        return result;
    }

    private void setInstructionValue(AnyContent target, com.gitb.tdl.Instruction instructionCommand, ExpressionHandler expressionHandler) {
        DataType computedValue = expressionHandler.processExpression(instructionCommand, instructionCommand.getType());
        if (instructionCommand.isForceDisplay()) {
            computedValue = computedValue.convertTo(DataType.STRING_DATA_TYPE);
        }
        DataTypeUtils.setContentValueWithDataType(target, computedValue);
        if (instructionCommand.getMimeType() != null && instructionCommand.getMimeType().startsWith("text/html")) {
            target.setMimeType("text/html");
            addMetadataToken(target, "sanitized", "true");
            target.setValue(TestCaseUtils.sanitizeInstructionStepValue(target.getValue()));
        }
    }

    private boolean validMimeType(String value) {
        try {
            MimeType.valueOf(value);
            return true;
        } catch (Exception e) {
            logger.warn(addMarker(), "Ignored invalid content type [{}]", value);
            return false;
        }
    }

    /**
     * Process TDL InputRequest command and convert it to TBS InputRequest object
     *
     * @param request request
     * @param stepId step id
     * @return input request
     */
    private InputRequest processRequest(UserRequest request, String stepId, String withValue, VariableResolver variableResolver) {
        InputRequest inputRequest = new InputRequest();
        inputRequest.setWith(withValue);
        inputRequest.setDesc(fixedValueOrVariable(request.getDesc(), variableResolver, null));
        inputRequest.setName(request.getValue()); //name is provided from value node
        inputRequest.setContentType(request.getContentType());
        inputRequest.setType(request.getType());
        inputRequest.setEncoding(request.getEncoding());
        inputRequest.setId(stepId);
        inputRequest.setInputType(request.getInputType());
        inputRequest.setMimeType(fixedValueOrVariable(request.getMimeType(), variableResolver, null));
        inputRequest.setRequired(TestCaseUtils.resolveBooleanFlag(request.getRequired(), false, () -> variableResolver));
        if (request.getInputType() == InputRequestInputType.UPLOAD) {
            // Handle uploads.
            if (request.getAccept() != null) {
                // Parse, calculate and validate accepted mime types.
                String acceptValues;
                if (VariableResolver.isVariableReference(request.getAccept())) {
                    acceptValues = resolveTokenValues(variableResolver, request.getAccept(), this::validMimeType);
                } else {
                    acceptValues = Arrays.stream(StringUtils.split(request.getAccept(), ','))
                            .filter(this::validMimeType)
                            .map(String::trim)
                            .collect(Collectors.joining(","));
                }
                if (acceptValues != null && !acceptValues.isEmpty()) {
                    inputRequest.setAccept(acceptValues);
                }
            }
            if (request.getMultiple() != null) {
                if (VariableResolver.isVariableReference(request.getMultiple())) {
                    inputRequest.setMultiple(variableResolver.resolveVariableAsBoolean(request.getMultiple()).getValue());
                } else {
                    inputRequest.setMultiple(Boolean.parseBoolean(request.getMultiple()));
                }
            }
        } else {
            // Handle text inputs.
            // Select options.
            if (request.getOptions() != null) {
                String options = request.getOptions();
                if (VariableResolver.isVariableReference(options)) {
                    options = resolveTokenValues(variableResolver, options);
                }
                inputRequest.setOptions(options);
                if (request.getOptionLabels() == null) {
                    // The options are the labels themselves.
                    inputRequest.setOptionLabels(inputRequest.getOptions());
                } else {
                    String labels = request.getOptionLabels();
                    if (VariableResolver.isVariableReference(labels)) {
                        labels = resolveTokenValues(variableResolver, labels);
                    }
                    inputRequest.setOptionLabels(labels);
                }
                // Check that the counts are correct.
                int optionCount = StringUtils.countMatches(inputRequest.getOptions(), ",");
                int labelCount = StringUtils.countMatches(inputRequest.getOptionLabels(), ",");
                if (optionCount != labelCount) {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "The number of options ("+optionCount+") doesn't match the number of option labels ("+labelCount+")"));
                }
                inputRequest.setMultiple(Boolean.FALSE);
                if (request.getMultiple() == null) {
                    if (inputRequest.getInputType() == InputRequestInputType.SELECT_MULTIPLE) {
                        inputRequest.setMultiple(Boolean.TRUE);
                    }
                } else {
                    if (VariableResolver.isVariableReference(request.getMultiple())) {
                        inputRequest.setMultiple(variableResolver.resolveVariableAsBoolean(request.getMultiple()).getValue());
                    } else {
                        inputRequest.setMultiple(Boolean.parseBoolean(request.getMultiple()));
                    }
                    if (inputRequest.isMultiple()) {
                        inputRequest.setInputType(InputRequestInputType.SELECT_MULTIPLE);
                    } else {
                        inputRequest.setInputType(InputRequestInputType.SELECT_SINGLE);
                    }
                }
            }
            if (inputRequest.getInputType() == null) {
                inputRequest.setInputType(InputRequestInputType.TEXT);
            }
            // Rows for multiline text, code editors and select multiple.
            if (inputRequest.getInputType() == InputRequestInputType.MULTILINE_TEXT
                    || inputRequest.getInputType() == InputRequestInputType.CODE
                    || inputRequest.getInputType() == InputRequestInputType.SELECT_MULTIPLE) {
                if (request.getSize() != null) {
                    Integer rowsToSet = null;
                    if (VariableResolver.isVariableReference(request.getSize())) {
                        rowsToSet = variableResolver.resolveVariableAsNumber(request.getSize()).intValue();
                    } else {
                        try {
                            rowsToSet = Integer.parseInt(request.getSize());
                        } catch (NumberFormatException e) {
                            logger.warn(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), "Ignoring 'size' on interaction step request as it was not a valid number");
                        }
                    }
                    if (rowsToSet != null && rowsToSet < 1) {
                        // Ignore if not at least 1.
                        logger.warn(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), "Ignoring 'size' on interaction step request as it was not a positive integer");
                        rowsToSet = null;
                    }
                    if (rowsToSet != null) {
                        inputRequest.setSize(BigInteger.valueOf(rowsToSet));
                    }
                }
            }
            // Default value(s)
            if (request.getDefault() != null) {
                String defaultValue;
                if (VariableResolver.isVariableReference(request.getDefault())) {
                    defaultValue = variableResolver.resolveVariableAsString(request.getDefault()).getValue();
                } else {
                    defaultValue = request.getDefault();
                }
                inputRequest.setDefault(defaultValue);
            }
            // Set this on the original object as we have now resolved any expressions as well.
            request.setInputType(inputRequest.getInputType());
        }
        return inputRequest;
    }

    private String resolveTokenValues(VariableResolver variableResolver, String expression) {
        return resolveTokenValues(variableResolver, expression, null);
    }

    private String resolveTokenValues(VariableResolver variableResolver, String expression, Function<String, Boolean> tokenValidator) {
        String tokenValues;
        DataType referencedType = variableResolver.resolveVariable(expression);
        if (DataType.isListType(referencedType.getType())) {
            // Convert to comma-delimited list.
            StringBuilder str = new StringBuilder();
            List<DataType> items = (List<DataType>)referencedType.getValue();
            if (items != null && !items.isEmpty()) {
                for (DataType item: items) {
                    String itemAsString = item.convertTo(DataType.STRING_DATA_TYPE).toString().trim();
                    if (tokenValidator == null || tokenValidator.apply(itemAsString)) {
                        str.append(itemAsString);
                        str.append(',');
                    }
                }
                str.deleteCharAt(str.length()-1);
            }
            tokenValues = str.toString();
        } else {
            tokenValues = (String)(referencedType.convertTo(DataType.STRING_DATA_TYPE).getValue());
        }
        return tokenValues;
    }

    @Override
    protected void handleInputEvent(InputEvent event) {
        processing();
        if (step.isAdmin() && !event.isAdmin()) {
            // This was an administrator-level interaction for which we received input from a non-administrator.
            // This is not normal and should be logged and recorded as an error.
            promise.tryFailure(new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_SESSION, String.format("User-provided inputs were expected to be received by an administrator - step [%s] - ID [%s]", TestCaseUtils.extractStepDescription(step, scope), stepId))));
        } else {
            logger.debug(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), String.format("Handling user-provided inputs - step [%s] - ID [%s]", TestCaseUtils.extractStepDescription(step, scope), stepId));
            List<UserInput> userInputs = event.getUserInputs();
            DataTypeFactory dataTypeFactory = DataTypeFactory.getInstance();
            // Create the Variable for Interaction Result if an id is given for the Interaction
            MapType interactionResult = (MapType) dataTypeFactory.create(DataType.MAP_DATA_TYPE);
            TAR report = new TAR();
            report.setResult(TestResultType.SUCCESS);
            try {
                report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
            } catch (DatatypeConfigurationException e) {
                throw new IllegalStateException(e);
            }
            if (!step.getInstructOrRequest().isEmpty()) {
                report.setContext(new AnyContent());
                report.getContext().setType("list");
                ExpressionHandler expressionHandler = new ExpressionHandler(scope);
                VariableResolver variableResolver = expressionHandler.getVariableResolver();
                // Determine the included elements (same numbering scheme used when the interaction request was
                // built - see collectIncludedItems), and a name-based lookup to resolve dependencies against.
                Map<String, IncludedInteractionItem> includedItemsByName = new HashMap<>();
                List<IncludedInteractionItem> includedItems = collectIncludedItems(step.getInstructOrRequest(), expressionHandler, includedItemsByName);
                // For each included element, determine whether its dependency (if any) is satisfied by the submitted inputs.
                Map<String, Boolean> dependencySatisfiedById = new HashMap<>();
                for (var includedItem : includedItems) {
                    dependencySatisfiedById.put(includedItem.id(), isDependencySatisfied(includedItem.item(), includedItemsByName, event, variableResolver));
                }
                // Determine the required request elements for which we expect inputs. A required element whose
                // dependency is not satisfied is ignored (never required).
                Set<Integer> requiredInputIndexes = includedItems.stream()
                        .filter(includedItem -> includedItem.item() instanceof UserRequest userRequest && isRequired(userRequest, variableResolver) && dependencySatisfiedById.get(includedItem.id()))
                        .map(includedItem -> Integer.parseInt(includedItem.id()) - 1)
                        .collect(Collectors.toSet());
                for (var includedItem : includedItems) {
                    InstructionOrRequest instructionOrRequest = includedItem.item();
                    boolean dependencySatisfied = dependencySatisfiedById.get(includedItem.id());
                    int index = Integer.parseInt(includedItem.id()) - 1;
                    if (instructionOrRequest instanceof com.gitb.tdl.Instruction instruction) {
                        // Process instruction.
                        if (instruction.isReport() && dependencySatisfied) {
                            var instructionContent = new AnyContent();
                            instructionContent.setName(fixedValueOrVariable(instruction.getDesc(), variableResolver, null));
                            instructionContent.setMimeType(fixedValueOrVariable(instruction.getMimeType(), expressionHandler.getVariableResolver(), null));
                            setInstructionValue(instructionContent, instruction, expressionHandler);
                            InstructionLevel level = getInstructionLevel(instruction, expressionHandler);
                            if (level != null) addMetadataToken(instructionContent, "level", level.value());
                            if (!instruction.isShowControls()) addMetadataToken(instructionContent, "showControls", "false");
                            if (instruction.isForceDisplay()) addMetadataToken(instructionContent, "forceDisplay", "true");
                            report.getContext().getItem().add(instructionContent);
                        }
                    } else if (instructionOrRequest instanceof UserRequest request) {
                        // Process request.
                        processUserInput(request, index, event, variableResolver, dataTypeFactory, requiredInputIndexes, report, interactionResult, dependencySatisfied);
                    }
                }
                if (!requiredInputIndexes.isEmpty()) {
                    // Not all required inputs were provided with inputs - fail.
                    throw new GITBEngineInternalError("Required request elements were found that were not provided with corresponding inputs");
                }
            }
            if (step.getId() != null && (!userInputs.isEmpty() || !scope.getVariable(step.getId()).isDefined())) {
                // We may want to skip creating a map in the scope in case this is a headless session (in which case no inputs
                // are provided) but we already have a variable in the session matching the step ID. This can be the case if
                // The test has started via REST call and the relevant map is provided as input.
                TestCaseScope.ScopedVariable scopedVariable = scope.createVariable(step.getId());
                scopedVariable.setValue(interactionResult);
            }
            promise.trySuccess(report);
        }
    }

    /**
     * Determines whether an interaction element's dependency (if any) is satisfied by the inputs submitted in the
     * given event. An element with no dependency, or with a dependency that can't be resolved to an included
     * request or that targets a file upload (not applicable), is always considered satisfied. Otherwise this is
     * satisfied when at least one of the submitted values for the referenced request matches the resolved
     * dependency value - this single check covers both the "exact value" rule (single-valued input types, where at
     * most one submitted value is ever present) and the "any selected value matches" rule (SELECT_MULTIPLE).
     */
    private boolean isDependencySatisfied(InstructionOrRequest item, Map<String, IncludedInteractionItem> includedItemsByName, InputEvent event, VariableResolver variableResolver) {
        if (item.getDependsOn() == null) {
            return true;
        }
        var target = includedItemsByName.get(item.getDependsOn());
        if (target == null || !(target.item() instanceof UserRequest targetRequest) || targetRequest.getInputType() == InputRequestInputType.UPLOAD) {
            return true;
        }
        String expectedValue = fixedValueOrVariable(item.getDependsOnValue(), variableResolver, null);
        if (expectedValue == null || event.getUserInputs() == null) {
            return true;
        }
        return event.getUserInputs().stream()
                .filter(userInput -> target.id().equals(userInput.getId()))
                .anyMatch(userInput -> expectedValue.equals(userInput.getValue()));
    }

    private void addMetadataToken(AnyContent content, String tokenKey, String tokenValue) {
        String metadataToAdd = tokenKey + "=" + tokenValue;
        if (content.getMetadata() != null) {
            content.setMetadata(content.getMetadata() + ";" + metadataToAdd);
        } else {
            content.setMetadata(metadataToAdd);
        }
    }

    private void processUserInput(UserRequest targetRequest, int requestIndex, InputEvent inputEvent, VariableResolver variableResolver, DataTypeFactory dataTypeFactory, Set<Integer> requiredInputIndexes, TAR report, MapType interactionResult, boolean dependencySatisfied) {
        if (inputEvent.getUserInputs() != null) {
            List<UserInput> matchingInputs = inputEvent.getUserInputs().stream()
                    .filter(userInput -> {
                        int stepIndex = Integer.parseInt(userInput.getId());
                        return requestIndex == stepIndex - 1;
                    })
                    .toList();
            if (!matchingInputs.isEmpty()) {
                boolean multipleExpected = false;
                if (targetRequest.getMultiple() != null) {
                    if (VariableResolver.isVariableReference(targetRequest.getMultiple())) {
                        multipleExpected = variableResolver.resolveVariableAsBoolean(targetRequest.getMultiple()).getValue();
                    } else {
                        multipleExpected = Boolean.parseBoolean(targetRequest.getMultiple());
                    }
                }
                boolean recordFileNames = StringUtils.isNotBlank(targetRequest.getFileName());
                AnyContent contentForContext;
                String dataTypeForContext;
                DataType fileNameValue = null;
                if (!multipleExpected) {
                    UserInput userInput = matchingInputs.getFirst();
                    if (userInput.getValue() != null && !userInput.getValue().isEmpty()) {
                        requiredInputIndexes.remove(requestIndex);
                        if (targetRequest.isReport() && dependencySatisfied) {
                            // Construct the value to return for the step's report.
                            report.getContext().getItem().add(getAnyContent(userInput, targetRequest));
                        }
                    }
                    // Value for session context.
                    contentForContext = userInput;
                    dataTypeForContext = targetRequest.getType();
                    // File name
                    if (recordFileNames && StringUtils.isNotBlank(userInput.getFileName())) {
                        fileNameValue = new StringType(userInput.getFileName());
                    }
                } else {
                    List<UserInput> inputsWithValues = matchingInputs.stream()
                            .filter(userInput -> userInput.getValue() != null && !userInput.getValue().isEmpty())
                            .toList();
                    if (!inputsWithValues.isEmpty()) {
                        requiredInputIndexes.remove(requestIndex);
                        if (targetRequest.isReport() && dependencySatisfied) {
                            // Construct the value to return for the step's report.
                            if (inputsWithValues.size() == 1) {
                                // Single item - add it without a list.
                                report.getContext().getItem().add(getAnyContent(inputsWithValues.getFirst(), targetRequest));
                            } else {
                                // Add items as a list.
                                AnyContent userInputs = new AnyContent();
                                userInputs.setType("list");
                                List<AnyContent> userInputItems = inputsWithValues.stream().map(userInput -> getAnyContent(userInput, targetRequest)).toList();
                                userInputs.setName(userInputItems.getFirst().getName());
                                userInputItems.forEach(userInputItem -> {
                                    userInputItem.setName(null);
                                    userInputs.getItem().add(userInputItem);
                                });
                                report.getContext().getItem().add(userInputs);
                            }
                        }
                    }
                    if (targetRequest.getType() == null) {
                        dataTypeForContext = "list";
                    } else {
                        dataTypeForContext = "list["+targetRequest.getType()+"]";
                    }
                    // Value for session context.
                    contentForContext = new AnyContent();
                    contentForContext.setType("list");
                    contentForContext.setName(matchingInputs.getFirst().getName());
                    contentForContext.getItem().addAll(matchingInputs);
                    // File names
                    if (recordFileNames) {
                        List<StringType> fileNames = matchingInputs.stream().filter(userInput -> StringUtils.isNotBlank(userInput.getFileName()))
                                .map(userInput -> new StringType(userInput.getFileName()))
                                .toList();
                        ListType fileNameTypes = new ListType(DataType.STRING_DATA_TYPE);
                        fileNameTypes.getElements().addAll(fileNames);
                        fileNameValue = fileNameTypes;
                    }
                }
                if (StringUtils.isNotBlank(targetRequest.getValue())) {
                    // Find the variable that the given input content is assigned(bound) to
                    String assignedVariableExpression = targetRequest.getValue();
                    DataType assignedVariable = variableResolver.resolveVariable(assignedVariableExpression);
                    if (targetRequest.isAsTemplate()) {
                        DataTypeUtils.setDataTypeValueWithAnyContent(assignedVariable, contentForContext, (dataType) -> {
                            DataType dataTypeAfterAppliedTemplate = TemplateUtils.generateDataTypeFromTemplate(scope, dataType, dataType.getType());
                            dataType.copyFrom(dataTypeAfterAppliedTemplate);
                        });
                    } else {
                        DataTypeUtils.setDataTypeValueWithAnyContent(assignedVariable, contentForContext);
                    }
                } else {
                    // Create an empty value
                    DataType assignedValue = dataTypeFactory.create(dataTypeForContext);
                    if (targetRequest.isAsTemplate()) {
                        DataTypeUtils.setDataTypeValueWithAnyContent(assignedValue, contentForContext, (dataType) -> {
                            DataType dataTypeAfterAppliedTemplate = TemplateUtils.generateDataTypeFromTemplate(scope, dataType, dataType.getType());
                            dataType.copyFrom(dataTypeAfterAppliedTemplate);
                        });
                    } else {
                        DataTypeUtils.setDataTypeValueWithAnyContent(assignedValue, contentForContext);
                    }
                    // Put it to the Interaction Result map
                    if (targetRequest.getName() != null) {
                        interactionResult.addItem(targetRequest.getName(), assignedValue);
                    }
                    if (fileNameValue != null) {
                        // Record the file name under the provided variable
                        String variableName;
                        if (VariableResolver.isVariableReference(targetRequest.getFileName())) {
                            variableName = variableResolver.resolveVariableAsString(targetRequest.getFileName()).toString();
                        } else {
                            variableName = targetRequest.getFileName().trim();
                        }
                        interactionResult.addItem(variableName, fileNameValue);
                    }
                }
            }
        }
    }

    private AnyContent getAnyContent(UserInput userInput, UserRequest requestInfo) {
        var reportItem = new AnyContent();
        if (requestInfo.getInputType() == InputRequestInputType.SECRET) {
            reportItem.setValue("**********");
        } else {
            reportItem.setValue(userInput.getValue());
        }
        reportItem.setName(requestInfo.getDesc());
        if (reportItem.getName() == null) {
            reportItem.setName(requestInfo.getName());
        }
        reportItem.setEmbeddingMethod(userInput.getEmbeddingMethod());
        reportItem.setMimeType(requestInfo.getMimeType());
        return reportItem;
    }

    @Override
    protected void stop() {
        super.stop();
        if (promise != null && !promise.isCompleted()) {
            promise.tryFailure(new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.CANCELLATION, "Test step ["+stepId+"] is cancelled.")));
        }
    }

    public static ActorRef create(ActorContext context, UserInteraction step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
        return context.actorOf(props(InteractionStepProcessorActor.class, step, scope, stepId, stepContext), getName(NAME));
    }
}
