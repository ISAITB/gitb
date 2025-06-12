package com.gitb.engine.testcase;

import com.gitb.core.ActorConfiguration;
import com.gitb.tbs.SUTConfiguration;
import com.gitb.tdl.TestCase;

import java.util.ArrayList;
import java.util.List;

public class StaticTestCaseContext extends TestCaseContext {

    public StaticTestCaseContext(TestCase testCase) {
        super(testCase, testCase.getId(), "");
    }

    @Override
    protected List<SUTConfiguration> configureDynamicActorProperties(TestCase testCase, List<ActorConfiguration> configurations) {
        /*
         * We skip contacting remote services to define dynamic actor configurations. Instead, we simply return an empty list.
         */
        return new ArrayList<>();
    }
}
