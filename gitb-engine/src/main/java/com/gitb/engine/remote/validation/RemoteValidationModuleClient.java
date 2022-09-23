package com.gitb.engine.remote.validation;

import com.gitb.core.Configuration;
import com.gitb.core.ValidationModule;
import com.gitb.engine.remote.RemoteServiceClient;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;
import com.gitb.utils.DataTypeUtils;
import com.gitb.validation.IValidationHandler;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.gitb.vs.ValidationService;
import com.gitb.vs.Void;

import javax.xml.ws.soap.AddressingFeature;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by serbay.
 */
public class RemoteValidationModuleClient extends RemoteServiceClient<ValidationModule> implements IValidationHandler {

	public RemoteValidationModuleClient(URL serviceURL, Properties callProperties, String sessionId) {
		super(serviceURL, callProperties, sessionId);
	}

	@Override
	public boolean isRemote() {
		return true;
	}

	@Override
	public ValidationModule getModuleDefinition() {
		if (serviceModule == null) {
			serviceModule = call(() -> getServiceClient().getModuleDefinition(new Void()).getModule());
		}
		return serviceModule;
	}

	@Override
	public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs, String stepId) {
		ValidateRequest validateRequest = new ValidateRequest();
		validateRequest.setSessionId(testSessionId);
		validateRequest.getConfig().addAll(configurations);
		for(Map.Entry<String, DataType> input: inputs.entrySet()) {
			validateRequest.getInput().add(DataTypeUtils.convertDataTypeToAnyContent(input.getKey(), input.getValue()));
		}
		ValidationResponse response = call(() -> getServiceClient().validate(validateRequest), stepIdMap(stepId));
		return response.getReport();
	}

	private ValidationService getServiceClient() {
		TestCaseUtils.prepareRemoteServiceLookup(getCallProperties());
		return new ValidationServiceClient(getServiceURL()).getValidationServicePort(new AddressingFeature(true));
	}

}
