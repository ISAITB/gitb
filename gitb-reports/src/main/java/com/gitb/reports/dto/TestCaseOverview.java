package com.gitb.reports.dto;

import com.gitb.reports.dto.tar.Report;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class TestCaseOverview {

    private String title;
    private String organisation;
    private String system;
    private String testDomain;
    private String testSpecification;
    private String testActor;
    private String testName;
    private String testDescription;
    private String reportResult;
    private String outputMessage;
    private String startTime;
    private String endTime;
    private String id;
    private String labelDomain;
    private String labelSpecification;
    private String labelActor;
    private String labelOrganisation;
    private String labelSystem;
    private String documentation;
    private List<LogMessage> logMessages;
    private boolean optional;
    private boolean disabled;

    private List<Report> steps = new ArrayList<>();

    public String getOutputMessage() {
        return outputMessage;
    }

    public void setOutputMessage(String outputMessage) {
        this.outputMessage = outputMessage;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getTestDomain() {
        return testDomain;
    }

    public void setTestDomain(String testDomain) {
        this.testDomain = testDomain;
    }

    public String getTestSpecification() {
        return testSpecification;
    }

    public void setTestSpecification(String testSpecification) {
        this.testSpecification = testSpecification;
    }

    public String getTestActor() {
        return testActor;
    }

    public void setTestActor(String testActor) {
        this.testActor = testActor;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getTestDescription() {
        return testDescription;
    }

    public void setTestDescription(String testDescription) {
        this.testDescription = StringUtils.normalizeSpace(testDescription);
    }

    public String getReportResult() {
        return reportResult;
    }

    public void setReportResult(String reportResult) {
        this.reportResult = reportResult;
    }

    public List<Report> getSteps() {
        return steps;
    }

    public void setSteps(List<Report> steps) {
        this.steps = steps;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getLabelDomain() {
        return labelDomain;
    }

    public void setLabelDomain(String labelDomain) {
        this.labelDomain = labelDomain;
    }

    public String getLabelSpecification() {
        return labelSpecification;
    }

    public void setLabelSpecification(String labelSpecification) {
        this.labelSpecification = labelSpecification;
    }

    public String getLabelActor() {
        return labelActor;
    }

    public void setLabelActor(String labelActor) {
        this.labelActor = labelActor;
    }

    public String getLabelOrganisation() {
        return labelOrganisation;
    }

    public void setLabelOrganisation(String labelOrganisation) {
        this.labelOrganisation = labelOrganisation;
    }

    public String getLabelSystem() {
        return labelSystem;
    }

    public void setLabelSystem(String labelSystem) {
        this.labelSystem = labelSystem;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public List<LogMessage> getLogMessages() {
        return logMessages;
    }

    public void setLogMessages(List<String> logLines) {
        logMessages = null;
        var tempMessages = new ArrayList<LogMessage>();
        var previousLevel = LogMessage.DEBUG;
        for (var line: logLines) {
            var messageParts = StringUtils.split(StringUtils.replaceChars(line, '\r', '\n'), '\n');
            if (messageParts != null) {
                for (var part: messageParts) {
                    if (part.length() > 0) {
                        short partLevel = previousLevel;
                        if (part.length() > 22) { // The timestamp part is of length 22 "[yyyy-mm-dd HH:MM:SS] "
                            var withoutTimestamp = part.substring(22);
                            if (withoutTimestamp.startsWith("DEBUG ")) {
                                partLevel = LogMessage.DEBUG;
                            } else if (withoutTimestamp.startsWith("INFO ")) {
                                partLevel = LogMessage.INFO;
                            } else if (withoutTimestamp.startsWith("WARN ")) {
                                partLevel = LogMessage.WARNING;
                            } else if (withoutTimestamp.startsWith("ERROR ")) {
                                partLevel = LogMessage.ERROR;
                            }
                        }
                        if (partLevel != LogMessage.DEBUG) {
                            tempMessages.add(new LogMessage(partLevel, part));
                        }
                        previousLevel = partLevel;
                    }
                }
            }
        }
        if (!tempMessages.isEmpty()) {
            logMessages = tempMessages;
        }
    }

    public static class LogMessage {

        static final short DEBUG = 0;
        static final short INFO = 1;
        static final short WARNING = 2;
        static final short ERROR = 3;

        private final short level;
        private final String text;

        public LogMessage(short level, String text) {
            this.level = level;
            this.text = text;
        }

        public short getLevel() {
            return level;
        }

        public String getText() {
            return text;
        }
    }
}
