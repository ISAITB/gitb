package com.gitb.vs.impl;

import com.gitb.core.AnyContent;
import com.gitb.core.ErrorCode;
import com.gitb.core.ValidationModule;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.*;
import com.gitb.types.DataType;
import com.gitb.utils.ErrorUtils;
import com.gitb.utils.XMLDateTimeUtils;
import com.gitb.utils.XMLUtils;
import com.gitb.vs.GetModuleDefinitionResponse;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.gitb.vs.ValidationService;
import com.sun.jersey.api.client.Client;
import net.validex.gitb.ValidexClient;
import net.validex.gitb.model.*;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.soap.Addressing;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by serbay.
 */
@Addressing(enabled = true, required = true)
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@WebService(name = "ValidationService", serviceName = "ValidationService", targetNamespace = "http://www.gitb.com/vs/v1/")
public class ValidationServiceImpl implements ValidationService {
	private static final String VALIDATOR_MODULE_DEFINITION_FILENAME = "validex-validator-definition.xml";

	private static final String VALIDATOR_MODULE_NAME_PARAM = "name";
	private static final String VALIDATOR_MODULE_DOCUMENT_PARAM = "document";

	private static final String VALIDATOR_MODULE_NAME_OUTPUT_PARAM = "name";
	private static final String VALIDATOR_MODULE_REPORT_ID_OUTPUT_PARAM = "reportId";
	private static final String VALIDATOR_MODULE_REPORT_LINK_OUTPUT_PARAM = "reportLink";
	private static final String VALIDATOR_MODULE_DOCUMENT_OUTPUT_PARAM = "document";

	private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

	@Override
	public GetModuleDefinitionResponse getModuleDefinition(@WebParam(name = "GetModuleDefinitionRequest", targetNamespace = "http://www.gitb.com/vs/v1/", partName = "parameters") com.gitb.vs.Void parameters) {
		try {
			ValidationModule validationModule = null;

			InputStream resource = getClass().getResourceAsStream(VALIDATOR_MODULE_DEFINITION_FILENAME);

			if(resource != null) {
				validationModule = XMLUtils.unmarshal(ValidationModule.class, new StreamSource(resource));
			}

			if(validationModule == null) {
				throw new IllegalStateException("Could not find the module definition.");
			}

			GetModuleDefinitionResponse response = new GetModuleDefinitionResponse();
			response.setModule(validationModule);

			return response;

		} catch (Exception e) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Could not read the module definition."), e);
		}
	}

	@Override
	public ValidationResponse validate(@WebParam(name = "ValidateRequest", targetNamespace = "http://www.gitb.com/vs/v1/", partName = "parameters") ValidateRequest parameters) {
		ValidexClient validexClient = ValidexClient.getInstance();

		String filename = getInputParameter(parameters.getInput(), VALIDATOR_MODULE_NAME_PARAM);
		String document = getInputParameter(parameters.getInput(), VALIDATOR_MODULE_DOCUMENT_PARAM);

		if(filename == null) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.MISSING_CONFIGURATION, "Filename parameter is required."));
		}

		if(document == null) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.MISSING_CONFIGURATION, "Document parameter is required."));
		}

		ValidateResponse validateResponse;
		ReportResponse reportResponse;

		try {
			validateResponse = validexClient.validate(filename, document);
		} catch (Exception e) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "An error occurred in the Validex validation phase"), e);
		}

		if(validateResponse == null) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "An error occurred in the Validex validation phase"));
		}

		try {
			reportResponse = validexClient.getReport(validateResponse.getId());
		} catch (Exception e) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "An error occurred while trying to get the validation report"), e);
		}

		if(reportResponse == null) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "An error occurred while trying to get the validation report"));
		}

		ValidationResponse response = new ValidationResponse();
		try {
			response.setReport(buildValidationReport(filename, document, validateResponse, reportResponse));
		} catch (Exception e) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "An error occurred while building the validation report"), e);
		}

		return response;
	}

	private TAR buildValidationReport(String filename, String document, ValidateResponse validateResponse, ReportResponse reportResponse) throws IOException, SAXException {
		Node node = XMLUtils.readXMLWithLineNumbers(new ByteArrayInputStream(document.getBytes()));

		TAR tar = new TAR();
		tar.setResult(validateResponse.isSuccessful() ? TestResultType.SUCCESS : TestResultType.FAILURE);
		tar.setName(validateResponse.getId());
		tar.setReports(new TestAssertionGroupReportsType());
        try {
            tar.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
        } catch (DatatypeConfigurationException e) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Exception while creating XMLGregorianCalendar"), e);
        }

		AnyContent attachment = new AnyContent();
		attachment.setType(DataType.MAP_DATA_TYPE);

		AnyContent filenameAttachment = new AnyContent();
		filenameAttachment.setName(VALIDATOR_MODULE_NAME_OUTPUT_PARAM);
		filenameAttachment.setValue(filename);
		filenameAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
		filenameAttachment.setType(DataType.STRING_DATA_TYPE);
		attachment.getItem().add(filenameAttachment);

		AnyContent reportIdAttachment = new AnyContent();
		reportIdAttachment.setName(VALIDATOR_MODULE_REPORT_ID_OUTPUT_PARAM);
		reportIdAttachment.setValue(validateResponse.getId());
		reportIdAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
		reportIdAttachment.setType(DataType.STRING_DATA_TYPE);
		attachment.getItem().add(reportIdAttachment);

		AnyContent reportLinkAttachment = new AnyContent();
		reportLinkAttachment.setName(VALIDATOR_MODULE_REPORT_LINK_OUTPUT_PARAM);
		reportLinkAttachment.setValue("https://validex.net/report/"+validateResponse.getId());
		reportLinkAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        reportLinkAttachment.setType(DataType.STRING_DATA_TYPE);
		attachment.getItem().add(reportLinkAttachment);

		AnyContent documentAttachment = new AnyContent();
		documentAttachment.setName(VALIDATOR_MODULE_DOCUMENT_OUTPUT_PARAM);
		documentAttachment.setValue(document);
		documentAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
		documentAttachment.setType(DataType.STRING_DATA_TYPE);
		attachment.getItem().add(documentAttachment);

		for(Validation validation : reportResponse.getReport().getValidations()) {
			for(ValidationResult validationResult : validation.getResults()) {
				if(!validationResult.isSuccess() && validationResult.getErrors() != null) {
					for(ValidationError validationError : validationResult.getErrors()) {

						BAR error = new BAR();
                        if(validationError.getText() != null && validationError.getMessage() != null) {
                            error.setDescription(validationError.getMessage() + '\n' + validationError.getText());
                        } else if(validationError.getText() != null) {
                            error.setDescription(validationError.getText());
                        } else {
                            error.setDescription(validationError.getMessage());
                        }
                        if(error.getDescription() != null) {
                            error.setDescription(error.getDescription());
                        }
                        error.setTest(validationError.getTest());

						String locationXPath =
                                validationError.getLocation() != null
                                        ? validationError.getLocation()
                                        : validationError.getXpath();

                        if(locationXPath != null) {
                            error.setLocation(VALIDATOR_MODULE_DOCUMENT_OUTPUT_PARAM + ':' + getLineNumbeFromXPath(node, locationXPath) + ":0");
                        } else {
                            error.setLocation(VALIDATOR_MODULE_DOCUMENT_OUTPUT_PARAM + ':' + validationError.getLine() + ':' + validationError.getColumn());
                        }

                        ObjectFactory objectFactory = new ObjectFactory();

						JAXBElement<TestAssertionReportType> element;
                        if(validationError.getFlag() != null && validationError.getFlag().equals("warning")) {
                            element = objectFactory.createTestAssertionGroupReportsTypeWarning(error);
                        } else {
                            element = objectFactory.createTestAssertionGroupReportsTypeError(error);
                        }

                        tar.getReports().getInfoOrWarningOrError().add(element);
					}
				}
			}
		}

		tar.setContext(attachment);

		return tar;
	}


	private String getLineNumbeFromXPath(Node node, String xpathExpression) {
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node childNode = null;
		try {
			childNode = (Node) xPath.evaluate(xpathExpression, node, XPathConstants.NODE);
            if(childNode != null) {
                return (String) childNode.getUserData("lineNumber");
            }
		} catch (XPathExpressionException e) {
			logger.error(e.getMessage());
		}
        return "0";
    }

	private String getInputParameter(List<AnyContent> inputs, String name) {
		String content = null;

		for(AnyContent anyContent : inputs) {
			if(anyContent.getName().equals(name)) {
				switch (anyContent.getEmbeddingMethod()) {
					case BASE_64:
						content = new String(Base64.decodeBase64(anyContent.getValue()));
						break;

					case STRING:
						content = anyContent.getValue();
						break;

					case URI:
						Client client = Client.create();
						content =
							client
								.resource(anyContent.getValue())
								.accept(MediaType.WILDCARD_TYPE)
								.get(String.class);
						break;
				}
				break;
			}
		}

		if(content == null) {
			for(AnyContent anyContent : inputs) {
				content = getInputParameter(anyContent.getItem(), name);

				if(content != null) {
					break;
				}
			}
		}

		return content;
	}
}
