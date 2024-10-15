package com.gitb.vs.tdl.rules;

import com.gitb.core.Documentation;
import com.gitb.core.TestRole;
import com.gitb.tdl.*;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ValidationReport;

public interface TestCaseObserver {

    void initialise(Context context, ValidationReport report);

    void initialiseTestCase(TestCase currentTestCase);

    void initialiseScriptlet(Scriptlet scriptlet);

    void sectionChanged(TestCaseSection section);

    void handleStep(Object step);

    void handleActor(TestRole testRole);

    void handleTestOutput(Output output);

    void handleImport(Object artifactObj);

    void handleDocumentation(Documentation documentation);

    void handleInputParameter(InputParameter param);

    void handleVariable(Variable var);

    void handleOutput(Binding binding);

    void finaliseScriptlet();

    void finaliseTestCase();

    void finalise();

}
