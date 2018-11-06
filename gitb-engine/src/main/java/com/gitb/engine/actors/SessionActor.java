package com.gitb.engine.actors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
import com.gitb.core.StepStatus;
import com.gitb.engine.SessionManager;
import com.gitb.engine.TestbedService;
import com.gitb.engine.actors.processors.TestCaseProcessorActor;
import com.gitb.engine.actors.supervisors.SessionSupervisor;
import com.gitb.engine.actors.util.ActorUtils;
import com.gitb.engine.commands.interaction.*;
import com.gitb.engine.events.model.TestStepStatusEvent;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.tbs.SUTConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by serbay on 9/4/14.
 *
 * <p>Actor that controls the test execution session.</p>
 *
 * <p>Valid commands for each session state {@link com.gitb.engine.testcase.TestCaseContext.TestCaseStateEnum}
 * for this actor are described below:</p>
 *
 * <p><b>{@link com.gitb.engine.testcase.TestCaseContext.TestCaseStateEnum#IDLE}:</b></p>
 * <ul>
 *     <li>
 *         {@link com.gitb.engine.commands.interaction.ConfigureCommand} Used to configure simulated actor endpoints.
 *         Given SUT configurations are sent to the corresponding messaging handlers.
 *         Test session state is set to {@link com.gitb.engine.testcase.TestCaseContext.TestCaseStateEnum#CONFIGURATION}.
 *     </li>
 * </ul>
 *
 * <p><b>{@link com.gitb.engine.testcase.TestCaseContext.TestCaseStateEnum#CONFIGURATION}:</b></p>
 * <ul>
 *     <li>
 *         {@link com.gitb.engine.commands.interaction.InitiatePreliminaryCommand}: Used to start the preliminary phase.
 *         Test session state is set to {@link com.gitb.engine.testcase.TestCaseContext.TestCaseStateEnum#PRELIMINARY}.
 *     </li>
 * </ul>
 *
 * <p><b>{@link com.gitb.engine.testcase.TestCaseContext.TestCaseStateEnum#PRELIMINARY}:</b></p>
 * <ul>
 *     <li>
 *         {@link com.gitb.engine.events.model.TestStepStatusEvent}: Used to track the status of the {@link com.gitb.engine.actors.processors.InteractionStepProcessorActor}
 *         collects the necessary preliminary inputs. Test session state is set to {@link com.gitb.engine.testcase.TestCaseContext.TestCaseStateEnum#READY}.
 *     </li>
 * </ul>
 *
 * <p><b>{@link com.gitb.engine.testcase.TestCaseContext.TestCaseStateEnum#READY}:</b></p>
 * <ul>
 *     <li>
 *         {@link com.gitb.engine.commands.interaction.StartCommand}: Starts the test execution. Test session state is set
 *         to {@link com.gitb.engine.testcase.TestCaseContext.TestCaseStateEnum#EXECUTION}.
 *     </li>
 *     <li>
 *         {@link com.gitb.engine.commands.interaction.StopCommand}: Stops the test session without starting executing test steps.
 *         Test session state is set to {@link com.gitb.engine.testcase.TestCaseContext.TestCaseStateEnum#STOPPED}.
 *     </li>
 * </ul>
 *
 * <p><b>{@link com.gitb.engine.testcase.TestCaseContext.TestCaseStateEnum#EXECUTION}:</b></p>
 * <ul>
 *     <li>
 *         {@link com.gitb.engine.events.model.TestStepStatusEvent}: Redirects the incoming test step status updates to
 *         {@link com.gitb.engine.TestbedService#updateStatus(String, String, com.gitb.core.StepStatus, com.gitb.tr.TestStepReportType)}.
 *     </li>
 *     <li>
 *         {@link com.gitb.engine.commands.interaction.StopCommand}: Stops the execution of the remaining test steps.
 *         Test session state is set to {@link com.gitb.engine.testcase.TestCaseContext.TestCaseStateEnum#STOPPED}.
 *     </li>
 * </ul>
 *
 * <p><b>{@link com.gitb.engine.testcase.TestCaseContext.TestCaseStateEnum#STOPPED}:</b></p>
 * <ul>
 *     <li>
 *         {@link com.gitb.engine.commands.interaction.RestartCommand}: Restarts the execution of the test steps with a new test
 *         execution session id and possibly new simulated actor configurations.
 *     </li>
 *     <li>
 *         {@link com.gitb.engine.events.model.TestStepStatusEvent}: Ignored
 *     </li>
 * </ul>
 *
 */
public class SessionActor extends Actor {
    private static Logger logger = LoggerFactory.getLogger(SessionActor.class);

    private String getSessionId() {
        return self().path().name();
    }

    @Override
    public void preStart() throws Exception {
        TestCaseProcessorActor.create(getContext(), getSessionId());
    }

    @Override
    public void onReceive(Object message) {
        super.onReceive(message);
        TestCaseContext context = SessionManager
                .getInstance()
                .getContext(getSessionId());
        if (context != null) {
            switch (context.getCurrentState()) {
                case IDLE:
                    /**
                     * Handle the configuration of test execution (given configuration list for each SUT actor)
                     */
                    if (message instanceof ConfigureCommand) {
                        try {
                            List<SUTConfiguration> sutConfigurations = context.configure(((ConfigureCommand) message).getActorConfigurations(), ((ConfigureCommand) message).getDomainConfiguration());
                            getSender().tell(sutConfigurations, self());

                            if (context.getTestCase().getPreliminary() != null) {
                                context.setCurrentState(TestCaseContext.TestCaseStateEnum.CONFIGURATION);
                            } else {
                                context.setCurrentState(TestCaseContext.TestCaseStateEnum.READY);
                            }
                        } catch (Exception e) {
                            getSender().tell(e, self());
                        }
                    } else {
                        unexpectedCommand(message, context);
                    }
                    break;
                case CONFIGURATION:
                    /**
                     * Initiate the preliminary phase for this execution
                     */
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
                        TestbedService
                                .updateStatus(event.getSessionId(), event.getStepId(), event.getStatus(), event.getReport());
                    } else {
                        unexpectedCommand(message, context);
                    }
                    break;
                case READY:
                    if (message instanceof StartCommand) {
                        ActorRef child = getContext()
                                .getChild(TestCaseProcessorActor.NAME);
                        child.tell(message, self());
                        context.setCurrentState(TestCaseContext.TestCaseStateEnum.EXECUTION);
                    } else if (message instanceof StopCommand) {
                        ActorRef child = getContext()
                                .getChild(TestCaseProcessorActor.NAME);
                        child.tell(message, self());
                        context.destroy();
                        context.setCurrentState(TestCaseContext.TestCaseStateEnum.STOPPED);
                    } else {
                        unexpectedCommand(message, context);
                    }
                    break;
                case EXECUTION:
                    if (message instanceof TestStepStatusEvent) {
                        TestStepStatusEvent event = (TestStepStatusEvent) message;

                        if (event.getStepId().equals(TestCaseProcessorActor.TEST_CASE_STEP_ID)
                                && (event.getStatus() == StepStatus.COMPLETED || event.getStatus() == StepStatus.ERROR)) {
                            context.destroy();
                            context.setCurrentState(TestCaseContext.TestCaseStateEnum.STOPPED);
                        }

                        sendStatusUpdate(event);
                    } else if (message instanceof PrepareForStopCommand) {
                        ActorRef child = getContext().getChild(TestCaseProcessorActor.NAME);
                        try {
                            ActorUtils.askBlocking(child, message);
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                        getSender().tell(Boolean.TRUE, self());
                    } else if (message instanceof StopCommand) {
                        ActorRef child = getContext()
                                .getChild(TestCaseProcessorActor.NAME);
                        child.tell(message, self());
                        context.destroy();
                        context.setCurrentState(TestCaseContext.TestCaseStateEnum.STOPPED);
                    } else {
                        unexpectedCommand(message, context);
                    }
                    break;
                case STOPPED:
                    if (message instanceof RestartCommand) {
                        ActorRef child = getContext()
                                .getChild(TestCaseProcessorActor.NAME);
                        child.tell(message, self());
                    } else if (message instanceof TestStepStatusEvent) {
                        ignoredStatusUpdate(message);
                    } else {
                        unexpectedCommand(message, context);
                    }
                    break;
            }
        }
    }

	private void sendStatusUpdate(final TestStepStatusEvent event) {
		Futures.future(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				TestbedService
					.updateStatus(event.getSessionId(), event.getStepId(), event.getStatus(), event.getReport());
				return null;
			}
		}, getContext().system().dispatchers().lookup(ActorSystem.BLOCKING_DISPATCHER));
	}

	private void ignoredStatusUpdate(Object message) {
		logger.debug("Ignoring the status update ["+message+"]");
	}

	private void unexpectedCommand(Object message, TestCaseContext context) {
        logger.error(MarkerFactory.getDetachedMarker(context.getSessionId()), "InternalError", "Invalid command [" + message.getClass().getName() + "] in state [" + context.getCurrentState() + "]");
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
