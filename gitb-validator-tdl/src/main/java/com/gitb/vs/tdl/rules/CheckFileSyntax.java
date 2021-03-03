package com.gitb.vs.tdl.rules;

import com.gitb.tdl.Scriptlet;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CheckFileSyntax extends AbstractCheck {

    private static final Logger LOG = LoggerFactory.getLogger(CheckFileSyntax.class);

    @Override
    public void doCheck(Context context, ValidationReport report) {
        ErrorHandler handler = new ErrorHandler(report, context);
        checkFileSyntax(context, handler, flatten(context.getTestSuitePaths().values()), ResourceType.TEST_SUITE);
        checkFileSyntax(context, handler, flatten(context.getTestCasePaths().values()), ResourceType.TEST_CASE);
        checkFileSyntax(context, handler, context.getScriptletPaths().keySet(), ResourceType.SCRIPTLET);
    }

    private List<Path> flatten(Collection<List<Path>> pathSets) {
        List<Path> result = new ArrayList<>();
        pathSets.forEach(result::addAll);
        return result;
    }

    private void checkFileSyntax(Context context, ErrorHandler handler, Collection<Path> paths, ResourceType resourceType) {
        for (Path path : paths) {
            try (InputStream is = Files.newInputStream(path)) {
                handler.setCurrentPath(path, resourceType);
                if (resourceType == ResourceType.TEST_SUITE) {
                    Utils.unmarshal(is, TestSuite.class, context.getJAXBContext(), context.getTDLSchema(), handler).getValue();
                } else if (resourceType == ResourceType.TEST_CASE) {
                    Utils.unmarshal(is, TestCase.class, context.getJAXBContext(), context.getTDLSchema(), handler).getValue();
                } else { // Scriptlets
                    Utils.unmarshal(is, Scriptlet.class, context.getJAXBContext(), context.getTDLSchema(), handler).getValue();
                }
            } catch (JAXBException e) {
                handleParseException(e);
            } catch (IOException e) {
                throw new IllegalStateException("Error while looking up test suite resource", e);
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

        private final ValidationReport report;
        private final Context context;
        private Path currentPath;
        private ErrorCode errorCodeToUse;

        ErrorHandler(ValidationReport report, Context context) {
            this.report = report;
            this.context = context;
        }

        ValidationReport getReport() {
            return report;
        }

        void setCurrentPath(Path currentPath, ResourceType resourceType) {
            this.currentPath = currentPath;
            switch (resourceType) {
                case TEST_SUITE: errorCodeToUse = ErrorCode.INVALID_TEST_SUITE_SYNTAX; break;
                case TEST_CASE: errorCodeToUse = ErrorCode.INVALID_TEST_CASE_SYNTAX; break;
                case SCRIPTLET: errorCodeToUse = ErrorCode.INVALID_SCRIPTLET_SYNTAX; break;
                default: throw new IllegalStateException("Unknown resource type ["+resourceType+"]");
            }
        }

        @Override
        public boolean handleEvent(ValidationEvent event) {
            report.addItem(errorCodeToUse, context.getTestSuiteRootPath().relativize(currentPath).toString(), event.getMessage());
            return event.getSeverity() == ValidationEvent.WARNING;
        }
    }

    enum ResourceType {
        TEST_SUITE, TEST_CASE, SCRIPTLET
    }
}
