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
import java.util.List;
import java.util.Map;

public class CheckIDUniqueness extends AbstractCheck {

    @Override
    public void doCheck(Context context, ValidationReport report) {
        for (Map.Entry<String, List<Path>> entry: context.getTestCasePaths().entrySet()) {
            if (!"".equals(entry.getKey())) {
                if (entry.getValue().size() > 1) {
                    report.addItem(ErrorCode.DUPLICATE_TEST_CASE_ID, getResourceLocation(context, entry.getValue().toArray(new Path[] {})), entry.getKey());
                }
            }
        }
    }

}
