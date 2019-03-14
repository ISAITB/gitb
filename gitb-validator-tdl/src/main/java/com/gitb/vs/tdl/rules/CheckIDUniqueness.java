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
        for (Map.Entry<String, List<Path>> entry: context.getTestSuitePaths().entrySet()) {
            if (!"".equals(entry.getKey())) {
                if (entry.getValue().size() > 1) {
                    report.addItem(ErrorCode.DUPLICATE_TEST_SUITE_ID, getResourceLocation(context, entry.getValue().toArray(new Path[] {})), entry.getKey());
                }
            }
        }
        for (Map.Entry<String, List<Path>> entry: context.getTestCasePaths().entrySet()) {
            if (!"".equals(entry.getKey())) {
                if (entry.getValue().size() > 1) {
                    report.addItem(ErrorCode.DUPLICATE_TEST_CASE_ID, getResourceLocation(context, entry.getValue().toArray(new Path[] {})), entry.getKey());
                }
            }
        }
    }

}
