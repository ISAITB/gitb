/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package com.gitb.engine.utils;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Documentation;
import com.gitb.core.ErrorCode;
import com.gitb.engine.ModuleManager;
import com.gitb.engine.SessionConfigurationData;
import com.gitb.engine.expr.StaticExpressionHandler;
import com.gitb.engine.testcase.StaticTestCaseContext;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.repository.ITestCaseRepository;
import com.gitb.tdl.*;
import com.gitb.tdl.Instruction;
import com.gitb.tdl.Process;
import com.gitb.tdl.UserRequest;
import com.gitb.tpl.*;
import com.gitb.tpl.Sequence;
import com.gitb.tpl.TestCase;
import com.gitb.tpl.TestStep;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

public class TestCaseConverter {

    private static final Logger logger = LoggerFactory.getLogger(TestCaseConverter.class);
    private static final String TRUE  =  "[T]";
    private static final String FALSE =  "[F]";
    private static final String ITERATION_OPENING_TAG = "[";
    private static final String ITERATION_CLOSING_TAG = "]";

    private final Stack<String> testSuiteContexts = new Stack<>();
    private final Stack<String> scriptletCallStack = new Stack<>();
    private final LinkedList<Pair<CallStep, Scriptlet>> scriptletStepStack = new LinkedList<>();
    private final LinkedList<Boolean> scriptletStepHiddenAttributeStack = new LinkedList<>();
    private final com.gitb.tdl.TestCase testCase;
    private final ScriptletCache scriptletCache;
    private final StaticExpressionHandler expressionHandler;
    private final TestCaseContext testCaseContext;
    private Set<String> actorIds = null;

    public TestCaseConverter(com.gitb.tdl.TestCase testCase, List<ActorConfiguration> configs) {
        this(testCase, null, configs);
    }

    public TestCaseConverter(com.gitb.tdl.TestCase testCase, ScriptletCache scriptletCache, List<ActorConfiguration> configs) {
        this.testCase = testCase;
        this.scriptletCache = Objects.requireNonNullElseGet(scriptletCache, ScriptletCache::new);
        testCaseContext = createTestCaseContext(testCase, configs);
        expressionHandler = new StaticExpressionHandler(testCaseContext.getScope());
    }

    private TestCaseContext createTestCaseContext(com.gitb.tdl.TestCase testCase, List<ActorConfiguration> configs) {
        var context = new StaticTestCaseContext(testCase);
        var configData = new SessionConfigurationData(configs);
        context.configure(configData.getActorConfigurations(), configData.getDomainConfiguration(), configData.getOrganisationConfiguration(), configData.getSystemConfiguration());
        return context;
    }

    public TestCase convertTestCase(String testCaseId) {
        TestCase presentation = new TestCase();
        presentation.setId(testCase.getId());
        presentation.setMetadata(testCase.getMetadata());
        presentation.setActors(testCase.getActors());
        presentation.setSupportsParallelExecution(testCase.isSupportsParallelExecution());
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

        String childIdPrefix = id.isEmpty() ? "" : (id + ".");

        for (int i=0; i<sequenceStep.getSteps().size(); i++) {
            Object step = sequenceStep.getSteps().get(i);
            switch (step) {
                case Verify verify -> {
                    String childId = childIdPrefix + index++;
                    addToSequence(sequence, convertVerifyStep(testCaseId, childId, verify));
                }
                case Process process -> {
                    String childId = childIdPrefix + index++;
                    addToSequence(sequence, convertProcessStep(testCaseId, childId, process));
                }
                case com.gitb.tdl.MessagingStep messagingStep -> {
                    String childId = childIdPrefix + index++;
                    addToSequence(sequence, convertMessagingStep(testCaseId, childId, messagingStep));
                }
                case IfStep ifStep1 -> {
                    String childId = childIdPrefix + index++;
                    var ifStep = convertDecisionStep(testCaseId, childId, ifStep1);
                    if (ifStep.isHidden()) {
                        /*
                         * Hidden if step with explicitly visible then or else block. Convert the visible block of steps to top level steps so that
                         * they are visible without the "if" boundaries.
                         */
                        if (ifStep.getThen() != null && !ifStep.getThen().isHidden()) {
                            ifStep.getThen().getSteps().forEach(childStep -> addToSequence(sequence, childStep));
                        }
                        if (ifStep.getElse() != null && !ifStep.getElse().isHidden()) {
                            ifStep.getElse().getSteps().forEach(childStep -> addToSequence(sequence, childStep));
                        }
                    } else {
                        addToSequence(sequence, ifStep);
                    }
                }
                case RepeatUntilStep repeatUntilStep -> {
                    String childId = childIdPrefix + index++;
                    addToSequence(sequence, convertRepUntilStep(testCaseId, childId, repeatUntilStep));
                }
                case ForEachStep forEachStep -> {
                    String childId = childIdPrefix + index++;
                    addToSequence(sequence, convertForEachStep(testCaseId, childId, forEachStep));
                }
                case WhileStep whileStep -> {
                    String childId = childIdPrefix + index++;
                    addToSequence(sequence, convertWhileStep(testCaseId, childId, whileStep));
                }
                case com.gitb.tdl.FlowStep flowStep -> {
                    String childId = childIdPrefix + index++;
                    addToSequence(sequence, convertFlowStep(testCaseId, childId, flowStep));
                }
                case CallStep callStep -> {
                    String childId = childIdPrefix + index++;
                    for (TestStep childStep : convertCallStep(testCaseId, childId, callStep).getSteps()) {
                        addToSequence(sequence, childStep);
                    }
                }
                case UserInteraction userInteraction -> {
                    String childId = childIdPrefix + index++;
                    addToSequence(sequence, convertUserInteraction(testCaseId, childId, userInteraction));
                }
                case com.gitb.tdl.ExitStep exitStep -> {
                    String childId = childIdPrefix + index++;
                    addToSequence(sequence, convertExitStep(testCaseId, childId, exitStep));
                }
                case Group group -> {
                    String childId = childIdPrefix + index++;
                    var groupStep = convertGroupStep(testCaseId, childId, group);
                    if (groupStep.isHiddenContainer() && !groupStep.isHidden()) {
                        // Skip the group step itself and add its children directly.
                        groupStep.getSteps().forEach(childStep -> addToSequence(sequence, childStep));
                    } else {
                        addToSequence(sequence, groupStep);
                    }
                }
                default -> {
                    // Ignore other steps
                }
            }
        }
        return sequence;
    }

    private String fixedOrVariableValueAsString(String originalValue) {
        return TestCaseUtils.fixedOrVariableValue(originalValue, String.class, scriptletStepStack, expressionHandler);
    }

    private Boolean fixedOrVariableValueAsBoolean(String originalValue, boolean defaultIfMissing) {
        var result = TestCaseUtils.fixedOrVariableValue(originalValue, Boolean.class, scriptletStepStack, expressionHandler);
        return Objects.requireNonNullElse(result, defaultIfMissing);
    }

    private String fixedOrVariableValueForActor(String originalValue) {
        var value = TestCaseUtils.fixedOrVariableValue(originalValue, String.class, scriptletStepStack, expressionHandler);
        if (actorIds == null) {
            actorIds = new HashSet<>();
            if (testCase.getActors() != null) {
                testCase.getActors().getActor().forEach(actor -> actorIds.add(actor.getId()));
            }
        }
        if (!actorIds.contains(value)) {
            throw new IllegalStateException("Actor identifier reference ["+originalValue+"] resolved to ["+value+"] which is not a valid actor");
        }
        return value;
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
        verify.setDesc(fixedOrVariableValueAsString(description.getDesc()));
        verify.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        verify.setHidden(hiddenValueToUse(description.getHidden(), false));
        return verify;
    }

    private com.gitb.tpl.ProcessStep convertProcessStep(String testCaseId, String id, Process description) {
        com.gitb.tpl.ProcessStep process = new com.gitb.tpl.ProcessStep();
        process.setId(id);
        process.setDesc(fixedOrVariableValueAsString(description.getDesc()));
        process.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        // Process steps are by default hidden.
        process.setHidden(hiddenValueToUse(description.getHidden(), true));
        return process;
    }

    private com.gitb.tpl.MessagingStep convertMessagingStep(String testCaseId, String id, com.gitb.tdl.MessagingStep step) {
        com.gitb.tpl.MessagingStep messaging = new com.gitb.tpl.MessagingStep();
        messaging.setId(id);
        messaging.setDesc(fixedOrVariableValueAsString(step.getDesc()));
        String fromActor;
        String toActor;
        if (step instanceof ReceiveOrListen) {
            fromActor = Objects.requireNonNullElseGet(step.getFrom(), testCaseContext::getDefaultSutActor);
            toActor = Objects.requireNonNullElseGet(step.getTo(), () -> testCaseContext.getDefaultNonSutActor(true));
        } else {
            fromActor = Objects.requireNonNullElseGet(step.getFrom(), () -> testCaseContext.getDefaultNonSutActor(true));
            toActor = Objects.requireNonNullElseGet(step.getTo(), testCaseContext::getDefaultSutActor);
        }
        messaging.setFrom(fixedOrVariableValueForActor(fromActor));
        messaging.setTo(fixedOrVariableValueForActor(toActor));
        messaging.setDocumentation(getDocumentation(testCaseId, step.getDocumentation()));
        messaging.setHidden(hiddenValueToUse(step.getHidden(), false));
        messaging.setReply(fixedOrVariableValueAsBoolean(step.getReply(), false));
        return messaging;
    }

    private DecisionStep convertDecisionStep(String testCaseId, String id, IfStep description) {
        DecisionStep decision = new DecisionStep();
        decision.setId(id);
        decision.setTitle(fixedOrVariableValueAsString(description.getTitle()));
        decision.setDesc(fixedOrVariableValueAsString(description.getDesc()));
        decision.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        decision.setHidden(hiddenValueToUse(description.getHidden(), false));
        decision.setCollapsed(description.isCollapsed());
        decision.setThen(convertSequence(testCaseId, id + TRUE , description.getThen()));
        if (description.getElse() != null) {
            decision.setElse(convertSequence(testCaseId, id + FALSE, description.getElse()));
        }
        // Determine step visibilities.
        if (description.isStatic()) {
            // The If is always hidden and its condition evaluated at load time to determine whether to include the then or else block.
            decision.setHidden(true);
            var includeThenBlock = fixedOrVariableValueAsBoolean(description.getCond().getValue(), false);
            decision.getThen().setHidden(hiddenValueToUse(description.getThen().getHidden(), !includeThenBlock));
            if (description.getElse() != null) {
                decision.getElse().setHidden(hiddenValueToUse(description.getElse().getHidden(), includeThenBlock));
            }
        } else if (decision.isHidden()) {
            // For a hidden if step without an else block, the then block is considered hidden by default unless explicitly set to non-hidden to show only its steps.
            if (decision.getElse() == null) {
                decision.getThen().setHidden(hiddenValueToUse(description.getThen().getHidden(), true));
            } else {
                decision.getThen().setHidden(true);
                decision.getElse().setHidden(true);
            }
        } else {
            // Regular if (non-static, non-hidden). Force everything to be not hidden.
            decision.getThen().setHidden(false);
            if (decision.getElse() != null) {
                decision.getElse().setHidden(false);
            }
        }
        return decision;
    }

    private LoopStep convertRepUntilStep(String testCaseId, String id, RepeatUntilStep description) {
        LoopStep loop = new LoopStep();
        loop.setId(id);
        loop.setTitle(fixedOrVariableValueAsString(description.getTitle()));
        loop.setDesc(fixedOrVariableValueAsString(description.getDesc()));
        loop.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        loop.setHidden(hiddenValueToUse(description.getHidden(), false));
        loop.setCollapsed(description.isCollapsed());
        loop.getSteps().addAll(
                convertSequence(testCaseId, id+ITERATION_OPENING_TAG+1+ITERATION_CLOSING_TAG, description.getDo()).getSteps());
        return loop;
    }

    private LoopStep convertForEachStep(String testCaseId, String id, ForEachStep description) {
        LoopStep loop = new LoopStep();
        loop.setId(id);
        loop.setTitle(fixedOrVariableValueAsString(description.getTitle()));
        loop.setDesc(fixedOrVariableValueAsString(description.getDesc()));
        loop.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        loop.setHidden(hiddenValueToUse(description.getHidden(), false));
        loop.setCollapsed(description.isCollapsed());
        loop.getSteps().addAll(
                convertSequence(testCaseId, id+ITERATION_OPENING_TAG+1+ITERATION_CLOSING_TAG, description.getDo()).getSteps());
        return loop;
    }

    private LoopStep convertWhileStep(String testCaseId, String id, WhileStep description) {
        LoopStep loop = new LoopStep();
        loop.setId(id);
        loop.setTitle(fixedOrVariableValueAsString(description.getTitle()));
        loop.setDesc(fixedOrVariableValueAsString(description.getDesc()));
        loop.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        loop.setHidden(hiddenValueToUse(description.getHidden(), false));
        loop.setCollapsed(description.isCollapsed());
        loop.getSteps().addAll(
                convertSequence(testCaseId, id+ITERATION_OPENING_TAG+1+ITERATION_CLOSING_TAG, description.getDo()).getSteps());
        return loop;
    }

    private com.gitb.tpl.FlowStep convertFlowStep(String testCaseId, String id, com.gitb.tdl.FlowStep description) {
        com.gitb.tpl.FlowStep flow = new com.gitb.tpl.FlowStep();
        flow.setId(id);
        flow.setTitle(fixedOrVariableValueAsString(description.getTitle()));
        flow.setDesc(fixedOrVariableValueAsString(description.getDesc()));
        flow.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        flow.setHidden(hiddenValueToUse(description.getHidden(), false));
        flow.setCollapsed(description.isCollapsed());
        for (int i=0; i<description.getThread().size(); i++) {
            com.gitb.tdl.Sequence thread = description.getThread().get(i);
            if (!hiddenValueToUse(thread.getHidden(), false)) {
                flow.getThread().add(convertSequence(testCaseId, id+ITERATION_OPENING_TAG+(i+1)+ITERATION_CLOSING_TAG, thread));
            }
        }

        return flow;
    }

    private com.gitb.tpl.GroupStep convertGroupStep(String testCaseId, String id, com.gitb.tdl.Group description) {
        GroupStep group = new GroupStep();
        group.setId(id);
        group.setTitle(fixedOrVariableValueAsString(description.getTitle()));
        group.setDesc(fixedOrVariableValueAsString(description.getDesc()));
        group.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        group.setHidden(hiddenValueToUse(description.getHidden(), false));
        group.setHiddenContainer(fixedOrVariableValueAsBoolean(description.getHiddenContainer(), false));
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
        Scriptlet scriptlet = scriptletCache.getScriptlet(testSuiteContext, callStep.getPath(), testCase, true).scriptlet();
        scriptletStepStack.addLast(Pair.of(callStep, scriptlet));
        scriptletStepHiddenAttributeStack.addLast(hiddenValueToUse(callStep.getHidden(), false));
        Sequence sequence = convertSequence(testCaseId, id, scriptlet.getSteps());
        if (callStep.getFrom() != null) {
            testSuiteContexts.pop();
        }
        scriptletCallStack.pop();
        scriptletStepHiddenAttributeStack.removeLast();
        scriptletStepStack.removeLast();
        return sequence;
    }

    private boolean hiddenValueToUse(String hiddenExpression, boolean defaultIfMissing) {
        /*
        We check to see first if this step is within a scriptlet. If yes and a parent scriptlet
        has been set to be hidden this will forcefully make everything beneath it hidden. These
        flag values are still maintained in a stack because we might have an internal scriptlet set
        as hidden that will at some point be popped.
         */
        return scriptletStepHiddenAttributeStack.stream().filter(Boolean.TRUE::equals).findAny().orElseGet(() -> fixedOrVariableValueAsBoolean(hiddenExpression, defaultIfMissing));
    }

    private UserInteractionStep convertUserInteraction(String testCaseId, String id, UserInteraction description) {
        UserInteractionStep interactionStep = new UserInteractionStep();
        interactionStep.setId(id);
        interactionStep.setTitle(fixedOrVariableValueAsString(description.getTitle()));
        interactionStep.setDesc(fixedOrVariableValueAsString(description.getDesc()));
        interactionStep.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        interactionStep.setHidden(hiddenValueToUse(description.getHidden(), false));
        interactionStep.setCollapsed(description.isCollapsed());
        interactionStep.setWith(description.getWith());
        interactionStep.setAdmin(description.isAdmin());

        int childIndex = 1;

        for (com.gitb.tdl.InstructionOrRequest interaction : description.getInstructOrRequest()){
            com.gitb.tpl.InstructionOrRequest ior = null;
            if (interaction instanceof Instruction) {
                ior = new com.gitb.tpl.Instruction();
                ((com.gitb.tpl.Instruction)ior).setForceDisplay(((Instruction) interaction).isForceDisplay());
            } else if (interaction instanceof UserRequest) {
                ior = new com.gitb.tpl.UserRequest();
            }
            if (ior != null) {
                ior.setId("" + childIndex);
                ior.setDesc(fixedOrVariableValueAsString(interaction.getDesc()));
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
        exit.setDesc(fixedOrVariableValueAsString(description.getDesc()));
        exit.setDocumentation(getDocumentation(testCaseId, description.getDocumentation()));
        exit.setHidden(hiddenValueToUse(description.getHidden(), false));
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
                        logger.warn("Unable to find documentation artifact from [{}] path [{}]", StringUtils.defaultString(testSuiteContext), documentation.getImport());
                    } else {
                        byte[] bytes = IOUtils.toByteArray(in);
                        result = new String(bytes, (documentation.getEncoding() == null)? Charset.defaultCharset(): Charset.forName(documentation.getEncoding()));
                    }
                } catch (Exception e) {
                    logger.warn("Error reading documentation artifact from [{}] path [{}]", StringUtils.defaultString(testSuiteContext), documentation.getImport(), e);
                }
            }
        }
        return result;
    }

}
