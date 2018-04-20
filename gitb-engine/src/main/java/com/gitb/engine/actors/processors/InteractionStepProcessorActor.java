package com.gitb.engine.actors.processors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.dispatch.Futures;
import com.gitb.core.ErrorCode;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.engine.TestbedService;
import com.gitb.engine.actors.ActorSystem;
import com.gitb.engine.events.TestStepInputEventBus;
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
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.MapType;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public InteractionStepProcessorActor(UserInteraction step, TestCaseScope scope, String stepId) {
        super(step, scope, stepId);
    }

    @Override
    protected void init() throws Exception {
        String classifier = TestStepInputEventBus.getClassifier(scope.getContext().getSessionId(), stepId);
        TestStepInputEventBus
                .getInstance()
                .subscribe(self(), classifier);
    }

    @Override
    protected void start() throws Exception {
        processing();
        Futures.future(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                //Process the instructions and request the interaction from TestbedClient
                List<InstructionOrRequest> instructionAndRequests = step.getInstructOrRequest();
                UserInteractionRequest userInteractionRequest = new UserInteractionRequest();
                userInteractionRequest.setWith(step.getWith());
                int childStepId = 1;
                for (InstructionOrRequest instructionOrRequest : instructionAndRequests) {
                    if (StringUtils.isBlank(instructionOrRequest.getType())) {
                        // Consider "string" as the default type if not specified
                        instructionOrRequest.setType(DataType.STRING_DATA_TYPE);
                    }
                    //At least one of 'with's must be specified in test description
                    if(userInteractionRequest.getWith() == null && instructionOrRequest.getWith() == null) {
                        throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "At least one of 'with's should be specified in UserInteraction step"));
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
            }
        }, getContext().system().dispatchers().lookup(ActorSystem.BLOCKING_DISPATCHER));
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
            if (instructionCommand.getType() == null)
                throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "The 'type' should be specified for InputRequest objects"));
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
            if (DataType.STRING_DATA_TYPE.equals(userInput.getType())) {
                userInput.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
            }
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
