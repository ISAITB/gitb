package com.gitb.vs.tdl.rules;

import com.gitb.core.TestRole;
import com.gitb.tdl.Binding;
import com.gitb.tdl.Scriptlet;
import com.gitb.tdl.TestCase;
import com.gitb.tdl.Variable;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ValidationReport;

public interface TestCaseObserver {

    void initialise(Context context, ValidationReport report);

    void initialiseTestCase(TestCase currentTestCase);

    void initialiseScriptlet(Scriptlet scriptlet);

    void sectionChanged(TestCaseSection section);

    void handleStep(Object step);

    void handleActor(TestRole testRole);

    void handleImport(Object artifactObj);

    void handleVariable(Variable var);

    void handleOutput(Binding binding);

    void finaliseScriptlet();

    void finaliseTestCase();

    void finalise();

}
