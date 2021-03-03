package com.gitb.vs.tdl.rules.testcase.expression;

import com.gitb.tdl.Scriptlet;
import com.gitb.tdl.TestCase;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;

import java.util.Map;

public interface VariableResolverProvider {

    Map<String, Boolean> getScope();
    void addReportItem(ErrorCode errorCode, String... parameters);
    Context getContext();
    TestCase getCurrentTestCase();
    Scriptlet getCurrentScriptlet();
    Object getCurrentStep();

}
