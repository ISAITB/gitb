package com.gitb.vs.tdl.rules.testcase;

import com.gitb.core.Documentation;
import com.gitb.core.TestRole;
import com.gitb.tdl.*;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;
import com.gitb.vs.tdl.rules.ScriptletAsTestCase;
import com.gitb.vs.tdl.rules.TestCaseObserver;
import com.gitb.vs.tdl.rules.TestCaseSection;
import com.gitb.vs.tdl.util.Utils;

public class AbstractTestCaseObserver implements TestCaseObserver {

    protected TestCaseSection section;
    protected Object currentStep;
    protected ValidationReport report;
    protected Context context;
    protected TestCase currentTestCase;
    protected Scriptlet currentScriptlet;
    protected boolean testCaseIsWrappedScriptlet;

    @Override
    public void initialise(Context context, ValidationReport report) {
        this.context = context;
        this.report = report;
    }

    @Override
    public void initialiseTestCase(TestCase currentTestCase) {
        this.currentTestCase = currentTestCase;
        testCaseIsWrappedScriptlet = (currentTestCase instanceof ScriptletAsTestCase);
    }

    @Override
    public void initialiseScriptlet(Scriptlet scriptlet) {
        this.currentScriptlet = scriptlet;
    }

    @Override
    public void sectionChanged(TestCaseSection section) {
        this.section = section;
    }

    @Override
    public void handleStep(Object step) {
        currentStep = step;
    }

    @Override
    public void handleActor(TestRole testRole) {
        // Do nothing by default.
    }

    @Override
    public void handleImport(Object artifactObj) {
        currentStep = artifactObj;
    }

    @Override
    public void handleDocumentation(Documentation documentation) {
        // Do nothing by default.
    }

    @Override
    public void handleVariable(Variable var) {
        currentStep = var;
    }

    @Override
    public void handleOutput(Binding binding) {
        // Do nothing by default.
    }

    @Override
    public void handleTestOutput(Output output) {
        currentStep = output;
    }

    @Override
    public void finaliseScriptlet() {
        this.currentScriptlet = null;
    }

    @Override
    public void finaliseTestCase() {
        this.currentTestCase = null;
    }

    @Override
    public void finalise() {
    }

    public String getLocation() {
        if (currentTestCase != null) {
            if (testCaseIsWrappedScriptlet) {
                return Utils.getScriptletLocation(((ScriptletAsTestCase)currentTestCase).getScriptletPath(), context);
            } else {
                return Utils.getTestCaseLocation(currentTestCase.getId(), context);
            }
        }
        return "";
    }

    public void addReportItem(ErrorCode error, String... parameters) {
        if (error.isPrefixWithResourceType()) {
            String[] newParameters = new String[(parameters == null)?1:parameters.length+1];
            newParameters[0] = testCaseIsWrappedScriptlet?"Scriptlet":"Test case";
            if (parameters != null) {
                System.arraycopy(parameters, 0, newParameters, 1, parameters.length);
            }
            report.addItem(error, getLocation(), newParameters);
        } else {
            report.addItem(error, getLocation(), parameters);
        }
    }
}
