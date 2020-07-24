package com.gitb.engine.utils;

import com.gitb.core.Configuration;
import com.gitb.engine.PropertyConstants;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.remote.RemoteCallContext;
import com.gitb.tdl.*;
import com.gitb.tdl.Instruction;
import com.gitb.tdl.UserRequest;
import com.gitb.tpl.*;
import com.gitb.tpl.ExitStep;
import com.gitb.tpl.FlowStep;
import com.gitb.tpl.InstructionOrRequest;
import com.gitb.tpl.MessagingStep;
import com.gitb.tpl.ObjectFactory;
import com.gitb.tpl.Sequence;
import com.gitb.tpl.TestCase;
import com.gitb.tpl.TestStep;
import org.apache.commons.lang3.StringUtils;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.List;
import java.util.Properties;

/**
 * Created by senan on 10/13/14.
 */
public class TestCaseUtils {

	// TODO add the test case construct classes to report their statuses (COMPLETED, ERROR, etc.)
	private static final Class<?>[] TEST_CONSTRUCTS_TO_REPORT = {
        com.gitb.tdl.MessagingStep.class, Verify.class, IfStep.class, RepeatUntilStep.class,
		ForEachStep.class, WhileStep.class, com.gitb.tdl.FlowStep.class,
		CallStep.class, com.gitb.tdl.ExitStep.class, Group.class, UserInteraction.class
	};

    private static final String TRUE  =  "[T]";
    private static final String FALSE =  "[F]";
    private static final String ITERATION_OPENING_TAG = "[";
    private static final String ITERATION_CLOSING_TAG = "]";

    public static Properties getStepProperties(List<Configuration> properties, VariableResolver resolver) {
        Properties result = new Properties();
        if (properties != null && !properties.isEmpty()) {
            for (Configuration config: properties) {
                String value = config.getValue();
                if (resolver.isVariableReference(value)) {
                    value = resolver.resolveVariableAsString(value).toString();
                }
                result.setProperty(config.getName(), value);
            }
        }
        return result;
    }

    public static void prepareRemoteServiceLookup(Properties stepProperties) {
        if (stepProperties != null && !StringUtils.isBlank(stepProperties.getProperty(PropertyConstants.AUTH_BASIC_USERNAME))) {
            /*
            The configuration specifies that we have basic authentication. To allow this to go through even if
            the WSDL is protected we use a thread-safe (via ThreadLocal) authenticator. This is because the
            new MessagingServiceClient(getServiceURL()) call results in a call to the WSDL (that needs authentication).
             */
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    Properties callProperties = RemoteCallContext.getCallProperties();
                    String username = callProperties.getProperty(PropertyConstants.AUTH_BASIC_USERNAME);
                    String password = callProperties.getProperty(PropertyConstants.AUTH_BASIC_PASSWORD);
                    return new PasswordAuthentication(
                            username,
                            password.toCharArray());
                }
            });
        }
    }

    public static TestCase convertTestCase(com.gitb.tdl.TestCase description) {
        TestCase presentation = new TestCase();
        presentation.setId(description.getId());
        presentation.setMetadata(description.getMetadata());
        presentation.setActors(description.getActors());
        if(description.getPreliminary() != null) {
            presentation.setPreliminary(convertPreliminary(description.getPreliminary()));
        }
        presentation.setSteps(convertSequence("", description.getScriptlets(), description.getSteps()));

        return presentation;
    }

	public static boolean shouldBeReported(Class<?> stepClass) {
		for(Class<?> c : TEST_CONSTRUCTS_TO_REPORT) {
            Class<?> current = stepClass;
            while(current != null) {
                if(current.equals(c)) {
                    return true;
                }
                current = current.getSuperclass();
			}
		}

		return false;
	}

    private static Sequence convertSequence(String id, Scriptlets scriptlets, com.gitb.tdl.Sequence description) {
        Sequence sequence = new Sequence();
        sequence.setId(id);
        int index = 1;

        String childIdPrefix = id.equals("") ? "" : (id + ".");

        for(int i=0; i<description.getSteps().size(); i++) {
            Object step = description.getSteps().get(i);

            if(step instanceof Verify) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().add(convertVerifyStep(childId, (Verify) step));
            } else if (step instanceof com.gitb.tdl.MessagingStep) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().add(convertMessagingStep(childId, (com.gitb.tdl.MessagingStep) step));
            } else if (step instanceof IfStep) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().add(convertDecisionStep(childId, scriptlets, (IfStep) step));
            } else if (step instanceof RepeatUntilStep) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().add(convertRepUntilStep(childId, scriptlets, (RepeatUntilStep) step));
            } else if (step instanceof ForEachStep) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().add(convertForEachStep(childId, scriptlets, (ForEachStep) step));
            } else if (step instanceof WhileStep) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().add(convertWhileStep(childId, scriptlets, (WhileStep) step));
            } else if (step instanceof com.gitb.tdl.FlowStep) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().add(convertFlowStep(childId, scriptlets, (com.gitb.tdl.FlowStep) step));
            } else if (step instanceof CallStep) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().addAll(convertCallStep(childId, scriptlets, (CallStep)step).getSteps());
            } else if (step instanceof UserInteraction) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().add(convertUserInteraction(childId, (UserInteraction) step));
            } else if (step instanceof com.gitb.tdl.ExitStep) {
                String childId = childIdPrefix + index++;
                sequence.getSteps().add(convertExitStep(childId, (com.gitb.tdl.ExitStep) step));
            } else if (step instanceof Group) {

            }
        }

        return sequence;
    }

    private static Preliminary convertPreliminary(UserInteraction description) {
        Preliminary preliminary = new Preliminary();
        ObjectFactory factory   = new ObjectFactory();
        for(com.gitb.tdl.InstructionOrRequest interaction : description.getInstructOrRequest()){
            InstructionOrRequest ior = new InstructionOrRequest();
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

    private static TestStep convertVerifyStep(String id, Verify description) {
        TestStep verify = new TestStep();
        verify.setId(id);
        verify.setDesc(description.getDesc());
        verify.setDocumentation(description.getDocumentation());
        return verify;
    }

    private static MessagingStep convertMessagingStep(String id, com.gitb.tdl.MessagingStep description) {
        MessagingStep messaging = new MessagingStep();
        messaging.setId(id);
        messaging.setDesc(description.getDesc());
        messaging.setFrom(description.getFrom());
        messaging.setTo(description.getTo());
        messaging.setDocumentation(description.getDocumentation());
        return messaging;
    }

    private static DecisionStep convertDecisionStep(String id, Scriptlets scriptlets, IfStep description) {
        DecisionStep decision = new DecisionStep();
        decision.setId(id);
        decision.setDesc(description.getDesc());
        decision.setDocumentation(description.getDocumentation());
        decision.setThen(convertSequence(id + TRUE , scriptlets, description.getThen()));
        if (description.getElse() != null) {
            decision.setElse(convertSequence(id + FALSE, scriptlets, description.getElse()));
        }
        return decision;
    }

    private static Sequence convertRepUntilStep(String id, Scriptlets scriptlets, RepeatUntilStep description) {
        Sequence loop = new Sequence();
        loop.setId(id);
        loop.setDesc(description.getDesc());
        loop.setDocumentation(description.getDocumentation());
        loop.getSteps().addAll(
                convertSequence(id+ITERATION_OPENING_TAG+1+ITERATION_CLOSING_TAG, scriptlets, description.getDo()).getSteps());
        return loop;
    }

    private static Sequence convertForEachStep(String id, Scriptlets scriptlets, ForEachStep description) {
        Sequence loop = new Sequence();
        loop.setId(id);
        loop.setDesc(description.getDesc());
        loop.setDocumentation(description.getDocumentation());
        loop.getSteps().addAll(
                convertSequence(id+ITERATION_OPENING_TAG+1+ITERATION_CLOSING_TAG, scriptlets, description.getDo()).getSteps());
        return loop;
    }

    private static Sequence convertWhileStep(String id, Scriptlets scriptlets, WhileStep description) {
        Sequence loop = new Sequence();
        loop.setId(id);
        loop.setDesc(description.getDesc());
        loop.setDocumentation(description.getDocumentation());
        loop.getSteps().addAll(
                convertSequence(id+ITERATION_OPENING_TAG+1+ITERATION_CLOSING_TAG, scriptlets, description.getDo()).getSteps());
        return loop;
    }

    private static FlowStep convertFlowStep(String id, Scriptlets scriptlets, com.gitb.tdl.FlowStep description) {
        FlowStep flow = new FlowStep();
        flow.setId(id);
        flow.setDesc(description.getDesc());
        flow.setDesc(description.getDocumentation());

        for(int i=0; i<description.getThread().size(); i++) {
            com.gitb.tdl.Sequence thread = description.getThread().get(i);
            flow.getThread().add(convertSequence(id+ITERATION_OPENING_TAG+(i+1)+ITERATION_CLOSING_TAG, scriptlets, thread));
        }

        return flow;
    }

    private static Sequence convertCallStep(String id, Scriptlets scriptlets, CallStep description) {
        if(scriptlets != null) {
            Scriptlet scriptlet = getScriptlet(description.getPath(), scriptlets);
            return convertSequence(id, scriptlets, scriptlet.getSteps());
        }
        return new Sequence();
    }

    private static UserInteractionStep convertUserInteraction(String id, UserInteraction description) {
        UserInteractionStep interactionStep = new UserInteractionStep();
        interactionStep.setId(id);
        interactionStep.setDesc(description.getDesc());
        interactionStep.setDocumentation(description.getDocumentation());
        interactionStep.setWith(description.getWith());

        int childIndex = 1;

        for(com.gitb.tdl.InstructionOrRequest interaction : description.getInstructOrRequest()){
	        InstructionOrRequest ior = null;
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

    private static ExitStep convertExitStep(String id, com.gitb.tdl.ExitStep description) {
        ExitStep exit = new ExitStep();
        exit.setId(id);
        exit.setDesc(description.getDesc());
        exit.setDocumentation(description.getDocumentation());
        return exit;
    }

    private static Scriptlet getScriptlet(String id, Scriptlets scriptlets) {
        for(Scriptlet scriptlet : scriptlets.getScriptlet()) {
            if(scriptlet.getId().equals(id)){
                return scriptlet;
            }
        }
        return null;
    }
}

