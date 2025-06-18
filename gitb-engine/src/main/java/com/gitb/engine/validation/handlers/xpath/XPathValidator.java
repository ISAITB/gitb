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

import com.gitb.core.Configuration;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.utils.HandlerUtils;
import com.gitb.engine.validation.ValidationHandler;
import com.gitb.engine.validation.handlers.common.SimpleValidator;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.*;

import javax.xml.xpath.XPathExpression;
import java.util.List;
import java.util.Map;

/**
 * Created by root on 19.02.2015.
 */
@ValidationHandler(name="XPathValidator")
public class XPathValidator extends SimpleValidator {

    private static final String XML_ARGUMENT_NAME = "xml";
    private static final String EXPRESSION_ARGUMENT_NAME = "expression";
    private static final String MODULE_DEFINITION_XML = "/validation/xpath-validator-definition.xml";

    public XPathValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        ObjectType contentToProcess = getAndConvert(inputs, XML_ARGUMENT_NAME, DataType.OBJECT_DATA_TYPE, ObjectType.class);
        StringType expression = getAndConvert(inputs, EXPRESSION_ARGUMENT_NAME, DataType.STRING_DATA_TYPE, StringType.class);
        MapType namespaces = getAndConvert(inputs, HandlerUtils.NAMESPACE_MAP_INPUT, DataType.MAP_DATA_TYPE, MapType.class);
        String sessionId = (String) getAndConvert(inputs, HandlerUtils.SESSION_INPUT, DataType.STRING_DATA_TYPE, StringType.class).getValue();

        // Compile expression
        XPathExpression xPathExpr = HandlerUtils.compileXPathExpression(namespaces, expression, new VariableResolver(getScope(sessionId)));
        // Process expression
        BooleanType result = (BooleanType) contentToProcess.processXPath(xPathExpr, DataType.BOOLEAN_DATA_TYPE);
        // Return report
        return createReport(inputs, () -> new XPathReportHandler(contentToProcess, expression, result).createReport());
    }
}
