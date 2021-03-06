package com.gitb.engine.actors.processors;

import akka.actor.ActorRef;
import com.gitb.core.ErrorCode;
import com.gitb.core.StepStatus;
import com.gitb.engine.commands.interaction.StartCommand;
import com.gitb.engine.commands.interaction.StopCommand;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.Process;
import com.gitb.tdl.*;
import com.gitb.utils.ErrorUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by serbay on 9/8/14.
 * <p>
 * Actor for processing sequence of test steps. Root class for many test constructs extending tdl:Sequence
 */
public class SequenceProcessorActor<T extends Sequence> extends AbstractTestStepActor<T> {
    public static final String NAME = "seq-p";

    private Map<Integer, Integer> childActorUidIndexMap;
    private Map<Integer, ActorRef> childStepIndexActorMap;
    private Map<Integer, StepStatus> childStepStatuses;
    private Map<Integer, Object> childSteps;

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
        this.childSteps = new ConcurrentHashMap<>();

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
                    childSteps.put(child.path().uid(), childStep);
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
        } else if (childStep instanceof com.gitb.tdl.Receive) {
            child = ReceiveStepProcessorActor.create(context, (com.gitb.tdl.Receive) childStep, scope, childStepId);
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
        } else if (childStep instanceof Log) {
            child = LogStepProcessorActor.create(context, (Log) childStep, scope, childStepId);
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
    protected void handleStatusEvent(StatusEvent event) throws Exception {
        if (scope.getContext().getCurrentState() == TestCaseContext.TestCaseStateEnum.STOPPED) {
            return;
        }
        StepStatus status = event.getStatus();
        //If a step is completed, continue from next step
        int senderUid = getSender().path().uid();
        int completedStepIndex = childActorUidIndexMap.get(senderUid);
        Object childStep = childSteps.get(senderUid);

        childStepStatuses.put(completedStepIndex, status);

        if (status == StepStatus.COMPLETED
                || status == StepStatus.WARNING
                || status == StepStatus.ERROR
                || status == StepStatus.SKIPPED) {

            ActorRef nextStep = null;
            boolean loadNextStep = true;
            if (scope.getContext().getCurrentState() == TestCaseContext.TestCaseStateEnum.STOPPING
                    || (status == StepStatus.ERROR && (
                            (childStep instanceof TestConstruct && ((TestConstruct) childStep).isStopOnError())
                                    || step.isStopOnError()
                        )
                    )
            ) {
                loadNextStep = false;
            }
            if (loadNextStep) {
                nextStep = startTestStepAtIndex(completedStepIndex + 1);
            }
            if (nextStep == null) {
                boolean childrenHasError = false;
                boolean childrenHasWarning = false;
                for (Map.Entry<Integer, StepStatus> childStepStatus : childStepStatuses.entrySet()) {
                    if (childStepStatus.getValue() == StepStatus.ERROR) {
                        childrenHasError = true;
                        break;
                    } else if (childStepStatus.getValue() == StepStatus.WARNING) {
                        childrenHasWarning = true;
                    }
                }
                if (childrenHasError) {
                    childrenHasError();
                } else if (childrenHasWarning) {
                    childrenHasWarning();
                } else {
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
