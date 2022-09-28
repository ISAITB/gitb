package com.gitb.reports;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.reports.dto.ConformanceStatementOverview;
import com.gitb.reports.dto.TestCaseOverview;
import com.gitb.reports.dto.tar.ContextItem;
import com.gitb.reports.dto.tar.Report;
import com.gitb.reports.dto.tar.ReportItem;
import com.gitb.tbs.TestStepStatus;
import com.gitb.tr.*;
import net.sf.jasperreports.engine.*;
import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.*;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportGenerator {

    private JAXBContext jaxbContext;

    public ReportGenerator() {
        try {
            jaxbContext = JAXBContext.newInstance(
                    TAR.class,
                    TestCaseReportType.class,
                    TestStepStatus.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    public void writeReport(InputStream reportStream, Map<String, Object> parameters, OutputStream outputStream) throws JRException {
        JasperPrint jasperPrint = JasperFillManager.fillReport(reportStream, parameters, new JREmptyDataSource());
        try {
            JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                } catch (IOException e) {
                }
            }
        }
    }

    public void writeClasspathReport(String reportPath, Map<String, Object> parameters, OutputStream outputStream) throws JRException {
        writeReport(Thread.currentThread().getContextClassLoader().getResourceAsStream(reportPath), parameters, outputStream);
    }

    public void writeTestStepStatusReport(InputStream inputStream, String title, OutputStream outputStream, boolean addContext) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<TestStepStatus> stepStatus = unmarshaller.unmarshal(new StreamSource(inputStream), TestStepStatus.class);
            writeTestStepStatusReport(stepStatus.getValue(), title, outputStream, addContext);
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
            marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);
            marshaller.marshal(new ObjectFactory().createTestStepReport(stepStatus.getValue().getReport()), outputStream);
        } catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void writeTestStepStatusReport(TestStepStatus statusReport, String title, OutputStream outputStream, boolean addContext) {
        if (statusReport.getReport() instanceof TAR) {
            writeTARReport((TAR)statusReport.getReport(), title, outputStream, addContext);
        }
    }

    public void writeTARReport(InputStream inputStream, String title, OutputStream outputStream) {
        writeTARReport(inputStream, title, outputStream, true);
    }

    public void writeTARReport(TAR reportType, String title, OutputStream outputStream) {
        writeTARReport(reportType, title, outputStream, true);
    }

    public void writeTARReport(TAR reportType, String title, OutputStream outputStream, boolean addContext) {
        writeTestStepReport(reportType, title, outputStream, addContext);
    }

    public void writeTARReport(InputStream inputStream, String title, OutputStream outputStream, boolean addContext) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<TAR> tar = unmarshaller.unmarshal(new StreamSource(inputStream), TAR.class);
            writeTARReport(tar.getValue(), title, outputStream, addContext);
        } catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void writeTestStepReport(TestStepReportType reportType, String title, OutputStream outputStream) {
        writeTestStepReport(reportType, title, outputStream, true);
    }

    public void writeTestStepReport(TestStepReportType reportType, String title, OutputStream outputStream, boolean addContext) {
        try {
            Report report = fromTestStepReportType(reportType, title, addContext);
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
            writeClasspathReport("reports/TAR.jasper", parameters, outputStream);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public <T extends TestStepReportType> Report fromTestStepStatus(InputStream inStream, String title, boolean addContext) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<TestStepStatus> testStepStatus = unmarshaller.unmarshal(new StreamSource(inStream), TestStepStatus.class);
            return fromTestStepReportType(testStepStatus.getValue().getReport(), title, addContext);
        } catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void addContextItem(AnyContent context, List<ContextItem> items, String keyPath) {
        if ("map".equals(context.getType()) || "list".equals(context.getType())) {
            if (context.getItem() != null) {
                if (!"".equals(keyPath)) {
                    keyPath += ".";
                }
                for (AnyContent internalContext: context.getItem()) {
                    addContextItem(internalContext, items, keyPath + context.getName());
                }
            }
        } else {
            if (context.getValue() != null) {
                if (!"".equals(keyPath)) {
                    keyPath += ".";
                }
                ContextItem item = new ContextItem();
                item.setKey(keyPath+context.getName());
                if (context.getEmbeddingMethod() == ValueEmbeddingEnumeration.STRING) {
                    item.setValue(context.getValue());
                } else if (context.getEmbeddingMethod() == ValueEmbeddingEnumeration.URI) {
                    item.setValue("[URI: "+context.getValue()+"]");
                } else if (context.getEmbeddingMethod() == ValueEmbeddingEnumeration.BASE_64) {
                    item.setValue("[BASE64 content]");
                }
                items.add(item);
            }
        }
    }

    public <T extends TestStepReportType> Report fromTestStepReportType(T reportType, String title, boolean addContext) {
        Report report = new Report();
        if (reportType.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss 'Z'");
            sdf.setTimeZone(TimeZone.getDefault());
            report.setReportDate(sdf.format(reportType.getDate().toGregorianCalendar().getTime()));
        }
        report.setReportResult(reportType.getResult().value());
        if (title == null) {
            report.setTitle("Report");
        } else {
            report.setTitle(title);
        }
        if (reportType instanceof TAR) {
            TAR tarReport = (TAR)reportType;
            if (addContext && tarReport.getContext() != null) {
                for (AnyContent context : tarReport.getContext().getItem()) {
                    addContextItem(context, report.getContextItems(), "");
                }
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
                report.setErrorCount(String.valueOf(errors));
                report.setWarningCount(String.valueOf(warnings));
                report.setMessageCount(String.valueOf(messages));
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

    public void writeConformanceStatementOverviewReport(ConformanceStatementOverview overview, OutputStream outputStream) {
        if (overview.getReportDate() == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            overview.setReportDate(sdf.format(new Date()));
        }
        if (overview.getTestCases() != null) {
            for (TestCaseOverview tc: overview.getTestCases()) {
                if (tc.getSubReportRoot() == null) {
                    tc.setSubReportRoot("reports/");
                }
            }
        }
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("title", overview.getTitle());
            parameters.put("organisation", overview.getOrganisation());
            parameters.put("system", overview.getSystem());
            parameters.put("testDomain", overview.getTestDomain());
            parameters.put("testSpecification", overview.getTestSpecification());
            parameters.put("testActor", overview.getTestActor());
            parameters.put("testStatus", overview.getTestStatus());
            parameters.put("overallStatus", overview.getOverallStatus());
            parameters.put("reportDate", overview.getReportDate());
            parameters.put("testCases", overview.getTestCases());
            parameters.put("includeTestCases", overview.getIncludeTestCases());
            parameters.put("includeMessage", overview.getIncludeMessage());
            if (overview.getIncludeMessage()) {
                parameters.put("message", overview.getMessage());
            }
            parameters.put("includeTestStatus", overview.getIncludeTestStatus());
            parameters.put("includeDetails", overview.getIncludeDetails());
            parameters.put("labelDomain", overview.getLabelDomain());
            parameters.put("labelSpecification", overview.getLabelSpecification());
            parameters.put("labelActor", overview.getLabelActor());
            parameters.put("labelOrganisation", overview.getLabelOrganisation());
            parameters.put("labelSystem", overview.getLabelSystem());
            writeClasspathReport("reports/ConformanceStatementOverview.jasper", parameters, outputStream);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void writeTestCaseOverviewReport(TestCaseOverview testCaseOverview, OutputStream outputStream) {
        if (testCaseOverview.getSubReportRoot() == null) {
            testCaseOverview.setSubReportRoot("reports/");
        }
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
            parameters.put("steps", testCaseOverview.getSteps());
            parameters.put("labelDomain", testCaseOverview.getLabelDomain());
            parameters.put("labelSpecification", testCaseOverview.getLabelSpecification());
            parameters.put("labelActor", testCaseOverview.getLabelActor());
            parameters.put("labelOrganisation", testCaseOverview.getLabelOrganisation());
            parameters.put("labelSystem", testCaseOverview.getLabelSystem());
            writeClasspathReport("reports/TestCaseOverview.jasper", parameters, outputStream);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
