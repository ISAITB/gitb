package com.gitb.engine.actors.processors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import com.gitb.core.ErrorCode;
import com.gitb.core.StepStatus;
import com.gitb.engine.commands.interaction.StartCommand;
import com.gitb.engine.commands.interaction.StopCommand;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.*;
import com.gitb.tdl.Process;
import com.gitb.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by serbay on 9/8/14.
 * <p>
 * Actor for processing sequence of test steps. Root class for many test constructs extending tdl:Sequence
 */
public class SequenceProcessorActor<T extends Sequence> extends AbstractTestStepActor<T> {
    private static Logger logger = LoggerFactory.getLogger(SequenceProcessorActor.class);
    public static final String NAME = "seq-p";

    private Map<Integer, Integer> childActorUidIndexMap;
    private Map<Integer, ActorRef> childStepIndexActorMap;
    private Map<Integer, StepStatus> childStepStatuses;
    private boolean stopping = false;

    public SequenceProcessorActor(T sequence, TestCaseScope scope) {
        super(sequence, scope);
    }

    public SequenceProcessorActor(T sequence, TestCaseScope scope, String stepId) {
        super(sequence, scope, stepId);
    }

    @Override
    protected void init() throws Exception {

        this.childActorUidIndexMap = new ConcurrentHashMap<>();
        this.childStepIndexActorMap = new ConcurrentHashMap<>();
        this.childStepStatuses = new ConcurrentHashMap<>();

        if (step != null) {
            int realIndex = 0;
            int reportedIndex = 1;
            for (Object childStep : step.getSteps()) {

                ActorRef child;

                if (TestCaseUtils.shouldBeReported(childStep.getClass())) {
                    child = createChildActor(reportedIndex, childStep);
                    reportedIndex++;
                } else {
                    child = createChildActor(-1, childStep);
                }


                if (child != null) {
                    childActorUidIndexMap.put(child.path().uid(), realIndex);
                    childStepIndexActorMap.put(realIndex, child);
                    realIndex++;
                }
            }
        }
    }

    private ActorRef createChildActor(int reportedIndex, Object childStep) throws Exception {
        ActorContext context = getContext();
        ActorRef child;
        String childStepId;
        if (reportedIndex != -1) { // report the status
            childStepId = stepId.isEmpty()
                    ? Integer.toString(reportedIndex)
                    : stepId + STEP_SEPARATOR + reportedIndex;
        } else {
            childStepId = "";
        }

        if (childStep instanceof Send) {
            child = SendStepProcessorActor.create(context, (Send) childStep, scope, childStepId);
        } else if (childStep instanceof Receive) {
            child = ReceiveStepProcessorActor.create(context, (Receive) childStep, scope, childStepId);
        } else if (childStep instanceof Listen) {
            child = ListenStepProcessorActor.create(context, (Listen) childStep, scope, childStepId);
        } else if (childStep instanceof BeginTransaction) {
            child = BeginTransactionStepProcessorActor.create(context, (BeginTransaction) childStep, scope, childStepId);
        } else if (childStep instanceof EndTransaction) {
            child = EndTransactionStepProcessorActor.create(context, (EndTransaction) childStep, scope, childStepId);
        } else if (childStep instanceof IfStep) {
            child = IfStepProcessorActor.create(context, (IfStep) childStep, scope, childStepId);
        } else if (childStep instanceof WhileStep) {
            child = WhileStepProcessorActor.create(context, (WhileStep) childStep, scope, childStepId);
        } else if (childStep instanceof RepeatUntilStep) {
            child = RepeatUntilStepProcessorActor.create(context, (RepeatUntilStep) childStep, scope, childStepId);
        } else if (childStep instanceof ForEachStep) {
            child = ForEachStepProcessorActor.create(context, (ForEachStep) childStep, scope, childStepId);
        } else if (childStep instanceof FlowStep) {
            child = FlowStepProcessorActor.create(context, (FlowStep) childStep, scope, childStepId);
        } else if (childStep instanceof ExitStep) {
            child = ExitStepProcessorActor.create(context, (ExitStep) childStep, scope, childStepId);
        } else if (childStep instanceof Assign) {
            child = AssignStepProcessorActor.create(context, (Assign) childStep, scope, childStepId);
        } else if (childStep instanceof Group) {
            child = GroupStepProcessorActor.create(context, (Group) childStep, scope, childStepId);
        } else if (childStep instanceof Verify) {
            child = VerifyStepProcessorActor.create(context, (Verify) childStep, scope, childStepId);
        } else if (childStep instanceof CallStep) {
            child = CallStepProcessorActor.create(context, (CallStep) childStep, scope, childStepId);
        } else if (childStep instanceof UserInteraction) {
            child = InteractionStepProcessorActor.create(context, (UserInteraction) childStep, scope, childStepId);
        } else if (childStep instanceof BeginProcessingTransaction) {
            child = BeginProcessingTransactionStepProcessorActor.create(context, (BeginProcessingTransaction) childStep, scope, childStepId);
        } else if (childStep instanceof Process) {
            child = ProcessStepProcessorActor.create(context, (Process) childStep, scope, childStepId);
        } else if (childStep instanceof EndProcessingTransaction) {
            child = EndProcessingTransactionStepProcessorActor.create(context, (EndProcessingTransaction) childStep, scope, childStepId);
        } else {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Unknown test step type [" + childStep.getClass().getName() + "]!"));
        }
        return child;
    }

    @Override
    protected void start() {
        processing();
        startTestStepAtIndex(0);
    }

    @Override
    protected void reactToPrepareForStop() {
        stopping = true;
    }

    @Override
    protected void stop() {
        StopCommand command = new StopCommand(scope.getContext().getSessionId());
        for (ActorRef child : getContext().getChildren()) {
            child.tell(command, self());
        }
        childActorUidIndexMap.clear();
        childStepIndexActorMap.clear();
    }

    @Override
    protected void handleStatusEvent(StatusEvent event) throws Exception {
//		super.handleStatusEvent(event);

        StepStatus status = event.getStatus();
        //If a step is completed, continue from next step
        int senderUid = getSender().path().uid();
        int completedStepIndex = childActorUidIndexMap.get(senderUid);

        childStepStatuses.put(completedStepIndex, status);

//		logger.debug("["+stepId+"] received a status event from its child step with index ["+completedStepIndex+"]: " + event);

        if (status == StepStatus.COMPLETED
                || status == StepStatus.ERROR
                || status == StepStatus.SKIPPED) {

            ActorRef nextStep = null;
            if (!stopping) {
                nextStep = startTestStepAtIndex(completedStepIndex + 1);
            }
            if (nextStep == null) {
                boolean childrenHasError = false;
                for (Map.Entry<Integer, StepStatus> childStepStatus : childStepStatuses.entrySet()) {
                    if (childStepStatus.getValue() == StepStatus.ERROR) {
                        childrenHasError = true;
                        break;
                    }
                }
                if (childrenHasError) {
//					logger.debug("["+stepId+"] has no child steps left to run. Sending error status report to parent.");
                    childrenHasError();
                } else {
//					logger.debug("["+stepId+"] has no child steps left to run. Sending completed status report to parent.");
                    completed();
                }
            }
        }
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
    }

    private ActorRef startTestStepAtIndex(int index) {
        ActorRef childStep = childStepIndexActorMap.get(index);
        if (childStep != null) {
//			logger.debug("["+stepId+"] starting child step with index ["+index+"]");
            childStep.tell(new StartCommand(scope.getContext().getSessionId()), self());
        }
        return childStep;
    }

    /**
     * Create the actor reference for the Sequence Processor Actor
     *
     * @param context context
     * @param step    test step
     * @param scope   scope
     * @param stepId  step id
     * @return sequence step actor reference
     */
    public static ActorRef create(ActorContext context, Sequence step, TestCaseScope scope, String stepId) throws Exception {
        return create(SequenceProcessorActor.class, context, step, scope, stepId);
    }
}
