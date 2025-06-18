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

package com.gitb.engine.validation.handlers.xsd;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.engine.validation.handlers.common.AbstractReportHandler;
import com.gitb.tr.*;
import com.gitb.types.DataType;
import com.gitb.types.ObjectType;
import com.gitb.types.SchemaType;
import org.springframework.http.MediaType;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import jakarta.xml.bind.JAXBElement;

/**
 * Created by senan on 10/10/14.
 */
public class XsdReportHandler extends AbstractReportHandler implements ErrorHandler {

	public static final String XML_ITEM_NAME = "xml";
	public static final String XSD_ITEM_NAME = "xsd";

    protected XsdReportHandler(ObjectType xml, SchemaType xsd) {
        super();

	    AnyContent attachment = new AnyContent();
	    attachment.setType(DataType.MAP_DATA_TYPE);

	    AnyContent xmlAttachment = new AnyContent();
	    xmlAttachment.setName(XML_ITEM_NAME);
        xmlAttachment.setMimeType(MediaType.APPLICATION_XML_VALUE);
	    xmlAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
	    xmlAttachment.setType(DataType.OBJECT_DATA_TYPE);
	    xmlAttachment.setValue(new String(xml.serializeByDefaultEncoding()));
	    attachment.getItem().add(xmlAttachment);

        if (xsd != null) {
            AnyContent xsdAttachment = new AnyContent();
            xsdAttachment.setName(XSD_ITEM_NAME);
            xsdAttachment.setType(DataType.SCHEMA_DATA_TYPE);
            xsdAttachment.setMimeType(MediaType.APPLICATION_XML_VALUE);
            xsdAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
            xsdAttachment.setValue(new String(xsd.serializeByDefaultEncoding()));
            attachment.getItem().add(xsdAttachment);
        }

        report.setName("XML Schema Validation");
        report.setReports(new TestAssertionGroupReportsType());
	    report.setContext(attachment);
    }

    @Override
    public TAR createReport() {
        //Report is filled by ErrorHandler methods
        return report;
    }

    @Override
    public void warning(SAXParseException exception) {
        BAR warning = new BAR();
        warning.setDescription( exception.getMessage());
	    warning.setLocation(XML_ITEM_NAME+":"+exception.getLineNumber()+":"+exception.getColumnNumber());
        JAXBElement<TestAssertionReportType> element = objectFactory.createTestAssertionGroupReportsTypeWarning(warning);
        report.getReports().getInfoOrWarningOrError().add(element);
    }

    @Override
    public void error(SAXParseException exception) {
        report.setResult(TestResultType.FAILURE);
        BAR error = new BAR();
        error.setDescription(exception.getMessage());
	    error.setLocation(XML_ITEM_NAME+":"+exception.getLineNumber()+":"+exception.getColumnNumber());
	    JAXBElement<TestAssertionReportType> element = objectFactory.createTestAssertionGroupReportsTypeError(error);
	    report.getReports().getInfoOrWarningOrError().add(element);
    }

    @Override
    public void fatalError(SAXParseException exception){
        report.setResult(TestResultType.FAILURE);
        BAR error = new BAR();
        error.setDescription(exception.getMessage());
	    error.setLocation(XML_ITEM_NAME+":"+exception.getLineNumber()+":"+exception.getColumnNumber());
        JAXBElement<TestAssertionReportType> element = objectFactory.createTestAssertionGroupReportsTypeError(error);
        report.getReports().getInfoOrWarningOrError().add(element);
    }
}
