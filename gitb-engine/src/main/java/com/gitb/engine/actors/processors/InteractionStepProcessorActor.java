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

package com.gitb.engine.actors.processors;

import com.gitb.core.AnyContent;
import com.gitb.core.ErrorCode;
import com.gitb.core.InputRequestInputType;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.engine.CallbackManager;
import com.gitb.engine.PropertyConstants;
import com.gitb.engine.TestbedService;
import com.gitb.engine.commands.messaging.NotificationReceived;
import com.gitb.engine.commands.messaging.TimeoutExpired;
import com.gitb.engine.events.TestStepInputEventBus;
import com.gitb.engine.events.model.InputEvent;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;
import com.gitb.engine.remote.HandlerTimeoutException;
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
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.ErrorUtils;
import com.gitb.utils.XMLDateTimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.dispatch.OnFailure;
import org.apache.pekko.dispatch.OnSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        promise.future().foreach(new OnSuccess<>() {
            @Override
            public void onSuccess(TestStepReportType result) {
                completed(result);
            }
        }, getContext().dispatcher());

        promise.future().failed().foreach(new OnFailure() {
            @Override
            public void onFailure(Throwable failure) {
                handleFutureFailure(failure);
            }
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
                    context.system().scheduler().scheduleOnce(
                            scala.concurrent.duration.Duration.apply(timeout, TimeUnit.MILLISECONDS), () -> {
                                if (!self().isTerminated()) {
                                    self().tell(new TimeoutExpired(), self());
                                }
                            },
                            context.dispatcher()
                    );
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

        future.failed().foreach(new OnFailure() {
            @Override
            public void onFailure(Throwable failure) {
                promise.tryFailure(failure);
            }
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

    private void processAsUserInterfaceInteraction(long timeout, ExpressionHandler expressionHandler) {
        boolean hasInstructions = false;
        boolean hasRequests = false;
        List<InstructionOrRequest> instructionAndRequests = step.getInstructOrRequest();
        var withValue = fixedValueOrVariable(step.getWith(), expressionHandler.getVariableResolver(), getSUTActor().getId());
        int childStepId = 1;
        // Prepare the message to send to the frontend.
        UserInteractionRequest userInteractionRequest = new UserInteractionRequest();
        userInteractionRequest.setInputTitle(fixedValueOrVariable(step.getInputTitle(), expressionHandler.getVariableResolver(), "User interaction"));
        userInteractionRequest.setWith(withValue);
        userInteractionRequest.setAdmin(step.isAdmin());
        userInteractionRequest.setDesc(step.getDesc());
        userInteractionRequest.setHasTimeout(timeout > 0);
        for (InstructionOrRequest instructionOrRequest : instructionAndRequests) {
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
            //If it is an instruction
            if (instructionOrRequest instanceof com.gitb.tdl.Instruction instruction) {
                hasInstructions = true;
                // If no expression is specified consider it an empty expression.
                if (StringUtils.isBlank(instruction.getValue())) {
                    instructionOrRequest.setValue("''");
                }
                userInteractionRequest.getInstructionOrRequest().add(processInstruction(instruction, "" + childStepId, withValue, expressionHandler));
            } else if (instructionOrRequest instanceof UserRequest request) { // If it is a request
                hasRequests = true;
                userInteractionRequest.getInstructionOrRequest().add(processRequest(request, "" + childStepId, withValue, expressionHandler.getVariableResolver()));
            } else {
                throw new IllegalStateException("Unsupported interaction type ["+instructionOrRequest+"]");
            }
            childStepId++;
        }
        logger.debug(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), String.format("Triggering user interaction - step [%s] - ID [%s]", TestCaseUtils.extractStepDescription(step, scope), stepId));
        TestbedService.interactWithUsers(scope.getContext().getSessionId(), stepId, userInteractionRequest);

        if (hasInstructions && !hasRequests && isNonBlocking(expressionHandler.getVariableResolver())) {
            // The step is a non-blocking interaction containing only instructions. Notify immediately for its completion.
            self().tell(new InputEvent(scope.getContext().getSessionId(), stepId, Collections.emptyList(), step.isAdmin()), self());
        }
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

        ExpressionHandler exprHandler = new ExpressionHandler(this.scope);
        DataType computedValue = exprHandler.processExpression(instructionCommand, instructionCommand.getType());

	    DataTypeUtils.setContentValueWithDataType(instruction, computedValue);

        return instruction;
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
        // Handle text inputs.
        if (request.getInputType() != InputRequestInputType.UPLOAD) {
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
            // Set this on the original object as we have now resolved any option-related expressions as well.
            request.setInputType(inputRequest.getInputType());
        }
        return inputRequest;
    }

    private String resolveTokenValues(VariableResolver variableResolver, String expression) {
        String tokenValues;
        DataType referencedType = variableResolver.resolveVariable(expression);
        if (DataType.isListType(referencedType.getType())) {
            // Convert to comma-delimited list.
            StringBuilder str = new StringBuilder();
            List<DataType> items = (List<DataType>)referencedType.getValue();
            if (items != null && !items.isEmpty()) {
                for (DataType item: items) {
                    str.append(item.convertTo(DataType.STRING_DATA_TYPE));
                    str.append(',');
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
            //Create the Variable for Interaction Result if an id is given for the Interaction
            MapType interactionResult = (MapType) dataTypeFactory.create(DataType.MAP_DATA_TYPE);
            TAR report = new TAR();
            report.setResult(TestResultType.SUCCESS);
            try {
                report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
            } catch (DatatypeConfigurationException e) {
                throw new IllegalStateException(e);
            }
            if (!step.getInstructOrRequest().isEmpty()) {
                VariableResolver variableResolver = new VariableResolver(scope);
                // Determine the required request elements for which we expect inputs.
                Set<Integer> requiredInputIndexes = IntStream.range(0, step.getInstructOrRequest().size())
                        .filter(i -> step.getInstructOrRequest().get(i) instanceof UserRequest userRequest && isRequired(userRequest, variableResolver))
                        .boxed()
                        .collect(Collectors.toSet());
                if (!userInputs.isEmpty()) {
                    report.setContext(new AnyContent());
                    report.getContext().setType("list");
                    //Assign the content for each input to either given variable or to the Interaction Map (with the given name as key)
                    for (UserInput userInput : userInputs) {
                        int stepIndex = Integer.parseInt(userInput.getId());
                        InstructionOrRequest targetRequest = step.getInstructOrRequest().get(stepIndex - 1);
                        if (targetRequest instanceof UserRequest requestInfo && userInput.getValue() != null && !userInput.getValue().isEmpty()) {
                            requiredInputIndexes.remove(stepIndex - 1);
                            if (requestInfo.isReport()) {
                                // Construct the value to return for the step's report.
                                report.getContext().getItem().add(getAnyContent(userInput, requestInfo));
                            }
                        }
                        if (StringUtils.isNotBlank(targetRequest.getValue())) {
                            //Find the variable that the given input content is assigned(bound) to
                            String assignedVariableExpression = targetRequest.getValue();
                            DataType assignedVariable = variableResolver.resolveVariable(assignedVariableExpression);
                            if (targetRequest.isAsTemplate()) {
                                DataTypeUtils.setDataTypeValueWithAnyContent(assignedVariable, userInput, (dataType) -> {
                                    DataType dataTypeAfterAppliedTemplate = TemplateUtils.generateDataTypeFromTemplate(scope, dataType, dataType.getType());
                                    dataType.copyFrom(dataTypeAfterAppliedTemplate);
                                });
                            } else {
                                DataTypeUtils.setDataTypeValueWithAnyContent(assignedVariable, userInput);
                            }
                        } else {
                            //Create an empty value
                            DataType assignedValue = dataTypeFactory.create(targetRequest.getType());
                            if (targetRequest.isAsTemplate()) {
                                DataTypeUtils.setDataTypeValueWithAnyContent(assignedValue, userInput, (dataType) -> {
                                    DataType dataTypeAfterAppliedTemplate = TemplateUtils.generateDataTypeFromTemplate(scope, dataType, dataType.getType());
                                    dataType.copyFrom(dataTypeAfterAppliedTemplate);
                                });
                            } else {
                                DataTypeUtils.setDataTypeValueWithAnyContent(assignedValue, userInput);
                            }
                            //Put it to the Interaction Result map
                            if (targetRequest.getName() != null) {
                                interactionResult.addItem(targetRequest.getName(), assignedValue);
                            }
                            if (targetRequest instanceof UserRequest userRequest && StringUtils.isNotBlank(userRequest.getFileName()) && StringUtils.isNotBlank(userInput.getFileName())) {
                                // Record the file name under the provided variable
                                String variableName;
                                if (VariableResolver.isVariableReference(userRequest.getFileName())) {
                                    variableName = variableResolver.resolveVariableAsString(userRequest.getFileName()).toString();
                                } else {
                                    variableName = userRequest.getFileName().trim();
                                }
                                interactionResult.addItem(variableName, new StringType(userInput.getFileName()));
                            }
                        }
                    }
                    if (!requiredInputIndexes.isEmpty()) {
                        // Not all required inputs were provided with inputs - fail.
                        throw new GITBEngineInternalError("Required request elements were found that were not provided with corresponding inputs");
                    }
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
        if (promise != null && !promise.isCompleted()) {
            promise.tryFailure(new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.CANCELLATION, "Test step ["+stepId+"] is cancelled.")));
        }
    }

    public static ActorRef create(ActorContext context, UserInteraction step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
        return context.actorOf(props(InteractionStepProcessorActor.class, step, scope, stepId, stepContext), getName(NAME));
    }
}
