package com.gitb.engine.utils;

import com.gitb.ModuleManager;
import com.gitb.core.Documentation;
import com.gitb.repository.ITestCaseRepository;
import com.gitb.tdl.Instruction;
import com.gitb.tdl.UserRequest;
import com.gitb.tdl.*;
import com.gitb.tpl.Sequence;
import com.gitb.tpl.TestCase;
import com.gitb.tpl.*;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.Charset;

public class TestCaseConverter {

    private static final String TRUE  =  "[T]";
    private static final String FALSE =  "[F]";
    private static final String ITERATION_OPENING_TAG = "[";
    private static final String ITERATION_CLOSING_TAG = "]";

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
        presentation.setSteps(convertSequence(testCaseId, "", testCase.getScriptlets(), testCase.getSteps()));

        return presentation;
    }

    private Sequence convertSequence(String testCaseId, String id, Scriptlets scriptlets, com.gitb.tdl.Sequence description) {
        Sequence sequence = new Sequence();
        sequence.setId(id);
        int index = 1;

        String childIdPrefix = id.equals("") ? "" : (id + ".");

        for(int i=0; i<description.getSteps().size(); i++) {
            Object step = description.getSteps().get(i);

            if(step instanceof Verify) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().add(convertVerifyStep(testCaseId, childId, (Verify) step));
            } else if (step instanceof com.gitb.tdl.MessagingStep) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().add(convertMessagingStep(testCaseId, childId, (com.gitb.tdl.MessagingStep) step));
            } else if (step instanceof IfStep) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().add(convertDecisionStep(testCaseId, childId, scriptlets, (IfStep) step));
            } else if (step instanceof RepeatUntilStep) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().add(convertRepUntilStep(testCaseId, childId, scriptlets, (RepeatUntilStep) step));
            } else if (step instanceof ForEachStep) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().add(convertForEachStep(testCaseId, childId, scriptlets, (ForEachStep) step));
            } else if (step instanceof WhileStep) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().add(convertWhileStep(testCaseId, childId, scriptlets, (WhileStep) step));
            } else if (step instanceof com.gitb.tdl.FlowStep) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().add(convertFlowStep(testCaseId, childId, scriptlets, (com.gitb.tdl.FlowStep) step));
            } else if (step instanceof CallStep) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().addAll(convertCallStep(testCaseId, childId, scriptlets, (CallStep)step).getSteps());
            } else if (step instanceof UserInteraction) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().add(convertUserInteraction(testCaseId, childId, (UserInteraction) step));
            } else if (step instanceof com.gitb.tdl.ExitStep) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().add(convertExitStep(testCaseId, childId, (com.gitb.tdl.ExitStep) step));
            } else if (step instanceof Group) {
                // Nothing.
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

    private com.gitb.tpl.TestStep convertVerifyStep(String testCaseId, String id, Verify description) {
        com.gitb.tpl.TestStep verify = new com.gitb.tpl.TestStep();
        verify.setId(id);
        verify.setDesc(description.getDesc());
        verify.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        return verify;
    }

    private com.gitb.tpl.MessagingStep convertMessagingStep(String testCaseId, String id, com.gitb.tdl.MessagingStep description) {
        com.gitb.tpl.MessagingStep messaging = new com.gitb.tpl.MessagingStep();
        messaging.setId(id);
        messaging.setDesc(description.getDesc());
        messaging.setFrom(description.getFrom());
        messaging.setTo(description.getTo());
        messaging.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        return messaging;
    }

    private DecisionStep convertDecisionStep(String testCaseId, String id, Scriptlets scriptlets, IfStep description) {
        DecisionStep decision = new DecisionStep();
        decision.setId(id);
        decision.setTitle(description.getTitle());
        decision.setDesc(description.getDesc());
        decision.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        decision.setThen(convertSequence(testCaseId, id + TRUE , scriptlets, description.getThen()));
        if (description.getElse() != null) {
            decision.setElse(convertSequence(testCaseId, id + FALSE, scriptlets, description.getElse()));
        }
        return decision;
    }

    private Sequence convertRepUntilStep(String testCaseId, String id, Scriptlets scriptlets, RepeatUntilStep description) {
        Sequence loop = new Sequence();
        loop.setId(id);
        loop.setTitle(description.getTitle());
        loop.setDesc(description.getDesc());
        loop.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        loop.getSteps().addAll(
                convertSequence(testCaseId, id+ITERATION_OPENING_TAG+1+ITERATION_CLOSING_TAG, scriptlets, description.getDo()).getSteps());
        return loop;
    }

    private Sequence convertForEachStep(String testCaseId, String id, Scriptlets scriptlets, ForEachStep description) {
        Sequence loop = new Sequence();
        loop.setId(id);
        loop.setTitle(description.getTitle());
        loop.setDesc(description.getDesc());
        loop.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        loop.getSteps().addAll(
                convertSequence(testCaseId, id+ITERATION_OPENING_TAG+1+ITERATION_CLOSING_TAG, scriptlets, description.getDo()).getSteps());
        return loop;
    }

    private Sequence convertWhileStep(String testCaseId, String id, Scriptlets scriptlets, WhileStep description) {
        Sequence loop = new Sequence();
        loop.setId(id);
        loop.setTitle(description.getTitle());
        loop.setDesc(description.getDesc());
        loop.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        loop.getSteps().addAll(
                convertSequence(testCaseId, id+ITERATION_OPENING_TAG+1+ITERATION_CLOSING_TAG, scriptlets, description.getDo()).getSteps());
        return loop;
    }

    private com.gitb.tpl.FlowStep convertFlowStep(String testCaseId, String id, Scriptlets scriptlets, com.gitb.tdl.FlowStep description) {
        com.gitb.tpl.FlowStep flow = new com.gitb.tpl.FlowStep();
        flow.setId(id);
        flow.setTitle(description.getTitle());
        flow.setDesc(description.getDesc());
        flow.setDesc(getDocumentation(testCaseId, description.getDocumentation()));

        for(int i=0; i<description.getThread().size(); i++) {
            com.gitb.tdl.Sequence thread = description.getThread().get(i);
            flow.getThread().add(convertSequence(testCaseId, id+ITERATION_OPENING_TAG+(i+1)+ITERATION_CLOSING_TAG, scriptlets, thread));
        }

        return flow;
    }

    private Sequence convertCallStep(String testCaseId, String id, Scriptlets scriptlets, CallStep callStep) {
        if (scriptlets != null) {
            Scriptlet scriptlet = scriptletCache.getScriptlet(callStep.getFrom(), callStep.getPath(), testCase, true);
            return convertSequence(testCaseId, id, scriptlets, scriptlet.getSteps());
        }
        return new Sequence();
    }

    private UserInteractionStep convertUserInteraction(String testCaseId, String id, UserInteraction description) {
        UserInteractionStep interactionStep = new UserInteractionStep();
        interactionStep.setId(id);
        interactionStep.setTitle(description.getTitle());
        interactionStep.setDesc(description.getDesc());
        interactionStep.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
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

    private static com.gitb.tpl.ExitStep convertExitStep(String testCaseId, String id, com.gitb.tdl.ExitStep description) {
        com.gitb.tpl.ExitStep exit = new com.gitb.tpl.ExitStep();
        exit.setId(id);
        exit.setDesc(description.getDesc());
        exit.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        return exit;
    }

    private static String getDocumentation(String testCaseId, Documentation documentation) {
        String result = null;
        if (documentation != null) {
            if (documentation.getValue() != null && !documentation.getValue().isBlank()) {
                result = documentation.getValue().trim();
            } else if (documentation.getImport() != null && !documentation.getImport().isBlank()) {
                ITestCaseRepository repository = ModuleManager.getInstance().getTestCaseRepository();
                try (InputStream in = repository.getTestArtifact(testCaseId, documentation.getImport())) {
                    byte[] bytes = IOUtils.toByteArray(in);
                    result = new String(bytes, (documentation.getEncoding() == null)? Charset.defaultCharset(): Charset.forName(documentation.getEncoding()));
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to read imported documentation artifact", e);
                }
            }
        }
        return result;
    }

}
