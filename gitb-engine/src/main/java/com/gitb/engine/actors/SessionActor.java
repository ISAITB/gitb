package com.gitb.engine.actors;

import com.gitb.engine.PropertyConstants;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Props;
import org.apache.pekko.dispatch.Futures;
import com.gitb.core.AnyContent;
import com.gitb.core.LogLevel;
import com.gitb.core.StepStatus;
import com.gitb.engine.SessionManager;
import com.gitb.engine.TestbedService;
import com.gitb.engine.actors.processors.TestCaseProcessorActor;
import com.gitb.engine.actors.supervisors.SessionSupervisor;
import com.gitb.engine.commands.interaction.*;
import com.gitb.engine.events.model.TestStepStatusEvent;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tbs.SUTConfiguration;
import com.gitb.tbs.TestStepStatus;
import com.gitb.tr.SR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.XMLDateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.gitb.engine.PropertyConstants.*;
import static com.gitb.engine.actors.processors.TestCaseProcessorActor.TEST_SESSION_END_EXTERNAL_STEP_ID;
import static com.gitb.engine.actors.processors.TestCaseProcessorActor.TEST_SESSION_END_STEP_ID;
import static com.gitb.engine.testcase.TestCaseContext.TestCaseStateEnum.*;

/**
 * Actor that controls the test execution session.
 */
public class SessionActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(SessionActor.class);

    @Override
    public Receive createReceive() {
        return active(new State(TestCaseProcessorActor.create(getContext(), getSessionId())));
    }

    private Receive active(State state) {
        return receiveBuilder()
                .match(LogCommand.class, (msg) -> withContext(msg, state, this::handleLogCommand))
                .match(SendUpdateCommand.class, (msg) -> withContext(msg, state, this::handleSendUpdateCommand))
                .match(UpdateSentEvent.class, (msg) -> withContext(msg, state, this::handleUpdateSentEvent))
                .match(ConnectionClosedEvent.class, (msg) -> withContext(msg, state, this::handleConnectionClosedEvent))
                .match(ConfigureCommand.class, (msg) -> withContext(msg, state, this::handleConfigureCommand))
                .match(StopCommand.class, (msg) -> withContext(msg, state, this::handleStopCommand))
                .match(InitiatePreliminaryCommand.class, (msg) -> withContext(msg, state, this::handleInitiatePreliminaryCommand))
                .match(TestStepStatusEvent.class, (msg) -> withContext(msg, state, this::handleTestStepStatusEvent))
                .match(StartCommand.class, (msg) -> withContext(msg, state, this::handleStartCommand))
                .match(PrepareForStopCommand.class, (msg) -> withContext(msg, state, this::handlePrepareForStopCommand))
                .match(TestSessionFinishedCommand.class, (msg) -> withContext(msg, state, this::handleTestSessionFinishedCommand))
                .match(UnexpectedErrorCommand.class, (msg) -> withContext(msg, state, this::handleUnexpectedErrorCommand))
                .match(RestartCommand.class, (msg) -> withContext(msg, state, this::handleRestartCommand))
                .match(SessionCleanupCommand.class, this::handleSessionCleanupCommand)
                .matchAny((msg) -> withContext(msg, state, this::handleUnexpected))
                .build();
    }

    private <T> void withContext(T message, State state, Consumer<Context<T>> handler) {
        if (!state.isCleanupPhase()) {
            TestCaseContext testCaseContext = SessionManager.getInstance().getContext(getSessionId());
            if (testCaseContext != null) {
                handler.accept(new Context<>(message, testCaseContext, state));
            }
        }
    }

    private <T> void allowForStates(Action handler, Context<T> ctx, TestCaseContext.TestCaseStateEnum... acceptedStates) {
        var currentState = ctx.getTestCaseContext().getCurrentState();
        if (acceptedStates != null) {
            boolean accepted = false;
            for (var acceptedState: acceptedStates) {
                if (currentState == acceptedState) {
                    handler.execute();
                    accepted = true;
                    break;
                }
            }
            if (!accepted) {
                unexpectedCommand(ctx.getMessage(), ctx.getTestCaseContext());
            }
        }
    }

    private State updateState(State newState) {
        getContext().become(active(newState), true);
        return newState;
    }

    private void handleLogCommand(Context<LogCommand> ctx) {
        if (shouldSignalLogEvent(ctx.getMessage(), ctx.getTestCaseContext())) {
            scheduleNextMessage(new UpdateMessage(ctx.getMessage().getTestStepStatus()), ctx);
        }
    }

    private void handleSendUpdateCommand(Context<SendUpdateCommand> ctx) {
        if (ctx.getTestCaseContext().getCurrentState() != TestCaseContext.TestCaseStateEnum.STOPPED) {
            if (ctx.getState().noEventBeingProcessed()) {
                if (!ctx.getState().getEventOutbox().isEmpty()) {
                    updateState(ctx.getState().newWithSetEventProcessingFlag(true));
                    sendUpdateSync(ctx.getState().getEventOutbox().values().iterator().next(), true);
                }
            }
        }
    }

    private void handleUpdateSentEvent(Context<UpdateSentEvent> ctx) {
        var activeState = updateState(ctx.getState().newWithRemovedEvent(ctx.getMessage().getEventUuid()));
        if (ctx.getTestCaseContext().getCurrentState() != TestCaseContext.TestCaseStateEnum.STOPPED) {
            activeState = updateState(activeState.newWithSetEventProcessingFlag(false));
            if (activeState.getSessionEndEvent() != null) {
                self().tell(new StopCommand(getSessionId()), self());
            } else {
                // Send the next update (if existing).
                self().tell(new SendUpdateCommand(), self());
            }
        }
    }

    private void handleConnectionClosedEvent(Context<ConnectionClosedEvent> ctx) {
        if (ctx.getTestCaseContext().getCurrentState() == TestCaseContext.TestCaseStateEnum.READY) {
            // In all other statuses we ignore a closed connection. In this case however we need to cleanup the test session.
            stopTestSession(ctx.getTestCaseContext(), ctx.getState(), null);
            logger.debug("Session ["+getSessionId()+"] stopped due to closed connection.");
        }
    }

    private void handleConfigureCommand(Context<ConfigureCommand> ctx) {
        allowForStates(() -> {
            var context = ctx.getTestCaseContext();
            try {
                List<SUTConfiguration> sutConfigurations = context.configure(
                        ctx.getMessage().getActorConfigurations(),
                        ctx.getMessage().getDomainConfiguration(),
                        ctx.getMessage().getOrganisationConfiguration(),
                        ctx.getMessage().getSystemConfiguration()
                );
                setInputsToSessionContext(context, ctx.getMessage().getInputs());
                if (context.getTestCase().getPreliminary() != null) {
                    context.setCurrentState(TestCaseContext.TestCaseStateEnum.CONFIGURATION);
                } else {
                    context.setCurrentState(TestCaseContext.TestCaseStateEnum.READY);
                }
                sendConfigurationResult(sutConfigurations);
            } catch (Exception e) {
                logger.warn("Error while preparing configuration for test session ["+getSessionId()+"]", e);
                logger.error(MarkerFactory.getDetachedMarker(getSessionId()), "Error while preparing test session configuration", e);
                sendConfigurationFailure(e);
            }
        }, ctx, IDLE);
    }

    private void handleStopCommand(Context<StopCommand> ctx) {
        allowForStates(() -> {
            UpdateMessage endMessage = ctx.getState().getSessionEndEvent();
            if (endMessage == null && ctx.getMessage().isExternalStop()) {
                endMessage = createSessionEndMessage(StepStatus.SKIPPED, null, true);
            }
            stopTestSession(ctx.getTestCaseContext(), ctx.getState(), endMessage);
            logger.debug("Session ["+getSessionId()+"] stopped in state ["+ctx.getTestCaseContext().getCurrentState()+"].");
        }, ctx, IDLE, READY, STOPPING, EXECUTION, OUTPUT);
    }

    private void handleInitiatePreliminaryCommand(Context<InitiatePreliminaryCommand> ctx) {
        allowForStates(() -> {
            for (ActorRef actorRef : getContext().getChildren()) {
                actorRef.tell(ctx.getMessage(), self());
            }
            ctx.getTestCaseContext().setCurrentState(TestCaseContext.TestCaseStateEnum.PRELIMINARY);
        }, ctx, CONFIGURATION);
    }

    private void handleTestStepStatusEvent(Context<TestStepStatusEvent> ctx) {
        allowForStates(() -> {
            var tcContext = ctx.getTestCaseContext();
            if (tcContext.getCurrentState() == PRELIMINARY) {
                var event = ctx.getMessage();
                if (event.getStepId().equals(TestCaseProcessorActor.PRELIMINARY_STEP_ID) && event.getStatus() == StepStatus.COMPLETED) {
                    tcContext.setCurrentState(TestCaseContext.TestCaseStateEnum.READY);
                }
                scheduleNextMessage(prepareStatusUpdate(event), ctx);
            } else if (tcContext.getCurrentState() == STOPPED) {
                ignoredStatusUpdate(ctx.getMessage());
            } else {
                scheduleNextMessage(prepareStatusUpdate(ctx.getMessage()), ctx);
            }
        }, ctx, PRELIMINARY, STOPPING, EXECUTION, OUTPUT, STOPPED);
    }

    private void handleStartCommand(Context<StartCommand> ctx) {
        allowForStates(() -> {
            ctx.getState().getTestCaseProcessorActor().tell(ctx.getMessage(), self());
            ctx.getTestCaseContext().setCurrentState(TestCaseContext.TestCaseStateEnum.EXECUTION);
        }, ctx, READY);
    }

    private void handlePrepareForStopCommand(Context<PrepareForStopCommand> ctx) {
        allowForStates(() -> {
            ctx.getTestCaseContext().setCurrentState(TestCaseContext.TestCaseStateEnum.STOPPING);
            ctx.getState().getTestCaseProcessorActor().tell(ctx.getMessage(), self());
        }, ctx, STOPPING, EXECUTION, OUTPUT);
    }

    private void handleTestSessionFinishedCommand(Context<TestSessionFinishedCommand> ctx) {
        allowForStates(() -> {
            logger.info(MarkerFactory.getDetachedMarker(getSessionId()), String.format("Session finished with result [%s]", ctx.getMessage().getStatus()));
            var endEvent = createSessionEndMessage(ctx.getMessage().getStatus(), ctx.getMessage().getResultReport(), false);
            var activeState = updateState(ctx.getState().newWithSessionEndEvent(endEvent));
            if (activeState.noEventBeingProcessed()) {
                self().tell(new StopCommand(getSessionId()), self());
            }
        }, ctx, STOPPING, EXECUTION, OUTPUT);
    }

    private void handleUnexpectedErrorCommand(Context<UnexpectedErrorCommand> ctx) {
        allowForStates(() -> self().tell(prepareStatusUpdate(getSessionId(), null, StepStatus.ERROR, null, true, null), self()), ctx, STOPPING, EXECUTION, OUTPUT);
    }

    private void handleRestartCommand(Context<RestartCommand> ctx) {
        allowForStates(() -> ctx.getState().getTestCaseProcessorActor().tell(ctx.getMessage(), self()), ctx, STOPPED);
    }

    private void handleSessionCleanupCommand(SessionCleanupCommand message) {
        var sessionEndEvent = (message.getSessionEndMessage() == null)?createSessionEndMessage(StepStatus.SKIPPED, null, false):message.getSessionEndMessage();
        logger.debug("Signalling end of session ["+getSessionId()+"]");
        Futures.future(() -> {
            try {
                for (UpdateMessage msg: message.getPendingUpdates()) {
                    sendUpdateSync(msg, false);
                }
            } finally {
                Await.result(Futures.future(() -> {
                    sendUpdateSync(sessionEndEvent, false);
                    return null;
                }, getContext().system().dispatchers().lookup(ActorSystem.BLOCKING_DISPATCHER)), Duration.apply(100, TimeUnit.MILLISECONDS));
            }
            return null;
        }, getContext().system().dispatchers().lookup(ActorSystem.BLOCKING_DISPATCHER));
        self().tell(PoisonPill.getInstance(), self());
    }

    private void handleUnexpected(Context<?> ctx) {
        unexpectedCommand(ctx.getMessage(), ctx.getTestCaseContext());
    }

    private void scheduleNextMessage(UpdateMessage message, Context<?> ctx) {
        updateState(ctx.getState().newWithNewEvent(message));
        if (ctx.getTestCaseContext().getCurrentState() != TestCaseContext.TestCaseStateEnum.STOPPED) {
            // After stopping, any such updates will be handled in the final cleanup.
            self().tell(new SendUpdateCommand(), self());
        }
    }

    private String getSessionId() {
        return self().path().name();
    }

    private void setInputsToSessionContext(TestCaseContext context, List<AnyContent> inputs) {
        if (inputs != null) {
            for (var input: inputs) {
                if (input != null) {
                    if (input.getName() == null) {
                        logger.warn("Session ["+getSessionId()+"] received input with no name");
                    } else if (input.getName().equals(DOMAIN_MAP) || input.getName().equals(ORGANISATION_MAP) || input.getName().equals(SYSTEM_MAP) || input.getName().equals(SESSION_MAP)) {
                        logger.warn("Session ["+getSessionId()+"] received input with reserved name ["+input.getName()+"]");
                    } else {
                        // Add the input to the scope. Note that this may override existing (a) actor configs, (b) imports, or (c) variables
                        var variable = context.getScope().createVariable(input.getName());
                        variable.setValue(DataTypeUtils.convertAnyContentToDataType(input));
                    }
                }
            }
        }
    }

    private boolean shouldSignalLogEvent(LogCommand message, TestCaseContext context) {
        var logLevel = context.getLogLevelToSignal();
        return (logLevel == LogLevel.DEBUG) ||
                (logLevel == LogLevel.INFO && message.getLogLevel() != LogLevel.DEBUG) ||
                (logLevel == LogLevel.WARNING && (message.getLogLevel() == LogLevel.ERROR || message.getLogLevel() == LogLevel.WARNING)) ||
                (logLevel == LogLevel.ERROR && message.getLogLevel() == LogLevel.ERROR);
    }

    private void stopTestSession(TestCaseContext context, State state, UpdateMessage sessionEndMessage) {
        context.setCurrentState(TestCaseContext.TestCaseStateEnum.STOPPED);
        SessionManager.getInstance().endSession(getSessionId());
        updateState(state.newForCleanupPhase());
        self().tell(new SessionCleanupCommand(sessionEndMessage, state.getEventOutbox().values()), self());
    }

    private UpdateMessage createSessionEndMessage(StepStatus result, TestStepReportType report, boolean isExternallyTriggered) {
        var stepIdToUse = isExternallyTriggered?TEST_SESSION_END_EXTERNAL_STEP_ID:TEST_SESSION_END_STEP_ID;
        return prepareStatusUpdate(getSessionId(), stepIdToUse, result, report, false, null);
    }

    private void sendConfigurationFailure(final Throwable error) {
        Futures.future(() -> {
            try {
                TestbedService.sendConfigurationFailure(getSessionId(), error);
            } catch (Exception e) {
                logger.error(MarkerFactory.getDetachedMarker(getSessionId()), "Error while sending configuration failure update", e);
            }
            return null;
        }, getContext().system().dispatchers().lookup(ActorSystem.BLOCKING_DISPATCHER));
    }

    private void sendConfigurationResult(final List<SUTConfiguration> sutConfigurations) {
        Futures.future(() -> {
            try {
                TestbedService.sendConfigurationResult(getSessionId(), sutConfigurations);
            } catch (Exception e) {
                logger.error(MarkerFactory.getDetachedMarker(getSessionId()), "Error while sending configuration completion update", e);
            }
            return null;
        }, getContext().system().dispatchers().lookup(ActorSystem.BLOCKING_DISPATCHER));
    }

    private void sendUpdateSync(final UpdateMessage msg, boolean sendConfirmation) {
        try {
            if (msg == null) {
                throw new IllegalArgumentException("Provided message was null");
            }
            TestbedService.sendStatusUpdate(getSessionId(), msg.getStatusMessage());
        } catch (Exception e) {
            if (msg != null) {
                if (PropertyConstants.LOG_EVENT_STEP_ID.equals(msg.getStatusMessage().getStepId())) {
                    logger.warn(MarkerFactory.getDetachedMarker(getSessionId()), "Error while recording log message in test session log", e);
                } else {
                    logger.warn(MarkerFactory.getDetachedMarker(getSessionId()), String.format("Error while sending update for message [%s] - step ID [%s] - status [%s])", msg.getUuid(), msg.getStatusMessage().getStepId(), msg.getStatusMessage().getStatus()), e);
                }
            } else {
                logger.warn(MarkerFactory.getDetachedMarker(getSessionId()), "Error while sending update - message was null", e);
            }
        } finally {
            if (sendConfirmation) {
                if (msg != null) {
                    self().tell(new UpdateSentEvent(msg.getUuid()), self());
                }
            }
        }
    }

    private UpdateMessage prepareStatusUpdate(TestStepStatusEvent event) {
        return prepareStatusUpdate(event.getSessionId(), event.getStepId(), event.getStatus(), event.getReport(), true, TestCaseUtils.extractStepDescription(event.getStep(), event.getScope()));
    }

    private UpdateMessage prepareStatusUpdate(String sessionId, String stepId, StepStatus status, TestStepReportType report, boolean sendLogMessage, String stepDescription) {
        if (sendLogMessage) {
            if (stepId != null && stepDescription != null) {
                logger.debug(MarkerFactory.getDetachedMarker(sessionId), String.format("Status update - step [%s] - ID [%s]: %s", stepDescription, stepId, status));
            } else {
                logger.debug(MarkerFactory.getDetachedMarker(sessionId), String.format("Status update: %s", status));
            }
        } else {
            logger.debug(String.format("updateStatus (%s, %s , %s) ", sessionId, stepId, status));
        }
        if (report == null && (status == StepStatus.COMPLETED || status == StepStatus.ERROR || status == StepStatus.SKIPPED || status == StepStatus.WARNING)) {
            report = new SR();
            try {
                report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
            } catch (DatatypeConfigurationException e) {
                throw new IllegalStateException(e);
            }
            if (status == StepStatus.COMPLETED) {
                report.setResult(TestResultType.SUCCESS);
            } else if (status == StepStatus.ERROR) {
                report.setResult(TestResultType.FAILURE);
            } else if (status == StepStatus.WARNING) {
                report.setResult(TestResultType.WARNING);
            } else {
                report.setResult(TestResultType.UNDEFINED);
            }
        }
        TestStepStatus testStepStatus = new TestStepStatus();
        testStepStatus.setTcInstanceId(sessionId);
        testStepStatus.setStepId(stepId);
        testStepStatus.setStatus(status);
        testStepStatus.setReport(report);
        return new UpdateMessage(testStepStatus);
    }

    private void ignoredStatusUpdate(TestStepStatusEvent message) {
        if (message.getStep() != null && message.getStepId() != null) {
            logger.debug(MarkerFactory.getDetachedMarker(message.getSessionId()), String.format("Ignoring status update - step [%s]- ID [%s]", TestCaseUtils.extractStepDescription(message.getStep(), message.getScope()), message.getStepId()));
        } else {
            logger.debug(MarkerFactory.getDetachedMarker(message.getSessionId()), String.format("Ignoring status update [%s]", message));
        }
	}

	private void unexpectedCommand(Object message, TestCaseContext context) {
        logger.error(MarkerFactory.getDetachedMarker(context.getSessionId()), "Invalid command [" + message.getClass().getName() + "] in state [" + context.getCurrentState() + "]");
        throw new GITBEngineInternalError("Invalid command [" + message.getClass().getName() + "] in state [" + context.getCurrentState() + "]");
    }

    /**
     * Create the ActorRef object for this Session Actor
     *
     * @param context context
     * @param sessionId session id
     * @return actor reference
     */
    public static ActorRef create(ActorContext context, String sessionId) {
        return context.actorOf(Props.create(SessionActor.class), sessionId);
    }

    /**
     * Construct the actor system path string for this Session Actor given the session id
     *
     * @param sessionId session id
     * @return session actor path
     */
    public static String getPath(String sessionId) {
        return "/user/" + SessionSupervisor.NAME + "/" + sessionId;
    }

    static class State {

        private final ActorRef testCaseProcessorActor;
        private final LinkedHashMap<String, UpdateMessage> eventOutbox;
        private final boolean eventProcessingInProgress;
        private final UpdateMessage sessionEndEvent;
        private final boolean cleanupPhase;

        State(ActorRef testCaseProcessorActor) {
            // Empty initializer for default state.
            this(testCaseProcessorActor, new LinkedHashMap<>(), false, null, false);
        }

        private State(ActorRef testCaseProcessorActor, LinkedHashMap<String, UpdateMessage> eventOutbox, boolean eventProcessingInProgress, UpdateMessage sessionEndEvent, boolean cleanupPhase) {
            this.testCaseProcessorActor = testCaseProcessorActor;
            this.eventOutbox = eventOutbox;
            this.eventProcessingInProgress = eventProcessingInProgress;
            this.sessionEndEvent = sessionEndEvent;
            this.cleanupPhase = cleanupPhase;
        }

        ActorRef getTestCaseProcessorActor() {
            return testCaseProcessorActor;
        }

        LinkedHashMap<String, UpdateMessage> getEventOutbox() {
            return eventOutbox;
        }

        boolean noEventBeingProcessed() {
            return !eventProcessingInProgress;
        }

        UpdateMessage getSessionEndEvent() {
            return sessionEndEvent;
        }

        boolean isCleanupPhase() {
            return cleanupPhase;
        }

        State newWithNewEvent(UpdateMessage message) {
            var newOutbox = new LinkedHashMap<>(eventOutbox);
            newOutbox.put(message.getUuid(), message);
            return new State(testCaseProcessorActor, newOutbox, eventProcessingInProgress, sessionEndEvent, cleanupPhase);
        }

        State newWithSetEventProcessingFlag(boolean eventProcessingInProgress) {
            return new State(testCaseProcessorActor, eventOutbox, eventProcessingInProgress, sessionEndEvent, cleanupPhase);
        }

        State newWithRemovedEvent(String uuidToRemove) {
            var newOutbox = new LinkedHashMap<>(eventOutbox);
            newOutbox.remove(uuidToRemove);
            return new State(testCaseProcessorActor, newOutbox, eventProcessingInProgress, sessionEndEvent, cleanupPhase);
        }

        State newWithSessionEndEvent(UpdateMessage sessionEndEvent) {
            return new State(testCaseProcessorActor, eventOutbox, eventProcessingInProgress, sessionEndEvent, cleanupPhase);
        }

        State newForCleanupPhase() {
            return new State(testCaseProcessorActor, eventOutbox, eventProcessingInProgress, sessionEndEvent, true);
        }

    }

    static class Context <T> {

        private final T message;
        private final TestCaseContext testCaseContext;
        private final State state;

        Context(T message, TestCaseContext testCaseContext, State state) {
            this.message = message;
            this.testCaseContext = testCaseContext;
            this.state = state;
        }

        T getMessage() {
            return message;
        }

        TestCaseContext getTestCaseContext() {
            return testCaseContext;
        }

        State getState() {
            return state;
        }
    }

    @FunctionalInterface
    interface Action {
        void execute();
    }
}
