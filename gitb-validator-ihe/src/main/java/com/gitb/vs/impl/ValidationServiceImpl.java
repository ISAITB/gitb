package com.gitb.vs.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.ws.rs.core.MediaType;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.soap.Addressing;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.gitb.core.AnyContent;
import com.gitb.core.ErrorCode;
import com.gitb.core.ValidationModule;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import com.gitb.tr.TestResultType;
import com.gitb.types.DataType;
import com.gitb.types.ObjectType;
import com.gitb.types.StringType;
import com.gitb.utils.ErrorUtils;
import com.gitb.utils.XMLDateTimeUtils;
import com.gitb.utils.XMLUtils;
import com.gitb.vs.GetModuleDefinitionResponse;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.gitb.vs.ValidationService;
import com.sun.jersey.api.client.Client;
import com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl;
import com.sun.org.apache.xpath.internal.jaxp.XPathImpl;

/**
 * Created by Roch Bertucat
 */
@Addressing(enabled = true, required = true)
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@WebService(
        name = "ValidationService",
        serviceName = "ValidationService",
        targetNamespace = "http://www.gitb.com/vs/v1/")
public class ValidationServiceImpl implements ValidationService {
    private static final String VALIDATOR_MODULE_DEFINITION_FILENAME = "ihe-gazelle-xdsmetadata-validator-definition.xml";

    private static final String WELLFORMED_XML_ITEM_NAME = "WELL FORMED XML";
    private static final String VALID_XSD_ITEM_NAME = "VALID XSD";
    private static final String VALID_MODEL_ITEM_NAME = "VALID MODEL";
    private static final String VALIDATED_XML_ITEM_NAME = "VALIDATED XML";
    private static final String SOAP_REQUEST_ITEM_NAME = "SOAP REQUEST";
    private static final String SOAP_RESPONSE_ITEM_NAME = "SOAP RESPONSE";
    private static final String PASSED = "PASSED";

    private static final String VALIDATOR_MODULE_XML_DOCUMENT_PARAM = "xmldocument";
    private static final String VALIDATOR_MODULE_VALIDATOR_NAME_PARAM = "validatorname";

    private static final String GAZELLE_XDS_METADATA_ENDPOINT = "http://131.254.209.20:8080/XDStarClient-XDStarClient-ejb/XDSMetadataValidatorWS";

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    @Override
    public GetModuleDefinitionResponse getModuleDefinition(@WebParam(
            name = "GetModuleDefinitionRequest",
            targetNamespace = "http://www.gitb.com/vs/v1/",
            partName = "parameters") com.gitb.vs.Void parameters) {
        try {
            ValidationModule validationModule = null;

            InputStream resource = getClass().getResourceAsStream(VALIDATOR_MODULE_DEFINITION_FILENAME);

            if (resource != null) {
                validationModule = XMLUtils.unmarshal(ValidationModule.class, new StreamSource(resource));
            }

            if (validationModule == null) {
                throw new IllegalStateException("Could not find the module definition.");
            }

            GetModuleDefinitionResponse response = new GetModuleDefinitionResponse();
            response.setModule(validationModule);

            return response;

        } catch (Exception e) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR,
                    "Could not read the module definition."), e);
        }
    }

    @Override
    public ValidationResponse validate(@WebParam(
            name = "ValidateRequest",
            targetNamespace = "http://www.gitb.com/vs/v1/",
            partName = "parameters") ValidateRequest parameters) {

        String xmlDocument = getInputParameter(parameters.getInput(), VALIDATOR_MODULE_XML_DOCUMENT_PARAM);
        String validatorName = getInputParameter(parameters.getInput(), VALIDATOR_MODULE_VALIDATOR_NAME_PARAM);

        if (xmlDocument == null) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.MISSING_CONFIGURATION,
                    "XML Document parameter is required."));
        }

        if (validatorName == null) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.MISSING_CONFIGURATION,
                    "Validator Name parameter is required."));
        }

        // encode
        String base64 = Base64.encodeBase64String(xmlDocument.getBytes());

        ObjectType requestObject = null;
        ObjectType responseObject = null;
        try {
            // create SOAP message
            MessageFactory mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
            SOAPMessage message = mf.createMessage();
            SOAPBody body = message.getSOAPBody();
            SOAPFactory soapFactory = SOAPFactory.newInstance();

            // call validateXDStarMetadataB64 service
            SOAPBodyElement bodyElement = body.addBodyElement(soapFactory.createName("validateXDStarMetadataB64",
                    "tns", "http://ws.mb.validator.gazelle.ihe.net"));

            // base64ObjectToValidate
            SOAPElement base64Element = bodyElement.addChildElement(soapFactory.createName("base64ObjectToValidate"));
            base64Element.addTextNode(base64);

            // arg1
            SOAPElement arg1Element = bodyElement.addChildElement(soapFactory.createName("arg1"));
            arg1Element.addTextNode(validatorName);

            // wrap request
            requestObject = new ObjectType(body);

            logger.debug("Constructed soap message to be sent to the validator");

            // call
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection connection = soapConnectionFactory.createConnection();
            SOAPMessage response = connection.call(message, new URL(GAZELLE_XDS_METADATA_ENDPOINT));

            logger.debug("Sent soap message to: " + GAZELLE_XDS_METADATA_ENDPOINT);

            // wrap response after unescaping XML
            responseObject = new ObjectType(response.getSOAPBody());
            responseObject.deserialize(StringEscapeUtils.unescapeXml(responseObject.toString()).getBytes());

        } catch (SOAPException e) {
            throw new GITBEngineInternalError(e);
        } catch (MalformedURLException e) {
            throw new GITBEngineInternalError(e);
        }

        ValidationResponse response = new ValidationResponse();
        try {
            // create the validation report
            response.setReport(buildValidationReport(xmlDocument, requestObject, responseObject));
        } catch (Exception e) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR,
                    "An error occurred while building the validation report"), e);
        }

        return response;
    }

    private TAR buildValidationReport(String xml, ObjectType request, ObjectType response) throws IOException,
            SAXException {

        // check response
        String wellFormed = getWellFormed(response);
        String validXSD = getValidXSD(response);
        String validModel = getValidModel(response);

        TAR tar = new TAR();
        tar.setResult(wellFormed.equals(PASSED) && validXSD.equals(PASSED) && validModel.equals(PASSED) ? TestResultType.SUCCESS
                : TestResultType.FAILURE);
        tar.setReports(new TestAssertionGroupReportsType());
        try {
            tar.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
        } catch (DatatypeConfigurationException e) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR,
                    "Exception while creating XMLGregorianCalendar"), e);
        }

        AnyContent attachment = new AnyContent();
        attachment.setType(DataType.MAP_DATA_TYPE);

        AnyContent wellFormedAttachment = new AnyContent();
        wellFormedAttachment.setName(WELLFORMED_XML_ITEM_NAME);
        wellFormedAttachment.setValue(wellFormed);
        wellFormedAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        wellFormedAttachment.setType(DataType.STRING_DATA_TYPE);
        attachment.getItem().add(wellFormedAttachment);

        AnyContent validXSDAttachment = new AnyContent();
        validXSDAttachment.setName(VALID_XSD_ITEM_NAME);
        validXSDAttachment.setValue(validXSD);
        validXSDAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        validXSDAttachment.setType(DataType.STRING_DATA_TYPE);
        attachment.getItem().add(validXSDAttachment);

        AnyContent validModelAttachment = new AnyContent();
        validModelAttachment.setName(VALID_MODEL_ITEM_NAME);
        validModelAttachment.setValue(validModel);
        validModelAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        validModelAttachment.setType(DataType.STRING_DATA_TYPE);
        attachment.getItem().add(validModelAttachment);

        AnyContent xmlAttachment = new AnyContent();
        xmlAttachment.setName(VALIDATED_XML_ITEM_NAME);
        xmlAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        xmlAttachment.setType(DataType.OBJECT_DATA_TYPE);
        xmlAttachment.setValue(xml);
        attachment.getItem().add(xmlAttachment);

        AnyContent requestAttachment = new AnyContent();
        requestAttachment.setName(SOAP_REQUEST_ITEM_NAME);
        xmlAttachment.setType(DataType.OBJECT_DATA_TYPE);
        requestAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        requestAttachment.setValue(request.toString());
        attachment.getItem().add(requestAttachment);

        AnyContent responseAttachment = new AnyContent();
        responseAttachment.setName(SOAP_RESPONSE_ITEM_NAME);
        xmlAttachment.setType(DataType.OBJECT_DATA_TYPE);
        responseAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        responseAttachment.setValue(response.toString());
        attachment.getItem().add(responseAttachment);

        tar.setName("Gazelle XDS Metadata Validation");
        tar.setReports(new TestAssertionGroupReportsType());
        tar.setContext(attachment);

        tar.setContext(attachment);

        return tar;
    }

    private String getValidModel(ObjectType response) {
        return getFromXPath(response, "//MDAValidation/Result/text()", DataType.STRING_DATA_TYPE);
    }

    private String getValidXSD(ObjectType response) {
        return getFromXPath(response, "//DocumentValidXSD/Result/text()", DataType.STRING_DATA_TYPE);
    }

    private String getWellFormed(ObjectType response) {
        return getFromXPath(response, "//DocumentWellFormed/Result/text()", DataType.STRING_DATA_TYPE);
    }

    private String getFromXPath(ObjectType object, String expression, String dt) {
        // compile xpath expression
        XPathImpl xPath = (XPathImpl) new XPathFactoryImpl().newXPath();
        XPathExpression xPathExpr;
        try {
            xPathExpr = ((XPath) xPath).compile(expression);
        } catch (XPathExpressionException e) {
            throw new GITBEngineInternalError(e);
        }
        // process xpath
        StringType result = (StringType) object.processXPath(xPathExpr, dt);
        return (String) result.getValue();
    }

    private String getInputParameter(List<AnyContent> inputs, String name) {
        String content = null;

        for (AnyContent anyContent : inputs) {
            if (anyContent.getName().equals(name)) {
                switch (anyContent.getEmbeddingMethod()) {
                case BASE_64:
                    content = new String(Base64.decodeBase64(anyContent.getValue()));
                    break;

                case STRING:
                    content = anyContent.getValue();
                    break;

                case URI:
                    Client client = Client.create();
                    content = client.resource(anyContent.getValue()).accept(MediaType.WILDCARD_TYPE).get(String.class);
                    break;
                }
                break;
            }
        }

        if (content == null) {
            for (AnyContent anyContent : inputs) {
                content = getInputParameter(anyContent.getItem(), name);

                if (content != null) {
                    break;
                }
            }
        }

        return content;
    }
}
