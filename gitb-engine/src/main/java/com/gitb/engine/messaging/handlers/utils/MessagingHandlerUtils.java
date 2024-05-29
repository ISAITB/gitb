package com.gitb.engine.messaging.handlers.utils;

import com.gitb.core.AnyContent;
import com.gitb.core.MessagingModule;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.engine.messaging.handlers.layer.AbstractMessagingHandler;
import com.gitb.tr.*;
import com.gitb.types.*;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.XMLDateTimeUtils;
import com.gitb.utils.XMLUtils;
import org.apache.commons.lang3.StringUtils;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.*;

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

	public static MessagingReport generateSuccessReport(Message message) {
		TAR report;
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

}
