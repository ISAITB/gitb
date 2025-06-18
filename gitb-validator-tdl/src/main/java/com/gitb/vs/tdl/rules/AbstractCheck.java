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
import com.gitb.vs.tdl.ValidationReport;
import com.gitb.vs.tdl.util.Utils;

import java.nio.file.Path;

public abstract class AbstractCheck {

    public static final short DEFAULT_ORDER = 0;

    public abstract void doCheck(Context context, ValidationReport report);

    public short getOrder() {
        return DEFAULT_ORDER;
    }

    public String getResourceLocation(Context context, Path... resources) {
        StringBuilder str = new StringBuilder();
        for (Path resource: resources) {
            str.append(context.getTestSuiteRootPath().relativize(resource)).append(", ");
        }
        if (!str.isEmpty()) {
            str.delete(str.length() - 2, str.length());
        }
        return str.toString();
    }

    public String getTestSuiteLocation(Context context) {
        if (context.getTestSuitePaths().size() == 1) {
            return getResourceLocation(context, context.getTestSuitePaths().values().iterator().next().getFirst());
        }
        return "";
    }

    public String getTestCaseLocation(String testCaseId, Context context) {
        return Utils.getTestCaseLocation(testCaseId, context);
    }

}
