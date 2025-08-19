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

package com.gitb.engine.validation.handlers.xpath;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.engine.validation.handlers.common.AbstractReportHandler;
import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import com.gitb.tr.TestResultType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.ObjectType;
import com.gitb.types.StringType;
import org.springframework.http.MediaType;

/**
 * Created by root on 20.02.2015.
 */
public class XPathReportHandler extends AbstractReportHandler {

    public static final String XML_ITEM_NAME    = "xml";
    public static final String XPATH_ITEM_NAME  = "xpath";

    protected XPathReportHandler(ObjectType content, StringType xpath, BooleanType result) {
        super();

        report.setName("XPath Validation");
        report.setReports(new TestAssertionGroupReportsType());

	    AnyContent attachment = new AnyContent();
	    attachment.setType(DataType.MAP_DATA_TYPE);

	    AnyContent xml = new AnyContent();
	    xml.setName(XML_ITEM_NAME);
	    xml.setType(DataType.OBJECT_DATA_TYPE);
		xml.setMimeType(MediaType.APPLICATION_XML_VALUE);
	    xml.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
	    xml.setValue(new String(content.serializeByDefaultEncoding()));
	    attachment.getItem().add(xml);

	    AnyContent schema = new AnyContent();
	    schema.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
	    schema.setType(DataType.SCHEMA_DATA_TYPE);
	    schema.setName(XPATH_ITEM_NAME);
	    schema.setValue(new String(xpath.serializeByDefaultEncoding()));
	    attachment.getItem().add(schema);

	    if(result.getValue()) {
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
