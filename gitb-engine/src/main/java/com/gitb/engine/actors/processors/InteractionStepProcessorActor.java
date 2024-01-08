package com.gitb.engine.actors.processors;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.dispatch.OnFailure;
import org.apache.pekko.dispatch.OnSuccess;
import com.gitb.core.AnyContent;
import com.gitb.core.ErrorCode;
import com.gitb.core.InputRequestInputType;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.engine.TestbedService;
import com.gitb.engine.actors.ActorSystem;
import com.gitb.engine.events.TestStepInputEventBus;
import com.gitb.engine.events.model.ErrorStatusEvent;
import com.gitb.engine.events.model.InputEvent;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.TemplateUtils;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tbs.InputRequest;
import com.gitb.tbs.Instruction;
import com.gitb.tbs.UserInput;
import com.gitb.tbs.UserInteractionRequest;
import com.gitb.tdl.InstructionOrRequest;
import com.gitb.tdl.UserInteraction;
import com.gitb.tdl.UserRequest;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.MapType;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.ErrorUtils;
import com.gitb.utils.XMLDateTimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.List;

/**
 * Created by tuncay on 9/24/14.
 *
 * User interaction step executor actor
 */
public class InteractionStepProcessorActor extends AbstractTestStepActor<UserInteraction> {

    public static final String NAME = "interaction-p";

    private static final Logger logger = LoggerFactory.getLogger(InteractionStepProcessorActor.class);

    private Promise<TestStepReportType> promise;

    public InteractionStepProcessorActor(UserInteraction step, TestCaseScope scope, String stepId) {
        super(step, scope, stepId);
    }

    @Override
    protected void init() {
        String classifier = TestStepInputEventBus.getClassifier(scope.getContext().getSessionId(), stepId);
        TestStepInputEventBus
                .getInstance()
                .subscribe(self(), classifier);

        final ActorContext context = getContext();

        promise = Futures.promise();

        promise.future().failed().foreach(new OnFailure() {
            @Override
            public void onFailure(Throwable failure) {
                updateTestStepStatus(context, new ErrorStatusEvent(failure, scope, self()), null, true);
            }
        }, getContext().dispatcher());
    }

    private String fixedValueOrVariable(String value, VariableResolver variableResolver, String defaultValue) {
        if (VariableResolver.isVariableReference(value)) {
            value = (String) variableResolver.resolveVariableAsString(value).getValue();
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
        //If it is a request
        Future<TestStepReportType> future = Futures.future(() -> {
            //Process the instructions and request the interaction from TestbedClient
            try {
                VariableResolver variableResolver = new VariableResolver(scope);
                List<InstructionOrRequest> instructionAndRequests = step.getInstructOrRequest();
                var withValue = fixedValueOrVariable(step.getWith(), variableResolver, getSUTActor().getId());
                int childStepId = 1;
                // Prepare the message to send to the frontend.
                UserInteractionRequest userInteractionRequest = new UserInteractionRequest();
                userInteractionRequest.setInputTitle(fixedValueOrVariable(step.getInputTitle(), variableResolver, "User interaction"));
                userInteractionRequest.setWith(withValue);
                for (InstructionOrRequest instructionOrRequest : instructionAndRequests) {
                    // Set the type in case this is missing.
                    if (StringUtils.isBlank(instructionOrRequest.getType())) {
                        if (instructionOrRequest.getContentType() == ValueEmbeddingEnumeration.BASE_64 || (instructionOrRequest instanceof UserRequest && ((UserRequest)instructionOrRequest).getInputType() == InputRequestInputType.UPLOAD)) {
                            // if the contentType is set to BASE64 or the inputType is UPLOAD this will be a file.
                            instructionOrRequest.setType(DataType.BINARY_DATA_TYPE);
                        } else {
                            if (VariableResolver.isVariableReference(instructionOrRequest.getValue())) {
                                // If a target variable is referenced we can use this to determine the type.
                                DataType targetVariable = variableResolver.resolveVariable(instructionOrRequest.getValue());
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
                        // If no expression is specified consider it an empty expression.
                        if (StringUtils.isBlank(instruction.getValue())) {
                            instructionOrRequest.setValue("''");
                        }
                        userInteractionRequest.getInstructionOrRequest().add(processInstruction(instruction, "" + childStepId, withValue, variableResolver));
                    } else if (instructionOrRequest instanceof UserRequest request) { // If it is a request
                        userInteractionRequest.getInstructionOrRequest().add(processRequest(request, "" + childStepId, withValue, variableResolver));
                    } else {
                        throw new IllegalStateException("Unsupported interaction type ["+instructionOrRequest+"]");
                    }
                    childStepId++;
                }
                logger.debug(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), String.format("Triggering user interaction - step [%s] - ID [%s]", TestCaseUtils.extractStepDescription(step, scope), stepId));
                TestbedService.interactWithUsers(scope.getContext().getSessionId(), stepId, userInteractionRequest);
                return null;
            } catch (Exception e) {
                logger.error(addMarker(), "Error in interaction step", e);
                throw new GITBEngineInternalError(e);
            }
        }, getContext().system().dispatchers().lookup(ActorSystem.BLOCKING_DISPATCHER));

        future.foreach(new OnSuccess<>() {

            @Override
            public void onSuccess(TestStepReportType result) {
                promise.trySuccess(result);
            }
        }, getContext().dispatcher());

        future.failed().foreach(new OnFailure() {
            @Override
            public void onFailure(Throwable failure) { promise.tryFailure(failure);
            }
        }, getContext().dispatcher());
        waiting();
    }

    /**
     * Process TDL Instruction command and convert it to Instruction TBS request object
     *
     * @param instructionCommand command
     * @param stepId step id
     * @return instruction
     */
    private Instruction processInstruction(com.gitb.tdl.Instruction instructionCommand, String stepId, String withValue, VariableResolver variableResolver) {
        Instruction instruction = new Instruction();
        instruction.setWith(withValue);
        instruction.setDesc(fixedValueOrVariable(instructionCommand.getDesc(), variableResolver, null));
        instruction.setId(stepId);
        instruction.setName(instructionCommand.getName());
        instruction.setEncoding(instructionCommand.getEncoding());
        instruction.setMimeType(fixedValueOrVariable(instructionCommand.getMimeType(), variableResolver, null));
        instruction.setForceDisplay(instructionCommand.isForceDisplay());

        ExpressionHandler exprHandler = new ExpressionHandler(this.scope);
        DataType computedValue = exprHandler.processExpression(instructionCommand, instructionCommand.getType());

	    DataTypeUtils.setContentValueWithDataType(instruction, computedValue);

        return instruction;
    }


    /**
     * Process TDL InputRequest command and convert it to TBS InputRequest object
     *
     * @param instructionCommand request
     * @param stepId step id
     * @return input request
     */
    private InputRequest processRequest(UserRequest instructionCommand, String stepId, String withValue, VariableResolver variableResolver) {
        InputRequest inputRequest = new InputRequest();
        inputRequest.setWith(withValue);
        inputRequest.setDesc(fixedValueOrVariable(instructionCommand.getDesc(), variableResolver, null));
        inputRequest.setName(instructionCommand.getValue()); //name is provided from value node
        inputRequest.setContentType(instructionCommand.getContentType());
        inputRequest.setType(instructionCommand.getType());
        inputRequest.setEncoding(instructionCommand.getEncoding());
        inputRequest.setId(stepId);
        inputRequest.setInputType(instructionCommand.getInputType());
        inputRequest.setMimeType(fixedValueOrVariable(instructionCommand.getMimeType(), variableResolver, null));
        // Handle text inputs.
        if (instructionCommand.getInputType() != InputRequestInputType.UPLOAD) {
            // Select options.
            if (instructionCommand.getOptions() != null) {
                String options = instructionCommand.getOptions();
                if (VariableResolver.isVariableReference(options)) {
                    options = resolveTokenValues(variableResolver, options);
                }
                inputRequest.setOptions(options);
                if (instructionCommand.getOptionLabels() == null) {
                    // The options are the labels themselves.
                    inputRequest.setOptionLabels(inputRequest.getOptions());
                } else {
                    String labels = instructionCommand.getOptionLabels();
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
                if (instructionCommand.getMultiple() == null) {
                    if (inputRequest.getInputType() == InputRequestInputType.SELECT_MULTIPLE) {
                        inputRequest.setMultiple(Boolean.TRUE);
                    }
                } else {
                    if (VariableResolver.isVariableReference(instructionCommand.getMultiple())) {
                        inputRequest.setMultiple((Boolean)(variableResolver.resolveVariableAsBoolean(instructionCommand.getMultiple()).getValue()));
                    } else {
                        inputRequest.setMultiple(Boolean.parseBoolean(instructionCommand.getMultiple()));
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
            instructionCommand.setInputType(inputRequest.getInputType());
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
        logger.debug(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), String.format("Handling user-provided inputs - step [%s] - ID [%s]", TestCaseUtils.extractStepDescription(step, scope), stepId));
        List<UserInput> userInputs = event.getUserInputs();
        TestCaseScope.ScopedVariable scopedVariable;
        DataTypeFactory dataTypeFactory = DataTypeFactory.getInstance();
        //Create the Variable for Interaction Result if an id is given for the Interaction
        MapType interactionResult = (MapType) dataTypeFactory.create(DataType.MAP_DATA_TYPE);
        if (step.getId() != null && (!userInputs.isEmpty() || !scope.getVariable(step.getId()).isDefined())) {
            // We may want to skip creating a map in the scope in case this is a headless session (in which case no inputs
            // are provided) but we already have a variable in the session matching the step ID. This can be the case if
            // The test has started via REST call and the relevant map is provided as input.
            scopedVariable = scope.createVariable(step.getId());
            scopedVariable.setValue(interactionResult);
        }
        TAR report = new TAR();
        report.setResult(TestResultType.SUCCESS);
        try {
            report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException(e);
        }
        if (!userInputs.isEmpty()) {
            report.setContext(new AnyContent());
            report.getContext().setType("list");
            //Assign the content for each input to either given variable or to the Interaction Map (with the given name as key)
            for (UserInput userInput : userInputs) {
                int stepIndex = Integer.parseInt(userInput.getId());
                InstructionOrRequest targetRequest = step.getInstructOrRequest().get(stepIndex - 1);
                if (targetRequest instanceof UserRequest requestInfo && userInput.getValue() != null && !userInput.getValue().isEmpty()) {
                    // Construct the value to return for the step's report.
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
                    report.getContext().getItem().add(reportItem);
                }
                if (StringUtils.isNotBlank(targetRequest.getValue())) {
                    //Find the variable that the given input content is assigned(bound) to
                    String assignedVariableExpression = targetRequest.getValue();
                    VariableResolver variableResolver = new VariableResolver(scope);
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
                }
            }
        }
        completed(report);
    }


    public static ActorRef create(ActorContext context, UserInteraction step, TestCaseScope scope, String stepId) throws Exception {
        return context.actorOf(props(InteractionStepProcessorActor.class, step, scope, stepId), getName(NAME));
    }
}
