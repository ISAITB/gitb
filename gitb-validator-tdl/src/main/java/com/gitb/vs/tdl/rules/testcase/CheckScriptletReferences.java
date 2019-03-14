package com.gitb.vs.tdl.rules.testcase;

import com.gitb.tdl.CallStep;
import com.gitb.tdl.Scriptlet;
import com.gitb.tdl.TestCase;
import com.gitb.vs.tdl.ErrorCode;

import java.util.HashSet;
import java.util.Set;

public class CheckScriptletReferences extends AbstractTestCaseObserver {

    private Set<String> definedScriptlets;
    private Set<String> calledScriptlets;

    @Override
    public void initialiseTestCase(TestCase currentTestCase) {
        super.initialiseTestCase(currentTestCase);
        definedScriptlets = new HashSet<>();
        calledScriptlets = new HashSet<>();
    }

    @Override
    public void initialiseScriptlet(Scriptlet scriptlet) {
        super.initialiseScriptlet(scriptlet);
        if (definedScriptlets.contains(scriptlet.getId())) {
            addReportItem(ErrorCode.DUPLICATE_SCRIPTLET_ID, currentTestCase.getId(), scriptlet.getId());
        } else {
            definedScriptlets.add(scriptlet.getId());
        }
    }

    @Override
    public void handleStep(Object step) {
        super.handleStep(step);
        if (step instanceof CallStep) {
            calledScriptlets.add(((CallStep)step).getPath());
        }
    }

    @Override
    public void finaliseTestCase() {
        for (String id: definedScriptlets) {
            if (!calledScriptlets.contains(id)) {
                addReportItem(ErrorCode.UNUSED_SCRIPTLET, currentTestCase.getId(), id);
            }
        }
        for (String id: calledScriptlets) {
            if (!definedScriptlets.contains(id)) {
                addReportItem(ErrorCode.INVALID_SCRIPTLET_REFERENCE, currentTestCase.getId(), id);
            }
        }
        super.finaliseTestCase();
    }
}
