/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package com.gitb.vs.tdl.rules.testcase;

import com.gitb.core.Documentation;
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
    public void handleDocumentation(Documentation documentation) {
        super.handleDocumentation(documentation);
        if (documentation != null) {
            checkEncoding(documentation.getEncoding());
        }
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
            } else if (section == TestCaseSection.METADATA) {
                stepToReport = "documentation";
            } else if (currentStep != null) {
                stepToReport = Utils.stepNameWithScriptlet(currentStep, currentScriptlet);
            } else {
                stepToReport = "";
            }
            addReportItem(ErrorCode.INVALID_ENCODING, currentTestCase.getId(), encoding, stepToReport);
        }
    }
}
