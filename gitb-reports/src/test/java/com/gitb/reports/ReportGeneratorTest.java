package com.gitb.reports;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.reports.dto.ConformanceStatementOverview;
import com.gitb.reports.dto.TestCaseOverview;
import com.gitb.reports.dto.TestSuiteOverview;
import com.gitb.tr.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import jakarta.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;

public class ReportGeneratorTest {

    private final ReportGenerator generator = ReportGenerator.getInstance();
    private final ObjectFactory objectFactory = new ObjectFactory();

    /*
     * When adapting the templates for development purposes it is useful to work with a specific directory that is not deleted
     * after each test completes. To do this comment the @TempDir and uncomment the definition of a specific directory.
     */
    @TempDir()
    Path tempDirectory;
    //    Path tempDirectory = Path.of("/tmp/gitb_pdf_tests/");

    @BeforeEach
    void setup() throws IOException {
        Files.createDirectories(tempDirectory);
        ((Logger)LoggerFactory.getLogger("org.apache.fontbox")).setLevel(Level.WARN);
        ((Logger)LoggerFactory.getLogger("org.apache.pdfbox")).setLevel(Level.WARN);
        ((Logger)LoggerFactory.getLogger("com.openhtmltopdf")).setLevel(Level.WARN);
    }

    private TestSuiteOverview getTestSuiteOverview(String name, String description, ReportSpecs specs) throws DatatypeConfigurationException {
        TestSuiteOverview data = new TestSuiteOverview();
        data.setOverallStatus("SUCCESS");
        data.setTestSuiteName(name);
        data.setTestSuiteDescription(description);
        data.setTestCases(List.of(
                getTestCaseOverview("Test Case Report #1", specs),
                getTestCaseOverview("Test Case Report #2", specs),
                getTestCaseOverview("Test Case Report #3", specs),
                getTestCaseOverview("Test Case Report #4", specs),
                getTestCaseOverview("Test Case Report #5", specs)
        ));
        return data;
    }

    private TestCaseOverview getTestCaseOverview(String title, ReportSpecs specs) throws DatatypeConfigurationException {
        TestCaseOverview data = new TestCaseOverview();
        // Labels
        data.setTitle(title);
        data.setLabelOrganisation("Organisation");
        data.setLabelSystem("System");
        data.setLabelDomain("Domain");
        data.setLabelSpecification("Specification");
//        data.setLabelSpecification("Specification profile to test");
        data.setLabelActor("Actor");
        // Basic data
        data.setOrganisation("My organisation");
        data.setSystem("My system");
        data.setTestDomain("My domain");
        data.setTestSpecification("Core Profile – v1.0");
//        data.setTestSpecification("My specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specification");
        data.setTestActor("My actor");
        data.setTestName("Test case 1");
//        data.setTestDescription("Description for test case 1");
        data.setTestDescription("Description for test case 1Description for test case 1Description for test case 1Description for test case 1Description for test case 1Description for test case 1Description for test case 1Description for test case 1Description for test case 1Description for test case 1Description for test case 1Description for test case 1Description for test case 1Description for test case 1Description for test case 1Description for test case 1Description for test case 1");
        data.setReportResult(TestResultType.SUCCESS.value());
        data.setStartTime("06/04/2023 10:21:43");
        data.setEndTime("06/04/2023 10:21:44");
        data.setOutputMessage("This is the output message for your test session. Check the different report steps for details.");
        // Test steps
        data.setSteps(List.of(
                generator.fromTestStepReportType(getTAR(), "Step 1.1: Define access identifiers", specs),
                generator.fromTestStepReportType(getTAR(), "Step 1.2", specs),
                generator.fromTestStepReportType(getTAR(), "Step 1.2.1:", specs),
                generator.fromTestStepReportType(getTAR(), "Step 1.2.2: Sub-step of step 2 (yet again) Sub-step of step 2 (yet again)", specs),
                generator.fromTestStepReportType(getTAR(), "Step 1.2.3: This sub-step", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.WARNING, false, true), "Step 2: Another step", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.WARNING,false,  false), "Step 3: Another step (3)", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.SUCCESS, true, true), "Step 3.1: Another step (3.1)", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.SUCCESS, false, false), "Step 4: Another step (4)", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.SUCCESS,false, false), "Step 5: Another step (5)", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.SUCCESS,false, false), "Step 6: Another step (6)", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.SUCCESS,false, false), "Step 7: Another step (7)", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.SUCCESS,false, false), "Step 8: Another step (8)", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.WARNING, false, false), "Step 9: Another step (9)", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.SUCCESS,false, false), "Step 10: Another step (10)", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.SUCCESS,false, false), "Step 11: Another step (11)", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.FAILURE,false, false), "Step 12: Another step (12)", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.SUCCESS,false, false), "Step 13: Another step (13)", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.SUCCESS,false, false), "Step 14: Another step (14)", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.SUCCESS,false, false), "Step 15: Another step (15)", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.SUCCESS,false, false), "Step 16: Another step (16)", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.UNDEFINED,false, false), "Step 17: Another step (17)", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.UNDEFINED,false, false), "Step 18: Another step (18)", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.UNDEFINED,false, false), "Step 19: Another step (19)", specs),
                generator.fromTestStepReportType(getTAR(TestResultType.UNDEFINED,false, false), "Step 20: Another step (20)", specs)
        ));
        // Documentation
        data.setDocumentation("<p><strong>[TC2] Invalid registration</strong></p>\n" +
                " <p>Passenger attempts to register in the wallet with an invalid photo. The wallet provider is expected to return an error with an appropriate error code.</p>\n" +
                " <p><img style=\"display:block;margin-left:auto;margin-right:auto\" src=\"resources/TC2.png\" alt=\"\" width=\"2500\" height=\"400\" /></p>\n" +
                " <p><strong>Assumptions:</strong></p>\n" +
                " <ul><li>The access event ID and access gate ID used in API calls are provided as inputs by the user.</li><li>The wallet provider has already registered to obtain an API key.</li><li>When provided with an invalid photo the registration call shall return at least one boarding pass. For the purposes of the PoC this is triggered by passing a value of &#34;<em>BAD_PHOTO</em>&#34; for either the access event ID or access gate ID.</li></ul>\n" +
                " <p>The following table summarises the <b>additional</b> test steps:</p><table style=\"border-collapse:collapse;width:100%;height:240.875px;border-color:#34495e;border-style:solid\"><tbody><tr style=\"height:27.375px\"><td style=\"width:12.9954%;height:27.375px;background-color:#34495e;border:1px solid #34495e\"><strong><span style=\"color:#ffffff\">  Test case</span></strong></td><td style=\"width:71.3364%;height:27.375px;background-color:#ecf0f1;border:1px solid #34495e\"><strong><span style=\"background-color:transparent\">Get requirements</span></strong></td><td style=\"width:7.74194%;height:27.375px;background-color:#34495e;text-align:center;border:1px solid #34495e\"><strong><span style=\"color:#ffffff\">ID</span></strong></td><td style=\"width:7.92627%;height:27.375px;background-color:#ecf0f1;text-align:center;border:1px solid #34495e\"><strong>TC1</strong></td></tr><tr style=\"height:27.75px\"><td style=\"height:27.75px;background-color:#34495e;width:12.9954%;border:1px solid #34495e\"><strong><span style=\"color:#ffffff\">  Actors</span></strong></td><td style=\"height:27.75px;width:87.0046%;border:1px solid #34495e\" colspan=\"3\"><strong>Evidence Requester (ER)¹</strong>, Evidence Broker (EB)</td></tr><tr style=\"height:27.375px\"><td style=\"width:12.9954%;height:27.375px;background-color:#34495e;border:1px solid #34495e\"><strong><span style=\"color:#ffffff\">  Description</span></strong></td><td style=\"height:27.375px;width:87.0046%;border:1px solid #34495e\" colspan=\"3\"><span style=\"background-color:transparent\">Test case to verify that an Evidence Requester (ER) can correctly query an Evidence Broker (EB) with a Get List of Requirements query.</span></td></tr><tr style=\"height:131px\"><td style=\"width:12.9954%;height:131px;background-color:#34495e;border:1px solid #34495e\"><strong><span style=\"color:#ffffff\">  Steps</span></strong></td><td style=\"height:131px;width:87.0046%;border:1px solid #34495e\" colspan=\"3\">\n" +
                " <p style=\"text-align:left\">The following steps will be carried out as part of this test case (validation steps shown in <em>italics</em>).</p>\n" +
                " <ol><li style=\"text-align:left\">ER sends to EB a “<a href=\"https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?pageId&#61;406946271#id-3.2EvidenceBroker%28EB%29-4.QueryInterfaceSpecification\" target=\"_blank\" rel=\"noopener noreferrer nofollow\">Get List of Requirements</a>” query (with any data from the shared test data).</li><li>EB responds with the requested requirements.\n" +
                " <ol><li><em>Check that the query type is as expected.</em></li><li><em>Check that the query was valid (no exceptions were raised). </em></li></ol>\n" +
                " </li></ol>\n" +
                " </td></tr></tbody></table>\n" +
                " <p><em><span style=\"font-size:8pt\">¹ This is the role that your system (the System Under Test - SUT) plays in this test case</span></em></p>\n" +
                " <p>For service definitions, data models and expected behaviours please refer to the <a href=\"https://ec.europa.eu/cefdigital/wiki/display/SDGOO/Chapter&#43;3%3A&#43;Common&#43;Services&#43;-&#43;December&#43;2021?src&#61;contextnavpagetreemode\" target=\"_blank\" rel=\"noopener noreferrer nofollow\">SDG Once-Only Collaborative Space (Chapter 3)</a>.</p>");
        // Log messages
        data.setLogMessages(List.of(
                "[2023-03-20 17:58:21] DEBUG - Configuring session [12c8548b-069b-4b61-85e6-1ce8a64f5911]",
                "[2023-03-20 17:58:22] INFO  - Starting session",
                "[2023-03-20 17:58:22] WARN  - No variable could be located in the session context for expression [$SUT{issuingAuthorityId}]",
                "[2023-03-20 17:58:22] DEBUG - Status update - step [Set issuing authority] - ID [1]: PROCESSING",
                "[2023-03-20 17:58:22] DEBUG - Status update - step [Sequence] - ID [1[T]]: PROCESSING",
                "[2023-03-20 17:58:22] DEBUG - Status update - step [Request information] - ID [1[T].1]: PROCESSING",
                "[2023-03-20 17:58:22] DEBUG - Status update - step [Request information] - ID [1[T].1]: WAITING",
                "[2023-03-20 17:58:22] DEBUG - Triggering user interaction - step [Request information] - ID [1[T].1]",
                "[2023-03-20 17:58:25] DEBUG - Handling user-provided inputs - step [Request information] - ID [1[T].1]",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [Request information] - ID [1[T].1]: PROCESSING",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [Request information] - ID [1[T].1]: COMPLETED",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [Set issuing authority] - ID [1]: COMPLETED",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [Sequence] - ID [1[T]]: COMPLETED",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [Validate authority id] - ID [2]: PROCESSING",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [Validate authority id] - ID [2]: COMPLETED",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [XSD validation: Validate Annex A form content] - ID [3]: PROCESSING",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [XSD validation: Validate Annex A form content] - ID [3]: COMPLETED",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [Check success] - ID [4]: PROCESSING",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [Check success] - ID [4[T]]: SKIPPED",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [Check success] - ID [4]: COMPLETED",
                "[2023-03-20 17:58:25] WARN  - No variable could be located in the session context for expression [$step_receive_other_info{formContent}]",
                "[2023-03-20 17:58:25] WARN  - No variable could be located in the session context for expression [$step_annex_a{formId}]",
                "[2023-03-20 17:58:25] WARN  - No variable could be located in the session context for expression [$step_receive_other_info{globalCaseId}]",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [Sequence] - ID [5]: PROCESSING",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [XSD form validation] - ID [5.1]: PROCESSING",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [XSD form validation] - ID [5.1]: COMPLETED",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [Check document parentFormId] - ID [5.2]: PROCESSING",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [Check document parentFormId] - ID [5.2]: COMPLETED",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [XSD validation: Validate Annex A form content] - ID [5.3]: PROCESSING",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [XSD validation: Validate Annex A form content] - ID [5.3]: COMPLETED",
                "[2023-03-20 17:58:25] WARN  - No variable could be located in the session context for expression [$globalCaseIdCheck]",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [Sequence] - ID [5]: COMPLETED",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [Check success] - ID [6]: PROCESSING",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [Check success] - ID [6[T]]: SKIPPED",
                "[2023-03-20 17:58:25] DEBUG - Status update - step [Check success] - ID [6]: COMPLETED",
                "[2023-03-20 17:58:25] INFO  - Session finished with result [COMPLETED]"
        ));
        return data;
    }

    @Test
    void testTestCaseOverview() throws IOException, DatatypeConfigurationException {
        var specs = ReportSpecs.build()
                .withContextItemTruncateLimit(1000)
                .withContextItems(true)
                .withTestSteps(true)
                .withResourceResolver((uri) -> Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(uri)).toString());
        var data = getTestCaseOverview("Test case report", specs);
        try (var outputStream = Files.newOutputStream(Path.of(tempDirectory.toString(), "TestCaseOverview.pdf"))) {
            generator.writeTestCaseOverviewReport(data, outputStream, specs);
        }
    }

    @Test
    void testTestStepReport() throws IOException, DatatypeConfigurationException {
        try (var outputStream = Files.newOutputStream(Path.of(tempDirectory.toString(), "TAR.pdf"))) {
            generator.writeTestStepReport(getTAR(), "Validation report", outputStream,
                    ReportSpecs.build().withContextItems(true)
            );
        }
    }

    @Test
    void testConformanceStatementOverview() throws IOException, DatatypeConfigurationException {
        var specs = ReportSpecs.build()
                .withContextItemTruncateLimit(1000)
                .withContextItems(true)
                .withTestSteps(true)
                .withResourceResolver((uri) -> Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(uri)).toString());
        ConformanceStatementOverview data = new ConformanceStatementOverview();
        // Labels
        data.setTitle("Η γρήγορη καφέ αλεπού πήδηξε πάνω από το τεμπέλικο σκυλί.");
        data.setLabelOrganisation("Organisation");
//        data.setLabelOrganisation("Organisation label that is big");
//        data.setLabelSystem("System label that is very big and should really be smaller");
        data.setLabelSystem("System");
        data.setLabelDomain("Domain");
        data.setLabelSpecification("Specification");
//        data.setLabelSpecification("Specification profile to test with a very big label");
        data.setLabelActor("Actor");
        // Basic data
        data.setOrganisation("My organisation");
        data.setSystem("Η γρήγορη καφέ αλεπού πήδηξε πάνω από το τεμπέλικο σκυλί.");
//        data.setSystem("My system");
        data.setTestDomain("My domain");
        data.setTestSpecification("My specification");
//        data.setTestSpecification("My specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specification");
        data.setTestActor("My actor");

        data.setIncludeDetails(true);
        data.setIncludeMessage(true);
        data.setIncludeTestStatus(true);
        data.setIncludeTestCases(true);
        data.setMessage("<strong>This is the result.</strong>");

        data.setOverallStatus("SUCCESS");
        data.setTestSuites(List.of(
//                getTestSuiteOverview("The first test suite", "Description for the first test suite", specs),
//                getTestSuiteOverview("The second test suite", "Description for the second test suite", specs),
                getTestSuiteOverview("The third test suite", "Description for the third test suite", specs),
                getTestSuiteOverview("Test suite 2", "Description for test suite 2", specs)
        ));
        data.setCompletedTests(245);
        data.setFailedTests(105);
        data.setUndefinedTests(30);

        try (var outputStream = Files.newOutputStream(Path.of(tempDirectory.toString(), "ConformanceStatementOverview.pdf"))) {
            generator.writeConformanceStatementOverviewReport(data, outputStream, specs);
        }
    }

    private TAR getTAR() throws DatatypeConfigurationException {
        return getTAR(TestResultType.FAILURE, true, true);
    }

    private TAR getTAR(TestResultType result, boolean withDetails, boolean withContext) throws DatatypeConfigurationException {
        TAR tar = new TAR();
        tar.setDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        tar.setResult(result);
        if (withDetails) {
            tar.setReports(new TestAssertionGroupReportsType());
            tar.getReports().getInfoOrWarningOrError().addAll(List.of(
                    createItem("[PO-05] The quantities of items for large orders must be greater than 10.", "error"),
                    createItem("[PO-06] A second error. [PO-06] A second error.[PO-06] A second error.[PO-06] A second error.[PO-06] A second error.[PO-06] A second error.[PO-06] A second error.[PO-06] A second error.[PO-06] A second error.[PO-06] A second error.[PO-06] A second error.[PO-06] A second error.[PO-06] A second error.[PO-06] A second error.[PO-06] A second error.[PO-06] A second error.[PO-06] A second error.", "error", "VALUE 1 > VALUE 2", "/root/location1"),
                    createItem("[PO-06] A second error.", "error", "VALUE 1 > VALUE 2", "/root/location1"),
                    createItem("[PO-06] A second error.", "error", "VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2VALUE 1 > VALUE 2", "/root/location1"),
                    createItem("[PO-05] The quantities of items for large orders must be greater than 10.", "warning"),
                    createItem("[PO-05] The quantities of items for large orders must be greater than 10.", "info")
            ));
        }
        if (withContext) {
            tar.setContext(new AnyContent());
            tar.getContext().getItem().add(new AnyContent());
            tar.getContext().getItem().get(0).setName("value");
            tar.getContext().getItem().get(0).setValue("The value of property [value]");
            tar.getContext().getItem().get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
            tar.getContext().getItem().add(new AnyContent());
            tar.getContext().getItem().get(1).setName("input");
            tar.getContext().getItem().get(1).getItem().add(new AnyContent());
            tar.getContext().getItem().get(1).getItem().get(0).setName("header");
            tar.getContext().getItem().get(1).getItem().get(0).setValue("The value of the header");
            tar.getContext().getItem().get(1).getItem().get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
            tar.getContext().getItem().get(1).getItem().add(new AnyContent());
            tar.getContext().getItem().get(1).getItem().get(1).setName("body");
            tar.getContext().getItem().get(1).getItem().get(1).getItem().add(new AnyContent());
            tar.getContext().getItem().get(1).getItem().get(1).getItem().get(0).setName("part1");
            tar.getContext().getItem().get(1).getItem().get(1).getItem().get(0).setValue("Value of part1");
            tar.getContext().getItem().get(1).getItem().get(1).getItem().get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
            tar.getContext().getItem().get(1).getItem().get(1).getItem().add(new AnyContent());
            tar.getContext().getItem().get(1).getItem().get(1).getItem().get(1).setName("part1");
            tar.getContext().getItem().get(1).getItem().get(1).getItem().get(1).setValue("Value of part2");
            tar.getContext().getItem().get(1).getItem().get(1).getItem().get(1).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);

            tar.getContext().getItem().add(new AnyContent());
            tar.getContext().getItem().get(2).setName("output");
            tar.getContext().getItem().get(2).getItem().add(new AnyContent());
            tar.getContext().getItem().get(2).getItem().get(0).setName("header");
            tar.getContext().getItem().get(2).getItem().get(0).setValue("The value of the header");
            tar.getContext().getItem().get(2).getItem().get(0).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
            tar.getContext().getItem().get(2).getItem().add(new AnyContent());
            tar.getContext().getItem().get(2).getItem().get(1).setName("body");
            tar.getContext().getItem().get(2).getItem().get(1).setValue("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<data>\n" +
                    "\t<metadata>\n" +
                    "\t\t<gitb:name>Simple TC</gitb:name>\n" +
                    "\t\t<gitb:type>CONFORMANCE</gitb:type>\n" +
                    "\t\t<gitb:version>1.0</gitb:version>\n" +
                    "\t\t<gitb:description>A simple test case.</gitb:description>\n" +
                    "\t</metadata>\n" +
                    "\t<actors>\n" +
                    "\t\t<gitb:actor id=\"Actor\" name=\"Actor\" role=\"SUT\"/>\n" +
                    "\t</actors>\n" +
                    "\t<steps>\n" +
                    "        B,C,D\n" +
                    "\t</steps>\n" +
                    "</data>");
            tar.getContext().getItem().get(2).getItem().get(1).setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        }
        return tar;
    }

    private JAXBElement<TestAssertionReportType> createItem(String description, String level, String test, String location) {
        var item = new BAR();
        item.setDescription(description);
        item.setTest(test);
        item.setLocation(location);
        if ("error".equals(level)) {
            return objectFactory.createTestAssertionGroupReportsTypeError(item);
        } else if ("warning".equals(level)) {
            return objectFactory.createTestAssertionGroupReportsTypeWarning(item);
        } else {
            return objectFactory.createTestAssertionGroupReportsTypeInfo(item);
        }
    }

    private JAXBElement<TestAssertionReportType> createItem(String description, String level) {
        return createItem(description, level, null, null);
    }

}
