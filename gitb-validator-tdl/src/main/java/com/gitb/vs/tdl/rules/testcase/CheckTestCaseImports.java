package com.gitb.vs.tdl.rules.testcase;

import com.gitb.tdl.TestArtifact;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;
import com.gitb.vs.tdl.util.Utils;

import java.nio.file.Path;

public class CheckTestCaseImports extends AbstractTestCaseObserver {

    @Override
    public void initialise(Context context, ValidationReport report) {
        super.initialise(context, report);
    }

    @Override
    public void handleImport(Object artifactObj) {
        super.handleImport(artifactObj);
        if (artifactObj instanceof TestArtifact) {
            TestArtifact artifact = (TestArtifact)artifactObj;
            if (!Utils.isVariableExpression(artifact.getValue())) {
                // Check only if not variable expression (case of a variable expression is handled in expression-specific rule).
                Path resolvedPath = context.resolveTestSuiteResourceIfValid(artifact.getValue());
                if (resolvedPath == null) {
                    addReportItem(ErrorCode.INVALID_TEST_CASE_IMPORT, currentTestCase.getId(), artifact.getValue());
                } else {
                    context.getReferencedResourcePaths().add(resolvedPath.toAbsolutePath());
                }
            }
        }
    }

    @Override
    public void finalise() {
        for (Path resourcePath: context.getResourcePaths()) {
            if (!context.getReferencedResourcePaths().contains(resourcePath.toAbsolutePath())) {
                String relativePath = context.getTestSuiteRootPath().relativize(resourcePath).toString();
                report.addItem(ErrorCode.UNUSED_RESOURCE, relativePath, Utils.standardisePath(relativePath));
            }
        }
        super.finalise();
    }

}
