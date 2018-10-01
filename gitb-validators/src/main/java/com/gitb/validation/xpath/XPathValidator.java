package com.gitb.validation.xpath;

import com.gitb.core.Configuration;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.*;
import com.gitb.validation.IValidationHandler;
import com.gitb.validation.common.AbstractValidator;
import com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl;
import com.sun.org.apache.xpath.internal.jaxp.XPathImpl;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.util.List;
import java.util.Map;

/**
 * Created by root on 19.02.2015.
 */
@MetaInfServices(IValidationHandler.class)
public class XPathValidator extends AbstractValidator {

    private static final Logger logger = LoggerFactory.getLogger(XPathValidator.class);

    private final String CONTENT_ARGUMENT_NAME = "xmldocument";
    private final String XPATH_ARGUMENT_NAME   = "xpathexpression";

    private final String MODULE_DEFINITION_XML = "/xpath-validator-definition.xml";

    public XPathValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        ObjectType contentToProcess = (ObjectType)inputs.get(CONTENT_ARGUMENT_NAME).convertTo(DataType.OBJECT_DATA_TYPE);
        StringType expression = (StringType) inputs.get(XPATH_ARGUMENT_NAME).convertTo(DataType.STRING_DATA_TYPE);

        //compile xpath expression
        XPathImpl xPath = (XPathImpl) new XPathFactoryImpl().newXPath();
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
