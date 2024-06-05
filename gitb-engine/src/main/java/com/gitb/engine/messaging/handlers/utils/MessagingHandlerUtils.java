package com.gitb.engine.messaging.handlers.utils;

import com.gitb.core.AnyContent;
import com.gitb.core.MessagingModule;
import com.gitb.engine.messaging.handlers.layer.AbstractMessagingHandler;
import com.gitb.engine.messaging.handlers.layer.application.soap.AttachmentInfo;
import com.gitb.engine.messaging.handlers.layer.application.soap.SoapVersion;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.tr.*;
import com.gitb.types.*;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.XMLDateTimeUtils;
import com.gitb.utils.XMLUtils;
import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.xml.soap.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.MimeType;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.function.Supplier;

/**
 * Created by serbay.
 */
public class MessagingHandlerUtils {

	public static final String CONTENT_TYPE_HEADER = "Content-Type";
	public static final String XML_CONTENT_TYPE = "application/xml";
	public static final Set<String> TEXT_CONTENT_TYPES = Set.of(
			"application/xml",
			"application/json",
			"application/ld+json",
			"application/rdf+xml",
			"application/n-triples",
			"application/soap+xml"
	);
	public static final String STREAM_CONTENT_TYPE = "application/octet-stream";
	public static final String SOAP_START_HEADER = "<root>";

	private static final ObjectFactory objectFactory = new ObjectFactory();

	public static MessagingReport getMessagingReportForTimeout(String timeoutVariableName, boolean timeoutIsError) {
		Message timeoutMessage = new Message();
		if (!StringUtils.isBlank(timeoutVariableName)) {
			timeoutMessage.getFragments().put(timeoutVariableName, new BooleanType(true));
		}
		if (timeoutIsError) {
			return generateErrorReport(timeoutMessage, null);
		} else {
			return generateSuccessReport(timeoutMessage);
		}
	}

	public static MessagingReport getMessagingReport(TAR tar) {
		Message outputMessage = getMessageFromReport(tar);
		// The TAR's context is itself adapted from this call - no copy is made.
		tar.setContext(DataTypeFactory.getInstance().applyFilter(tar.getContext(), AnyContent::isForDisplay));
		return new MessagingReport(tar, outputMessage);
	}

	private static Message getMessageFromReport(TAR report) {
		Message message = new Message();
		AnyContent context = report.getContext();
		if (context.isForContext() && (context.getValue() != null || !context.getItem().isEmpty())) {
			var declaredType = DataTypeFactory.determineDataType(context);
			if (DataType.MAP_DATA_TYPE.equals(declaredType)) {
				for (AnyContent child : context.getItem()) {
					if (child.getName() != null) {
						var dataType = DataTypeFactory.getInstance().create(child, AnyContent::isForContext);
						if (dataType != null) {
							message.getFragments().put(child.getName(), dataType);
						}
					}
				}
			} else if (context.getName() != null) {
				var dataType = DataTypeFactory.getInstance().create(context, AnyContent::isForContext);
				if (dataType != null) {
					message.getFragments().put(context.getName(), dataType);
				}
			} else {
				throw new IllegalStateException("The type of the context set in the TAR report was invalid. " +
						"This should either be a 'map' with named child elements, or a named element of another type ('list', 'string', 'number', 'binary', 'object', 'schema'). " +
						"Returning an unnamed 'list' element is not allowed.");
			}
		}
		return message;
	}

	private static TAR reportFromMessage(Message message, TestResultType result) {
		TAR report;
		try {
			report = new TAR();
			report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
			report.setResult(result);
			AnyContent attachment = new AnyContent();
			if (message != null) {
				attachment.setType(DataType.MAP_DATA_TYPE);
				for (Map.Entry<String, DataType> fragmentEntry : message.getFragments().entrySet()) {
					attachment.getItem().add(DataTypeUtils.convertDataTypeToAnyContent(fragmentEntry.getKey(), fragmentEntry.getValue()));
				}
			}
			report.setContext(attachment);
		} catch (Exception e) {
			report = reportFromException(e);
		}
		return report;
	}

	private static TAR reportFromException(Exception e) {
		var report = new TAR();
		report.setResult(TestResultType.FAILURE);
		report.setReports(new TestAssertionGroupReportsType());
		BAR errorReport = new BAR();
		errorReport.setDescription(e.getMessage());
		report.getReports()
				.getInfoOrWarningOrError()
				.add(objectFactory.createTestAssertionGroupReportsTypeError(errorReport));
		return report;
	}

    public static MessagingReport generateErrorReport(Message message, Collection<Exception> nonCriticalErrors) {
        TAR report;
        try {
            report = reportFromMessage(message, TestResultType.FAILURE);
            if (nonCriticalErrors != null) {
				for (Exception nonCriticalError : nonCriticalErrors) {
					BAR errorReport = new BAR();
					errorReport.setDescription(nonCriticalError.getMessage());
					report.setReports(new TestAssertionGroupReportsType());
					report.getReports()
							.getInfoOrWarningOrError()
							.add(objectFactory.createTestAssertionGroupReportsTypeError(errorReport));
				}
			}
        } catch (Exception e) {
            report = reportFromException(e);
        }
        return new MessagingReport(report, message);
    }

	public static MessagingReport generateErrorReport(String message) {
		TAR report = new TAR();
		report.setResult(TestResultType.FAILURE);
		report.setReports(new TestAssertionGroupReportsType());

		BAR errorReport = new BAR();
		errorReport.setDescription(message);

		report.getReports()
				.getInfoOrWarningOrError()
				.add(objectFactory.createTestAssertionGroupReportsTypeError(errorReport));
		return new MessagingReport(report);
	}

	public static MessagingReport generateErrorReport(GITBEngineInternalError error) {
		TAR report = null;
		try {
			if(error != null) {
				report = new TAR();
				report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
				report.setResult(TestResultType.FAILURE);
				report.setReports(new TestAssertionGroupReportsType());

				BAR errorReport = generateBARReport(error);

				if(errorReport != null) {
					report.getReports()
						.getInfoOrWarningOrError()
						.add(objectFactory.createTestAssertionGroupReportsTypeError(errorReport)); // TODO add error report
				}

			}
		} catch (Exception e) {
			report = reportFromException(e);
		}
		return new MessagingReport(report);
	}

	public static MessagingReport generateSkipReport() {
		TAR report = null;
		try {
			report = new TAR();
			report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
			report.setResult(TestResultType.UNDEFINED);
		} catch (Exception e) {
			// Ignore
		}
		return new MessagingReport(report, null);
	}

	public static MessagingReport generateErrorReport(Message message) {
		return generateErrorReport(message, null);
	}

	public static MessagingReport generateSuccessReport(Message message) {
		return new MessagingReport(reportFromMessage(message, TestResultType.SUCCESS), message);
	}


	private static BAR generateBARReport(GITBEngineInternalError error) {
		BAR errorReport = null;

		if(error.getMessage() != null) {
			errorReport = new BAR();
			errorReport.setDescription(error.getMessage());
		} else if(error.getCause() != null) {
			errorReport = new BAR();
			errorReport.setDescription(error.getCause().getMessage());
		}

		return errorReport;
	}

	public static MessagingModule readModuleDefinition(String file) {
	    try {
	        MessagingModule module = null;
	        InputStream resource = AbstractMessagingHandler.class.getResourceAsStream(file);

	        if(resource != null) {
	            module = XMLUtils.unmarshal(MessagingModule.class, new StreamSource(resource));
	        }
	        return module;
	    } catch (Exception e) {
	        throw new GITBEngineInternalError(e);
	    }
	}

	public static <T extends DataType> T getAndConvert(Map<String, DataType> inputs, String inputName, String dataType, Class<T> dataTypeClass) {
		var input = inputs.get(inputName);
		if (input != null) {
			return dataTypeClass.cast(input.convertTo(dataType));
		} else {
			return null;
		}
	}

	public static Map<String, List<String>> getMapOfValues(Map<String, DataType> inputs, String argumentName) {
		return Optional.ofNullable(getAndConvert(inputs, argumentName, DataType.MAP_DATA_TYPE, MapType.class))
				.map(inputMap -> {
					Map<String, List<String>> headerMap = new HashMap<>();
					inputMap.getItems().forEach((key, value) -> {
						if (key != null && !key.isBlank()) {
							var headersForKey = headerMap.computeIfAbsent(key, k -> new ArrayList<>());
							if (value instanceof MapType mapType) {
								headersForKey.addAll(mapType.getItems().values().stream().map(x -> x.convertTo(DataType.STRING_DATA_TYPE).toString()).toList());
							} else if (value instanceof ListType listType) {
								headersForKey.addAll(listType.getElements().stream().map(x -> x.convertTo(DataType.STRING_DATA_TYPE).toString()).toList());
							} else {
								headersForKey.add(value.convertTo(DataType.STRING_DATA_TYPE).toString());
							}
						}
					});
					return headerMap;
				}).orElse(new HashMap<>(0));
	}

	public static Optional<BinaryType> getResponseBody(HttpResponse<byte[]> response) {
		return getResponseBody(response.body(), response.headers().map());
	}

	public static Optional<BinaryType> getResponseBody(byte[] responseBody, Map<String, List<String>> responseHeaders) {
		if (responseBody != null && responseBody.length > 0) {
			var dataItem = new BinaryType(responseBody);
			if (responseHeaders.containsKey(CONTENT_TYPE_HEADER)) {
				responseHeaders.get(CONTENT_TYPE_HEADER).stream()
						.findFirst()
						.flatMap(contentType -> Arrays.stream(StringUtils.split(contentType, ';')).findFirst())
						.ifPresent(contentType -> dataItem.setContentType(contentType.trim()));
			}
			return Optional.of(dataItem);
		}
		return Optional.empty();
	}


	public static Optional<String> getUriExtension(Map<String, DataType> data, String inputName) {
		return Optional.ofNullable(getAndConvert(data, inputName, DataType.STRING_DATA_TYPE, StringType.class))
				.flatMap(value -> {
					var valueStr = value.toString();
					if (valueStr == null || valueStr.isBlank()) {
						return Optional.empty();
					} else {
						return Optional.of(valueStr.trim().toLowerCase());
					}
				});
	}

	public static Optional<HttpMethod> getMethod(Map<String, DataType> data, String inputName) {
		return Optional.ofNullable(getAndConvert(data, inputName, DataType.STRING_DATA_TYPE, StringType.class))
				.map(value -> HttpMethod.valueOf(value.toString()));
	}

	public static HttpStatus getStatus(Map<String, DataType> data, String inputName, Supplier<HttpStatus> defaultSupplier) {
		return Optional.ofNullable(getAndConvert(data, inputName, DataType.NUMBER_DATA_TYPE, NumberType.class))
				.map(value -> HttpStatus.valueOf(value.intValue()))
				.orElseGet(defaultSupplier);
	}

	public static byte[] getSoapEnvelope(Map<String, DataType> data, String inputName) {
		return Optional.ofNullable(getAndConvert(data, inputName, DataType.OBJECT_DATA_TYPE, ObjectType.class))
				.map(ObjectType::serializeByDefaultEncoding)
				.orElseThrow(() -> new IllegalArgumentException("Input [%s] must be provided".formatted(inputName)));
	}

	public static SoapVersion getSoapVersion(Map<String, DataType> data, String inputName, Supplier<SoapVersion> defaultSupplier) {
		return Optional.ofNullable(getAndConvert(data, inputName, DataType.STRING_DATA_TYPE, StringType.class))
				.map(v -> SoapVersion.forInput(v.toString()))
				.orElseGet(defaultSupplier);
	}

	public static Optional<List<AttachmentInfo>> getSoapAttachments(Map<String, DataType> data, String inputName) {
		return Optional.ofNullable(getAndConvert(data, inputName, DataType.LIST_DATA_TYPE, ListType.class))
				.map(listType -> listType.getElements().stream().map(item -> {
					if (item instanceof MapType mapItem) {
						/*
						 * Each map entry corresponds to an attachment to add. The map has the following sub-entries:
						 * - name: The content ID.
						 * - contentType: The mime type.
						 * - content: The attachment content.
						 * - forceText: To force the attachment content to be added as text in the HTTP request.
						 */
						String contentType = Optional.ofNullable(mapItem.getItem("contentType")).map(value -> value.convertTo(DataType.STRING_DATA_TYPE).toString()).orElse(STREAM_CONTENT_TYPE);
						boolean includeAsString = Optional.ofNullable(mapItem.getItem("forceText")).map(value -> value.convertTo(DataType.BOOLEAN_DATA_TYPE))
								.map(v -> (Boolean) v.getValue())
								.orElseGet(() -> "text".equals(MimeType.valueOf(contentType).getType()) || TEXT_CONTENT_TYPES.contains(contentType));
						DataType content;
						if (includeAsString) {
							content = mapItem.getItem("content").convertTo(DataType.STRING_DATA_TYPE);
						} else {
							content = mapItem.getItem("content").convertTo(DataType.BINARY_DATA_TYPE);
						}
						content.setContentType(contentType);
						return new AttachmentInfo(
								Objects.requireNonNull(mapItem.getItem("name"), "The [%s] input must be a map containing a [name] entry.".formatted(inputName)).convertTo(DataType.STRING_DATA_TYPE).toString(),
								contentType,
								content,
								includeAsString
						);
					} else {
						throw new IllegalStateException("The [%s] input must contain map entries (one per attachment)");
					}
				}).toList());
	}

	public static ObjectType getSoapEnvelope(SOAPMessage message) {
		var type = new ObjectType(message.getSOAPPart());
		type.setContentType(XML_CONTENT_TYPE);
		return type;
	}

	public static Optional<ObjectType> getSoapBody(SOAPMessage message) {
		try {
			return getFirstElement(message.getSOAPBody()).map(v -> {
				var type = new ObjectType(v);
				type.setContentType(XML_CONTENT_TYPE);
				return type;
			});
		} catch (SOAPException e) {
			throw new IllegalStateException("Unable to parse SOAP body", e);
		}
	}

	private static Optional<Node> getFirstElement(SOAPBody soapBody) {
		Iterator<?> it = soapBody.getChildElements();
		while (it.hasNext()) {
			Node el = (Node) it.next();
			if (el.getNodeType() == Node.ELEMENT_NODE) {
				return Optional.of(el);
			}
		}
		return Optional.empty();
	}

	public static SOAPMessage deserialiseSoapMessage(SoapVersion soapVersion, byte[] body, Optional<String> contentTypeHeader) {
		return deserialiseSoapMessage(soapVersion, new ByteArrayInputStream(body), contentTypeHeader);
	}

	public static SOAPMessage deserialiseSoapMessage(SoapVersion soapVersion, InputStream body, Optional<String> contentTypeHeader) {
		var mimeHeaders = new MimeHeaders();
		contentTypeHeader.ifPresent(header -> mimeHeaders.addHeader(CONTENT_TYPE_HEADER, header));
		SOAPMessage soapMessage;
		try (body) {
			soapMessage = soapVersion.buildMessageFactory().createMessage(mimeHeaders, body);
		} catch (IOException | SOAPException e) {
			throw new IllegalStateException("Unable to create SOAP message", e);
		}
		return soapMessage;
	}

	public static SOAPMessage createSoapMessage(SoapVersion soapVersion, byte[] soapEnvelopeContent, Optional<List<AttachmentInfo>> attachments) {
		SOAPMessage soapMessage;
		try (var in = new ByteArrayInputStream(soapEnvelopeContent)) {
			soapMessage = soapVersion.buildMessageFactory().createMessage(null, in);
		} catch (IOException | SOAPException e) {
			throw new IllegalStateException("Unable to create SOAP message", e);
		}
		soapMessage.getSOAPPart().setContentId(SOAP_START_HEADER);
		attachments.ifPresent(parts -> parts.forEach(part -> {
			AttachmentPart attachmentPart = soapMessage.createAttachmentPart();
			if (part.isText()) {
				attachmentPart.setContent(part.content().toString(), part.contentType());
			} else {
				attachmentPart.setDataHandler(new DataHandler(new ByteArrayDataSource(part.content().serializeByDefaultEncoding(), part.contentType())));
			}
			attachmentPart.setContentId(part.name());
			soapMessage.addAttachmentPart(attachmentPart);
		}));
		return soapMessage;
	}

	public static String createSoapContentTypeHeader(SOAPMessage soapMessage, SoapVersion soapVersion) {
		String[] soapHeaders = soapMessage.getMimeHeaders().getHeader(CONTENT_TYPE_HEADER);
		if (soapMessage.countAttachments() != 0 && soapHeaders != null) {
			// Add MTOM specific Content-Type.
			var soapContentType = new StringBuilder("multipart/related; type=\"application/xop+xml\"; start-info=\"")
					.append(soapVersion.getContentType())
					.append("\"; start=\"").append(SOAP_START_HEADER)
					.append("\";");
			// Add boundaries
			for (String soapHeader: soapHeaders[0].split(";")) {
				if (soapHeader.contains("boundary")) {
					soapContentType.append(soapHeader).append(";");
				}
			}
			return soapContentType.toString();
		} else {
			return soapVersion.getContentType();
		}
	}

	public static byte[] soapMessageToBytes(SOAPMessage soapMessage) {
		byte[] httpBody;
		try (var outputStream = new ByteArrayOutputStream()) {
			soapMessage.writeTo(outputStream);
			httpBody = outputStream.toByteArray();
		} catch (IOException | SOAPException e) {
			throw new IllegalStateException("Unable to create HTTP message from SOAP envelope", e);
		}
		return httpBody;
	}

	public static Optional<MapType> getAttachments(SOAPMessage soapMessage) {
		if (soapMessage.countAttachments() > 0) {
			var attachmentType = new MapType();
			soapMessage.getAttachments().forEachRemaining(attachment -> {
				try {
					var content = attachment.getContent();
					DataType item = null;
					if (content instanceof String stringContent) {
						item = new StringType(stringContent);
					} else if (content instanceof InputStream streamContent) {
						item = new BinaryType(IOUtils.toByteArray(streamContent));
					}
					if (item != null) {
						item.setContentType(attachment.getContentType());
						attachmentType.addItem(attachment.getContentId(), item);
					}
				} catch (SOAPException | IOException e) {
					throw new IllegalStateException("Error while reading received attachment", e);
				}
			});
			if (!attachmentType.isEmpty()) {
				return Optional.of(attachmentType);
			}
		}
		return Optional.empty();
	}

	public static Optional<MapType> getHeadersForReport(Map<String, List<String>> headers) {
		if (headers.isEmpty()) {
			return Optional.empty();
		} else {
			var headersItem = new MapType();
			headers.forEach((key, value) -> {
				headersItem.addItem(key, new StringType(String.join(", ", value)));
			});
			return Optional.of(headersItem);
		}
	}

	public static SoapVersion soapVersionFromHeaders(Map<String, List<String>> headers) {
		return headers.entrySet().stream()
				.filter(entry -> CONTENT_TYPE_HEADER.equals(entry.getKey()))
				.findFirst()
				.flatMap(entry -> entry.getValue().stream()
						.filter(value -> value.contains(SoapVersion.VERSION_1_2.getContentType()))
						.findAny()
						.map(value -> SoapVersion.VERSION_1_2))
				.orElse(SoapVersion.VERSION_1_1);
	}
}
