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

package com.gitb.engine.remote.messaging;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.AnyContent;
import com.gitb.core.Configuration;
import com.gitb.core.MessagingModule;
import com.gitb.engine.CallbackManager;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;
import com.gitb.engine.remote.RemoteServiceClient;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.messaging.DeferredMessagingReport;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.ms.Void;
import com.gitb.ms.*;
import com.gitb.tdl.MessagingStep;
import com.gitb.types.DataType;
import com.gitb.utils.DataTypeUtils;
import jakarta.xml.ws.soap.AddressingFeature;
import org.apache.cxf.endpoint.Client;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Created by serbay.
 */
public class RemoteMessagingModuleClient extends RemoteServiceClient implements IMessagingHandler {

	private MessagingModule serviceModule;

	public RemoteMessagingModuleClient(URL serviceURL, Properties callProperties, String sessionId) {
		super(serviceURL, callProperties, sessionId);
	}

	@Override
	protected String getServiceLocation() {
		if (serviceModule != null) {
			return serviceModule.getServiceLocation();
		}
		return null;
	}

	@Override
	public boolean isRemote() {
		return true;
	}

	private MessagingService getServiceClient() {
		TestCaseUtils.prepareRemoteServiceLookup(getCallProperties());
		var client = new MessagingServiceClient(getServiceURL()).getMessagingServicePort(new AddressingFeature(true));
		prepareClient((Client)client);
		return client;
	}

	@Override
	public MessagingModule getModuleDefinition() {
		if (serviceModule == null) {
			serviceModule = call(() -> Optional.ofNullable(getServiceClient().getModuleDefinition(new Void()).getModule()).orElseGet(MessagingModule::new));
		}
		return serviceModule;
	}

	@Override
	public InitiateResponse initiate(List<ActorConfiguration> actorConfigurations) {
        InitiateRequest request = new InitiateRequest();
        request.getActorConfiguration().addAll(actorConfigurations);
		InitiateResponse wsResponse = call(() -> {
			try {
				return getServiceClient().initiate(request);
			} catch (Exception e) {
				throw new IllegalStateException("Error raised by remote messaging service while processing the initiate call.", e);
			}
		});
		if (wsResponse.getSessionId() == null) {
			// Set the test session ID as the default.
			wsResponse.setSessionId(testSessionId);
		}
		return wsResponse;
	}

	@Override
	public void beginTransaction(String sessionId, String transactionId, String stepId, String from, String to, List<Configuration> configurations) {
        BeginTransactionRequest request = new BeginTransactionRequest();
        request.setSessionId(sessionId);
        request.setFrom(from);
        request.setTo(to);
        request.getConfig().addAll(configurations);
        call(() -> getServiceClient().beginTransaction(request), stepIdMap(stepId));
	}

	@Override
	public MessagingReport sendMessage(String sessionId, String transactionId, String stepId, List<Configuration> configurations, Message message) {
		SendRequest request = new SendRequest();
		request.setSessionId(sessionId);
		for (Map.Entry<String, DataType> fragmentEntry: message.getFragments().entrySet()) {
			AnyContent attachment = DataTypeUtils.convertDataTypeToAnyContent(fragmentEntry.getKey(), fragmentEntry.getValue());
			request.getInput().add(attachment);
		}
		SendResponse response = call(() -> getServiceClient().send(request), stepIdMap(stepId));
		if (response == null || response.getReport() == null) {
			return MessagingHandlerUtils.generateErrorReport("No response received");
		} else {
			return MessagingHandlerUtils.getMessagingReport(response.getReport());
		}
	}

	@Override
	public MessagingReport receiveMessage(String sessionId, String transactionId, String callId, MessagingStep step, Message inputs, List<Thread> messagingThreads) {
		ReceiveRequest request = new ReceiveRequest();
		request.setCallId(callId);
		request.setSessionId(sessionId);
		// Prepare inputs
		for (Map.Entry<String, DataType> fragmentEntry: inputs.getFragments().entrySet()) {
			AnyContent input = DataTypeUtils.convertDataTypeToAnyContent(fragmentEntry.getKey(), fragmentEntry.getValue());
			request.getInput().add(input);
		}
		call(() -> getServiceClient().receive(request), stepIdMap(step.getId()));
		return new DeferredMessagingReport();
	}

	@Override
	public MessagingReport listenMessage(String sessionId, String transactionId, String stepId, String from, String to, List<Configuration> configurations, Message inputs) {
		// Not applicable
		return null;
	}

	@Override
	public void endTransaction(String sessionId, String transactionId, String stepId) {
        BasicRequest request = new BasicRequest();
        request.setSessionId(sessionId);
        call(() -> getServiceClient().endTransaction(request), stepIdMap(stepId));
	}

	@Override
	public void endSession(String sessionId) {
		try {
			FinalizeRequest request = new FinalizeRequest();
			request.setSessionId(sessionId);
			call(() -> getServiceClient().finalize(request));
		} finally {
			CallbackManager.getInstance().sessionEnded(sessionId);
		}
	}

}
