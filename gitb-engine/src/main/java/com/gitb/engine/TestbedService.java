/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package com.gitb.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gitb.core.ActorConfiguration;
import com.gitb.core.AnyContent;
import com.gitb.core.ErrorCode;
import com.gitb.engine.commands.interaction.*;
import com.gitb.engine.commands.session.CreateCommand;
import com.gitb.engine.events.TestStepInputEventBus;
import com.gitb.engine.events.model.InputEvent;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.repository.ITestCaseRepository;
import com.gitb.tbs.*;
import com.gitb.utils.ErrorUtils;
import com.gitb.utils.HmacUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.tika.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
	public static String initiate(String testCaseId, String sessionIdToAssign) {
		//Create a Session in TestEngine and return session id
		String sessionId = SessionManager
			.getInstance()
			.newSession(testCaseId, sessionIdToAssign);
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
	public static void configure(String sessionId, List<ActorConfiguration> allConfigurations, List<AnyContent> inputs) {
		logger.debug(MarkerFactory.getDetachedMarker(sessionId), String.format("Starting to configure session [%s]", sessionId));
		SessionManager sessionManager = SessionManager.getInstance();
		if (sessionManager.notExists(sessionId)) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_SESSION, "Could not find session [" + sessionId + "]..."));
		}
		var configData = new SessionConfigurationData(allConfigurations);
		TestEngine
				.getInstance()
				.getEngineActorSystem()
				.getSessionSupervisor()
				.tell(new ConfigureCommand(sessionId, configData.getActorConfigurations(), configData.getDomainConfiguration(), configData.getOrganisationConfiguration(), configData.getSystemConfiguration(), inputs), ActorRef.noSender());
	}

	private static List<String> extractFailureDetails(Throwable error) {
		var messages = new ArrayList<String>();
		var handledErrors = new HashSet<Throwable>();
		extractFailureDetailsInternal(error, handledErrors, messages);
		return messages.stream().filter(Objects::nonNull).collect(Collectors.toList());
	}

	private static void extractFailureDetailsInternal(Throwable error, Set<Throwable> handledErrors, List<String> messages) {
		if (error != null && !handledErrors.contains(error)) {
			handledErrors.add(error);
			messages.add(error.getMessage());
			extractFailureDetailsInternal(error.getCause(), handledErrors, messages);
		}
	}


	/**
	 * Provide the expected user inputs to the test engine for the interaction step or preliminary phase
	 *
	 * @param sessionId
	 * @param stepId
	 * @param userInputs
	 */
	public static void provideInput(String sessionId, String stepId, List<UserInput> userInputs, boolean isAdmin)  {
		TestStepInputEventBus.getInstance().publish(new InputEvent(sessionId, stepId, userInputs, isAdmin));
	}

	public static void initiatePreliminary(String sessionId) {
		logger.debug(MarkerFactory.getDetachedMarker(sessionId), "Initiating preliminary exchange");
		TestEngine
			.getInstance()
			.getEngineActorSystem()
			.getSessionSupervisor()
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
			.getSessionSupervisor()
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
				msg = new StopCommand(sessionId, true);
			}
			TestEngine
					.getInstance()
					.getEngineActorSystem()
					.getSessionSupervisor()
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
		String newSessionId = SessionManager
				.getInstance()
				.duplicateSession(sessionId);
		TestEngine
			.getInstance()
			.getEngineActorSystem()
			.getSessionSupervisor()
			.tell(new RestartCommand(sessionId, newSessionId), ActorRef.noSender());
		logger.debug(MarkerFactory.getDetachedMarker(newSessionId), "Restarted session [{}] as new session [{}]", sessionId, newSessionId);
		return newSessionId;
	}

	private static void sendToClient(String sessionId, Consumer<TestbedClient> fn) {
		var tbsCallbackHandle = TestEngine
				.getInstance()
				.getTbsCallbackHandle();
		if (tbsCallbackHandle != null) {
			var testbedClient = tbsCallbackHandle.getTestbedClient(sessionId);
			if (testbedClient != null) {
				fn.accept(testbedClient);
			}
		}
	}

	public static void sendConfigurationFailure(String sessionId, Throwable error) {
		var details = extractFailureDetails(error);
		String message;
		if (details.isEmpty()) {
			message = "Error during test session configuration";
		} else {
			message = details.get(details.size() - 1);
		}

		var request = new ConfigurationCompleteRequest();
		request.setTcInstanceId(sessionId);
		request.setErrorCode("INTERNAL_ERROR");
		request.setErrorDescription(message);
		sendToClient(sessionId, (client) -> client.configurationComplete(request));
	}

	public static void sendConfigurationSuccessResult(String sessionId, List<SUTConfiguration> configurations) {
		var request = new ConfigurationCompleteRequest();
		request.setTcInstanceId(sessionId);
		request.getConfigs().addAll(configurations);
		sendToClient(sessionId, (client) -> {
			logger.debug("Signalling configuration completion or session [{}]", sessionId);
			client.configurationComplete(request);
		});
	}

	public static void sendStatusUpdate(String sessionId, TestStepStatus testStepStatus) {
		sendToClient(sessionId, (client) -> client.updateStatus(testStepStatus));
	}

	/**
	 * Callback to inform the client for expected user interaction
	 *
	 * @param sessionId
	 * @param interaction
	 */
	public static void interactWithUsers(String sessionId, String stepId, UserInteractionRequest interaction) {
		//Construct the Callback
		var request = new InteractWithUsersRequest();
		request.setTcInstanceid(sessionId);
		request.setStepId(stepId);
		request.setInteraction(interaction);
		sendToClient(sessionId, (client) -> client.interactWithUsers(request));
	}

	public static String healthCheck(Consumer<TestStepStatus> healthCheckSender) {
		// 1. Call the GITB Test Bed Client
		HealthCheckResult tbsResult;
		TestStepStatus messageToSend = new TestStepStatus();
		messageToSend.setTcInstanceId("com.gitb.HEALTH_CHECK");
		messageToSend.setStepId(PropertyConstants.LOG_EVENT_STEP_ID);
		try {
			healthCheckSender.accept(messageToSend);
			tbsResult = HealthCheckResult.success("tbs");
		} catch (Exception e) {
			tbsResult = HealthCheckResult.failure("tbs", serialiseThrowable(e));
		}
		// 2. Call the test case repository.
		HealthCheckResult repoResult;
		ITestCaseRepository repository = ModuleManager.getInstance().getTestCaseRepository();
        try {
            String result = repository.healthCheck("test");
			if (logger.isInfoEnabled()) {
				logger.info("Health check ping returned [{}]", result);
			}
			repoResult = HealthCheckResult.success("repo");
        } catch (Exception e) {
			repoResult = HealthCheckResult.failure("repo", serialiseThrowable(e));
        }
		// Create result.
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode root = mapper.createObjectNode();
		ArrayNode resultArray = mapper.createArrayNode();
		resultArray.add(mapper.valueToTree(tbsResult));
		resultArray.add(mapper.valueToTree(repoResult));
		root.set("items", resultArray);
		root.put("repositoryUrl", TestEngineConfiguration.REPOSITORY_ROOT_URL);
		root.put("hmacHash", HmacUtils.getHashedKey());
        try {
            return mapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unexpected error while serialising health check status", e);
        }
    }

	private static String serialiseThrowable(Throwable error) {
		var messages = new ArrayList<String>();
		var handledErrors = new ArrayList<Throwable>();
		extractFailureDetailsInternal(error, handledErrors, messages);
		var errorLines = messages.stream().filter(Objects::nonNull).toList();
		// Construct string.
		var buffer = new StringBuilder();
		var lineNumber = -1;
		for (var line: errorLines) {
			lineNumber += 1;
			if (!buffer.isEmpty()) {
				var padding = StringUtils.leftPad("", lineNumber-1, "   ");
				buffer.append('\n').append(padding).append("└─ ");
			}
			buffer.append(line);
		}
		return buffer.toString();
    }

	private static void extractFailureDetailsInternal(Throwable error, List<Throwable> handledErrors, List<String> messages) {
		if (error != null && !handledErrors.contains(error)) {
			handledErrors.add(error);
			messages.add(error.getMessage());
			extractFailureDetailsInternal(error.getCause(), handledErrors, messages);
		}
	}

	private record HealthCheckResult(String name, boolean status, String message) {

		private static HealthCheckResult success(String name) {
			return new HealthCheckResult(name, true, "");
		}

		private static HealthCheckResult failure(String name, String message) {
			return new HealthCheckResult(name, false, message);
		}

	}

}
