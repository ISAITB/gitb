package com.gitb.vs.tdl.rules.testcase;

import com.gitb.tdl.InstructionOrRequest;
import com.gitb.tdl.TestArtifact;
import com.gitb.tdl.UserInteraction;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;
import com.gitb.vs.tdl.rules.TestCaseSection;
import com.gitb.vs.tdl.util.Utils;

import java.nio.charset.Charset;
import java.util.Set;

public class CheckEncodings extends AbstractTestCaseObserver {

    private Set<String> availableCharsets;

    @Override
    public void initialise(Context context, ValidationReport report) {
        super.initialise(context, report);
        availableCharsets = Charset.availableCharsets().keySet();
    }

    @Override
    public void handleImport(Object artifactObj) {
        super.handleImport(artifactObj);
        if (artifactObj instanceof TestArtifact) {
            checkEncoding(((TestArtifact)artifactObj).getEncoding());
        }
    }

    @Override
    public void handleStep(Object step) {
        super.handleStep(step);
        if (step instanceof UserInteraction) {
            if (((UserInteraction)step).getInstructOrRequest() != null) {
                for (InstructionOrRequest ir: ((UserInteraction)step).getInstructOrRequest()) {
                    checkEncoding(ir.getEncoding());
                }
            }
        }

    }

    private void checkEncoding(String encoding) {
        if (encoding != null && !availableCharsets.contains(encoding)) {
            String stepToReport;
            if (section == TestCaseSection.IMPORTS) {
                stepToReport = "import";
            } else if (currentStep != null) {
                stepToReport = Utils.getStepName(currentStep);
            } else {
                stepToReport = "";
            }
            addReportItem(ErrorCode.INVALID_ENCODING, currentTestCase.getId(), encoding, stepToReport);
        }
    }
}
