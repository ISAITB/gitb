package com.gitb.reports;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.reports.dto.ConformanceOverview;
import com.gitb.reports.dto.ConformanceStatementOverview;
import com.gitb.reports.dto.TestCaseOverview;
import com.gitb.reports.dto.tar.ContextItem;
import com.gitb.reports.dto.tar.Report;
import com.gitb.reports.dto.tar.ReportItem;
import com.gitb.reports.extensions.EscapeHtml;
import com.gitb.reports.extensions.PrintResult;
import com.gitb.reports.extensions.TestCoverageBlock;
import com.gitb.tbs.TestStepStatus;
import com.gitb.tr.*;
import com.openhtmltopdf.extend.FSCacheEx;
import com.openhtmltopdf.extend.FSCacheValue;
import com.openhtmltopdf.extend.impl.FSDefaultCacheStore;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.slf4j.Slf4jLogger;
import com.openhtmltopdf.swing.NaiveUserAgent;
import com.openhtmltopdf.util.XRLog;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import jakarta.xml.bind.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ReportGenerator {

    static {
        // Use SLF4J logging.
        XRLog.setLoggerImpl(new Slf4jLogger());
    }

    private static final Logger LOG = LoggerFactory.getLogger(ReportGenerator.class);
    private static final ReportGenerator INSTANCE = new ReportGenerator();
    private final JAXBContext jaxbContext;
    private final Map<String, Template> templateCache;
    private final Map<String, TemplateMethodModelEx> extensionFunctions;
    private final FSCacheEx<String, FSCacheValue> fontCache = new FSDefaultCacheStore();

    private ReportGenerator() {
        templateCache = new ConcurrentHashMap<>();
        try {
            jaxbContext = JAXBContext.newInstance(
                    TAR.class,
                    TestCaseOverviewReportType.class,
                    TestCaseReportType.class,
                    TestStepStatus.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Error initialising report generator", e);
        }
        extensionFunctions = Map.of(
            "escape", new EscapeHtml(),
            "coverageBlock", new TestCoverageBlock(),
            "printResult", new PrintResult()
        );
    }

    public static ReportGenerator getInstance() {
        return INSTANCE;
    }

    private Template getTemplate(String reportPath) {
        return templateCache.computeIfAbsent(reportPath, path -> {
            var configuration = new Configuration(Configuration.VERSION_2_3_32);
            configuration.setTemplateLoader(new ClassTemplateLoader(ReportGenerator.class, "/"));
            Template template;
            try {
                template = configuration.getTemplate(path);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load report template ["+path+"]", e);
            }
            return template;
        });
    }

    private void loadFonts(PdfRendererBuilder builder) {
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeSans/FreeSans.ttf"), "FreeSans", 400, BaseRendererBuilder.FontStyle.NORMAL, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeSans/FreeSansBold.ttf"), "FreeSans", 700, BaseRendererBuilder.FontStyle.NORMAL, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeSans/FreeSansOblique.ttf"), "FreeSans", 400, BaseRendererBuilder.FontStyle.ITALIC, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeSans/FreeSansOblique.ttf"), "FreeSans", 400, BaseRendererBuilder.FontStyle.OBLIQUE, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeSans/FreeSansBoldOblique.ttf"), "FreeSans", 700, BaseRendererBuilder.FontStyle.ITALIC, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeSans/FreeSansBoldOblique.ttf"), "FreeSans", 700, BaseRendererBuilder.FontStyle.OBLIQUE, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeMono/FreeMono.ttf"), "FreeMono", 400, BaseRendererBuilder.FontStyle.NORMAL, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeMono/FreeMonoBold.ttf"), "FreeMono", 700, BaseRendererBuilder.FontStyle.NORMAL, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeMono/FreeMonoOblique.ttf"), "FreeMono", 400, BaseRendererBuilder.FontStyle.ITALIC, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeMono/FreeMonoOblique.ttf"), "FreeMono", 400, BaseRendererBuilder.FontStyle.OBLIQUE, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeMono/FreeMonoBoldOblique.ttf"), "FreeMono", 700, BaseRendererBuilder.FontStyle.ITALIC, true);
        builder.useFont(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/FreeMono/FreeMonoBoldOblique.ttf"), "FreeMono", 700, BaseRendererBuilder.FontStyle.OBLIQUE, true);
    }

    public void writeClasspathReport(String reportPath, Map<String, Object> parameters, OutputStream outputStream, ReportSpecs specs) {
        ReportSpecs specsToUse = Objects.requireNonNullElseGet(specs, ReportSpecs::build);
        // Add custom extension functions.
        parameters = Objects.requireNonNullElse(parameters, Collections.emptyMap());
        parameters.putAll(extensionFunctions);
        // Generate HTML report.
        File tempHtmlFile = null;
        String tempHtmlString = null;
        try {
            if (specsToUse.getTempFolderPath() == null) {
                var writer = new StringWriter();
                getTemplate(reportPath).process(parameters, writer);
                writer.flush();
                tempHtmlString = writer.toString();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("### Report HTML - START ###\n\n{}\n\n### Report HTML - END ###", tempHtmlString);
                }
            } else {
                Files.createDirectories(specsToUse.getTempFolderPath());
                tempHtmlFile = Path.of(specsToUse.getTempFolderPath().toString(), UUID.randomUUID().toString()).toFile();
                try (var writer = new FileWriter(tempHtmlFile, StandardCharsets.UTF_8)) {
                    getTemplate(reportPath).process(parameters, writer);
                    writer.flush();
                }
            }
            // Convert to PDF.
            var builder = new PdfRendererBuilder();
            builder.useCacheStore(PdfRendererBuilder.CacheStore.PDF_FONT_METRICS, fontCache);
            loadFonts(builder);
            builder.useUriResolver(new NaiveUserAgent.DefaultUriResolver() {
                @Override
                public String resolveURI(String baseUri, String uri) {
                    if (uri.startsWith("classpath:")) {
                        // A predefined image.
                        return Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(StringUtils.removeStart(uri, "classpath:"))).toString();
                    } else if (specsToUse.getResourceResolver() != null) {
                        // Check if this is a community-specific resource.
                        String resolvedResource = specsToUse.getResourceResolver().apply(uri);
                        return Objects.requireNonNullElseGet(resolvedResource, () -> super.resolveURI(baseUri, uri));
                    }
                    return super.resolveURI(baseUri, uri);
                }
            });

            if (tempHtmlFile != null) {
                builder.withW3cDocument(new W3CDom().fromJsoup(Jsoup.parse(tempHtmlFile, StandardCharsets.UTF_8.name())), "reports");
            } else {
                builder.withW3cDocument(new W3CDom().fromJsoup(Jsoup.parse(tempHtmlString)), "reports");
            }

            builder.toStream(outputStream);
            builder.run();
        } catch (IOException | TemplateException e) {
            throw new IllegalStateException("Error while generating report for template ["+reportPath+"]", e);
        } finally {
            if (tempHtmlFile != null) {
                FileUtils.deleteQuietly(tempHtmlFile);
            }
        }
    }

    public void writeTestStepStatusReport(InputStream inputStream, String title, OutputStream outputStream, ReportSpecs specs) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<TestStepStatus> stepStatus = unmarshaller.unmarshal(new StreamSource(inputStream), TestStepStatus.class);
            writeTestStepStatusReport(stepStatus.getValue(), title, outputStream, specs);
        } catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void writeTestStepStatusXmlReport(InputStream inputStream, OutputStream outputStream, boolean addContext) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<TestStepStatus> stepStatus = unmarshaller.unmarshal(new StreamSource(inputStream), TestStepStatus.class);
            if (!addContext && stepStatus.getValue().getReport() instanceof TAR) {
                ((TAR) stepStatus.getValue().getReport()).setContext(null);
            }
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(new ObjectFactory().createTestStepReport(stepStatus.getValue().getReport()), outputStream);
        } catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void writeTestStepStatusXmlReport(TAR report, OutputStream outputStream, boolean addContext) {
        try {
            if (!addContext) {
                report.setContext(null);
            }
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(new ObjectFactory().createTestStepReport(report), outputStream);
        } catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void writeTestStepStatusReport(TestStepStatus statusReport, String title, OutputStream outputStream, ReportSpecs specs) {
        if (statusReport.getReport() instanceof TAR) {
            writeTARReport((TAR)statusReport.getReport(), title, outputStream, specs);
        }
    }

    public void writeTARReport(TAR reportType, String title, OutputStream outputStream, ReportSpecs specs) {
        writeTestStepReport(reportType, title, outputStream, specs);
    }

    public void writeTARReport(InputStream inputStream, String title, OutputStream outputStream, ReportSpecs specs) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<TAR> tar = unmarshaller.unmarshal(new StreamSource(inputStream), TAR.class);
            writeTARReport(tar.getValue(), title, outputStream, specs);
        } catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void writeTestStepReport(TestStepReportType reportType, String title, OutputStream outputStream, ReportSpecs specs) {
        try {
            Report report = fromTestStepReportType(reportType, title, specs);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("title", report.getTitle());
            parameters.put("reportDate", report.getReportDate());
            parameters.put("reportResult", report.getReportResult());
            parameters.put("errorCount", report.getErrorCount());
            parameters.put("warningCount", report.getWarningCount());
            parameters.put("messageCount", report.getMessageCount());
            if (report.getReportItems() != null && !report.getReportItems().isEmpty()) {
                parameters.put("reportItems", report.getReportItems());
            }
            if (report.getContextItems() != null && !report.getContextItems().isEmpty()) {
                parameters.put("contextItems", report.getContextItems());
            }
            writeClasspathReport("reports/TAR.ftl", parameters, outputStream, specs);
        } catch (Exception e) {
            throw new IllegalStateException("Error while creating test step report", e);
        }
    }

    public <T extends TestStepReportType> Report fromTestStepStatus(InputStream inStream, String title, ReportSpecs specs) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<TestStepStatus> testStepStatus = unmarshaller.unmarshal(new StreamSource(inStream), TestStepStatus.class);
            return fromTestStepReportType(testStepStatus.getValue().getReport(), title, specs);
        } catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String contextValueAsString(String value) {
        if (StringUtils.startsWith(value, "___[[") && StringUtils.endsWith(value, "]]___")) {
            // This is a pointer to a large file.
            value = "[File content]";
        }
        return value;
    }

    private ContextItem toContextItem(AnyContent content, ReportSpecs specs) {
        ContextItem item = null;
        if (content != null && content.isForDisplay()) {
            if (content.getItem().isEmpty()) {
                if (StringUtils.isNotBlank(content.getValue())) {
                    String value;
                    if (content.getEmbeddingMethod() == ValueEmbeddingEnumeration.URI) {
                        value = "["+content.getValue()+"]";
                    } else if (content.getEmbeddingMethod() == ValueEmbeddingEnumeration.BASE_64) {
                        if (content.getMimeType() != null && specs.getMimeTypesToConvertToStrings().contains(content.getMimeType())) {
                            value = contextValueAsString(new String(Base64.getDecoder().decode(content.getValue())));
                        } else {
                            value = "[File content]";
                        }
                    } else {
                        value = contextValueAsString(content.getValue());
                    }
                    item = new ContextItem(StringUtils.defaultString(content.getName()), truncateIfNeeded(value, specs));
                }
            } else {
                var children = content.getItem().stream()
                        .filter(AnyContent::isForDisplay)
                        .map((childItem) -> toContextItem(childItem, specs))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (!children.isEmpty()) {
                    item = new ContextItem(StringUtils.defaultString(content.getName()), children);
                }

            }
        }
        return item;
    }

    private String truncateIfNeeded(String value, ReportSpecs specs) {
        if (specs.getContextItemTruncateLimit() > 0) {
            var truncatedValue = StringUtils.truncate(value, specs.getContextItemTruncateLimit());
            if (value.length() > truncatedValue.length()) {
                truncatedValue += " [...]";
            }
            return truncatedValue;
        } else {
            return value;
        }
    }

    private void addContextItems(TAR report, List<ContextItem> items, ReportSpecs specs) {
        var contextItem = toContextItem(report.getContext(), specs);
        if (contextItem != null && contextItem.getItems() != null) {
            items.addAll(contextItem.getItems());
        }
    }

    public <T extends TestStepReportType> Report fromTestStepReportType(T reportType, String title, ReportSpecs specs) {
        specs = Objects.requireNonNullElseGet(specs, ReportSpecs::build);
        Report report = new Report();
        if (reportType.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            sdf.setTimeZone(TimeZone.getDefault());
            report.setReportDate(sdf.format(reportType.getDate().toGregorianCalendar().getTime()));
        }
        report.setReportResult(reportType.getResult().value());
        report.setTitle(Objects.requireNonNullElse(title, "Report"));
        if (reportType instanceof TAR) {
            TAR tarReport = (TAR)reportType;
            if (specs.isIncludeContextItems()) {
                addContextItems(tarReport, report.getContextItems(), specs);
            }
            if (tarReport.getReports() != null && tarReport.getReports().getInfoOrWarningOrError() != null) {
                int errors = 0;
                int warnings = 0;
                int messages = 0;
                for (JAXBElement<TestAssertionReportType> element : tarReport.getReports().getInfoOrWarningOrError()) {
                    if (element.getValue() instanceof BAR) {
                        BAR tarItem = (BAR) element.getValue();
                        ReportItem reportItem = new ReportItem();
                        reportItem.setLevel(element.getName().getLocalPart());
                        if ("error".equalsIgnoreCase(reportItem.getLevel())) {
                            errors += 1;
                        } else if ("warning".equalsIgnoreCase(reportItem.getLevel())) {
                            warnings += 1;
                        } else {
                            messages += 1;
                        }
                        reportItem.setDescription(StringUtils.defaultIfBlank(tarItem.getDescription(), "-"));
                        reportItem.setTest(StringUtils.trimToNull(tarItem.getTest()));
                        reportItem.setLocation(StringUtils.trimToNull(tarItem.getLocation()));
                        report.getReportItems().add(reportItem);
                    }
                }
                report.setErrorCount(errors);
                report.setWarningCount(warnings);
                report.setMessageCount(messages);
            }
        }
        if (report.getReportItems().isEmpty()) {
            report.setReportItems(null);
        }
        if (report.getContextItems().isEmpty()) {
            report.setContextItems(null);
        }
        return report;
    }

    public void writeConformanceOverviewReport(ConformanceOverview overview, OutputStream outputStream, ReportSpecs specs) {
        if (overview.getReportDate() == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            overview.setReportDate(sdf.format(new Date()));
        }
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("data", overview);
            writeClasspathReport("reports/ConformanceOverview.ftl", parameters, outputStream, specs);
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected error while generating report", e);
        }
    }

    public void writeConformanceStatementOverviewReport(ConformanceStatementOverview overview, OutputStream outputStream, ReportSpecs specs) {
        if (overview.getReportDate() == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            overview.setReportDate(sdf.format(new Date()));
        }
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("data", overview);
            writeClasspathReport("reports/ConformanceStatementOverview.ftl", parameters, outputStream, specs);
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected error while generating report", e);
        }
    }

    private void emptyTestStepContext(TestCaseOverviewReportType testCaseOverview) {
        if (testCaseOverview != null && testCaseOverview.getSteps() != null) {
            for (TestCaseStepReportType step: testCaseOverview.getSteps().getStep()) {
                if (step.getReport() instanceof TAR) {
                    ((TAR) step.getReport()).setContext(null);
                }
            }
        }
    }

    public void writeTestCaseOverviewXmlReport(TestCaseOverviewReportType testCaseOverview, OutputStream outputStream) {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            emptyTestStepContext(testCaseOverview);
            marshaller.marshal(new ObjectFactory().createTestCaseOverviewReport(testCaseOverview), outputStream);
        } catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void writeConformanceStatementXmlReport(ConformanceStatementReportType report, OutputStream outputStream) {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            if (report.getStatement() != null && report.getStatement().getTestDetails() != null && report.getStatement().getTestDetails().getTestCase() != null) {
                for (TestCaseOverviewReportType testCaseOverview: report.getStatement().getTestDetails().getTestCase()) {
                    emptyTestStepContext(testCaseOverview);
                }
            }
            marshaller.marshal(new ObjectFactory().createConformanceStatementReport(report), outputStream);
        } catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void writeConformanceOverviewXmlReport(ConformanceOverviewReportType report, OutputStream outputStream) {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(new ObjectFactory().createConformanceOverviewReport(report), outputStream);
        } catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void writeTestCaseDocumentationPreviewReport(String documentation, OutputStream outputStream, ReportSpecs specs) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("documentation", documentation);
            writeClasspathReport("reports/TestCaseDocumentationPreview.ftl", parameters, outputStream, specs);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void writeTestCaseOverviewReport(TestCaseOverview testCaseOverview, OutputStream outputStream, ReportSpecs specs) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("title", testCaseOverview.getTitle());
            parameters.put("organisation", testCaseOverview.getOrganisation());
            parameters.put("system", testCaseOverview.getSystem());
            parameters.put("testDomain", testCaseOverview.getTestDomain());
            parameters.put("testSpecification", testCaseOverview.getTestSpecification());
            parameters.put("testActor", testCaseOverview.getTestActor());
            parameters.put("reportResult", testCaseOverview.getReportResult());
            parameters.put("outputMessage", testCaseOverview.getOutputMessage());
            parameters.put("startTime", testCaseOverview.getStartTime());
            parameters.put("endTime", testCaseOverview.getEndTime());
            parameters.put("testName", testCaseOverview.getTestName());
            parameters.put("testDescription", testCaseOverview.getTestDescription());
            parameters.put("specReference", testCaseOverview.getSpecReference());
            parameters.put("specDescription", testCaseOverview.getSpecDescription());
            parameters.put("specLink", testCaseOverview.getSpecLink());
            if (specs.isIncludeTestSteps()) {
                parameters.put("steps", testCaseOverview.getSteps());
            }
            parameters.put("labelDomain", testCaseOverview.getLabelDomain());
            parameters.put("labelSpecification", testCaseOverview.getLabelSpecification());
            parameters.put("labelActor", testCaseOverview.getLabelActor());
            parameters.put("labelOrganisation", testCaseOverview.getLabelOrganisation());
            parameters.put("labelSystem", testCaseOverview.getLabelSystem());
            if (specs.isIncludeDocumentation()) {
                parameters.put("documentation", testCaseOverview.getDocumentation());
            }
            if (specs.isIncludeLogs()) {
                parameters.put("logMessages", testCaseOverview.getLogMessages());
            }
            writeClasspathReport("reports/TestCaseOverview.ftl", parameters, outputStream, specs);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
