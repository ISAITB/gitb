package com.gitb.engine.actors;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.dispatch.Futures;
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
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tbs.SUTConfiguration;
import com.gitb.tbs.TestStepStatus;
import com.gitb.tr.SR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.ErrorUtils;
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

import static com.gitb.engine.actors.processors.TestCaseProcessorActor.TEST_CASE_STEP_ID;

/**
 * Actor that controls the test execution session.
 */
public class SessionActor extends Actor {
    private static final Logger logger = LoggerFactory.getLogger(SessionActor.class);

    private String getSessionId() {
        return self().path().name();
    }

    private ActorRef testCaseProcessorActor;

    private final LinkedHashMap<String, UpdateMessage> eventOutbox = new LinkedHashMap<>();
    private boolean eventProcessingInProgress = false;
    private UpdateMessage sessionEndEvent = null;

    @Override
    public void preStart() throws Exception {
        testCaseProcessorActor = TestCaseProcessorActor.create(getContext(), getSessionId());
    }

    @Override
    public void onReceive(Object message) throws Exception {
        super.onReceive(message);
        TestCaseContext context = SessionManager
                .getInstance()
                .getContext(getSessionId());
        if (context != null) {
            if (message instanceof LogCommand) {
                if (shouldSignalLogEvent((LogCommand)message, context)) {
                    var msg = new UpdateMessage(((LogCommand) message).getTestStepStatus());
                    eventOutbox.put(msg.getUuid(), msg);
                    if (context.getCurrentState() != TestCaseContext.TestCaseStateEnum.STOPPED) {
                        // After stopping, any such updates will be handled in the final cleanup.
                        self().tell(new SendUpdateCommand(), self());
                    }
                }
            } else if (message instanceof SendUpdateCommand) {
                if (context.getCurrentState() != TestCaseContext.TestCaseStateEnum.STOPPED) {
                    if (!eventProcessingInProgress) {
                        eventProcessingInProgress = true;
                        sendUpdateSync(eventOutbox.values().iterator().next(), true);
                    }
                }
            } else if (message instanceof UpdateSentEvent) {
                eventOutbox.remove(((UpdateSentEvent) message).getEventUuid());
                if (context.getCurrentState() != TestCaseContext.TestCaseStateEnum.STOPPED) {
                    if (eventOutbox.isEmpty()) {
                        eventProcessingInProgress = false;
                        if (sessionEndEvent != null) {
                            self().tell(new StopCommand(getSessionId()), self());
                        }
                    } else {
                        sendUpdateSync(eventOutbox.values().iterator().next(), true);
                    }
                }
            } else if (message instanceof ConnectionClosedEvent) {
                if (context.getCurrentState() == TestCaseContext.TestCaseStateEnum.READY) {
                    // In all other statuses we ignore a closed connection. In this case however we need to cleanup the test session.
                    stopTestSession(context);
                    logger.debug("Session ["+getSessionId()+"] stopped due to closed connection.");
                }
            } else {
                switch (context.getCurrentState()) {
                    case IDLE:
                        if (message instanceof ConfigureCommand) {
                            try {
                                List<SUTConfiguration> sutConfigurations = context.configure(
                                        ((ConfigureCommand) message).getActorConfigurations(),
                                        ((ConfigureCommand) message).getDomainConfiguration(),
                                        ((ConfigureCommand) message).getOrganisationConfiguration(),
                                        ((ConfigureCommand) message).getSystemConfiguration()
                                );
                                setInputsToSessionContext(context, ((ConfigureCommand) message).getInputs());
                                sendConfigurationResult(sutConfigurations);
                                if (context.getTestCase().getPreliminary() != null) {
                                    context.setCurrentState(TestCaseContext.TestCaseStateEnum.CONFIGURATION);
                                } else {
                                    context.setCurrentState(TestCaseContext.TestCaseStateEnum.READY);
                                }
                            } catch (Exception e) {
                                logger.warn("Error while preparing configuration for test session ["+getSessionId()+"]", e);
                                logger.error(MarkerFactory.getDetachedMarker(getSessionId()), "Error while preparing test session configuration", e);
                                sendConfigurationFailure(e);
                            }
                        } else if (message instanceof StopCommand) {
                            stopTestSession(context);
                            logger.debug("Session ["+getSessionId()+"] stopped before having started.");
                        } else {
                            unexpectedCommand(message, context);
                        }
                        break;
                    case CONFIGURATION:
                        if (message instanceof InitiatePreliminaryCommand) {
                            for (ActorRef actorRef : getContext().getChildren()) {
                                actorRef.tell(message, self());
                            }
                            context.setCurrentState(TestCaseContext.TestCaseStateEnum.PRELIMINARY);
                        } else {
                            unexpectedCommand(message, context);
                        }
                        break;
                    case PRELIMINARY:
                        if (message instanceof TestStepStatusEvent) {
                            TestStepStatusEvent event = (TestStepStatusEvent) message;
                            if (event.getStepId().equals(TestCaseProcessorActor.PRELIMINARY_STEP_ID) && event.getStatus() == StepStatus.COMPLETED) {
                                context.setCurrentState(TestCaseContext.TestCaseStateEnum.READY);
                            }
                            var msg = prepareStatusUpdate(event);
                            eventOutbox.put(msg.getUuid(), msg);
                            self().tell(new SendUpdateCommand(), self());
                        } else {
                            unexpectedCommand(message, context);
                        }
                        break;
                    case READY:
                        if (message instanceof StartCommand) {
                            testCaseProcessorActor.tell(message, self());
                            context.setCurrentState(TestCaseContext.TestCaseStateEnum.EXECUTION);
                        } else if (message instanceof StopCommand) {
                            stopTestSession(context);
                            logger.debug("Session ["+getSessionId()+"] stopped before having started.");
                        } else {
                            unexpectedCommand(message, context);
                        }
                        break;
                    case STOPPING:
                    case EXECUTION:
                    case OUTPUT:
                        if (message instanceof TestStepStatusEvent) {
                            var msg = prepareStatusUpdate((TestStepStatusEvent)message);
                            eventOutbox.put(msg.getUuid(), msg);
                            if (context.getCurrentState() != TestCaseContext.TestCaseStateEnum.STOPPED) {
                                // After stopping, any such updates will be handled in the final cleanup.
                                self().tell(new SendUpdateCommand(), self());
                            }
                        } else if (message instanceof PrepareForStopCommand) {
                            context.setCurrentState(TestCaseContext.TestCaseStateEnum.STOPPING);
                            testCaseProcessorActor.tell(message, self());
                        } else if (message instanceof TestSessionFinishedCommand) {
                            logger.info(MarkerFactory.getDetachedMarker(getSessionId()), String.format("Session finished with result [%s]", ((TestSessionFinishedCommand) message).getStatus()));
                            sessionEndEvent = createSessionEndMessage(((TestSessionFinishedCommand) message).getStatus(), ((TestSessionFinishedCommand) message).getResultReport());
                            if (!eventProcessingInProgress) {
                                self().tell(new StopCommand(getSessionId()), self());
                            }
                        } else if (message instanceof StopCommand) {
                            stopTestSession(context);
                            logger.debug("Session ["+getSessionId()+"] stopped.");
                        } else if (message instanceof UnexpectedErrorCommand) {
                            self().tell(prepareStatusUpdate(getSessionId(), null, StepStatus.ERROR, null, true, null), self());
                        } else {
                            unexpectedCommand(message, context);
                        }
                        break;
                    case STOPPED:
                        if (message instanceof RestartCommand) {
                            testCaseProcessorActor.tell(message, self());
                        } else if (message instanceof TestStepStatusEvent) {
                            ignoredStatusUpdate((TestStepStatusEvent)message);
                        } else {
                            unexpectedCommand(message, context);
                        }
                        break;
                }
            }
        }
    }

    private void setInputsToSessionContext(TestCaseContext context, List<AnyContent> inputs) {
        if (inputs != null) {
            for (var input: inputs) {
                if (input != null) {
                    if (input.getName() == null) {
                        logger.warn("Session ["+getSessionId()+"] received input with no name");
                    } else if (input.getName().equals("DOMAIN") || input.getName().equals("ORGANISATION") || input.getName().equals("SYSTEM")) {
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

    private void stopTestSession(TestCaseContext context) {
        context.setCurrentState(TestCaseContext.TestCaseStateEnum.STOPPED);
        SessionManager.getInstance().endSession(getSessionId());
        self().tell(PoisonPill.getInstance(), self());
    }

    private UpdateMessage createSessionEndMessage(StepStatus result, TestStepReportType report) {
        return prepareStatusUpdate(getSessionId(), TEST_CASE_STEP_ID, result, report, false, null);
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        if (sessionEndEvent == null) {
            sessionEndEvent = createSessionEndMessage(StepStatus.SKIPPED, null);
        }
        logger.debug("Signalling end of session ["+getSessionId()+"]");
        Futures.future(() -> {
            try {
                for (UpdateMessage msg: eventOutbox.values()) {
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
    }

    private void sendUpdateAsync(final UpdateMessage msg) {
        Futures.future(() -> {
            sendUpdateSync(msg, true);
            return null;
        }, getContext().system().dispatchers().lookup(ActorSystem.BLOCKING_DISPATCHER));
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
                logger.warn(MarkerFactory.getDetachedMarker(getSessionId()), String.format("Error while sending update for message [%s] - step ID [%s] - status [%s])", msg.getUuid(), msg.getStatusMessage().getStepId(), msg.getStatusMessage().getStatus()), e);
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
        return prepareStatusUpdate(event.getSessionId(), event.getStepId(), event.getStatus(), event.getReport(), true, ErrorUtils.extractStepName(event.getStep()));
    }

    private UpdateMessage prepareStatusUpdate(String sessionId, String stepId, StepStatus status, TestStepReportType report, boolean sendLogMessage, String stepType) {
        if (sendLogMessage) {
            if (stepId != null && stepType != null) {
                logger.debug(MarkerFactory.getDetachedMarker(sessionId), String.format("Status update - step [%s] - ID [%s]: %s", stepType, stepId, status));
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
            logger.debug(MarkerFactory.getDetachedMarker(message.getSessionId()), String.format("Ignoring status update - step [%s]- ID [%s]", ErrorUtils.extractStepName(message.getStep()), message.getStepId()));
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
}
