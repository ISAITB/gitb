package com.gitb.engine;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.gitb.core.Actor;
import com.gitb.core.ActorConfiguration;
import com.gitb.core.ErrorCode;
import com.gitb.core.StepStatus;
import com.gitb.engine.actors.SessionActor;
import com.gitb.engine.actors.util.ActorUtils;
import com.gitb.engine.commands.interaction.*;
import com.gitb.engine.commands.session.CreateCommand;
import com.gitb.engine.events.TestStepInputEventBus;
import com.gitb.engine.events.model.InputEvent;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tbs.*;
import com.gitb.tpl.TestCase;
import com.gitb.tr.TestStepReportType;
import com.gitb.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by serbay on 9/5/14.
 */
public class TestbedService {
	private static Logger logger = LoggerFactory.getLogger(TestbedService.class);

	/**
	 * Returns the test case definition (TPL representation) for the given TestCase id
	 *
	 * @param testCaseId
	 * @return
	 */
	public static TestCase getTestCaseDefinition(String testCaseId) {
		logger.debug("getTestCaseDefinition"
			+ " ( "
			+ testCaseId
			+ " ) ");

		return TestCaseManager.getTestCasePresentation(testCaseId);
	}

	/**
	 * Return the Actor Definition (required configuration parameters) given the TestSuite.id and the Actor.id
	 *
	 * @param testSuiteId
	 * @param actorId
	 * @return
	 */
	public static Actor getActorDefinition(String testSuiteId, String actorId) {
		logger.debug("getActorDefinition"
			+ " ( "
			+ actorId
			+ " ) ");

		return TestCaseManager.getActorDescription(testSuiteId, actorId);
	}

	/**
	 * Initiate a TestCase Session, return session id
	 *
	 * @param testCaseId
	 * @return
	 */
	public static String initiate(String testCaseId) {
		logger.debug("Initiating the test case [" + testCaseId + "]...");
		//Create a Session in TestEngine and return session id
		String sessionId = SessionManager
			.getInstance()
			.newSession(testCaseId);
		logger.debug("New session [" + sessionId + "] created for the execution of test case [" + testCaseId + "]...");
		TestEngine
			.getInstance()
			.getEngineActorSystem()
			.getSessionSupervisor()
			.tell(new CreateCommand(sessionId), ActorRef.noSender());
		logger.debug("Test execution environment initialized for session [" + sessionId + "]...");
		return sessionId;
	}

	/**
	 * Save the configurations for the given actors
	 *
	 * @param sessionId
	 * @param actorConfigurations
	 */
	public static List<SUTConfiguration> configure(String sessionId, List<ActorConfiguration> actorConfigurations) {
		logger.debug("configure"
			+ " ( "
			+ sessionId + " , "
			+ "actors - " + actorConfigurations.size()
			+ " ) ");

		//Find the Processor for the session
		ActorSystem actorSystem = TestEngine
			.getInstance()
			.getEngineActorSystem()
			.getActorSystem();

		SessionManager sessionManager = SessionManager.getInstance();

		if(sessionManager.getContext(sessionId) == null) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_SESSION, "Could not find session [" + sessionId + "]..."));
		}

		try {
			ActorRef sessionActor = ActorUtils.getIdentity(actorSystem, SessionActor.getPath(sessionId));
			//Call the configure command
			Object response = ActorUtils.askBlocking(sessionActor, new ConfigureCommand(sessionId, actorConfigurations));
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
		logger.debug("provideInput"
			+ " ( "
			+ sessionId + " , "
			+ stepId
			+ " ) ");

		//Fire TestStepInputEvent
		TestStepInputEventBus
			.getInstance().publish(new InputEvent(sessionId, stepId, userInputs));
	}

	public static void initiatePreliminary(String sessionId) {
		logger.debug("initiatePreliminary"
			+ " ( "
			+ sessionId
			+ " ) ");

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
		logger.debug("start"
			+ " ( "
			+ sessionId
			+ " ) ");

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
		logger.debug("stop"
			+ " ( "
			+ sessionId
			+ " ) ");

		if (SessionManager.getInstance().exists(sessionId)) {
			TestEngine
					.getInstance()
					.getEngineActorSystem()
					.getActorSystem()
					.actorSelection(SessionActor.getPath(sessionId))
					.tell(new StopCommand(sessionId), ActorRef.noSender());

			SessionManager.getInstance().endSession(sessionId);
		}
	}

	/**
	 * Restart the TestCase session execution
	 *
	 * @param sessionId
	 * @return new test execution session id
	 */
	public static String restart(String sessionId) {

		logger.debug("restart"
			+ " ( "
			+ sessionId
			+ " ) ");

		TestEngine
			.getInstance()
			.getEngineActorSystem()
			.getActorSystem()
			.actorSelection(SessionActor.getPath(sessionId))
			.tell(new RestartCommand(sessionId), ActorRef.noSender());

		String newSessionId = SessionManager
			.getInstance()
			.duplicateSession(sessionId);
		logger.debug("Restarted session [" + sessionId + "] with the session id [" + newSessionId + "]...");

		TestEngine
			.getInstance()
			.getEngineActorSystem()
			.getSessionSupervisor()
			.tell(new CreateCommand(newSessionId), ActorRef.noSender());
		logger.debug("Test execution environment initialized for session [" + newSessionId + "]...");

		return newSessionId;
	}

	/**
	 * Callback for informing the client side about latest status of execution
	 *
	 * @param sessionId
	 * @param stepId
	 * @param status
	 * @param report
	 */
	public static void updateStatus(String sessionId, String stepId, StepStatus status, TestStepReportType report) {
		logger.debug("updateStatus"
			+ " ( "
			+ sessionId + " , "
			+ stepId + " , "
			+ status + " , "
			+ report
			+ " ) ");
		//Get the Callback client
		ITestbedServiceCallbackHandler tbsCallbackHandle = TestEngine
			.getInstance()
			.getTbsCallbackHandle();

		if(tbsCallbackHandle == null) {
			return;
		}

        if(stepId == null || stepId.length() == 0) {
            return;
        }

		TestbedClient testbedClient = tbsCallbackHandle
			.getTestbedClient(sessionId);
		//Construct the Callback response
		TestStepStatus testStepStatus = new TestStepStatus();
		testStepStatus.setTcInstanceId(sessionId);
		testStepStatus.setStepId(stepId);
		testStepStatus.setStatus(status);
		testStepStatus.setReport(report);
		//Call the UpdateStatus callback
		testbedClient.updateStatus(testStepStatus);
	}

	/**
	 * Callback to inform the client for expected user interaction
	 *
	 * @param sessionId
	 * @param interaction
	 */
	public static void interactWithUsers(String sessionId, String stepId, UserInteractionRequest interaction) {
		logger.debug("interactWithUsers"
			+ " ( "
			+ sessionId
			+ " ) ");
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
