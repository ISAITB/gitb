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
