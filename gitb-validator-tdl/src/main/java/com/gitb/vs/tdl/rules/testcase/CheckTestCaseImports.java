package com.gitb.vs.tdl.rules.testcase;

import com.gitb.tdl.TestArtifact;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;
import com.gitb.vs.tdl.util.Utils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public class CheckTestCaseImports extends AbstractTestCaseObserver {

    private Set<Pair<String, String>> externalResourceReferences;

    @Override
    public void initialise(Context context, ValidationReport report) {
        super.initialise(context, report);
        externalResourceReferences = new LinkedHashSet<>();
    }

    @Override
    public void handleImport(Object artifactObj) {
        super.handleImport(artifactObj);
        if (artifactObj instanceof TestArtifact artifact) {
            if (artifact.getFrom() == null || artifact.getFrom().equals(context.getTestSuite().getId())) {
                // Resource from the test suite.
                if (!Utils.isVariableExpression(artifact.getValue())) {
                    // Check only if not variable expression (case of a variable expression is handled in expression-specific rule).
                    Path resolvedPath = context.resolveTestSuiteResourceIfValid(artifact.getValue());
                    if (resolvedPath == null) {
                        addReportItem(ErrorCode.INVALID_TEST_CASE_IMPORT, currentTestCase.getId(), artifact.getValue());
                    } else {
                        context.getReferencedResourcePaths().add(resolvedPath.toAbsolutePath());
                    }
                }
            } else {
                // External resource.
                externalResourceReferences.add(new ImmutablePair<>(artifact.getFrom(), artifact.getValue()));
            }
        }
    }

    @Override
    public void finalise() {
        super.finalise();
        for (var ref: externalResourceReferences) {
            if (Utils.isVariableExpression(ref.getRight())) {
                addReportItem(ErrorCode.EXTERNAL_DYNAMIC_IMPORT_USED, ref.getLeft(), ref.getRight());
            } else {
                addReportItem(ErrorCode.EXTERNAL_STATIC_IMPORT_USED, ref.getRight(), ref.getLeft());
            }
        }
    }

}
