package com.gitb.engine.actors.processors;

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
import com.gitb.engine.utils.StepContext;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.Process;
import com.gitb.tdl.*;
import com.gitb.tr.TestResultType;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pekko.actor.ActorRef;
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

    public SequenceProcessorActor(T sequence, TestCaseScope scope, String stepId, StepContext stepContext) {
        super(sequence, scope, stepId, stepContext);
        this.isRoot = stepContext == null;
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
        var stepContext = StepContext.from(step);
        child = switch (childStep) {
            case Send send -> SendStepProcessorActor.create(context, send, scope, childStepId, stepContext);
            case com.gitb.tdl.Receive receive -> ReceiveStepProcessorActor.create(context, receive, scope, childStepId, stepContext);
            case Listen listen -> ListenStepProcessorActor.create(context, listen, scope, childStepId, stepContext);
            case BeginTransaction beginTransaction -> BeginTransactionStepProcessorActor.create(context, beginTransaction, scope, childStepId, stepContext);
            case EndTransaction endTransaction -> EndTransactionStepProcessorActor.create(context, endTransaction, scope, childStepId, stepContext);
            case IfStep ifStep ->  IfStepProcessorActor.create(context, ifStep, scope, childStepId, stepContext);
            case WhileStep whileStep -> WhileStepProcessorActor.create(context, whileStep, scope, childStepId, stepContext);
            case RepeatUntilStep repeatUntilStep -> RepeatUntilStepProcessorActor.create(context, repeatUntilStep, scope, childStepId, stepContext);
            case ForEachStep forEachStep -> ForEachStepProcessorActor.create(context, forEachStep, scope, childStepId, stepContext);
            case FlowStep flowStep -> FlowStepProcessorActor.create(context, flowStep, scope, childStepId, stepContext);
            case ExitStep exitStep -> ExitStepProcessorActor.create(context, exitStep, scope, childStepId, stepContext);
            case Assign assign -> AssignStepProcessorActor.create(context, assign, scope, childStepId, stepContext);
            case Log log -> LogStepProcessorActor.create(context, log, scope, childStepId, stepContext);
            case Group group -> GroupStepProcessorActor.create(context, group, scope, childStepId, stepContext);
            case Verify verify -> VerifyStepProcessorActor.create(context, verify, scope, childStepId, stepContext);
            case CallStep callStep -> CallStepProcessorActor.create(context, callStep, scope, childStepId, stepContext);
            case UserInteraction userInteraction -> InteractionStepProcessorActor.create(context, userInteraction, scope, childStepId, stepContext);
            case BeginProcessingTransaction beginProcessingTransaction -> BeginProcessingTransactionStepProcessorActor.create(context, beginProcessingTransaction, scope, childStepId, stepContext);
            case Process process -> ProcessStepProcessorActor.create(context, process, scope, childStepId, stepContext);
            case EndProcessingTransaction endProcessingTransaction -> EndProcessingTransactionStepProcessorActor.create(context, endProcessingTransaction, scope, childStepId, stepContext);
            case null -> throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Unknown test step type!"));
            default -> throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Unknown test step type [" + childStep.getClass().getName() + "]!"));
        };
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
                if (status == StepStatus.ERROR && childStep instanceof TestConstruct construct) {
                    boolean stopExecution;
                    boolean skipNextStep;
                    if (Boolean.TRUE.equals(construct.isStopOnError())) {
                        if (Boolean.FALSE.equals(step.isStopOnChildError())) {
                            stopExecution = false;
                            skipNextStep = false;
                        } else if (Boolean.TRUE.equals(step.isStopOnChildError())) {
                            skipNextStep = true;
                            if (Boolean.FALSE.equals(stepContext.parentStopOnChildError())) {
                                stopExecution = false;
                            } else if (Boolean.TRUE.equals(stepContext.parentStopOnChildError())) {
                                stopExecution = Boolean.TRUE.equals(stepContext.parentStopOnError());
                            } else {
                                stopExecution = false;
                            }
                        } else {
                            stopExecution = true;
                            skipNextStep = true;
                        }
                    } else {
                        if (Boolean.TRUE.equals(step.isStopOnError())) {
                            if (Boolean.FALSE.equals(step.isStopOnChildError())) {
                                stopExecution = false;
                                skipNextStep = false;
                            } else if (Boolean.TRUE.equals(step.isStopOnChildError())) {
                                stopExecution = true;
                                skipNextStep = true;
                            } else {
                                stopExecution = true;
                                skipNextStep = true;
                            }
                        } else {
                            if (Boolean.TRUE.equals(step.isStopOnChildError())) {
                                stopExecution = false;
                                skipNextStep = true;
                            } else if (Boolean.FALSE.equals(step.isStopOnChildError())) {
                                stopExecution = false;
                                skipNextStep = false;
                            } else {
                                stopExecution = false;
                                skipNextStep = false;
                            }
                        }
                    }
                    if (stopExecution) {
                        scope.getContext().setCurrentState(TestCaseContext.TestCaseStateEnum.STOPPING);
                        try {
                            getContext().system().actorSelection(SessionActor.getPath(scope.getContext().getSessionId())).tell(new PrepareForStopCommand(scope.getContext().getSessionId(), self()), self());
                        } catch (Exception e) {
                            LOG.error(addMarker(), "Error sending the signal to stop the test session from test step actor [{}].", stepId);
                        }
                    } else if (!skipNextStep) {
                        nextStep = startTestStepAtIndex(completedStepIndex + 1);
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
    public static ActorRef create(ActorContext context, Sequence step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
        return create(SequenceProcessorActor.class, context, step, scope, stepId, stepContext);
    }
}
