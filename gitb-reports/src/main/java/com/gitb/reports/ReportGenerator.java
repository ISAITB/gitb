package com.gitb.reports;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.reports.dto.tar.ContextItem;
import com.gitb.reports.dto.tar.Report;
import com.gitb.reports.dto.tar.ReportItem;
import com.gitb.tr.BAR;
import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionReportType;
import net.sf.jasperreports.engine.*;
import org.apache.commons.lang.StringUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class ReportGenerator {

    private JAXBContext jaxbContext;

    public ReportGenerator() {
        try {
            jaxbContext = JAXBContext.newInstance(TAR.class);
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

    public void writeTARReport(InputStream tarStream, String title, OutputStream outputStream) throws JAXBException, JRException {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        JAXBElement<TAR> tar = (JAXBElement<TAR>)unmarshaller.unmarshal(tarStream);
        writeTARReport(tar.getValue(), title, outputStream);
    }


    public void writeTARReport(TAR tar, String title, OutputStream outputStream) throws JAXBException, JRException {
        Report report = new Report();
        report.setReportDate(tar.getDate().toString());
        report.setReportResult(tar.getResult().value());
        if (title == null) {
            report.setTitle("Report");
        } else {
            report.setTitle(title);
        }
        for (AnyContent context: tar.getContext().getItem()) {
            if (context.getEmbeddingMethod() == ValueEmbeddingEnumeration.STRING) {
                ContextItem item = new ContextItem();
                item.setKey(context.getName());
                item.setValue(context.getValue());
                report.getContextItems().add(item);
            }
        }

        for (JAXBElement<TestAssertionReportType> element: tar.getReports().getInfoOrWarningOrError()) {
            if (element.getValue() instanceof BAR) {
                BAR tarItem = (BAR)element.getValue();
                ReportItem reportItem = new ReportItem();
                reportItem.setLevel(element.getName().getLocalPart());
                reportItem.setDescription(StringUtils.defaultIfBlank(tarItem.getDescription(), "-"));
                reportItem.setTest(StringUtils.defaultIfBlank(tarItem.getTest(), "-"));
                reportItem.setLocation(StringUtils.defaultIfBlank(tarItem.getLocation(), "-"));
                report.getReportItems().add(reportItem);
            }
        }

        report.setErrorCount(tar.getCounters().getNrOfErrors().toString());
        report.setWarningCount(tar.getCounters().getNrOfWarnings().toString());
        report.setMessageCount(tar.getCounters().getNrOfAssertions().toString());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("title", report.getTitle());
        parameters.put("reportDate", report.getReportDate());
        parameters.put("reportResult", report.getReportResult());
        parameters.put("errorCount", report.getErrorCount());
        parameters.put("warningCount", report.getWarningCount());
        parameters.put("messageCount", report.getMessageCount());
        if (!report.getReportItems().isEmpty()) {
            parameters.put("reportItems", report.getReportItems());
        }
        if (!report.getContextItems().isEmpty()) {
            parameters.put("contextItems", report.getContextItems());
        }

        writeClasspathReport("reports/TAR.jasper", parameters, outputStream);
    }

}
