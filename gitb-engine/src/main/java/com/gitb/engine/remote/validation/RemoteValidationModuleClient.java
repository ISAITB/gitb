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
public class RemoteValidationModuleClient extends RemoteServiceClient implements IValidationHandler {

	private ValidationModule serviceModule;

	public RemoteValidationModuleClient(URL serviceURL, Properties callProperties, String sessionId) {
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

	@Override
	public ValidationModule getModuleDefinition() {
		if (serviceModule == null) {
			serviceModule = call(() -> Optional.ofNullable(getServiceClient().getModuleDefinition(new Void()).getModule()).orElseGet(ValidationModule::new));
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
		var client = new ValidationServiceClient(getServiceURL()).getValidationServicePort(new AddressingFeature(true));
		prepareClient((Client)client);
		return client;
	}

}
