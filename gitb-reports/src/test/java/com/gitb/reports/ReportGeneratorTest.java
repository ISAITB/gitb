package com.gitb.reports;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.reports.dto.ConformanceOverview;
import com.gitb.reports.dto.ConformanceStatementOverview;
import com.gitb.reports.dto.TestCaseGroup;
import com.gitb.reports.dto.TestCaseOverview;
import com.gitb.reports.dto.TestSuiteOverview;
import com.gitb.reports.dto.*;
import com.gitb.tr.*;
import jakarta.xml.bind.JAXBElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

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
//        Path tempDirectory = Path.of("/tmp/gitb_pdf_tests/");

    @BeforeEach
    void setup() throws IOException {
        ((Logger)LoggerFactory.getLogger(ReportGenerator.class)).setLevel(Level.DEBUG);
        Files.createDirectories(tempDirectory);
        ((Logger)LoggerFactory.getLogger("org.apache.fontbox")).setLevel(Level.WARN);
        ((Logger)LoggerFactory.getLogger("org.apache.pdfbox")).setLevel(Level.WARN);
        ((Logger)LoggerFactory.getLogger("com.openhtmltopdf")).setLevel(Level.WARN);
    }

    private TestSuiteOverview getTestSuiteOverviewWithGroups(Long testSuiteId, String name, String description, ReportSpecs specs) throws DatatypeConfigurationException {
        TestSuiteOverview data = new TestSuiteOverview();
        data.setTestSuiteId(testSuiteId);
        data.setOverallStatus("SUCCESS");
        data.setTestSuiteName(name);
        data.setTestSuiteDescription(description);
        data.setSpecReference("Chapter 1.1");
        data.setSpecDescription("Specification reference with further details online.");
        data.setSpecLink("https://joinup.ec.europa.eu/");
        data.setTestCases(List.of(
                getTestCaseOverview("Test Case Report #0", specs, false, false, null, null),
                getTestCaseOverview("Test Case Report #1", specs, false, false, null, "G1", "FAILURE"),
                getTestCaseOverview("Test Case Report #2", specs, false, false, List.of(new TestCaseOverview.Tag("security", "Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. ", "#FFFFFF", "#ff3c33"), new TestCaseOverview.Tag("performance", "Test case relevant to performance issues.", "#FFFFFF", "#000000")), "G2"),
                getTestCaseOverview("Test Case Report #3", specs, false, false, null, "G2", "FAILURE"),
                getTestCaseOverview("Test Case Report #4", specs, false, true, null, "G2", "FAILURE"),
                getTestCaseOverview("Test Case Report #5", specs, true, false, null, "G3"),
                getTestCaseOverview("Test Case Report #6", specs, true, false, null, "G3"),
                getTestCaseOverview("Test Case Report #7", specs, false, false, List.of(new TestCaseOverview.Tag("security", "Test case relevant to security issues.", "#FFFF00", "#ff3c33"), new TestCaseOverview.Tag("first issue", "First issue to deal with.", "#FFFFFF", "#ff3c33")), null)
        ));
        data.setTestCaseGroups(List.of(
                getTestCaseGroup("G1", "Group 1", "Description for group 1"),
                getTestCaseGroup("G2", "Group 2", "Description for group 2"),
                getTestCaseGroup("G3", null, "Description for group 3")
        ));
        return data;
    }

    private TestCaseGroup getTestCaseGroup(String id, String name, String description) {
        var group = new TestCaseGroup();
        group.setId(id);
        group.setName(name);
        group.setDescription(description);
        return group;
    }

    private TestSuiteOverview getTestSuiteOverview(Long testSuiteId, String name, String description, ReportSpecs specs) throws DatatypeConfigurationException {
        TestSuiteOverview data = new TestSuiteOverview();
        data.setTestSuiteId(testSuiteId);
        data.setOverallStatus("SUCCESS");
        data.setTestSuiteName(name);
        data.setTestSuiteDescription(description);
        data.setSpecReference("Chapter 1.1");
        data.setSpecDescription("Specification reference with further details online.");
        data.setSpecLink("https://joinup.ec.europa.eu/");
        data.setTestCases(List.of(
                getTestCaseOverview("Test Case Report #1", specs),
                getTestCaseOverview("Test Case Report #2", specs, false, false, List.of(new TestCaseOverview.Tag("security", "Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. Test case relevant to security issues. ", "#FFFFFF", "#ff3c33"), new TestCaseOverview.Tag("performance", "Test case relevant to performance issues.", "#FFFFFF", "#000000")), null),
                getTestCaseOverview("Test Case Report #3", specs, false, false, null, null),
                getTestCaseOverview("Test Case Report #4", specs),
                getTestCaseOverview("Test Case Report #5", specs, false, false, List.of(new TestCaseOverview.Tag("security", "Test case relevant to security issues.", "#FFFF00", "#ff3c33"), new TestCaseOverview.Tag("first issue", "First issue to deal with.", "#FFFFFF", "#ff3c33")), null)
        ));
        return data;
    }

    private TestCaseOverview getTestCaseOverview(String title, ReportSpecs specs) throws DatatypeConfigurationException {
        return getTestCaseOverview(title, specs, false, false, null, null);
    }

    private TestCaseOverview getTestCaseOverview(String title, ReportSpecs specs, boolean optional, boolean disabled, List<TestCaseOverview.Tag> tags, String groupId) throws DatatypeConfigurationException {
        return getTestCaseOverview(title, specs, optional, disabled, tags, groupId, "SUCCESS");
    }

    private TestCaseOverview getTestCaseOverview(String title, ReportSpecs specs, boolean optional, boolean disabled, List<TestCaseOverview.Tag> tags, String groupId, String result) throws DatatypeConfigurationException {
        TestCaseOverview data = new TestCaseOverview();
        // Labels
        data.setTitle(title);
        data.setTags(tags);
        data.setLabelOrganisation("Organisation");
        data.setLabelSystem("System");
        data.setLabelDomain("Domain");
        data.setLabelSpecification("Specification");
//        data.setLabelSpecification("Specification profile to test");
        data.setLabelActor("Actor");
        // Basic data
        data.setGroup(groupId);
        data.setOrganisation("My organisation");
        data.setSystem("My system");
        data.setTestDomain("My domain");
        data.setTestSpecification("Core Profile – v1.0");
//        data.setTestSpecification("My specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specificationMy specification");
        data.setTestActor("My actor");
        data.setTestName("Test case 1");
//        data.setTestName("Test case 1 Test case 1 Test case 1 Test case 1 Test case 1 Test case 1 Test case 1 Test case 1 Test case 1 Test case 1 Test case 1 Test case 1 Test case 1 Test case 1 Test case 1 Test case 1 Test case 1 Test case 1 Test case 1 Test case 1 Test case 1 ");
        data.setTestDescription("Description for test case 1");
//        data.setTestDescription("");
//        data.setTestDescription("Description for test case 1. Description for test case 1. Description for test case 1. Description for test case 1. Description for test case 1. Description for test case 1. Description for test case 1. Description for test case 1. Description for test case 1. ");
        data.setSpecReference("Chapter 1.1");
        data.setSpecDescription("Specification reference with further details online.");
//        data.setSpecDescription("Specification reference with further details online. Specification reference with further details online. Specification reference with further details online. Specification reference with further details online. Specification reference with further details online. Specification reference with further details online.");
        data.setSpecLink("https://joinup.ec.europa.eu/");
        data.setReportResult(result);
        data.setStartTime("06/04/2023 10:21:43");
        data.setEndTime("06/04/2023 10:21:44");
        data.setOutputMessages(List.of("This is the output message for your test session. Check the different report steps for details.", "This is an extra message."));
        data.setOptional(optional);
        data.setDisabled(disabled);
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
        data.setDocumentation("""
                <p><strong>[TC2] Invalid registration</strong></p>
                 <p>Passenger attempts to register in the wallet with an invalid photo. The wallet provider is expected to return an error with an appropriate error code.</p>
                 <p><strong>Assumptions:</strong></p>
                 <ul><li>The access event ID and access gate ID used in API calls are provided as inputs by the user.</li><li>The wallet provider has already registered to obtain an API key.</li><li>When provided with an invalid photo the registration call shall return at least one boarding pass. For the purposes of the PoC this is triggered by passing a value of &#34;<em>BAD_PHOTO</em>&#34; for either the access event ID or access gate ID.</li></ul>
                 <p>The following table summarises the <b>additional</b> test steps:</p><table style="border-collapse:collapse;width:100%;height:240.875px;border-color:#34495e;border-style:solid"><tbody><tr style="height:27.375px"><td style="width:12.9954%;height:27.375px;background-color:#34495e;border:1px solid #34495e"><strong><span style="color:#ffffff">  Test case</span></strong></td><td style="width:71.3364%;height:27.375px;background-color:#ecf0f1;border:1px solid #34495e"><strong><span style="background-color:transparent">Get requirements</span></strong></td><td style="width:7.74194%;height:27.375px;background-color:#34495e;text-align:center;border:1px solid #34495e"><strong><span style="color:#ffffff">ID</span></strong></td><td style="width:7.92627%;height:27.375px;background-color:#ecf0f1;text-align:center;border:1px solid #34495e"><strong>TC1</strong></td></tr><tr style="height:27.75px"><td style="height:27.75px;background-color:#34495e;width:12.9954%;border:1px solid #34495e"><strong><span style="color:#ffffff">  Actors</span></strong></td><td style="height:27.75px;width:87.0046%;border:1px solid #34495e" colspan="3"><strong>Evidence Requester (ER)¹</strong>, Evidence Broker (EB)</td></tr><tr style="height:27.375px"><td style="width:12.9954%;height:27.375px;background-color:#34495e;border:1px solid #34495e"><strong><span style="color:#ffffff">  Description</span></strong></td><td style="height:27.375px;width:87.0046%;border:1px solid #34495e" colspan="3"><span style="background-color:transparent">Test case to verify that an Evidence Requester (ER) can correctly query an Evidence Broker (EB) with a Get List of Requirements query.</span></td></tr><tr style="height:131px"><td style="width:12.9954%;height:131px;background-color:#34495e;border:1px solid #34495e"><strong><span style="color:#ffffff">  Steps</span></strong></td><td style="height:131px;width:87.0046%;border:1px solid #34495e" colspan="3">
                 <p style="text-align:left">The following steps will be carried out as part of this test case (validation steps shown in <em>italics</em>).</p>
                 <ol><li style="text-align:left">ER sends to EB a “<a href="https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?pageId&#61;406946271#id-3.2EvidenceBroker%28EB%29-4.QueryInterfaceSpecification" target="_blank" rel="noopener noreferrer nofollow">Get List of Requirements</a>” query (with any data from the shared test data).</li><li>EB responds with the requested requirements.
                 <ol><li><em>Check that the query type is as expected.</em></li><li><em>Check that the query was valid (no exceptions were raised). </em></li></ol>
                 </li></ol>
                 </td></tr></tbody></table>
                 <p><em><span style="font-size:8pt">¹ This is the role that your system (the System Under Test - SUT) plays in this test case</span></em></p>
                 <p>For service definitions, data models and expected behaviours please refer to the <a href="https://ec.europa.eu/cefdigital/wiki/display/SDGOO/Chapter&#43;3%3A&#43;Common&#43;Services&#43;-&#43;December&#43;2021?src&#61;contextnavpagetreemode" target="_blank" rel="noopener noreferrer nofollow">SDG Once-Only Collaborative Space (Chapter 3)</a>.</p>""");
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
                .withResourceResolver((uri) -> {
                    if (uri.startsWith("file://")) {
                        return uri;
                    } else {
                        return Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(uri), "URI [%s]".formatted(uri)).toString();
                    }
                });
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
    void testConformanceOverview() throws IOException, DatatypeConfigurationException {
        var specs = ReportSpecs.build()
                .withResourceResolver((uri) -> Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(uri)).toString());
        ConformanceOverview data = new ConformanceOverview();

        data.setIncludeMessage(true);
        data.setIncludeConformanceItems(true);
        data.setIncludeTestCases(true);

        data.setMessage("<b>This is your report</b>");
        data.setLabelOrganisation("Organisation");
        data.setLabelSystem("System");
        data.setLabelDomain("Domain");
        data.setLabelSpecification("Specification");
        data.setLabelSpecificationGroup("Group");
        data.setLabelSpecificationInGroup("Option");
        data.setLabelActor("Actor");
        data.setTitle("Conformance overview");
        data.setTestDomain("Domain 1");

        data.setOrganisation("Organisation");
        data.setSystem("System");
        data.setOverallStatus("SUCCESS");

        // Items
        var group1 = getConformanceItem("Group1", "Description for group 1", "FAILURE");
        var spec1_1 = getConformanceItem("Specification 1", "Description for specification 1", "SUCCESS");
        spec1_1.setData(new ConformanceStatementData());
        spec1_1.getData().setTestDomain("Domain 1");
        spec1_1.getData().setTestSpecification("Specification 1");
        spec1_1.getData().setTestActor("Actor 1");
        spec1_1.getData().setCompletedTests(245);
        spec1_1.getData().setFailedTests(0);
        spec1_1.getData().setUndefinedTests(0);
        spec1_1.getData().setOverallStatus("SUCCESS");
        spec1_1.getData().setLastUpdated("12/02/2024 12:30:21");
        spec1_1.getData().setTestSuites(List.of(
                getTestSuiteOverview(1L, "The third test suite", "Description for the third test suite", specs),
                getTestSuiteOverview(2L, "Test suite 2", "Description for test suite 2", specs)
        ));
        var spec1_2 = getConformanceItem("Specification 2", "Description for specification 2", "UNDEFINED");
        spec1_2.setData(new ConformanceStatementData());
        spec1_2.getData().setTestDomain("Domain 1");
        spec1_2.getData().setTestSpecification("Specification 2");
        spec1_2.getData().setTestActor("Actor 1");
        spec1_2.getData().setCompletedTests(0);
        spec1_2.getData().setFailedTests(0);
        spec1_2.getData().setUndefinedTests(30);
        spec1_2.getData().setOverallStatus("UNDEFINED");
        spec1_2.getData().setTestSuites(List.of(
                getTestSuiteOverview(3L, "The third test suite", "Description for the third test suite", specs),
                getTestSuiteOverview(4L, "Test suite 2", "Description for test suite 2", specs)
        ));
        var spec1_3 = getConformanceItem("Specification 3", "Description for specification 3", "FAILURE");
        spec1_3.setData(new ConformanceStatementData());
        spec1_3.getData().setTestDomain("Domain 1");
        spec1_3.getData().setTestSpecification("Specification 3");
        spec1_3.getData().setTestActor("Actor 1");
        spec1_3.getData().setCompletedTests(245);
        spec1_3.getData().setFailedTests(105);
        spec1_3.getData().setUndefinedTests(0);
        spec1_3.getData().setOverallStatus("FAILURE");
        spec1_3.getData().setTestSuites(List.of(
                getTestSuiteOverview(5L,"The third test suite", "Description for the third test suite", specs),
                getTestSuiteOverview(6L, "Test suite 2", "Description for test suite 2", specs)
        ));

        group1.setItems(List.of(spec1_1, spec1_2, spec1_3));
//        var group2 = getConformanceItem("Group2", "Description for group 2", "SUCCESS");
//        group1.setItems(List.of(
//                getConformanceItem("Specification 1", "Description for specification 1", "SUCCESS"),
//                getConformanceItem("Specification 2", "Description for specification 2", "SUCCESS"),
//                getConformanceItem("Specification 3", "Description for specification 3", "SUCCESS")
//        ));

        var domain = getConformanceItem("Domain1", "Description for domain 1", "FAILURE");
        domain.setItems(List.of(group1));

        data.setConformanceItems(List.of(domain));

        try (var outputStream = Files.newOutputStream(Path.of(tempDirectory.toString(), "ConformanceOverview.pdf"))) {
            generator.writeConformanceOverviewReport(data, outputStream, specs);
        }
    }

    private ConformanceItem getConformanceItem(String name, String description, String status) {
        var item = new ConformanceItem();
        item.setName(name);
        item.setDescription(description);
        item.setOverallStatus(status);
        return item;
    }

    @Test
    void testConformanceStatementOverview() throws IOException, DatatypeConfigurationException {
        var specs = ReportSpecs.build()
                .withContextItemTruncateLimit(1000)
                .withContextItems(true)
                .withTestSteps(true)
                .withResourceResolver((uri) -> {
                    if (uri.startsWith("file://")) {
                        return uri;
                    } else {
                        return Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(uri), "URI [%s]".formatted(uri)).toString();
                    }
                });
        ConformanceStatementOverview data = new ConformanceStatementOverview();
        // Labels
        data.setTitle("Η γρήγορη καφέ αλεπού πήδηξε πάνω από το τεμπέλικο σκυλί.");
        data.setLabelOrganisation("Organisation");
//        data.setLabelOrganisation("Organisation label that is big");
//        data.setLabelSystem("System label that is very big and should really be smaller");
        data.setLabelSystem("System");
        data.setLabelDomain("Domain");
        data.setLabelSpecification("Specification");
        data.setLabelSpecificationGroup("Group");
        data.setLabelSpecificationInGroup("Option");
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
        data.setIncludeTestCases(false);
        data.setIncludePageNumbers(false);

//        data.setMessage("<strong>This is the result.</strong><img src=\"%s\"/>".formatted(Path.of("C:\\work\\gitb-repository\\files\\badges\\latest\\1\\SUCCESS.png").toUri()));
        data.setMessage("<strong>This is the result.</strong>");

        data.setOverallStatus("SUCCESS");
        data.setTestSuites(List.of(
//                getTestSuiteOverview("The first test suite", "Description for the first test suite", specs),
//                getTestSuiteOverview("The second test suite", "Description for the second test suite", specs),
                getTestSuiteOverviewWithGroups(1L, "The first test suite", "Description for the first test suite", specs),
                getTestSuiteOverview(2L, "The third test suite", "Description for the third test suite", specs),
                getTestSuiteOverview(3L, "Test suite 2", "Description for test suite 2", specs)
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
            tar.getContext().getItem().get(2).getItem().get(1).setValue("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <data>
                    \t<metadata>
                    \t\t<gitb:name>Simple TC</gitb:name>
                    \t\t<gitb:type>CONFORMANCE</gitb:type>
                    \t\t<gitb:version>1.0</gitb:version>
                    \t\t<gitb:description>A simple test case.</gitb:description>
                    \t</metadata>
                    \t<actors>
                    \t\t<gitb:actor id="Actor" name="Actor" role="SUT"/>
                    \t</actors>
                    \t<steps>
                            B,C,D
                    \t</steps>
                    </data>""");
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
