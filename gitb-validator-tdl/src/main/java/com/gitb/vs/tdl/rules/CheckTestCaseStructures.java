package com.gitb.vs.tdl.rules;

import com.gitb.core.Documentation;
import com.gitb.core.TestRole;
import com.gitb.tdl.*;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ValidationReport;

import java.util.List;

public class CheckTestCaseStructures extends AbstractCheck {

    private final List<TestCaseObserver> observers;

    public CheckTestCaseStructures() {
        observers = RuleFactory.getInstance().getTestCaseObservers();
    }

    @Override
    public void doCheck(Context context, ValidationReport report) {
        initialise(context, report);
        // Check test cases.
        for (TestCase testCase: context.getTestCases().values()) {
            if (context.isValidTestCaseId(testCase.getId())) {
                var validator = new TestCaseContentValidator(testCase, observers);
                validator.process();
            }
        }
        // Check standalone scriptlets.
        for (var scriptletEntry: context.getScriptletPaths().entrySet()) {
            if (context.isValidScriptletId(scriptletEntry.getValue().getId())) {
                var validator = new TestCaseContentValidator(new ScriptletAsTestCase(scriptletEntry.getValue(), scriptletEntry.getKey()), observers);
                validator.process();
            }
        }
        finalise();
    }

    public short getOrder() {
        // Ensure this check runs later.
        return 1;
    }

    void initialise(Context context, ValidationReport report) {
        for (TestCaseObserver observer: observers) {
            observer.initialise(context, report);
        }
    }

    void finalise() {
        for (TestCaseObserver observer: observers) {
            observer.finalise();
        }
    }

    private static class TestCaseContentValidator {

        private final TestCase testCase;
        private final List<TestCaseObserver> observers;
        private final boolean isScriptlet;

        TestCaseContentValidator(TestCase testCase, List<TestCaseObserver> observers) {
            this.testCase = testCase;
            this.observers = observers;
            isScriptlet = testCase instanceof ScriptletAsTestCase;
        }

        void process() {
            initialiseTestCase(testCase);
            sectionChanged(TestCaseSection.METADATA);
            if (testCase.getMetadata() != null) {
                handleDocumentation(testCase.getMetadata().getDocumentation());
            }
            sectionChanged(TestCaseSection.START);
            sectionChanged(TestCaseSection.IMPORTS);
            if (testCase.getImports() != null && testCase.getImports().getArtifactOrModule() != null) {
                for (Object artifactObj : testCase.getImports().getArtifactOrModule()) {
                    handleImport(artifactObj);
                }
            }
            if (isScriptlet) {
                sectionChanged(TestCaseSection.SCRIPTLET_PARAMETERS);
                var scriptlet = ((ScriptletAsTestCase) testCase).getWrappedScriptlet();
                if (scriptlet.getParams() != null && scriptlet.getParams().getVar() != null) {
                    for (Variable var: scriptlet.getParams().getVar()) {
                        handleVariable(var);
                    }
                }
            }
            sectionChanged(TestCaseSection.ACTORS);
            if (testCase.getActors() != null && testCase.getActors().getActor() != null) {
                for (TestRole testRole: testCase.getActors().getActor()) {
                    handleActor(testRole);
                }
            }
            sectionChanged(TestCaseSection.VARIABLES);
            if (testCase.getVariables() != null && testCase.getVariables().getVar() != null) {
                for (Variable var: testCase.getVariables().getVar()) {
                    handleVariable(var);
                }
            }
            /*
             We run the preliminary step here as we may have references to actors and variables. This
             matches how a test case is actually executed.
              */
            if (!isScriptlet) {
                sectionChanged(TestCaseSection.PRELIMINARY);
                handleStep(testCase.getPreliminary());
            }
            sectionChanged(TestCaseSection.STEPS);
            checkSteps(testCase.getSteps());
            if (isScriptlet) {
                sectionChanged(TestCaseSection.SCRIPTLET_OUTPUT);
                var outputs = ((ScriptletAsTestCase) testCase).getWrappedScriptlet().getOutput();
                if (outputs != null) {
                    for (var binding: outputs) {
                        handleOutput(binding);
                    }
                }
            } else {
                sectionChanged(TestCaseSection.OUTPUT);
                handleTestOutput(testCase.getOutput());
                sectionChanged(TestCaseSection.SCRIPTLETS);
                if (testCase.getScriptlets() != null && testCase.getScriptlets().getScriptlet() != null) {
                    for (Scriptlet scriptlet: testCase.getScriptlets().getScriptlet()) {
                        initialiseScriptlet(scriptlet);
                        sectionChanged(TestCaseSection.SCRIPTLET_START);
                        sectionChanged(TestCaseSection.SCRIPTLET_IMPORTS);
                        if (scriptlet.getImports() != null && scriptlet.getImports().getArtifactOrModule() != null) {
                            for (Object artifactObj : scriptlet.getImports().getArtifactOrModule()) {
                                handleImport(artifactObj);
                            }
                        }
                        sectionChanged(TestCaseSection.SCRIPTLET_PARAMETERS);
                        if (scriptlet.getParams() != null && scriptlet.getParams().getVar() != null) {
                            for (Variable var: scriptlet.getParams().getVar()) {
                                handleVariable(var);
                            }
                        }
                        sectionChanged(TestCaseSection.SCRIPTLET_VARIABLES);
                        if (scriptlet.getVariables() != null && scriptlet.getVariables().getVar() != null) {
                            for (Variable var: scriptlet.getVariables().getVar()) {
                                handleVariable(var);
                            }
                        }
                        sectionChanged(TestCaseSection.SCRIPTLET_STEPS);
                        checkSteps(scriptlet.getSteps());
                        sectionChanged(TestCaseSection.SCRIPTLET_OUTPUT);
                        if (scriptlet.getOutput() != null) {
                            for (Binding binding: scriptlet.getOutput()) {
                                handleOutput(binding);
                            }
                        }
                        sectionChanged(TestCaseSection.SCRIPTLET_END);
                        finaliseScriptlet();
                    }
                }
            }
            sectionChanged(TestCaseSection.END);
            finaliseTestCase();
        }

        private void checkSteps(Sequence steps) {
            if (steps != null && steps.getSteps() != null) {
                for (Object stepObj: steps.getSteps()) {
                    handleStep(stepObj);
                    if (stepObj instanceof IfStep) {
                        checkSteps(((IfStep)stepObj).getThen());
                        checkSteps(((IfStep)stepObj).getElse());
                    } else if (stepObj instanceof WhileStep) {
                        checkSteps(((WhileStep)stepObj).getDo());
                    } else if (stepObj instanceof RepeatUntilStep) {
                        checkSteps(((RepeatUntilStep)stepObj).getDo());
                    } else if (stepObj instanceof ForEachStep) {
                        checkSteps(((ForEachStep)stepObj).getDo());
                    } else if (stepObj instanceof FlowStep) {
                        if (((FlowStep)stepObj).getThread() != null) {
                            for (Sequence flowSequence: ((FlowStep)stepObj).getThread()) {
                                checkSteps(flowSequence);
                            }
                        }
                    } else if (stepObj instanceof Sequence) {
                        checkSteps((Sequence)stepObj);
                    }
                }
            }
        }

        private void initialiseTestCase(TestCase currentTestCase) {
            for (TestCaseObserver observer: observers) {
                observer.initialiseTestCase(currentTestCase);
            }
        }

        private void initialiseScriptlet(Scriptlet scriptlet) {
            for (TestCaseObserver observer: observers) {
                observer.initialiseScriptlet(scriptlet);
            }
        }

        private void sectionChanged(TestCaseSection section) {
            for (TestCaseObserver observer: observers) {
                observer.sectionChanged(section);
            }
        }

        private void handleStep(Object step) {
            for (TestCaseObserver observer: observers) {
                observer.handleStep(step);
            }
            if (step instanceof TestStep) {
                handleDocumentation(((TestStep)step).getDocumentation());
            }
        }

        private void handleTestOutput(Output output) {
            for (TestCaseObserver observer: observers) {
                observer.handleTestOutput(output);
            }
        }

        private void handleActor(TestRole testRole) {
            for (TestCaseObserver observer: observers) {
                observer.handleActor(testRole);
            }
        }

        private void handleImport(Object artifactObj) {
            for (TestCaseObserver observer: observers) {
                observer.handleImport(artifactObj);
            }
        }

        private void handleDocumentation(Documentation documentation) {
            for (TestCaseObserver observer: observers) {
                observer.handleDocumentation(documentation);
            }
        }

        private void handleVariable(Variable var) {
            for (TestCaseObserver observer: observers) {
                observer.handleVariable(var);
            }
        }

        private void handleOutput(Binding binding) {
            for (TestCaseObserver observer: observers) {
                observer.handleOutput(binding);
            }
        }

        private void finaliseTestCase() {
            for (TestCaseObserver observer: observers) {
                observer.finaliseTestCase();
            }
        }

        private void finaliseScriptlet() {
            for (TestCaseObserver observer: observers) {
                observer.finaliseScriptlet();
            }
        }

    }

}
