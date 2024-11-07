package com.gitb.engine.actors.processors;

import org.apache.pekko.actor.ActorRef;
import com.gitb.core.ErrorCode;
import com.gitb.core.StepStatus;
import com.gitb.engine.actors.SessionActor;
import com.gitb.engine.commands.interaction.PrepareForStopCommand;
import com.gitb.engine.commands.interaction.StartCommand;
import com.gitb.engine.events.TestStepStatusEventBus;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.events.model.TestStepStatusEvent;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.Process;
import com.gitb.tdl.*;
import com.gitb.tr.TestResultType;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by serbay on 9/8/14.
 * <p>
 * Actor for processing sequence of test steps. Root class for many test constructs extending tdl:Sequence
 */
public class SequenceProcessorActor<T extends Sequence> extends AbstractTestStepActor<T> {
    public static final String NAME = "seq-p";
    private static final Logger LOG = LoggerFactory.getLogger(SequenceProcessorActor.class);
    private final boolean isRoot;

    private Map<Integer, Integer> childActorUidIndexMap;
    private Map<Integer, StepStatus> childStepStatuses;
    private Map<Integer, Object> childSteps;
    private List<Pair<Integer, Object>> childStepSpecs;

    public SequenceProcessorActor(T sequence, TestCaseScope scope, String stepId) {
        this(sequence, scope, stepId, false);
    }

    public SequenceProcessorActor(T sequence, TestCaseScope scope, String stepId, boolean isRoot) {
        super(sequence, scope, stepId);
        this.isRoot = isRoot;
    }

    @Override
    protected void init() throws Exception {

        this.childActorUidIndexMap = new ConcurrentHashMap<>();
        this.childStepStatuses = new ConcurrentHashMap<>();
        this.childSteps = new ConcurrentHashMap<>();
        this.childStepSpecs = Collections.synchronizedList(new ArrayList<>());

        if (step != null) {
            int reportedIndex = 1;
            for (Object childStep : step.getSteps()) {
                if (TestCaseUtils.shouldBeReported(childStep.getClass())) {
                    childStepSpecs.add(Pair.of(reportedIndex, childStep));
                    reportedIndex += 1;
                } else {
                    childStepSpecs.add(Pair.of(-1, childStep));
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
            if (scope.getContext().getCurrentState() != TestCaseContext.TestCaseStateEnum.STOPPING && scope.getContext().getCurrentState() != TestCaseContext.TestCaseStateEnum.STOPPED) {
                boolean childStopOnError = (childStep instanceof TestConstruct) && ((TestConstruct)childStep).isStopOnError() != null && ((TestConstruct)childStep).isStopOnError();
                if (childStopOnError && status == StepStatus.ERROR) {
                    // Stop processing and signal stop.
                    scope.getContext().setCurrentState(TestCaseContext.TestCaseStateEnum.STOPPING);
                    try {
                        getContext().system().actorSelection(SessionActor.getPath(scope.getContext().getSessionId())).tell(new PrepareForStopCommand(scope.getContext().getSessionId(), self()), self());
                    } catch (Exception e) {
                        LOG.error(addMarker(), "Error sending the signal to stop the test session from test step actor [{}].", stepId);
                    }
                } else {
                    // Proceed.
                    nextStep = startTestStepAtIndex(completedStepIndex + 1);
                }
            }
            if (nextStep == null) {
                // If there are remaining steps these should be marked as skipped.
                if ((completedStepIndex + 1) < childStepSpecs.size()) {
                    for (int i = completedStepIndex + 1; i < childStepSpecs.size(); i++) {
                        var pendingStep = childStepSpecs.get(i).getRight();
                        if (pendingStep instanceof TestConstruct && ((TestConstruct)pendingStep).getId() != null) {
                            TestCaseUtils.updateStepStatusMaps(getStepSuccessMap(), getStepStatusMap(), (TestConstruct) pendingStep, scope, StepStatus.SKIPPED);
                            var pendingEvent = new TestStepStatusEvent(scope.getContext().getSessionId(), ((TestConstruct)pendingStep).getId(), StepStatus.SKIPPED, constructDefaultReport(TestResultType.UNDEFINED), self(), pendingStep, scope);
                            TestStepStatusEventBus.getInstance().publish(pendingEvent);
                        }
                    }
                }
                completeStep();
            }
        }
    }

    void completeStep() {
        if (isRoot && scope.getContext().getForcedFinalResult() != null) {
            var finalStatus = StepStatus.COMPLETED;
            if (scope.getContext().getForcedFinalResult() == TestResultType.FAILURE) {
                finalStatus = StepStatus.ERROR;
            } else if (scope.getContext().getForcedFinalResult() == TestResultType.UNDEFINED) {
                finalStatus = StepStatus.SKIPPED;
            }
            updateTestStepStatus(finalStatus, null);
        } else {
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

    @Override
    public void postStop() throws Exception {
        super.postStop();
    }

    private ActorRef startTestStepAtIndex(int index) {
        ActorRef childActor = null;
        if (index < childStepSpecs.size()) {
            var childStepSpec = childStepSpecs.get(index);
            try {
                childActor = createChildActor(childStepSpec.getLeft(), childStepSpec.getRight());
                if (childActor != null) {
                    childActorUidIndexMap.put(childActor.path().uid(), index);
                    childSteps.put(childActor.path().uid(), childStepSpec.getRight());
                    childActor.tell(new StartCommand(scope.getContext().getSessionId()), self());
                }
            } catch (GITBEngineInternalError e) {
                throw e;
            } catch (Exception e) {
                throw new GITBEngineInternalError(e);
            }
        }
        return childActor;
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
