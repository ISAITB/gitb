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

package com.gitb.utils;

import com.gitb.core.AnyContent;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.tr.*;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;

public class MessagingReportUtils {

    private static final ObjectFactory objectFactory = new ObjectFactory();

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

}
