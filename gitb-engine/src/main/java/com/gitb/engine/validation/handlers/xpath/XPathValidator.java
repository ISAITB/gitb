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

    private static final String CONTENT_ARGUMENT_NAME = "xmldocument";
    private static final String XPATH_ARGUMENT_NAME = "xpathexpression";
    private static final String MODULE_DEFINITION_XML = "/validation/xpath-validator-definition.xml";

    public XPathValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        ObjectType contentToProcess = (ObjectType)inputs.get(CONTENT_ARGUMENT_NAME).convertTo(DataType.OBJECT_DATA_TYPE);
        StringType expression = (StringType) inputs.get(XPATH_ARGUMENT_NAME).convertTo(DataType.STRING_DATA_TYPE);
        MapType namespaces = (MapType) inputs.get(HandlerUtils.NAMESPACE_MAP_INPUT);
        String sessionId = (String) inputs.get(HandlerUtils.SESSION_INPUT).getValue();

        // Compile expression
        XPathExpression xPathExpr = HandlerUtils.compileXPathExpression(namespaces, expression, new VariableResolver(getScope(sessionId)));
        // Process expression
        BooleanType result = (BooleanType) contentToProcess.processXPath(xPathExpr, DataType.BOOLEAN_DATA_TYPE);
        // Return report
        return createReport(inputs, () -> new XPathReportHandler(contentToProcess, expression, result).createReport());
    }
}
