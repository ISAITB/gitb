package com.gitb.engine.remote.validation;

import com.gitb.core.Configuration;
import com.gitb.core.ErrorCode;
import com.gitb.core.ValidationModule;
import com.gitb.engine.remote.RemoteCallContext;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.ErrorUtils;
import com.gitb.validation.IValidationHandler;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.gitb.vs.ValidationService;
import com.gitb.vs.Void;

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
public class RemoteValidationModuleClient implements IValidationHandler {

	private URL serviceURL;
	private ValidationModule validationModule;
	private final Properties connectionProperties;

	public RemoteValidationModuleClient(URL serviceURL, Properties connectionProperties) {
		this.serviceURL = serviceURL;
		this.connectionProperties = connectionProperties;
	}

	@Override
	public boolean isRemote() {
		return true;
	}

	private <T> T call(Supplier<T> supplier) {
		try {
			RemoteCallContext.setCallProperties(connectionProperties);
			return supplier.get();
		} finally {
			RemoteCallContext.clearCallProperties();
		}
	}

	@Override
	public ValidationModule getModuleDefinition() {
		if (validationModule == null) {
			validationModule = call(() -> getServiceClient().getModuleDefinition(new Void()).getModule());
		}
		return validationModule;
	}

	@Override
	public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
		ValidateRequest validateRequest = new ValidateRequest();
		validateRequest.getConfig().addAll(configurations);
		for(Map.Entry<String, DataType> input: inputs.entrySet()) {
			validateRequest.getInput().add(DataTypeUtils.convertDataTypeToAnyContent(input.getKey(), input.getValue()));
		}
		ValidationResponse response = call(() -> getServiceClient().validate(validateRequest));
		return response.getReport();
	}

	private URL getServiceURL() {
		if (serviceURL == null) {
			if (validationModule == null) {
				throw new IllegalStateException("Remote validation module found but with no URL or ValidationModule definition");
			} else {
				try {
					serviceURL = new URI(validationModule.getServiceLocation()).toURL();
				} catch (MalformedURLException e) {
					throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote validation module found named ["+validationModule.getId()+"] with an malformed URL ["+validationModule.getServiceLocation()+"]"), e);
				} catch (URISyntaxException e) {
					throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote validation module found named ["+validationModule.getId()+"] with an invalid URI syntax ["+validationModule.getServiceLocation()+"]"), e);
				}
			}
		}
		return serviceURL;
	}

	private ValidationService getServiceClient() {
		TestCaseUtils.prepareRemoteServiceLookup(connectionProperties);
		return new ValidationServiceClient(getServiceURL()).getValidationServicePort();
	}

}
