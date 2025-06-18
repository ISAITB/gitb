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

package com.gitb.engine.validation.handlers.edi;

import jakarta.xml.bind.JAXBElement;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.BAR;
import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import com.gitb.tr.TestAssertionReportType;
import com.gitb.tr.TestResultType;
import com.gitb.types.DataType;
import com.gitb.types.StringType;
import com.gitb.engine.validation.handlers.common.AbstractReportHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EDIReportHandler extends AbstractReportHandler{

    private static final Logger logger = LoggerFactory.getLogger(EDIReportHandler.class);

	public static final String EDI_ITEM_NAME = "EDI";

    protected EDIReportHandler(StringType edi) {
        super();

        AnyContent attachment = new AnyContent();
	    attachment.setType(DataType.MAP_DATA_TYPE);

	    AnyContent ediAttachment = new AnyContent();
	    ediAttachment.setName(EDI_ITEM_NAME);
	    ediAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
	    ediAttachment.setType(StringType.STRING_DATA_TYPE);
	    ediAttachment.setValue((String)edi.getValue());
	    attachment.getItem().add(ediAttachment);
	    
	    report.setName("EDI Document Validation");
        report.setReports(new TestAssertionGroupReportsType());
	    report.setContext(attachment);
    }

    public TAR createReport() {
        //Report is filled by ErrorHandler methods
        return report;
    }

    public void addError(int result, String errorMessage) {
        report.setResult(TestResultType.FAILURE);
        BAR error = new BAR();
        error.setDescription(errorMessage);
        error.setLocation(EDI_ITEM_NAME+":"+(-result/100)+":"+(-result%100));
	    JAXBElement<TestAssertionReportType> element = objectFactory.createTestAssertionGroupReportsTypeError(error);
	    report.getReports().getInfoOrWarningOrError().add(element);
    }

    public void addError(String errorMessage) {
        report.setResult(TestResultType.FAILURE);
        BAR error = new BAR();
        error.setDescription(errorMessage);
        JAXBElement<TestAssertionReportType> element = objectFactory.createTestAssertionGroupReportsTypeError(error);
        report.getReports().getInfoOrWarningOrError().add(element);
    }
}