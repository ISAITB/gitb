package com.gitb.engine.utils;

import com.gitb.ModuleManager;
import com.gitb.core.Documentation;
import com.gitb.core.ErrorCode;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.repository.ITestCaseRepository;
import com.gitb.tdl.Instruction;
import com.gitb.tdl.Process;
import com.gitb.tdl.UserRequest;
import com.gitb.tdl.*;
import com.gitb.tpl.Sequence;
import com.gitb.tpl.TestCase;
import com.gitb.tpl.*;
import com.gitb.tpl.TestStep;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Stack;

public class TestCaseConverter {

    private static final Logger logger = LoggerFactory.getLogger(TestCaseConverter.class);
    private static final String TRUE  =  "[T]";
    private static final String FALSE =  "[F]";
    private static final String ITERATION_OPENING_TAG = "[";
    private static final String ITERATION_CLOSING_TAG = "]";

    private final Stack<String> testSuiteContexts = new Stack<>();
    private final Stack<String> scriptletCallStack = new Stack<>();
    private final com.gitb.tdl.TestCase testCase;
    private final ScriptletCache scriptletCache;

    public TestCaseConverter(com.gitb.tdl.TestCase testCase) {
        this(testCase, null);
    }

    public TestCaseConverter(com.gitb.tdl.TestCase testCase, ScriptletCache scriptletCache) {
        this.testCase = testCase;
        if (scriptletCache == null) {
            this.scriptletCache = new ScriptletCache();
        } else {
            this.scriptletCache = scriptletCache;
        }
    }

    public TestCase convertTestCase(String testCaseId) {
        TestCase presentation = new TestCase();
        presentation.setId(testCase.getId());
        presentation.setMetadata(testCase.getMetadata());
        presentation.setActors(testCase.getActors());
        if (testCase.getPreliminary() != null) {
            presentation.setPreliminary(convertPreliminary(testCase.getPreliminary()));
        }
        presentation.setSteps(convertSequence(testCaseId, "", testCase.getSteps()));
        return presentation;
    }

    private <T extends TestStep> void addToSequence(Sequence sequence, T step) {
        if (step != null && !step.isHidden()) {
            sequence.getSteps().add(step);
        }
    }

    private Sequence convertSequence(String testCaseId, String id, com.gitb.tdl.Sequence sequenceStep) {
        Sequence sequence = new Sequence();
        sequence.setId(id);
        int index = 1;

        String childIdPrefix = id.equals("") ? "" : (id + ".");

        for(int i=0; i<sequenceStep.getSteps().size(); i++) {
            Object step = sequenceStep.getSteps().get(i);
            if(step instanceof Verify) {
                String childId = childIdPrefix + index++;
                addToSequence(sequence, convertVerifyStep(testCaseId, childId, (Verify) step));
            } else if (step instanceof Process) {
                String childId = childIdPrefix + index++;
                addToSequence(sequence, convertProcessStep(testCaseId, childId, (Process) step));
            } else if (step instanceof com.gitb.tdl.MessagingStep) {
                String childId = childIdPrefix + index++;
                addToSequence(sequence, convertMessagingStep(testCaseId, childId, (com.gitb.tdl.MessagingStep) step));
            } else if (step instanceof IfStep) {
                String childId = childIdPrefix + index++;
                addToSequence(sequence, convertDecisionStep(testCaseId, childId, (IfStep) step));
            } else if (step instanceof RepeatUntilStep) {
                String childId = childIdPrefix + index++;
                addToSequence(sequence, convertRepUntilStep(testCaseId, childId, (RepeatUntilStep) step));
            } else if (step instanceof ForEachStep) {
                String childId = childIdPrefix + index++;
                addToSequence(sequence, convertForEachStep(testCaseId, childId, (ForEachStep) step));
            } else if (step instanceof WhileStep) {
                String childId = childIdPrefix + index++;
                addToSequence(sequence, convertWhileStep(testCaseId, childId, (WhileStep) step));
            } else if (step instanceof com.gitb.tdl.FlowStep) {
                String childId = childIdPrefix + index++;
                addToSequence(sequence, convertFlowStep(testCaseId, childId, (com.gitb.tdl.FlowStep) step));
            } else if (step instanceof CallStep) {
                String childId = childIdPrefix + index++;
                for (TestStep callStepChild: convertCallStep(testCaseId, childId, (CallStep)step).getSteps()) {
                    addToSequence(sequence, callStepChild);
                }
            } else if (step instanceof UserInteraction) {
                String childId = childIdPrefix + index++;
                addToSequence(sequence, convertUserInteraction(testCaseId, childId, (UserInteraction) step));
            } else if (step instanceof com.gitb.tdl.ExitStep) {
                String childId = childIdPrefix + index++;
                addToSequence(sequence, convertExitStep(testCaseId, childId, (com.gitb.tdl.ExitStep) step));
            } else if (step instanceof Group) {
                String childId = childIdPrefix + index++;
                addToSequence(sequence, convertGroupStep(testCaseId, childId, (com.gitb.tdl.Group) step));
            }
        }

        return sequence;
    }

    private Preliminary convertPreliminary(UserInteraction description) {
        Preliminary preliminary = new Preliminary();
        for(com.gitb.tdl.InstructionOrRequest interaction : description.getInstructOrRequest()){
            com.gitb.tpl.InstructionOrRequest ior = new com.gitb.tpl.InstructionOrRequest();
            ior.setDesc(interaction.getDesc());
            ior.setWith(interaction.getWith());

            if(interaction instanceof Instruction){
                preliminary.getInstructOrRequest().add(ior);
            } else if(interaction instanceof UserRequest){
                preliminary.getInstructOrRequest().add(ior);
            }
        }
        return preliminary;
    }

    private com.gitb.tpl.VerifyStep convertVerifyStep(String testCaseId, String id, Verify description) {
        com.gitb.tpl.VerifyStep verify = new com.gitb.tpl.VerifyStep();
        verify.setId(id);
        verify.setDesc(description.getDesc());
        verify.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        verify.setHidden(description.isHidden() != null && description.isHidden());
        return verify;
    }

    private com.gitb.tpl.ProcessStep convertProcessStep(String testCaseId, String id, Process description) {
        com.gitb.tpl.ProcessStep process = new com.gitb.tpl.ProcessStep();
        process.setId(id);
        process.setDesc(description.getDesc());
        process.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        // Process steps are by default hidden.
        process.setHidden(description.isHidden() == null || description.isHidden());
        return process;
    }

    private com.gitb.tpl.MessagingStep convertMessagingStep(String testCaseId, String id, com.gitb.tdl.MessagingStep description) {
        com.gitb.tpl.MessagingStep messaging = new com.gitb.tpl.MessagingStep();
        messaging.setId(id);
        messaging.setDesc(description.getDesc());
        messaging.setFrom(description.getFrom());
        messaging.setTo(description.getTo());
        messaging.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        messaging.setHidden(description.isHidden() != null && description.isHidden());
        messaging.setReply(description.isReply());
        return messaging;
    }

    private DecisionStep convertDecisionStep(String testCaseId, String id, IfStep description) {
        DecisionStep decision = new DecisionStep();
        decision.setId(id);
        decision.setTitle(description.getTitle());
        decision.setDesc(description.getDesc());
        decision.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        decision.setHidden(description.isHidden() != null && description.isHidden());
        decision.setCollapsed(description.isCollapsed());
        decision.setThen(convertSequence(testCaseId, id + TRUE , description.getThen()));
        if (description.getElse() != null) {
            decision.setElse(convertSequence(testCaseId, id + FALSE, description.getElse()));
        }
        return decision;
    }

    private LoopStep convertRepUntilStep(String testCaseId, String id, RepeatUntilStep description) {
        LoopStep loop = new LoopStep();
        loop.setId(id);
        loop.setTitle(description.getTitle());
        loop.setDesc(description.getDesc());
        loop.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        loop.setHidden(description.isHidden() != null && description.isHidden());
        loop.setCollapsed(description.isCollapsed());
        loop.getSteps().addAll(
                convertSequence(testCaseId, id+ITERATION_OPENING_TAG+1+ITERATION_CLOSING_TAG, description.getDo()).getSteps());
        return loop;
    }

    private LoopStep convertForEachStep(String testCaseId, String id, ForEachStep description) {
        LoopStep loop = new LoopStep();
        loop.setId(id);
        loop.setTitle(description.getTitle());
        loop.setDesc(description.getDesc());
        loop.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        loop.setHidden(description.isHidden() != null && description.isHidden());
        loop.setCollapsed(description.isCollapsed());
        loop.getSteps().addAll(
                convertSequence(testCaseId, id+ITERATION_OPENING_TAG+1+ITERATION_CLOSING_TAG, description.getDo()).getSteps());
        return loop;
    }

    private LoopStep convertWhileStep(String testCaseId, String id, WhileStep description) {
        LoopStep loop = new LoopStep();
        loop.setId(id);
        loop.setTitle(description.getTitle());
        loop.setDesc(description.getDesc());
        loop.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        loop.setHidden(description.isHidden() != null && description.isHidden());
        loop.setCollapsed(description.isCollapsed());
        loop.getSteps().addAll(
                convertSequence(testCaseId, id+ITERATION_OPENING_TAG+1+ITERATION_CLOSING_TAG, description.getDo()).getSteps());
        return loop;
    }

    private com.gitb.tpl.FlowStep convertFlowStep(String testCaseId, String id, com.gitb.tdl.FlowStep description) {
        com.gitb.tpl.FlowStep flow = new com.gitb.tpl.FlowStep();
        flow.setId(id);
        flow.setTitle(description.getTitle());
        flow.setDesc(description.getDesc());
        flow.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        flow.setHidden(description.isHidden() != null && description.isHidden());
        flow.setCollapsed(description.isCollapsed());
        for(int i=0; i<description.getThread().size(); i++) {
            com.gitb.tdl.Sequence thread = description.getThread().get(i);
            flow.getThread().add(convertSequence(testCaseId, id+ITERATION_OPENING_TAG+(i+1)+ITERATION_CLOSING_TAG, thread));
        }

        return flow;
    }

    private com.gitb.tpl.GroupStep convertGroupStep(String testCaseId, String id, com.gitb.tdl.Group description) {
        GroupStep group = new GroupStep();
        group.setId(id);
        group.setTitle(description.getTitle());
        group.setDesc(description.getDesc());
        group.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        group.setHidden(description.isHidden() != null && description.isHidden());
        group.setCollapsed(description.isCollapsed());
        group.getSteps().addAll(convertSequence(testCaseId, id, description).getSteps());
        return group;
    }

    private Sequence convertCallStep(String testCaseId, String id, CallStep callStep) {
        String testSuiteContext = callStep.getFrom();
        if (callStep.getFrom() != null) {
            /*
                Record the test suite context for this call step (if one is specified). This is done so that any
                imported documentation is loaded from the scriptlet's own test suite if a "from" is not explicitly
                provided.
             */
            testSuiteContexts.push(callStep.getFrom());
        } else if (!testSuiteContexts.empty()) {
            testSuiteContext = testSuiteContexts.peek();
        }
        /*
            Check to see if the target scriptlet has already been called. This check is done to ensure that a scriptlet
            cannot include itself (directly or indirectly), so that we don't have infinite scriptlet calls.
         */
        String callKey = StringUtils.defaultString(testSuiteContext)+"|"+callStep.getPath();
        if (scriptletCallStack.contains(callKey)) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE,
                    "The test case is invalid due to a call step that imported a scriptlet which in turn tried " +
                            "to re-import itself. The offending scriptlet was loaded from ["+StringUtils.defaultString(testSuiteContext)+"] and path ["+callStep.getPath()+"]."));
        } else {
            scriptletCallStack.push(callKey);
        }
        Scriptlet scriptlet = scriptletCache.getScriptlet(testSuiteContext, callStep.getPath(), testCase, true);
        Sequence sequence = convertSequence(testCaseId, id, scriptlet.getSteps());
        if (callStep.getFrom() != null) {
            testSuiteContexts.pop();
        }
        scriptletCallStack.pop();
        return sequence;
    }

    private UserInteractionStep convertUserInteraction(String testCaseId, String id, UserInteraction description) {
        UserInteractionStep interactionStep = new UserInteractionStep();
        interactionStep.setId(id);
        interactionStep.setTitle(description.getTitle());
        interactionStep.setDesc(description.getDesc());
        interactionStep.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        interactionStep.setHidden(description.isHidden() != null && description.isHidden());
        interactionStep.setCollapsed(description.isCollapsed());
        interactionStep.setWith(description.getWith());

        int childIndex = 1;

        for(com.gitb.tdl.InstructionOrRequest interaction : description.getInstructOrRequest()){
            com.gitb.tpl.InstructionOrRequest ior = null;
            if(interaction instanceof Instruction) {
                ior = new com.gitb.tpl.Instruction();
            } else if (interaction instanceof UserRequest) {
                ior = new com.gitb.tpl.UserRequest();
            }
            if(ior != null) {
                ior.setId("" + childIndex);
                ior.setDesc(interaction.getDesc());
                ior.setWith(interaction.getWith());
            }
            interactionStep.getInstructOrRequest().add(ior);
            childIndex++;
        }
        return interactionStep;
    }

    private com.gitb.tpl.ExitStep convertExitStep(String testCaseId, String id, com.gitb.tdl.ExitStep description) {
        com.gitb.tpl.ExitStep exit = new com.gitb.tpl.ExitStep();
        exit.setId(id);
        exit.setDesc(description.getDesc());
        exit.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        exit.setHidden(description.isHidden() != null && description.isHidden());
        return exit;
    }

    private String getDocumentation(String testCaseId, Documentation documentation) {
        String result = null;
        if (documentation != null) {
            if (documentation.getValue() != null && !documentation.getValue().isBlank()) {
                result = documentation.getValue().trim();
            } else if (documentation.getImport() != null && !documentation.getImport().isBlank()) {
                ITestCaseRepository repository = ModuleManager.getInstance().getTestCaseRepository();
                String testSuiteContext = documentation.getFrom();
                if (testSuiteContext == null && !testSuiteContexts.empty()) {
                    /*
                        In case we have a documentation import from a scriptlet of another test suite, the implicit
                        test suite reference should be the test suite containing the scriptlet, not the test suite
                        that included the call step.
                     */
                    testSuiteContext = testSuiteContexts.peek();
                }
                try (InputStream in = repository.getTestArtifact(testSuiteContext, testCaseId, documentation.getImport())) {
                    if (in == null) {
                        logger.warn("Unable to find documentation artifact from ["+ StringUtils.defaultString(testSuiteContext)+"] path ["+documentation.getImport()+"]");
                    } else {
                        byte[] bytes = IOUtils.toByteArray(in);
                        result = new String(bytes, (documentation.getEncoding() == null)? Charset.defaultCharset(): Charset.forName(documentation.getEncoding()));
                    }
                } catch (Exception e) {
                    logger.warn("Error reading documentation artifact from ["+ StringUtils.defaultString(testSuiteContext)+"] path ["+documentation.getImport()+"]", e);
                }
            }
        }
        return result;
    }

}
