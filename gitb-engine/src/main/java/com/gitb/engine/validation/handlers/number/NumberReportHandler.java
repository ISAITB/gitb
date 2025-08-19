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

package com.gitb.engine.validation.handlers.number;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import com.gitb.tr.TestResultType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.NumberType;
import com.gitb.engine.validation.handlers.common.AbstractReportHandler;

/**
 * Created by Roch Bertucat
 */
public class NumberReportHandler extends AbstractReportHandler {

    public static final String ACTUAL_ITEM_NAME = "actual";
    public static final String EXPECTED_ITEM_NAME = "expected";

    protected NumberReportHandler(NumberType actual, NumberType expected, BooleanType result) {
        super();

        report.setName("Number Validation");
        report.setReports(new TestAssertionGroupReportsType());

        AnyContent attachment = new AnyContent();
        attachment.setType(DataType.MAP_DATA_TYPE);

        AnyContent actualItem = new AnyContent();
        actualItem.setName(ACTUAL_ITEM_NAME);
        actualItem.setType(DataType.NUMBER_DATA_TYPE);
        actualItem.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        actualItem.setValue(new String(actual.serializeByDefaultEncoding()));
        attachment.getItem().add(actualItem);

        AnyContent expectedItem = new AnyContent();
        expectedItem.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        expectedItem.setType(DataType.NUMBER_DATA_TYPE);
        expectedItem.setName(EXPECTED_ITEM_NAME);
        expectedItem.setValue(new String(expected.serializeByDefaultEncoding()));
        attachment.getItem().add(expectedItem);

        if (result.getValue()) {
            report.setResult(TestResultType.SUCCESS);
        } else {
            report.setResult(TestResultType.FAILURE);
        }
        report.setContext(attachment);
    }

    @Override
    public TAR createReport() {
        return report;
    }
}
