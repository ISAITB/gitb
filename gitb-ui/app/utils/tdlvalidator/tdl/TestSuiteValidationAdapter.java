package utils.tdlvalidator.tdl;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.gitb.vs.ValidationService;
import com.gitb.vs.ValidationService_Service;
import com.gitb.vs.tdl.*;
import config.Configurations;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class TestSuiteValidationAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(TestSuiteValidationAdapter.class);
    private static final Object MUTEX = new Object();
    private static TestSuiteValidationAdapter INSTANCE;

    private final ObjectFactory objectFactory = new ObjectFactory();
    private Set<String> dataTypes;
    private Set<String> containerDataTypes;
    private Set<String> containedDataTypes;
    private Set<String> acceptedMimeTypes;
    private final Map<String, ExternalConfiguration.MessagingHandlerConfiguration> embeddedMessagingHandlers = new HashMap<>();
    private final Map<String, ExternalConfiguration.ProcessingHandlerConfiguration> embeddedProcessingHandlers = new HashMap<>();
    private final Map<String, ExternalConfiguration.ValidationHandlerConfiguration> embeddedValidationHandlers = new HashMap<>();
    private ValidationService remoteServiceClient;

    public static TestSuiteValidationAdapter getInstance() {
        if (INSTANCE == null) {
            synchronized (MUTEX) {
                if (INSTANCE == null) {
                    TestSuiteValidationAdapter temp = new TestSuiteValidationAdapter();
                    temp.initialise();
                    INSTANCE = temp;
                }
            }
        }
        return INSTANCE;
    }

    private void initialise() {
        if (!Configurations.VALIDATION_TDL_EXTERNAL_ENABLED()) {
            Properties configProperties = new Properties();
            try {
                configProperties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("validator-tdl/validator.properties"));
            } catch (IOException e) {
                throw new IllegalStateException("Error while loading GITB TDL validator configuration", e);
            }
            dataTypes = asSet(getConfigValue("validator.dataTypes", configProperties));
            containerDataTypes = asSet(getConfigValue("validator.containerDataTypes", configProperties));
            containedDataTypes = asSet(getConfigValue("validator.containedDataTypes", configProperties));
            acceptedMimeTypes = asSet(getConfigValue("validator.acceptedMimeTypes", configProperties));
            // Handler configuration
            for (String handler : asSet(getConfigValue("validator.validationHandlers", configProperties))) {
                ExternalConfiguration.ValidationHandlerConfiguration config = new ExternalConfiguration.ValidationHandlerConfiguration();
                config.setDeprecated(Boolean.parseBoolean(Optional.ofNullable(getConfigValue("validator.validationHandlers." + handler + ".deprecated", configProperties)).orElse("false")));
                if (config.isDeprecated()) {
                    config.setReplacement(getConfigValue("validator.validationHandlers." + handler + ".replacedBy", configProperties));
                }
                config.getRequiredConfigs().addAll(asSet(getConfigValue("validator.validationHandlers." + handler + ".config.required", configProperties)));
                config.getOptionalConfigs().addAll(asSet(getConfigValue("validator.validationHandlers." + handler + ".config.optional", configProperties)));
                config.getRequiredInputs().addAll(asSet(getConfigValue("validator.validationHandlers." + handler + ".input.required", configProperties)));
                config.getOptionalInputs().addAll(asSet(getConfigValue("validator.validationHandlers." + handler + ".input.optional", configProperties)));
                embeddedValidationHandlers.put(handler, config);
            }
            for (String handler : asSet(getConfigValue("validator.processingHandlers", configProperties))) {
                ExternalConfiguration.ProcessingHandlerConfiguration config = new ExternalConfiguration.ProcessingHandlerConfiguration();
                config.setDeprecated(Boolean.parseBoolean(Optional.ofNullable(getConfigValue("validator.processingHandlers." + handler + ".deprecated", configProperties)).orElse("false")));
                if (config.isDeprecated()) {
                    config.setReplacement(getConfigValue("validator.processingHandlers." + handler + ".replacedBy", configProperties));
                }
                config.getRequiredConfigs().addAll(asSet(getConfigValue("validator.processingHandlers." + handler + ".config.required", configProperties)));
                config.getOptionalConfigs().addAll(asSet(getConfigValue("validator.processingHandlers." + handler + ".config.optional", configProperties)));
                // Operation-specific configuration.
                Set<String> operations = asSet(getConfigValue("validator.processingHandlers." + handler + ".operations", configProperties));
                for (String operation: operations) {
                    ExternalConfiguration.BasicConfiguration operationConfig = new ExternalConfiguration.BasicConfiguration();
                    operationConfig.getRequiredInputs().addAll(asSet(getConfigValue("validator.processingHandlers." + handler + ".operations."+operation+".input.required", configProperties)));
                    operationConfig.getOptionalInputs().addAll(asSet(getConfigValue("validator.processingHandlers." + handler + ".operations."+operation+".input.optional", configProperties)));
                    config.getOperations().put(operation, operationConfig);
                }
                embeddedProcessingHandlers.put(handler, config);
            }
            for (String handler : asSet(getConfigValue("validator.messagingHandlers", configProperties))) {
                ExternalConfiguration.MessagingHandlerConfiguration config = new ExternalConfiguration.MessagingHandlerConfiguration();
                config.setDeprecated(Boolean.parseBoolean(Optional.ofNullable(getConfigValue("validator.messagingHandlers." + handler + ".deprecated", configProperties)).orElse("false")));
                if (config.isDeprecated()) {
                    config.setReplacement(getConfigValue("validator.messagingHandlers." + handler + ".replacedBy", configProperties));
                }
                config.getRequiredTxConfigs().addAll(asSet(getConfigValue("validator.messagingHandlers." + handler + ".config.tx.required", configProperties)));
                config.getOptionalTxConfigs().addAll(asSet(getConfigValue("validator.messagingHandlers." + handler + ".config.tx.optional", configProperties)));
                config.getRequiredSendConfigs().addAll(asSet(getConfigValue("validator.messagingHandlers." + handler + ".config.send.required", configProperties)));
                config.getOptionalSendConfigs().addAll(asSet(getConfigValue("validator.messagingHandlers." + handler + ".config.send.optional", configProperties)));
                config.getRequiredReceiveConfigs().addAll(asSet(getConfigValue("validator.messagingHandlers." + handler + ".config.receive.required", configProperties)));
                config.getOptionalReceiveConfigs().addAll(asSet(getConfigValue("validator.messagingHandlers." + handler + ".config.receive.optional", configProperties)));
                config.getRequiredInputs().addAll(asSet(getConfigValue("validator.messagingHandlers." + handler + ".input.required", configProperties)));
                config.getOptionalInputs().addAll(asSet(getConfigValue("validator.messagingHandlers." + handler + ".input.optional", configProperties)));
                config.getRequiredSendInputs().addAll(asSet(getConfigValue("validator.messagingHandlers." + handler + ".input.send.required", configProperties)));
                config.getOptionalSendInputs().addAll(asSet(getConfigValue("validator.messagingHandlers." + handler + ".input.send.optional", configProperties)));
                config.getRequiredReceiveInputs().addAll(asSet(getConfigValue("validator.messagingHandlers." + handler + ".input.receive.required", configProperties)));
                config.getOptionalReceiveInputs().addAll(asSet(getConfigValue("validator.messagingHandlers." + handler + ".input.receive.optional", configProperties)));
                embeddedMessagingHandlers.put(handler, config);
            }
        }
    }

    private ValidationService getRemoteValidationServiceClient() {
        if (remoteServiceClient == null) {
            try {
                remoteServiceClient = new ValidationService_Service(new URL(Configurations.VALIDATION_TDL_EXTERNAL_URL())).getValidationServicePort();
            } catch (MalformedURLException e) {
                throw new IllegalStateException("The GITB TDL validation service URL configured ["+Configurations.VALIDATION_TDL_EXTERNAL_URL()+"] was invalid", e);
            }
        }
        return remoteServiceClient;
    }

    public TAR doValidation(InputStreamSource testSuite, Set<String> externalActorIds, Set<String> externalParams, String tmpFolderPath) {
        TAR report;
        if (Configurations.VALIDATION_TDL_EXTERNAL_ENABLED()) {
            report = doValidationRemote(testSuite, externalActorIds, externalParams);
        } else {
            report = doValidationLocal(testSuite, externalActorIds, externalParams, tmpFolderPath);
        }
        LOG.info("Completed validation with result {}. Resulted in {} error(s), {} warning(s) and {} information message(s).", report.getResult(), report.getCounters().getNrOfErrors(), report.getCounters().getNrOfWarnings(), report.getCounters().getNrOfAssertions());
        return report;
    }

    private TAR doValidationRemote(InputStreamSource testSuite, Set<String> externalActorIds, Set<String> externalParams) {
        ValidateRequest request = new ValidateRequest();
        request.setSessionId("ID");
        // Test suite input.
        AnyContent testSuiteInput = new AnyContent();
        testSuiteInput.setName("testSuite");
        testSuiteInput.setEmbeddingMethod(ValueEmbeddingEnumeration.BASE_64);
        try (InputStream is = testSuite.getInputStream()) {
            testSuiteInput.setValue(Base64.encodeBase64String(IOUtils.toByteArray(is)));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open stream for test suite to validate", e);
        }
        request.getInput().add(testSuiteInput);
        // Domain actors.
        addInputAsListOfStrings(externalActorIds, request, "externalActors");
        // Domain parameters.
        addInputAsListOfStrings(externalParams, request, "externalParameters");
        // Call service.
        ValidationResponse response = getRemoteValidationServiceClient().validate(request);
        return response.getReport();
    }

    private void addInputAsListOfStrings(Set<String> values, ValidateRequest request, String inputName) {
        if (values != null && !values.isEmpty()) {
            AnyContent valuesInput = new AnyContent();
            valuesInput.setName(inputName);
            for (String value : values) {
                AnyContent valueInput = new AnyContent();
                valueInput.setValue(value);
                valuesInput.getItem().add(valueInput);
            }
            request.getInput().add(valuesInput);
        }
    }

    private TAR doValidationLocal(InputStreamSource testSuite, Set<String> externalActorIds, Set<String> externalParams, String tmpFolderPath) {
        // Prepare configuration.
        ExternalConfiguration externalConfig = new ExternalConfiguration();
        externalConfig.setDataTypes(dataTypes);
        externalConfig.setContainerDataTypes(containerDataTypes);
        externalConfig.setContainedDataTypes(containedDataTypes);
        externalConfig.setExternalActorIds(externalActorIds);
        externalConfig.setExternalParameters(externalParams);
        externalConfig.setEmbeddedMessagingHandlers(embeddedMessagingHandlers);
        externalConfig.setEmbeddedProcessingHandlers(embeddedProcessingHandlers);
        externalConfig.setEmbeddedValidationHandlers(embeddedValidationHandlers);
        externalConfig.setAcceptedMimeTypes(acceptedMimeTypes);
        // Run the validation.
        Validator validator = new Validator(tmpFolderPath, externalConfig);
        ValidationReport result = validator.validate(testSuite);
        // Construct TAR report.
        TAR report = new TAR();
        // Add the current timestamp to the report.
        report.setReports(new TestAssertionGroupReportsType());
        try {
            GregorianCalendar calendar = new GregorianCalendar();
            report.setDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar));
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException(e);
        }
        // Add the input received in the report's context to be reported back to the client.
        report.setContext(new AnyContent());
        // Construct the report's items based on the validation result.
        int infos = 0;
        int warnings = 0;
        int errors = 0;
        report.setReports(new TestAssertionGroupReportsType());

        for (ValidationReport.ValidationReportItem item: result.getItems()) {
            if (item.getLevel() == ErrorLevel.ERROR) {
                errors += 1;
                addReportItemError(item, report.getReports().getInfoOrWarningOrError());
            } else if (item.getLevel() == ErrorLevel.WARNING) {
                warnings += 1;
                addReportItemWarning(item, report.getReports().getInfoOrWarningOrError());
            } else if (item.getLevel() == ErrorLevel.INFO) {
                infos += 1;
                addReportItemInfo(item, report.getReports().getInfoOrWarningOrError());
            }
        }
        // Add the overall validation counters to the report.
        report.setCounters(new ValidationCounters());
        report.getCounters().setNrOfAssertions(BigInteger.valueOf(infos));
        report.getCounters().setNrOfWarnings(BigInteger.valueOf(warnings));
        report.getCounters().setNrOfErrors(BigInteger.valueOf(errors));
        // Determine the overall result to report based on the validation results.
        if (errors == 0) {
            report.setResult(TestResultType.SUCCESS);
        } else {
            report.setResult(TestResultType.FAILURE);
        }
        return report;
    }

    /**
     * Add an info message to the report.
     *
     * @param item The report item.
     * @param reportItems The report's items.
     */
    private void addReportItemInfo(ValidationReport.ValidationReportItem item, List<JAXBElement<TestAssertionReportType>> reportItems) {
        reportItems.add(objectFactory.createTestAssertionGroupReportsTypeInfo(createReportItemContent(item)));
    }

    /**
     * Add a warning message to the report.
     *
     * @param item The report item.
     * @param reportItems The report's items.
     */
    private void addReportItemWarning(ValidationReport.ValidationReportItem item, List<JAXBElement<TestAssertionReportType>> reportItems) {
        reportItems.add(objectFactory.createTestAssertionGroupReportsTypeWarning(createReportItemContent(item)));
    }

    /**
     * Add an error message to the report.
     *
     * @param item The report item.
     * @param reportItems The report's items.
     */
    private void addReportItemError(ValidationReport.ValidationReportItem item, List<JAXBElement<TestAssertionReportType>> reportItems) {
        reportItems.add(objectFactory.createTestAssertionGroupReportsTypeError(createReportItemContent(item)));
    }

    /**
     * Create the internal content of a report's item.
     *
     * @param item The report item.
     * @return The content to wrap.
     */
    private BAR createReportItemContent(ValidationReport.ValidationReportItem item) {
        BAR itemContent = new BAR();
        itemContent.setAssertionID(item.getCode());
        itemContent.setDescription(item.getDescription());
        itemContent.setLocation(item.getLocation());
        return itemContent;
    }

    private String getConfigValue(String name, Properties props) {
        return java.lang.System.getenv().getOrDefault(name, props.getProperty(name));
    }

    private Set<String> asSet(String value) {
        Set<String> valueSet = new HashSet<>();
        if (value != null) {
            for (String val: StringUtils.split(value, ',')) {
                valueSet.add(val.trim());
            }
        }
        return valueSet;
    }

}
