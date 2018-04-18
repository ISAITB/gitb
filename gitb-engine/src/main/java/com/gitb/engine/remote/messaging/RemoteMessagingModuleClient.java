package com.gitb.engine.remote.messaging;

import com.gitb.core.*;
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

/**
 * Created by serbay.
 */
public class RemoteMessagingModuleClient implements IMessagingHandler {

	private final String testSessionId;
	private URL serviceURL;
	private MessagingModule messagingModule;

	public RemoteMessagingModuleClient(URL serviceURL, String sessionId) {
		this.serviceURL = serviceURL;
		this.testSessionId = sessionId;
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
		return new MessagingServiceClient(getServiceURL()).getMessagingServicePort(new AddressingFeature(true));
	}

	private static ValueEmbeddingEnumeration getValueEmbeddingMethod(String dataType) {
		switch (dataType) {
			case DataType.OBJECT_DATA_TYPE:
			case DataType.BINARY_DATA_TYPE:
			case DataType.SCHEMA_DATA_TYPE:
				return ValueEmbeddingEnumeration.BASE_64;
			default:
				return ValueEmbeddingEnumeration.STRING;
		}
	}

	@Override
	public MessagingModule getModuleDefinition() {
		if (messagingModule == null) {
			messagingModule = getServiceClient().getModuleDefinition(new Void()).getModule();
		}
		return messagingModule;
	}

	@Override
	public InitiateResponse initiate(List<ActorConfiguration> actorConfigurations) {
        InitiateRequest request = new InitiateRequest();
        request.getActorConfiguration().addAll(actorConfigurations);
        com.gitb.ms.InitiateResponse wsResponse = getServiceClient().initiate(request);
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
        getServiceClient().beginTransaction(request);
	}

	@Override
	public MessagingReport sendMessage(String sessionId, String transactionId, List<Configuration> configurations, Message message) {
		SendRequest request = new SendRequest();
		request.setSessionId(sessionId);
		for (Map.Entry<String, DataType> fragmentEntry: message.getFragments().entrySet()) {
			AnyContent attachment = DataTypeUtils.convertDataTypeToAnyContent(fragmentEntry.getKey(), fragmentEntry.getValue());
			request.getInput().add(attachment);
		}
		SendResponse response = getServiceClient().send(request);
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
		getServiceClient().receive(request);
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
        getServiceClient().endTransaction(request);
	}

	@Override
	public void endSession(String sessionId) {
		try {
			FinalizeRequest request = new FinalizeRequest();
			request.setSessionId(sessionId);
			getServiceClient().finalize(request);
		} finally {
			CallbackManager.getInstance().sessionEnded(sessionId);
		}
	}

}
