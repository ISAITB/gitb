package com.gitb.vs.tdl.rules.testcase;

import com.gitb.tdl.TestArtifact;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;
import com.gitb.vs.tdl.util.Utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class CheckTestCaseImports extends AbstractTestCaseObserver {

    private String testSuiteName;
    Set<Path> validImports = new HashSet<>();

    @Override
    public void initialise(Context context, ValidationReport report) {
        super.initialise(context, report);
        if (context.getTestSuite() != null) {
            testSuiteName = context.getTestSuite().getMetadata().getName();
        }
    }

    @Override
    public void handleImport(Object artifactObj) {
        if (artifactObj instanceof TestArtifact) {
            TestArtifact artifact = (TestArtifact)artifactObj;
            Path resolvedPath = resolveImport(context.getTestSuiteRootPath(), testSuiteName, artifact.getValue());
            if (resolvedPath == null) {
                addReportItem(ErrorCode.INVALID_TEST_CASE_IMPORT, currentTestCase.getId(), artifact.getValue());
            } else {
                validImports.add(resolvedPath.toAbsolutePath());
            }
        }
    }

    @Override
    public void finalise() {
        for (Path resourcePath: context.getResourcePaths()) {
            if (!validImports.contains(resourcePath.toAbsolutePath())) {
                String relativePath = context.getTestSuiteRootPath().relativize(resourcePath).toString();
                report.addItem(ErrorCode.UNUSED_RESOURCE, relativePath, Utils.standardisePath(relativePath));
            }
        }
        super.finalise();
    }

    private Path resolveImport(Path testSuiteRootPath, String testSuiteName, String importPath) {
        Path path = testSuiteRootPath.resolve(importPath);
        if (Files.exists(testSuiteRootPath.resolve(importPath))) {
            return path;
        } else {
            if (testSuiteName != null && importPath.startsWith(testSuiteName) && importPath.length() > testSuiteName.length()) {
                String pathWithoutTestSuiteName = importPath.substring(testSuiteName.length() + 1);
                path = testSuiteRootPath.resolve(pathWithoutTestSuiteName);
                return path;
            }
        }
        return null;
    }
}
