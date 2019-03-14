package com.gitb.vs.tdl.rules;

import com.gitb.tdl.TestCase;
import com.gitb.tdl.TestSuite;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;
import com.gitb.vs.tdl.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class CheckFileSyntax extends AbstractCheck {

    private static final Logger LOG = LoggerFactory.getLogger(CheckFileSyntax.class);

    @Override
    public void doCheck(Context context, ValidationReport report) {
        ErrorHandler handler = new ErrorHandler(report, context);
        checkFileSyntax(context, handler, context.getTestSuitePaths(), true);
        checkFileSyntax(context, handler, context.getTestCasePaths(), false);
    }

    private void checkFileSyntax(Context context, ErrorHandler handler, Map<String, List<Path>> pathSets, boolean isTestSuite) {
        for (List<Path> paths : pathSets.values()) {
            for (Path path : paths) {
                try (InputStream is = Files.newInputStream(path)) {
                    handler.setCurrentPath(path, isTestSuite);
                    if (isTestSuite) {
                        Utils.unmarshal(is, TestSuite.class, context.getJAXBContext(), context.getTDLSchema(), handler).getValue();
                    } else {
                        Utils.unmarshal(is, TestCase.class, context.getJAXBContext(), context.getTDLSchema(), handler).getValue();
                    }
                } catch (JAXBException e) {
                    handleParseException(e);
                } catch (IOException e) {
                    throw new IllegalStateException("Error while looking up test suite resource", e);
                }
            }
        }
    }

    private void handleParseException(JAXBException e) {
        if (e instanceof UnmarshalException
                && e.getLinkedException() != null
                && e.getLinkedException() instanceof SAXParseException) {
            // XSD parse failure - ignore.
        } else {
            LOG.warn("Unexpected error while checking file syntax", e);
        }
    }

    static class ErrorHandler implements ValidationEventHandler {

        private ValidationReport report;
        private Context context;
        private Path currentPath;
        private boolean isTestSuite;

        ErrorHandler(ValidationReport report, Context context) {
            this.report = report;
            this.context = context;
        }

        ValidationReport getReport() {
            return report;
        }

        void setCurrentPath(Path currentPath, boolean isTestSuite) {
            this.currentPath = currentPath;
            this.isTestSuite = isTestSuite;
        }

        @Override
        public boolean handleEvent(ValidationEvent event) {
            report.addItem(isTestSuite?ErrorCode.INVALID_TEST_SUITE_SYNTAX:ErrorCode.INVALID_TEST_CASE_SYNTAX, context.getTestSuiteRootPath().relativize(currentPath).toString(), event.getMessage());
            return event.getSeverity() == ValidationEvent.WARNING;
        }
    }

}
