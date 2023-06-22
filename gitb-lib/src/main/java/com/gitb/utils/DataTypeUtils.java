package com.gitb.utils;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.ListType;
import com.gitb.types.MapType;
import org.apache.commons.codec.binary.Base64;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Created by serbay.
 */
public class DataTypeUtils {

	private static final DataTypePostProcessor noActionPostProcessor = dataType -> {
		// Do nothing.
	};

	public static DataType convertAnyContentToDataType(AnyContent anyContent) {
		if (anyContent.getType() == null) {
			anyContent.setType(guessDataType(anyContent));
		}
		DataType data = DataTypeFactory.getInstance().create(anyContent.getType());
		setDataTypeValueWithAnyContent(data, anyContent);
		return data;
	}

	private static String guessDataType(AnyContent anyContent) {
		if (anyContent.getType() != null) {
			return anyContent.getType();
		} else if (anyContent.getItem().isEmpty()) {
			return DataType.STRING_DATA_TYPE;
		} else if (anyContent.getItem().get(0).getName() != null) {
			return DataType.MAP_DATA_TYPE;
		} else {
			return DataType.LIST_DATA_TYPE;
		}
	}

	public static AnyContent convertDataTypeToAnyContent(String name, DataType fragment) {

		AnyContent attachment = new AnyContent();
		attachment.setName(name);
		attachment.setType(fragment.getType());

		String typeToCheck = fragment.getType();
		if (DataType.isListType(fragment.getType())) {
			typeToCheck = DataType.LIST_DATA_TYPE;
		}
		switch (typeToCheck) {
			case DataType.OBJECT_DATA_TYPE:
			case DataType.SCHEMA_DATA_TYPE:
			case DataType.BINARY_DATA_TYPE: {
				attachment.setEmbeddingMethod(ValueEmbeddingEnumeration.BASE_64);
				if (fragment.getValue() == null) {
					attachment.setValue(null);
				} else {
					byte[] value = Base64.encodeBase64(fragment.serializeByDefaultEncoding());
					attachment.setValue(new String(value));
				}
				break;
			}
			case DataType.BOOLEAN_DATA_TYPE:
			case DataType.NUMBER_DATA_TYPE:
			case DataType.STRING_DATA_TYPE: {
				if (fragment.getValue() == null) {
					attachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
					attachment.setValue(null);
				} else {
					byte[] value = fragment.serializeByDefaultEncoding();
					attachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
					attachment.setValue(new String(value));
				}
				break;
			}
			case DataType.LIST_DATA_TYPE: {
				if (fragment.getValue() != null) {
					ListType listFragment = (ListType) fragment;
					for (int i = 0; i < listFragment.getSize(); i++) {
						DataType item = listFragment.getItem(i);
						AnyContent content = convertDataTypeToAnyContent(null, item);
						attachment.getItem().add(content);
					}
					attachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
				}
				break;
			}
			case DataType.MAP_DATA_TYPE:
				if (fragment.getValue() != null) {
					Map<String, DataType> values = (Map<String, DataType>) ((MapType)fragment).getValue();
					for (Map.Entry<String, DataType> entry : values.entrySet()) {
						AnyContent content = convertDataTypeToAnyContent(entry.getKey(), entry.getValue());
						attachment.getItem().add(content);
					}
				}
				break;
		}

		return attachment;
	}

	public static void setContentValueWithDataType(AnyContent content, DataType data) {
		AnyContent value = convertDataTypeToAnyContent(null, data);
		content.setEmbeddingMethod(value.getEmbeddingMethod());
		content.setType(value.getType());
		content.setValue(value.getValue());
	}

	public static void setDataTypeValueWithAnyContent(DataType data, AnyContent anyContent, DataTypePostProcessor postProcessor) {
		switch (anyContent.getType()) {
			case DataType.LIST_DATA_TYPE -> {
				ListType list = (ListType) data;

				for (AnyContent childItem : anyContent.getItem()) {
					DataType childData = convertAnyContentToDataType(childItem);
					postProcessor.process(childData);

					if (list.getContainedType() == null) {
						list.setContainedType(childData.getType());
					}

					list.append(childData);
				}
			}
			case DataType.MAP_DATA_TYPE -> {
				MapType map = (MapType) data;

				for (AnyContent childItem : anyContent.getItem()) {
					DataType childData = convertAnyContentToDataType(childItem);
					postProcessor.process(childData);

					map.addItem(childItem.getName(), childData);
				}

			}
			default -> {
				switch (anyContent.getEmbeddingMethod()) {
					case STRING -> {
						if (anyContent.getValue() == null) {
							data.setValue(null);
						} else {
							data.deserialize(anyContent.getValue().getBytes());
						}
						postProcessor.process(data);
					}
					case BASE_64 -> {
						data.deserialize(Base64.decodeBase64(
								EncodingUtils.extractBase64FromDataURL(anyContent.getValue())));
						postProcessor.process(data);
					}
					case URI -> {
						try {
							var request = HttpRequest
									.newBuilder()
									.uri(new URI(anyContent.getValue()))
									.header("Accept", "*/*")
									.GET()
									.build();
							var content = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
							data.deserialize(content.getBytes());
							postProcessor.process(data);
						} catch (URISyntaxException e) {
							throw new IllegalStateException("URI had invalid syntax ["+anyContent.getValue()+"]", e);
						} catch (Exception e) {
							throw new IllegalStateException("Error while calling URI ["+anyContent.getValue()+"]", e);
						}
					}
				}
			}
		}
	}

	public static void setDataTypeValueWithAnyContent(DataType data, AnyContent anyContent) {
		setDataTypeValueWithAnyContent(data, anyContent, noActionPostProcessor);
	}

	public interface DataTypePostProcessor {
		void process(DataType dataType);
	}

}
