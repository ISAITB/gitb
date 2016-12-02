package com.gitb.engine.remote.messaging;

import com.gitb.core.*;
import com.gitb.engine.SessionManager;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.messaging.model.InitiateResponse;
import com.gitb.ms.*;
import com.gitb.ms.Void;
import com.gitb.tr.TAR;
import com.gitb.types.*;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.codec.binary.Base64;

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
		return new MessagingReport(response.getReport());
	}

	@Override
	public MessagingReport receiveMessage(String sessionId, String transactionId, List<Configuration> configurations, Message inputs) {
		ReceiveRequest request = new ReceiveRequest();
		request.setSessionId(sessionId);
		// Prepare inputs
		for (Map.Entry<String, DataType> fragmentEntry: inputs.getFragments().entrySet()) {
			AnyContent input = DataTypeUtils.convertDataTypeToAnyContent(fragmentEntry.getKey(), fragmentEntry.getValue());
			request.getInput().add(input);
		}
		getServiceClient().receive(request);
		MessagingReport report = null;
		NotifyForMessageRequest callback = CallbackManager.getInstance().waitForCallback(sessionId);
		if (SessionManager.getInstance().getContext(testSessionId).getCurrentState() != TestCaseContext.TestCaseStateEnum.STOPPED) {
			Message outputMessage = getMessageFromReport(callback.getReport());
			report = new MessagingReport(callback.getReport(), outputMessage);
		}
		return report;
	}

	private Message getMessageFromReport(TAR report) {
		Message message = new Message();
		AnyContent context = report.getContext();
		if (DataType.MAP_DATA_TYPE.equals(context.getType())) {
			for (AnyContent child: context.getItem()) {
				message.getFragments().put(child.getName(), toDataType(child));
			}
		} else {
			throw new IllegalStateException("Invalid context type of report");
		}
		return message;
	}

	private DataType toDataType(AnyContent content) {
		DataType type;
		if (DataType.MAP_DATA_TYPE.equals(content.getType())) {
			type = new MapType();
			for (AnyContent child: content.getItem()) {
				((MapType)type).addItem(child.getName(), toDataType(child));
			}
		} else if (DataType.STRING_DATA_TYPE.equals(content.getType())) {
			type = new StringType();
			type.setValue(content.getValue());
		} else if (DataType.BINARY_DATA_TYPE.equals(content.getType())) {
			type = new BinaryType();
			if (ValueEmbeddingEnumeration.BASE_64.equals(content.getEmbeddingMethod())) {
				type.setValue(Base64.decodeBase64(content.getValue()));
			} else {
				throw new IllegalStateException("Only base64 embedding supported for binary types");
			}
		} else if (DataType.BOOLEAN_DATA_TYPE.equals(content.getType())) {
			type = new BooleanType();
			type.setValue(Boolean.valueOf(content.getValue()));
		} else if (DataType.NUMBER_DATA_TYPE.equals(content.getType())) {
			type = new NumberType();
			type.setValue(content.getValue());
		} else if (DataType.LIST_DATA_TYPE.equals(content.getType())) {
			type = new ListType();
			for (AnyContent child: content.getItem()) {
				((ListType)type).append(toDataType(child));
			}
		} else if (DataType.OBJECT_DATA_TYPE.equals(content.getType())) {
			type = new ObjectType();
			if (ValueEmbeddingEnumeration.BASE_64.equals(content.getEmbeddingMethod())) {
				type.deserialize(Base64.decodeBase64(content.getValue()));
			} else if (ValueEmbeddingEnumeration.STRING.equals(content.getEmbeddingMethod())) {
				type.deserialize(content.getValue().getBytes());
			} else {
				throw new IllegalStateException("Only base64 and string embedding supported for object types");
			}
		} else {
			throw new IllegalStateException("Unsupported data type ["+content.getType()+"]");
		}
		return type;
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
