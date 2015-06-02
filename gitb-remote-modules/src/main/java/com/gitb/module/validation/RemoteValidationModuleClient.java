package com.gitb.module.validation;

import com.gitb.core.*;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;
import com.gitb.types.ListType;
import com.gitb.types.MapType;
import com.gitb.utils.ErrorUtils;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.gitb.vs.ValidationService_Service;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.Charsets;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Created by serbay.
 */
public class RemoteValidationModuleClient {
	public static TestStepReportType validate(ValidationModule validationModule, List<Configuration> configurations, Map<String, DataType> inputs) {
		if(validationModule.isIsRemote()) {
			ValidationService_Service serviceClient;

			try {
				serviceClient = new ValidationService_Service(new URI(validationModule.getServiceLocation()).toURL());
			} catch (Exception e) {
				throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote service client could not be constructed. Please check 'serviceLocation' field."), e);
			}

			ValidateRequest validateRequest = new ValidateRequest();

			validateRequest.getConfig().addAll(configurations);
			for(Map.Entry<String, DataType> input : inputs.entrySet()) {

				validateRequest.getInput()
					.add(createContentFromDataType(input.getKey(), input.getValue()));
			}

			ValidationResponse response = serviceClient.getValidationServicePort().validate(validateRequest);

			return response.getReport();
		} else {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Local validation modules should be invoked via their own implementations"));
		}
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
