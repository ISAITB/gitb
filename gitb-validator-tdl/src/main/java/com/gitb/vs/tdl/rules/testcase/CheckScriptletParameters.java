package com.gitb.vs.tdl.rules.testcase;

import com.gitb.tdl.InputParameter;
import com.gitb.vs.tdl.ErrorCode;

public class CheckScriptletParameters extends AbstractTestCaseObserver {

    @Override
    public void handleInputParameter(InputParameter param) {
        super.handleInputParameter(param);
        if (param.isDefaultEmpty() && !param.getValue().isEmpty()) {
            addReportItem(ErrorCode.SCRIPTLET_EMPTY_AND_DEFAULT_INPUTS, currentTestCase.getId(), param.getName());
        }
    }

}
