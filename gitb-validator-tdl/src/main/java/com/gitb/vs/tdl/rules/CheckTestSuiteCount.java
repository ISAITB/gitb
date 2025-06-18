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

import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class CheckTestSuiteCount extends AbstractCheck {

    @Override
    public void doCheck(Context context, ValidationReport report) {
        int testSuiteCount = context.getTestSuiteCount();
        if (testSuiteCount == 0) {
            report.addItem(ErrorCode.NO_TEST_SUITE_FOUND, "");
        } else if (testSuiteCount > 1) {
            Set<Path> paths = new HashSet<>(testSuiteCount);
            context.getTestSuitePaths().values().forEach(paths::addAll);
            report.addItem(ErrorCode.MULTIPLE_TEST_SUITES_FOUND, getResourceLocation(context, paths.toArray(new Path[] {})), String.valueOf(testSuiteCount));
        }
    }

}
