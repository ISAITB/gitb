package com.gitb.engine.remote.messaging;

import com.gitb.core.*;
import com.gitb.engine.remote.RemoteCallContext;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.CallbackManager;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.messaging.model.InitiateResponse;
import com.gitb.messaging.utils.MessagingHandlerUtils;
import com.gitb.ms.*;
import com.gitb.ms.Void;
import com.gitb.types.DataType;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.ErrorUtils;

import javax.xml.ws.soap.AddressingFeature;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Created by serbay.
 */
public class RemoteMessagingModuleClient implements IMessagingHandler {

	private final String testSessionId;
	private URL serviceURL;
	private MessagingModule messagingModule;
	private final Properties transactionProperties;

	public RemoteMessagingModuleClient(URL serviceURL, Properties transactionProperties, String sessionId) {
		this.serviceURL = serviceURL;
		this.testSessionId = sessionId;
		this.transactionProperties = transactionProperties;
	}

	private URL getServiceURL() {
		if (serviceURL == null) {
			if (messagingModule == null) {
				throw new IllegalStateException("Remote messaging module found but with no URL or MessagingModule definition");
			} else {
				try {
					serviceURL = new URI(messagingModule.getServiceLocation()).toURL();
				} catch (MalformedURLException e) {
					throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote messaging module found named ["+messagingModule.getId()+"] with an malformed URL ["+messagingModule.getServiceLocation()+"]"), e);
				} catch (URISyntaxException e) {
					throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote messaging module found named ["+messagingModule.getId()+"] with an invalid URI syntax ["+messagingModule.getServiceLocation()+"]"), e);
				}
			}
		}
		return serviceURL;
	}

	private MessagingService getServiceClient() {
		TestCaseUtils.prepareRemoteServiceLookup(transactionProperties);
		return new MessagingServiceClient(getServiceURL()).getMessagingServicePort(new AddressingFeature(true));
	}

	private <T> T call(Supplier<T> supplier) {
		try {
			RemoteCallContext.setCallProperties(transactionProperties);
			return supplier.get();
		} finally {
			RemoteCallContext.clearCallProperties();
		}
	}

	@Override
	public MessagingModule getModuleDefinition() {
		if (messagingModule == null) {
			messagingModule = call(() -> getServiceClient().getModuleDefinition(new Void()).getModule());
		}
		return messagingModule;
	}

	@Override
	public InitiateResponse initiate(List<ActorConfiguration> actorConfigurations) {
        InitiateRequest request = new InitiateRequest();
        request.getActorConfiguration().addAll(actorConfigurations);
		com.gitb.ms.InitiateResponse wsResponse = call(() -> getServiceClient().initiate(request));
        InitiateResponse response = new InitiateResponse(wsResponse.getSessionId(), wsResponse.getActorConfiguration());
		return response;
	}

	@Override
	public void beginTransaction(String sessionId, String transactionId, String from, String to, List<Configuration> configurations) {
        BeginTransactionRequest request = new BeginTransactionRequest();
        request.setSessionId(sessionId);
        request.setFrom(from);
        request.setTo(to);
        request.getConfig().addAll(configurations);
        call(() -> getServiceClient().beginTransaction(request));
	}

	@Override
	public MessagingReport sendMessage(String sessionId, String transactionId, List<Configuration> configurations, Message message) {
		SendRequest request = new SendRequest();
		request.setSessionId(sessionId);
		for (Map.Entry<String, DataType> fragmentEntry: message.getFragments().entrySet()) {
			AnyContent attachment = DataTypeUtils.convertDataTypeToAnyContent(fragmentEntry.getKey(), fragmentEntry.getValue());
			request.getInput().add(attachment);
		}
		SendResponse response = call(() -> getServiceClient().send(request));
		return MessagingHandlerUtils.getMessagingReport(response.getReport());
	}

	@Override
	public MessagingReport receiveMessage(String sessionId, String transactionId, String callId, List<Configuration> configurations, Message inputs, List<Thread> messagingThreads) {
		ReceiveRequest request = new ReceiveRequest();
		request.setCallId(callId);
		request.setSessionId(sessionId);
		// Prepare inputs
		for (Map.Entry<String, DataType> fragmentEntry: inputs.getFragments().entrySet()) {
			AnyContent input = DataTypeUtils.convertDataTypeToAnyContent(fragmentEntry.getKey(), fragmentEntry.getValue());
			request.getInput().add(input);
		}
		call(() -> getServiceClient().receive(request));
		return CallbackManager.getInstance().waitForCallback(sessionId, callId);
	}

	@Override
	public MessagingReport listenMessage(String sessionId, String transactionId, String from, String to, List<Configuration> configurations, Message inputs) {
		// Not applicable
		return null;
	}

	@Override
	public void endTransaction(String sessionId, String transactionId) {
        BasicRequest request = new BasicRequest();
        request.setSessionId(sessionId);
        call(() -> getServiceClient().endTransaction(request));
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
