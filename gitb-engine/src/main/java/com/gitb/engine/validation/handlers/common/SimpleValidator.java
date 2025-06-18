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

package com.gitb.engine.validation.handlers.common;

import com.gitb.tr.*;
import com.gitb.types.DataType;
import com.gitb.types.StringType;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class SimpleValidator extends AbstractValidator {

    protected static final String SUCCESS_MESSAGE_ARGUMENT_NAME   = "successMessage";
    protected static final String FAILURE_MESSAGE_ARGUMENT_NAME = "failureMessage";

    protected TAR createReport(Map<String, DataType> inputs, Supplier<TAR> reportSupplier) {
        TAR report = reportSupplier.get();
        Optional<String> customMessage;
        if (report.getResult() == TestResultType.SUCCESS) {
            customMessage = Optional.ofNullable(getAndConvert(inputs, SUCCESS_MESSAGE_ARGUMENT_NAME, DataType.STRING_DATA_TYPE, StringType.class)).map(StringType::toString);
        } else if (report.getResult() == TestResultType.FAILURE || report.getResult() == TestResultType.WARNING) {
            customMessage = Optional.ofNullable(getAndConvert(inputs, FAILURE_MESSAGE_ARGUMENT_NAME, DataType.STRING_DATA_TYPE, StringType.class)).map(StringType::toString);
        } else {
            customMessage = Optional.empty();
        }
        if (customMessage.isPresent()) {
            if (report.getReports() == null) {
                report.setReports(new TestAssertionGroupReportsType());
            }
            int errors = 0;
            int warnings = 0;
            int messages = 0;
            if (report.getCounters() == null) {
                report.setCounters(new ValidationCounters());
            } else {
                errors = report.getCounters().getNrOfErrors().intValue();
                warnings = report.getCounters().getNrOfWarnings().intValue();
                messages = report.getCounters().getNrOfAssertions().intValue();
            }
            BAR messageItem = new BAR();
            messageItem.setDescription(customMessage.get());
            switch (report.getResult()) {
                case TestResultType.SUCCESS -> {
                    messages += 1;
                    report.getCounters().setNrOfAssertions(BigInteger.valueOf(messages));
                    var item = objectFactory.createTestAssertionGroupReportsTypeInfo(messageItem);
                    report.getReports().getInfoOrWarningOrError().add(item);
                }
                case TestResultType.WARNING -> {
                    warnings += 1;
                    report.getCounters().setNrOfWarnings(BigInteger.valueOf(warnings));
                    var item = objectFactory.createTestAssertionGroupReportsTypeWarning(messageItem);
                    report.getReports().getInfoOrWarningOrError().add(item);
                }
                default -> {
                    errors += 1;
                    report.getCounters().setNrOfErrors(BigInteger.valueOf(errors));
                    var item = objectFactory.createTestAssertionGroupReportsTypeError(messageItem);
                    report.getReports().getInfoOrWarningOrError().add(item);
                }
            }
        }
        return report;
    }

}
