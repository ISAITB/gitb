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

package com.gitb.vs.tdl.rules;

import com.gitb.tdl.TestSuite;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;

public class CheckTestSuiteDocumentation extends AbstractCheck {

    @Override
    public void doCheck(Context context, ValidationReport report) {
        TestSuite testSuite = context.getTestSuite();
        if (testSuite != null && testSuite.getMetadata() != null && testSuite.getMetadata().getDocumentation() != null) {
            context.validateDocumentation(testSuite.getMetadata().getDocumentation(),
                    (String path, String value) -> report.addItem(ErrorCode.TEST_SUITE_DOCUMENTATION_BOTH_AS_VALUE_AND_IMPORT, getTestSuiteLocation(context)),
                    (String path) -> report.addItem(ErrorCode.TEST_SUITE_DOCUMENTATION_REFERENCE_INVALID, getTestSuiteLocation(context), path)
            );
        }
    }

}
