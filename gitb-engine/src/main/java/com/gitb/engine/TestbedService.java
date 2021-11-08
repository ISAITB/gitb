package com.gitb.engine;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.gitb.core.ActorConfiguration;
import com.gitb.core.ErrorCode;
import com.gitb.engine.actors.SessionActor;
import com.gitb.engine.actors.util.ActorUtils;
import com.gitb.engine.commands.interaction.*;
import com.gitb.engine.commands.session.CreateCommand;
import com.gitb.engine.events.TestStepInputEventBus;
import com.gitb.engine.events.model.InputEvent;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tbs.*;
import com.gitb.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by serbay on 9/5/14.
 */
public class TestbedService {

	private static final Logger logger = LoggerFactory.getLogger(TestbedService.class);
	private static final Logger sessionLogger = LoggerFactory.getLogger("TEST_SESSION");

	/**
	 * Initiate a TestCase Session, return session id
	 *
	 * @param testCaseId
	 * @return
	 */
	public static String initiate(String testCaseId) {
		//Create a Session in TestEngine and return session id
		String sessionId = SessionManager
			.getInstance()
			.newSession(testCaseId);
		TestEngine
			.getInstance()
			.getEngineActorSystem()
			.getSessionSupervisor()
			.tell(new CreateCommand(sessionId), ActorRef.noSender());
		return sessionId;
	}

	/**
	 * Save the configurations for the given actors
	 *
	 * @param sessionId
	 * @param allConfigurations
	 */
	public static List<SUTConfiguration> configure(String sessionId, List<ActorConfiguration> allConfigurations) {
		logger.debug(MarkerFactory.getDetachedMarker(sessionId), String.format("Configuring session [%s]", sessionId));

		//Find the Processor for the session
		ActorSystem actorSystem = TestEngine
			.getInstance()
			.getEngineActorSystem()
			.getActorSystem();

		SessionManager sessionManager = SessionManager.getInstance();

		if(sessionManager.getContext(sessionId) == null) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_SESSION, "Could not find session [" + sessionId + "]..."));
		}

		List<ActorConfiguration> actorConfigurations = new ArrayList<>();
		ActorConfiguration domainConfiguration = null;
		ActorConfiguration organisationConfiguration = null;
		ActorConfiguration systemConfiguration = null;
		if (allConfigurations != null) {
			for (ActorConfiguration configuration: allConfigurations) {
				if ("com.gitb.DOMAIN".equals(configuration.getActor())) {
					domainConfiguration = configuration;
				} else if ("com.gitb.ORGANISATION".equals(configuration.getActor())) {
					organisationConfiguration = configuration;
				} else if ("com.gitb.SYSTEM".equals(configuration.getActor())) {
					systemConfiguration = configuration;
				} else {
					actorConfigurations.add(configuration);
				}
			}
		}

		try {
			ActorRef sessionActor = ActorUtils.getIdentity(actorSystem, SessionActor.getPath(sessionId));
			//Call the configure command
			Object response = ActorUtils.askBlocking(sessionActor, new ConfigureCommand(sessionId, actorConfigurations, domainConfiguration, organisationConfiguration, systemConfiguration));
			return (List<SUTConfiguration>) response;
		} catch (GITBEngineInternalError e) {
			throw e;
		} catch (Exception e) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Exception during configuration phase of session [" + sessionId + "]..."), e);
		}
	}

	/**
	 * Provide the expected user inputs to the test engine for the interaction step or preliminary phase
	 *
	 * @param sessionId
	 * @param stepId
	 * @param userInputs
	 */
	public static void provideInput(String sessionId, String stepId, List<UserInput> userInputs) {
		logger.debug(MarkerFactory.getDetachedMarker(sessionId), String.format("Handling user-provided inputs - step [UserInteraction] - ID [%s]", stepId));
		//Fire TestStepInputEvent
		TestStepInputEventBus
			.getInstance().publish(new InputEvent(sessionId, stepId, userInputs));
	}

	public static void initiatePreliminary(String sessionId) {
		logger.debug(MarkerFactory.getDetachedMarker(sessionId), "Initiating preliminary exchange");
		TestEngine
			.getInstance()
			.getEngineActorSystem()
			.getActorSystem()
			.actorSelection(SessionActor.getPath(sessionId))
			.tell(new InitiatePreliminaryCommand(sessionId), ActorRef.noSender());
	}

	/**
	 * Start the TestCase session execution
	 *
	 * @param sessionId
	 */
	public static void start(String sessionId) {
		logger.debug("Starting session {}", sessionId);
		sessionLogger.info(MarkerFactory.getDetachedMarker(sessionId), "Starting session");
		TestEngine
			.getInstance()
			.getEngineActorSystem()
			.getActorSystem()
			.actorSelection(SessionActor.getPath(sessionId))
			.tell(new StartCommand(sessionId), ActorRef.noSender());
	}

	/**
	 * Stop the TestCase session execution
	 *
	 * @param sessionId
	 */
	public static void stop(String sessionId) {
		boolean isClosedConnectionSignal = false;
		if (sessionId.startsWith("CONNECTION_CLOSED|")) {
			sessionId = sessionId.substring(sessionId.indexOf('|')+1);
			isClosedConnectionSignal = true;
		}
		if (SessionManager.getInstance().exists(sessionId)) {
			Object msg;
			if (isClosedConnectionSignal) {
				msg = new ConnectionClosedEvent(sessionId);
			} else {
				// Regular stop
				logger.debug("Stopping session {}", sessionId);
				sessionLogger.info(MarkerFactory.getDetachedMarker(sessionId), "Stopping session");
				msg = new StopCommand(sessionId);
			}
			TestEngine
					.getInstance()
					.getEngineActorSystem()
					.getActorSystem()
					.actorSelection(SessionActor.getPath(sessionId))
					.tell(msg, ActorRef.noSender());
		}
	}

	/**
	 * Restart the TestCase session execution
	 *
	 * @param sessionId
	 * @return new test execution session id
	 */
	public static String restart(String sessionId) {
		logger.debug("Restarting session {}", sessionId);
		sessionLogger.info(MarkerFactory.getDetachedMarker(sessionId), "Restarting session");
		TestEngine
			.getInstance()
			.getEngineActorSystem()
			.getActorSystem()
			.actorSelection(SessionActor.getPath(sessionId))
			.tell(new RestartCommand(sessionId), ActorRef.noSender());
		String newSessionId = SessionManager
			.getInstance()
			.duplicateSession(sessionId);
		logger.debug(MarkerFactory.getDetachedMarker(newSessionId), String.format("Restarted session [" + sessionId + "] as new session", sessionId));
		TestEngine
			.getInstance()
			.getEngineActorSystem()
			.getSessionSupervisor()
			.tell(new CreateCommand(newSessionId), ActorRef.noSender());
		return newSessionId;
	}

	public static void sendStatusUpdate(String sessionId, TestStepStatus testStepStatus) {
		//Get the Callback client
		ITestbedServiceCallbackHandler tbsCallbackHandle = TestEngine
				.getInstance()
				.getTbsCallbackHandle();
		if (tbsCallbackHandle == null) {
			return;
		}
		TestbedClient testbedClient = tbsCallbackHandle.getTestbedClient(sessionId);
		if (testbedClient != null) {
			//Call the UpdateStatus callback
			testbedClient.updateStatus(testStepStatus);
		}
	}

	/**
	 * Callback to inform the client for expected user interaction
	 *
	 * @param sessionId
	 * @param interaction
	 */
	public static void interactWithUsers(String sessionId, String stepId, UserInteractionRequest interaction) {
		logger.debug(MarkerFactory.getDetachedMarker(sessionId), String.format("Triggering user interaction - step [UserInteraction] - ID [%s]", stepId));
		//Get the Callback client
		ITestbedServiceCallbackHandler tbsCallbackHandle = TestEngine
			.getInstance()
			.getTbsCallbackHandle();

		if(tbsCallbackHandle == null) {
			return;
		}

		TestbedClient testbedClient = tbsCallbackHandle
			.getTestbedClient(sessionId);
		//Construct the Callback
		InteractWithUsersRequest request = new InteractWithUsersRequest();
		request.setTcInstanceid(sessionId);
		request.setStepId(stepId);
		request.setInteraction(interaction);
		//Call the InteractWithUsers callback
		testbedClient.interactWithUsers(request);
	}
}
