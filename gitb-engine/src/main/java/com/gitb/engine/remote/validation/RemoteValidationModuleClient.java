package com.gitb.engine.remote.validation;

import com.gitb.core.*;
import com.gitb.engine.remote.RemoteCallContext;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;
import com.gitb.types.ListType;
import com.gitb.types.MapType;
import com.gitb.utils.ErrorUtils;
import com.gitb.validation.IValidationHandler;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.gitb.vs.ValidationService;
import com.gitb.vs.Void;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.Charsets;

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
			validateRequest.getInput().add(createContentFromDataType(input.getKey(), input.getValue()));
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

	private static AnyContent createContentFromDataType(String name, DataType data) {
		AnyContent content = new AnyContent();
		content.setType(data.getType());
		content.setEncoding(Charsets.UTF_8.displayName());
		content.setName(name);

		switch (data.getType()) {
			case DataType.BINARY_DATA_TYPE:
			case DataType.BOOLEAN_DATA_TYPE:
			case DataType.NUMBER_DATA_TYPE:
			case DataType.OBJECT_DATA_TYPE:
			case DataType.SCHEMA_DATA_TYPE:
			case DataType.STRING_DATA_TYPE:
				content.setEmbeddingMethod(getValueEmbeddingMethod(data.getType()));
				content.setValue(convertDataToValue(data));
				break;
			case DataType.LIST_DATA_TYPE: {
				ListType list = (ListType) data;

				for (int i = 0; i < list.getSize(); i++) {
					content.getItem().add(createContentFromDataType(null, list.getItem(i)));
				}
				break;
			}
			case DataType.MAP_DATA_TYPE: {
				MapType map = (MapType) data;
				for(Map.Entry<String, DataType> entry : ((Map<String, DataType>)map.getValue()).entrySet()) {
					content.getItem().add(createContentFromDataType(entry.getKey(), entry.getValue()));
				}
			}
		}

		return content;
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

	private static String convertDataToValue(DataType data) {
		ValueEmbeddingEnumeration embeddingMethod = getValueEmbeddingMethod(data.getType());
		byte[] serializedValue = data.serializeByDefaultEncoding();
		if(serializedValue == null) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.DATATYPE_ERROR, "A null value can not be serialized"));
		}
		switch (embeddingMethod) {
			case BASE_64:
				return new String(Base64.encodeBase64(serializedValue));
			default:
				return new String(data.serializeByDefaultEncoding());
		}
	}

}
