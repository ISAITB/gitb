/*
 * Copyright (C) 2026 European Union
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

package com.gitb.engine.validation.handlers.pdf;

import com.gitb.core.AnyContent;
import com.gitb.core.Configuration;
import com.gitb.engine.utils.HandlerUtils;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.engine.validation.ValidationHandler;
import com.gitb.engine.validation.handlers.common.AbstractValidator;
import com.gitb.tr.*;
import com.gitb.types.*;
import com.gitb.utils.DataTypeUtils;
import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.verapdf.core.VeraPDFException;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.gf.model.impl.containers.StaticContainers;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.results.TestAssertion;
import org.verapdf.pdfa.validation.profiles.RuleId;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Validates binary content as a PDF document, in two layers:
 * <ol>
 *     <li>a core parse with Apache PDFBox, surfacing structural/parsing problems, and</li>
 *     <li>zero or more PDF/A or PDF/UA-1 conformance profile validations with veraPDF.</li>
 * </ol>
 * See {@link CoreRule} for the catalogue of rule identifiers used for core-parse findings, and
 * {@link PdfProfile} for the supported conformance profiles.
 */
@ValidationHandler(name="PdfValidator")
public class PdfValidator extends AbstractValidator {

    private static final Logger LOG = LoggerFactory.getLogger(PdfValidator.class);
    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    private static final String MODULE_DEFINITION_XML = "/validation/pdf-validator-definition.xml";

    private static final String CONTENT_ARGUMENT_NAME = "content";
    private static final String PROFILES_ARGUMENT_NAME = "profiles";
    private static final String STRICT_MODE_ARGUMENT_NAME = "strictMode";
    private static final String SORT_BY_SEVERITY_ARGUMENT_NAME = "sortBySeverity";
    private static final String PASSWORD_ARGUMENT_NAME = "password";
    private static final String MAXIMUM_FINDINGS_PER_RULE_ARGUMENT_NAME = "maximumFindingsPerRule";

    private static final int DEFAULT_MAXIMUM_FINDINGS_PER_RULE = 10;
    /** The value veraPDF itself uses to signal "no cap on the number of findings displayed per rule". */
    private static final int VERAPDF_UNLIMITED_DISPLAYED_FAILED_CHECKS = -1;

    static {
        // Register veraPDF's Greenfield parser and validator implementations as the default foundry. Idempotent.
        VeraGreenfieldFoundryProvider.initialise();
    }

    public PdfValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        // Retrieve and check inputs.
        byte[] content = Objects.requireNonNull(getAndConvert(inputs, CONTENT_ARGUMENT_NAME, DataType.BINARY_DATA_TYPE, BinaryType.class),
                "Input [%s] must be provided".formatted(CONTENT_ARGUMENT_NAME)).serializeByDefaultEncoding();
        List<String> rawProfiles = Optional.ofNullable(getAndConvert(inputs, PROFILES_ARGUMENT_NAME, DataType.LIST_DATA_TYPE, ListType.class))
                .map(list -> list.getElements().stream().map(element -> (String) element.convertTo(DataType.STRING_DATA_TYPE).getValue()).toList())
                .orElseGet(List::of);
        boolean strictMode = Optional.ofNullable(getAndConvert(inputs, STRICT_MODE_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class))
                .map(BooleanType::getValue)
                .orElse(false);
        boolean sortBySeverity = Optional.ofNullable(getAndConvert(inputs, SORT_BY_SEVERITY_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class))
                .map(BooleanType::getValue)
                .orElse(true);
        Optional<String> password = Optional.ofNullable(getAndConvert(inputs, PASSWORD_ARGUMENT_NAME, DataType.STRING_DATA_TYPE, StringType.class))
                .map(StringType::getValue)
                .filter(StringUtils::isNotBlank);
        int maximumFindingsPerRule = Optional.ofNullable(getAndConvert(inputs, MAXIMUM_FINDINGS_PER_RULE_ARGUMENT_NAME, DataType.NUMBER_DATA_TYPE, NumberType.class))
                .map(NumberType::intValue)
                .orElse(DEFAULT_MAXIMUM_FINDINGS_PER_RULE);
        String sessionId = (String) inputs.get(HandlerUtils.SESSION_INPUT).getValue();
        // Proceed with validation.
        List<Finding> findings = new ArrayList<>();
        boolean documentParsed = parseCore(content, strictMode, password, findings);
        if (documentParsed && !rawProfiles.isEmpty()) {
            List<PdfProfile> profiles = PdfProfile.parse(rawProfiles, invalidValue -> LOG.warn(MarkerFactory.getDetachedMarker(sessionId),
                    "Ignoring unknown PDF conformance profile [{}] requested for the PdfValidator.", invalidValue));
            for (var profile: profiles) {
                validateProfile(content, profile, password, maximumFindingsPerRule, findings);
            }
        }
        return createReport(content, findings, sortBySeverity);
    }

    /**
     * Carry out the core (PDFBox-based) parse of the document, recording core-parse findings.
     *
     * @return Whether the document was successfully opened - if {@code false}, no further processing
     * (structural checks or profile validation) is possible or was attempted.
     */
    private boolean parseCore(byte[] content, boolean strictMode, Optional<String> password, List<Finding> findings) {
        try (var capture = PdfBoxLogCapture.start()) {
            PDDocument document;
            try {
                document = password.isPresent() ? Loader.loadPDF(content, password.get()) : Loader.loadPDF(content);
            } catch (InvalidPasswordException e) {
                if (password.isPresent()) {
                    // A password was supplied but rejected - this is a test-case configuration problem, not a validation finding.
                    throw new IllegalArgumentException("The provided password could not decrypt the PDF document.", e);
                }
                findings.add(new Finding(CoreRule.ENCRYPTED.id(), CoreRule.ENCRYPTED.defaultMessage(), Finding.Severity.ERROR));
                return false;
            } catch (IOException e) {
                findings.add(new Finding(CoreRule.UNPARSEABLE.id(), "%s (%s)".formatted(CoreRule.UNPARSEABLE.defaultMessage(), e.getMessage()), Finding.Severity.ERROR));
                return false;
            }
            var recoverableSeverity = strictMode ? Finding.Severity.ERROR : Finding.Severity.WARNING;
            try {
                checkStructure(document, recoverableSeverity, findings);
            } finally {
                try {
                    document.close();
                } catch (IOException e) {
                    LOG.warn("Error while closing a parsed PDF document", e);
                }
            }
            for (var message: capture.messages()) {
                findings.add(new Finding(CoreRule.classify(message).id(), message, recoverableSeverity));
            }
            return true;
        }
    }

    /**
     * Explicit structural checks against the parsed PDFBox object model. These complement the checks
     * that PDFBox itself only reports through log messages (captured separately via {@link PdfBoxLogCapture}).
     */
    private void checkStructure(PDDocument document, Finding.Severity recoverableSeverity, List<Finding> findings) {
        float version = document.getVersion();
        if (version < 1.0f || version > 2.0f) {
            findings.add(new Finding(CoreRule.HEADER.id(), "%s (reported version: %s)".formatted(CoreRule.HEADER.defaultMessage(), version), recoverableSeverity));
        }
        if (document.getNumberOfPages() == 0) {
            findings.add(new Finding(CoreRule.PAGES.id(), CoreRule.PAGES.defaultMessage(), recoverableSeverity));
        }
        for (PDPage page: document.getPages()) {
            if (page.hasContents()) {
                PDFStreamParser parser = null;
                try {
                    parser = new PDFStreamParser(page);
                    parser.parse();
                } catch (IOException e) {
                    findings.add(new Finding(CoreRule.CONTENT.id(), "%s (%s)".formatted(CoreRule.CONTENT.defaultMessage(), e.getMessage()), recoverableSeverity));
                } finally {
                    if (parser != null) {
                        try {
                            parser.close();
                        } catch (IOException e) {
                            LOG.warn("Error while closing a PDF content stream parser", e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Parse and validate the document against a single PDF conformance profile using veraPDF, recording
     * a finding for every failed (or indeterminate) assertion, capped per rule at {@code maximumFindingsPerRule}.
     */
    private void validateProfile(byte[] content, PdfProfile profile, Optional<String> password, int maximumFindingsPerRule, List<Finding> findings) {
        int displayCap = maximumFindingsPerRule <= 0 ? VERAPDF_UNLIMITED_DISPLAYED_FAILED_CHECKS : maximumFindingsPerRule;
        try (var stream = new ByteArrayInputStream(content);
             var veraFoundry = Foundries.defaultInstance();
             var parser = veraFoundry.createParser(stream, profile.flavour(), password.orElse(null))) {
            var validator = veraFoundry.createValidator(profile.flavour(), displayCap, false, true, false);
            var result = validator.validate(parser);
            for (var assertion: result.getTestAssertions()) {
                var severity = severityFor(assertion.getStatus());
                if (severity != null) {
                    var message = Optional.ofNullable(assertion.getErrorMessage()).filter(StringUtils::isNotBlank).orElseGet(assertion::getMessage);
                    findings.add(new Finding(assertionId(profile, assertion.getRuleId()), message, severity));
                }
            }
            if (displayCap != VERAPDF_UNLIMITED_DISPLAYED_FAILED_CHECKS) {
                for (var entry: result.getFailedChecks().entrySet()) {
                    if (entry.getValue() > displayCap) {
                        var ruleId = assertionId(profile, entry.getKey());
                        findings.add(new Finding(ruleId, "Rule [%s] failed %s times; only the first %s findings are reported."
                                .formatted(ruleId, entry.getValue(), displayCap), Finding.Severity.WARNING));
                    }
                }
            }
        } catch (VeraPDFException | IOException e) {
            // Not expected to occur - the document was already parsed successfully by PDFBox above - but handled defensively.
            findings.add(new Finding(CoreRule.UNPARSEABLE.id(),
                    "The document could not be validated against profile [%s]: %s".formatted(profile.name(), e.getMessage()), Finding.Severity.ERROR));
        } finally {
            // veraPDF's Greenfield parser keeps thread-local state bound to the parsed document/flavour - clear it
            // so that a pooled worker thread does not retain it after this pass completes.
            StaticContainers.clearAllContainers();
        }
    }

    private Finding.Severity severityFor(TestAssertion.Status status) {
        return switch (status) {
            case FAILED -> Finding.Severity.ERROR;
            case UNKNOWN -> Finding.Severity.WARNING;
            case PASSED -> null;
        };
    }

    private String assertionId(PdfProfile profile, RuleId ruleId) {
        return "%s-%s-%s".formatted(profile.name(), ruleId.getClause(), ruleId.getTestNumber());
    }

    private TAR createReport(byte[] content, List<Finding> findings, boolean sortBySeverity) {
        var report = TestCaseUtils.createEmptyReport();
        report.setName("PDF validation");
        var context = new AnyContent();
        var contentItem = DataTypeUtils.convertDataTypeToAnyContent(CONTENT_ARGUMENT_NAME, new BinaryType(content));
        contentItem.setMimeType("application/pdf");
        contentItem.setForContext(true);
        contentItem.setForDisplay(true);
        context.getItem().add(contentItem);
        report.setContext(context);
        // Add report items.
        int errors = 0;
        int warnings = 0;
        int infos = 0;
        if (!findings.isEmpty()) {
            report.setReports(new TestAssertionGroupReportsType());
            for (var finding: findings) {
                var item = new BAR();
                item.setDescription(finding.message());
                item.setAssertionID(finding.ruleId());
                JAXBElement<TestAssertionReportType> element;
                switch (finding.severity()) {
                    case ERROR -> {
                        element = OBJECT_FACTORY.createTestAssertionGroupReportsTypeError(item);
                        errors += 1;
                    }
                    case WARNING -> {
                        element = OBJECT_FACTORY.createTestAssertionGroupReportsTypeWarning(item);
                        warnings += 1;
                    }
                    default -> {
                        element = OBJECT_FACTORY.createTestAssertionGroupReportsTypeInfo(item);
                        infos += 1;
                    }
                }
                report.getReports().getInfoOrWarningOrError().add(element);
            }
        }
        report.setCounters(new ValidationCounters());
        report.getCounters().setNrOfErrors(BigInteger.valueOf(errors));
        report.getCounters().setNrOfWarnings(BigInteger.valueOf(warnings));
        report.getCounters().setNrOfAssertions(BigInteger.valueOf(infos));
        report.setResult(errors > 0 ? TestResultType.FAILURE : TestResultType.SUCCESS);
        if (sortBySeverity) {
            sortReport(report, false);
        }
        return report;
    }

}
