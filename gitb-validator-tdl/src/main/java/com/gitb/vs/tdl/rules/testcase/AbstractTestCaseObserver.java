package com.gitb.vs.tdl.rules.testcase;

import com.gitb.core.TestRole;
import com.gitb.tdl.Binding;
import com.gitb.tdl.Scriptlet;
import com.gitb.tdl.TestCase;
import com.gitb.tdl.Variable;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;
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

    @Override
    public void initialise(Context context, ValidationReport report) {
        this.context = context;
        this.report = report;
    }

    @Override
    public void initialiseTestCase(TestCase currentTestCase) {
        this.currentTestCase = currentTestCase;
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
        // Do nothing by default.
    }

    @Override
    public void handleVariable(Variable var) {
        // Do nothing by default.
    }

    @Override
    public void handleOutput(Binding binding) {
        // Do nothing by default.
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
            return Utils.getTestCaseLocation(currentTestCase.getId(), context);
        }
        return "";
    }

    public void addReportItem(ErrorCode error, String... parameters) {
        report.addItem(error, getLocation(), parameters);
    }
}
