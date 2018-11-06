package com.gitb.messaging.utils;

import com.gitb.core.AnyContent;
import com.gitb.core.MessagingModule;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.messaging.layer.AbstractMessagingHandler;
import com.gitb.tr.*;
import com.gitb.types.*;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.XMLDateTimeUtils;
import com.gitb.utils.XMLUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;

/**
 * Created by serbay.
 */
public class MessagingHandlerUtils {

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
		MessagingReport report = new MessagingReport(tar, outputMessage);
		return report;
	}

	public static Message getMessageFromReport(TAR report) {
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

	public static DataType toDataType(AnyContent content) {
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
				if (StringUtils.isBlank(content.getEncoding())) {
					type.deserialize(content.getValue().getBytes(), Charset.defaultCharset().toString());
				} else {
					type.deserialize(content.getValue().getBytes(Charset.forName(content.getEncoding())), content.getEncoding());
				}
			} else {
				throw new IllegalStateException("Only base64 and string embedding supported for object types");
			}
		} else {
			throw new IllegalStateException("Unsupported data type ["+content.getType()+"]");
		}
		return type;
	}

    public static MessagingReport generateErrorReport(Message message, Collection<Exception> nonCriticalErrors) {
        TAR report = null;
        try {
            report = new TAR();
            report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
            report.setResult(TestResultType.FAILURE);

	        AnyContent attachment = new AnyContent();

            if(message != null) {
	            attachment.setType(DataType.MAP_DATA_TYPE);
                for(Map.Entry<String, DataType> fragmentEntry : message.getFragments().entrySet()) {
	                attachment.getItem().add(DataTypeUtils.convertDataTypeToAnyContent(fragmentEntry.getKey(), fragmentEntry.getValue()));
                }
            }

            if (nonCriticalErrors != null) {
				for(Exception nonCriticalError : nonCriticalErrors) {
					BAR errorReport = new BAR();
					errorReport.setDescription(nonCriticalError.getMessage());

					report.setReports(new TestAssertionGroupReportsType());
					report.getReports()
							.getInfoOrWarningOrError()
							.add(objectFactory.createTestAssertionGroupReportsTypeError(errorReport));
				}
			}
        } catch (Exception e) {
            report = new TAR();
            report.setResult(TestResultType.FAILURE);
            report.setReports(new TestAssertionGroupReportsType());

            BAR errorReport = new BAR();
            errorReport.setDescription(e.getMessage());

            report.getReports()
                    .getInfoOrWarningOrError()
                    .add(objectFactory.createTestAssertionGroupReportsTypeError(errorReport));
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
			report = new TAR();
			report.setResult(TestResultType.FAILURE);
			report.setReports(new TestAssertionGroupReportsType());

			BAR errorReport = new BAR();
			errorReport.setDescription(e.getMessage());

			report.getReports()
				.getInfoOrWarningOrError()
				.add(objectFactory.createTestAssertionGroupReportsTypeError(errorReport));
		}
		return new MessagingReport(report);
	}

	public static MessagingReport generateSuccessReport(Message message) {
		TAR report = null;
		try {
			report = new TAR();
			report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
			report.setResult(TestResultType.SUCCESS);

			AnyContent attachment = new AnyContent();

            if(message != null) {
	            attachment.setType(DataType.MAP_DATA_TYPE);
                for(Map.Entry<String, DataType> fragmentEntry : message.getFragments().entrySet()) {
	                attachment.getItem().add(DataTypeUtils.convertDataTypeToAnyContent(fragmentEntry.getKey(), fragmentEntry.getValue()));
                }
            }

			report.setContext(attachment);

		} catch (Exception e) {
			report = new TAR();
			report.setResult(TestResultType.FAILURE);
			report.setReports(new TestAssertionGroupReportsType());

			BAR errorReport = new BAR();
			errorReport.setDescription(e.getMessage());

			report.getReports()
				.getInfoOrWarningOrError()
				.add(objectFactory.createTestAssertionGroupReportsTypeError(errorReport));
		}

		return new MessagingReport(report, message);
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
}
