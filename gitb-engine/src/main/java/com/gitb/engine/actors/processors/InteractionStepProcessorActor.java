package com.gitb.engine.actors.processors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
import com.gitb.core.ErrorCode;
import com.gitb.core.StepStatus;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.engine.TestbedService;
import com.gitb.engine.actors.ActorSystem;
import com.gitb.engine.events.TestStepInputEventBus;
import com.gitb.engine.events.model.ErrorStatusEvent;
import com.gitb.engine.events.model.InputEvent;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tbs.InputRequest;
import com.gitb.tbs.Instruction;
import com.gitb.tbs.UserInput;
import com.gitb.tbs.UserInteractionRequest;
import com.gitb.tdl.InstructionOrRequest;
import com.gitb.tdl.UserInteraction;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.MapType;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by tuncay on 9/24/14.
 *
 * User interaction step executor actor
 */
public class InteractionStepProcessorActor extends AbstractTestStepActor<UserInteraction> {
    private static final String INSTRUCT_ELEMENT_NAME = "instruct";
    public static final String NAME = "interaction-p";

    private static Logger logger = LoggerFactory.getLogger(InteractionStepProcessorActor.class);

    private Promise<TestStepReportType> promise;
    private Future<TestStepReportType> future;

    public InteractionStepProcessorActor(UserInteraction step, TestCaseScope scope, String stepId) {
        super(step, scope, stepId);
    }

    @Override
    protected void init() throws Exception {
        String classifier = TestStepInputEventBus.getClassifier(scope.getContext().getSessionId(), stepId);
        TestStepInputEventBus
                .getInstance()
                .subscribe(self(), classifier);

        final ActorContext context = getContext();

        promise = Futures.promise();

        promise.future().onSuccess(new OnSuccess<TestStepReportType>() {
            @Override
            public void onSuccess(TestStepReportType result) throws Throwable {
                if(result != null) {
                    if (result.getResult() == TestResultType.SUCCESS) {
                        updateTestStepStatus(context, StepStatus.COMPLETED, result);
                    } else {
                        updateTestStepStatus(context, StepStatus.ERROR, result);
                    }
                } else {
                    updateTestStepStatus(context, StepStatus.COMPLETED, null);
                }
            }
        }, getContext().dispatcher());

        promise.future().onFailure(new OnFailure() {
            @Override
            public void onFailure(Throwable failure) throws Throwable {
                updateTestStepStatus(context, new ErrorStatusEvent(failure), null, true);
            }
        }, getContext().dispatcher());

    }

    @Override
    protected void start() throws Exception {
        processing();
        future = Futures.future(new Callable<TestStepReportType>() {
            @Override
            public TestStepReportType call() throws Exception {
                //Process the instructions and request the interaction from TestbedClient
                try {
                    VariableResolver variableResolver = new VariableResolver(scope);
                    List<InstructionOrRequest> instructionAndRequests = step.getInstructOrRequest();
                    UserInteractionRequest userInteractionRequest = new UserInteractionRequest();
                    String sutActorId = getSUTActor().getId();
                    if (StringUtils.isBlank(step.getWith())) {
                        userInteractionRequest.setWith(sutActorId);
                    } else {
                        userInteractionRequest.setWith(step.getWith());
                    }
                    int childStepId = 1;
                    for (InstructionOrRequest instructionOrRequest : instructionAndRequests) {
                        // Set the type in case this is missing.
                        if (StringUtils.isBlank(instructionOrRequest.getType())) {
                            if (instructionOrRequest.getContentType() == ValueEmbeddingEnumeration.BASE_64) {
                                // if the contentTYpe is set to BASE64 this will be a file.
                                instructionOrRequest.setType(DataType.BINARY_DATA_TYPE);
                            } else {
                                if (variableResolver.isVariableReference(instructionOrRequest.getValue())) {
                                    // If a target variable is referenced we can use this to determine the type.
                                    DataType targetVariable = variableResolver.resolveVariable(instructionOrRequest.getValue());
                                    instructionOrRequest.setType(targetVariable.getType());
                                } else {
                                    // Set "string" if no other type can be determined.
                                    instructionOrRequest.setType(DataType.STRING_DATA_TYPE);
                                }
                            }
                        }
                        // Set the default content type based on the type.
                        if (instructionOrRequest.getContentType() == null) {
                            if (DataType.isFileType(instructionOrRequest.getType())) {
                                instructionOrRequest.setContentType(ValueEmbeddingEnumeration.BASE_64);
                            } else {
                                instructionOrRequest.setContentType(ValueEmbeddingEnumeration.STRING);
                            }
                        }
                        if (StringUtils.isBlank(instructionOrRequest.getWith())) {
                            instructionOrRequest.setWith(userInteractionRequest.getWith());
                        }
                        //If it is an instruction
                        if (instructionOrRequest instanceof com.gitb.tdl.Instruction) {
                            // If no expression is specified consider it an empty expression.
                            if (StringUtils.isBlank(instructionOrRequest.getValue())) {
                                instructionOrRequest.setValue("''");
                            }
                            userInteractionRequest.getInstructionOrRequest().add(processInstruction(instructionOrRequest, "" + childStepId));
                        }
                        //If it is a request
                        else {
                            userInteractionRequest.getInstructionOrRequest().add(processRequest(instructionOrRequest, "" + childStepId));
                        }
                        childStepId++;
                    }
                    TestbedService.interactWithUsers(scope.getContext().getSessionId(), stepId, userInteractionRequest);
                    return null;
                } catch (Exception e) {
                    logger.error("Error in preliminary step", e);
                    throw new GITBEngineInternalError(e);
                }
            }
        }, getContext().system().dispatchers().lookup(ActorSystem.BLOCKING_DISPATCHER));
        future.onSuccess(new OnSuccess<TestStepReportType>() {

            @Override
            public void onSuccess(TestStepReportType result) throws Throwable { promise.trySuccess(result);
            }
        }, getContext().dispatcher());
        future.onFailure(new OnFailure() {
            @Override
            public void onFailure(Throwable failure) throws Throwable { promise.tryFailure(failure);
            }
        }, getContext().dispatcher());
        waiting();
    }

    @Override
    protected void stop() {

    }

    /**
     * Process TDL Instruction command and convert it to Instruction TBS request object
     *
     * @param instructionCommand command
     * @param stepId step id
     * @return instruction
     */
    private Instruction processInstruction(InstructionOrRequest instructionCommand, String stepId) throws Exception {
        Instruction instruction = new Instruction();
        instruction.setWith(instructionCommand.getWith());
        instruction.setDesc(instructionCommand.getDesc());
        instruction.setId(stepId);
        instruction.setName(instructionCommand.getName());
        instruction.setEncoding(instructionCommand.getEncoding());

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
    private InputRequest processRequest(InstructionOrRequest instructionCommand, String stepId) {
        InputRequest inputRequest = new InputRequest();
        inputRequest.setWith(instructionCommand.getWith());
        inputRequest.setDesc(instructionCommand.getDesc());
        inputRequest.setName(instructionCommand.getValue()); //name is provided from value node
        inputRequest.setContentType(instructionCommand.getContentType());
        if (instructionCommand.getContentType() == null) {
            inputRequest.setContentType(ValueEmbeddingEnumeration.STRING);
        }
        if (instructionCommand.getValue() != null && !instructionCommand.getValue().equals("")) {
            String assignedVariableExpression = instructionCommand.getValue();
            VariableResolver variableResolver = new VariableResolver(scope);
            DataType assignedVariable = variableResolver.resolveVariable(assignedVariableExpression);
            inputRequest.setType(assignedVariable.getType());
        } else {
            if (instructionCommand.getType() == null) {
                throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "The 'type' should be specified for InputRequest objects"));
            }
            inputRequest.setType(instructionCommand.getType());
        }
        inputRequest.setEncoding(instructionCommand.getEncoding());
        inputRequest.setId(stepId);
        return inputRequest;
    }


    @Override
    protected void handleInputEvent(InputEvent event) throws Exception {
        processing();
        List<UserInput> userInputs = event.getUserInputs();
        TestCaseScope.ScopedVariable scopedVariable = null;
        DataTypeFactory dataTypeFactory = DataTypeFactory.getInstance();
        //Create the Variable for Interaction Result if an id is given for the Interaction
        MapType interactionResult = (MapType) dataTypeFactory.create(DataType.MAP_DATA_TYPE);
        if (step.getId() != null) {
            scopedVariable = scope.createVariable(step.getId());
            scopedVariable.setValue(interactionResult);
        }
        //Assign the content for each input to either given variable or to the Interaction Map (with the given name as key)
        for (UserInput userInput : userInputs) {
            int stepIndex = Integer.parseInt(userInput.getId());
            InstructionOrRequest targetRequest = step.getInstructOrRequest().get(stepIndex - 1);
            if (targetRequest.getValue() != null && !targetRequest.getValue().equals("")) {
                //Find the variable that the given input content is assigned(bound) to
                String assignedVariableExpression = targetRequest.getValue();
                VariableResolver variableResolver = new VariableResolver(scope);
                DataType assignedVariable = variableResolver.resolveVariable(assignedVariableExpression);
                DataTypeUtils.setDataTypeValueWithAnyContent(assignedVariable, userInput);
            } else {
                //Create an empty value
                DataType assignedValue = dataTypeFactory.create(targetRequest.getType());
                DataTypeUtils.setDataTypeValueWithAnyContent(assignedValue, userInput);
                //Put it to the Interaction Result map
                if (targetRequest.getName() != null) {
                    interactionResult.addItem(targetRequest.getName(), assignedValue);
                }
            }
        }
        completed();
    }

    public static ActorRef create(ActorContext context, UserInteraction step, TestCaseScope scope, String stepId) throws Exception {
        return context.actorOf(props(InteractionStepProcessorActor.class, step, scope, stepId), getName(NAME));
    }
}
