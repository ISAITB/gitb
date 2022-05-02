package com.gitb.engine.validation.handlers.xpath;

import com.gitb.core.Configuration;
import com.gitb.engine.validation.ValidationHandler;
import com.gitb.engine.validation.handlers.common.AbstractValidator;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.*;
import com.gitb.utils.NamespaceContext;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by root on 19.02.2015.
 */
@ValidationHandler(name="XPathValidator")
public class XPathValidator extends AbstractValidator {

    public static final String NAMESPACE_MAP_INPUT = "com.gitb.Namespaces";
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
        MapType namespaces = (MapType) inputs.get(NAMESPACE_MAP_INPUT);

        //compile xpath expression
        XPath xPath = new net.sf.saxon.xpath.XPathFactoryImpl().newXPath();
        if (namespaces != null) {
            var nsMap = new HashMap<String, String>();
            for (var entry: ((Map<String, DataType>)namespaces.getValue()).entrySet()) {
                nsMap.put(entry.getKey(), (String) entry.getValue().getValue());
            }
            xPath.setNamespaceContext(new NamespaceContext(nsMap));
        }
        XPathExpression xPathExpr;
        try {
            xPathExpr = xPath.compile((String)expression.getValue());
        } catch (XPathExpressionException e) {
            throw new GITBEngineInternalError(e);
        }

        //process xpath
        BooleanType result = (BooleanType) contentToProcess.processXPath(xPathExpr, DataType.BOOLEAN_DATA_TYPE);

        //return report
        XPathReportHandler handler = new XPathReportHandler(contentToProcess, expression, result);
        return handler.createReport();
    }
}
