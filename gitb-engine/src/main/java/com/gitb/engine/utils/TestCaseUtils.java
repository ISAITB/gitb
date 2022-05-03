package com.gitb.engine.utils;

import com.gitb.engine.ModuleManager;
import com.gitb.core.Configuration;
import com.gitb.core.ErrorCode;
import com.gitb.engine.PropertyConstants;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.remote.RemoteCallContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.repository.ITestCaseRepository;
import com.gitb.tdl.*;
import com.gitb.tdl.Process;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import com.gitb.utils.ErrorUtils;
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
		ForEachStep.class, WhileStep.class, com.gitb.tdl.FlowStep.class, Process.class,
		CallStep.class, com.gitb.tdl.ExitStep.class, Group.class, UserInteraction.class
	};

    public static Properties getStepProperties(List<Configuration> properties, VariableResolver resolver) {
        Properties result = new Properties();
        if (properties != null && !properties.isEmpty()) {
            for (Configuration config: properties) {
                String value = config.getValue();
                if (VariableResolver.isVariableReference(value)) {
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

	private static Scriptlet lookupExternalScriptlet(String from, String testCaseId, String scriptletPath) {
        ITestCaseRepository repository = ModuleManager.getInstance().getTestCaseRepository();
        return repository.getScriptlet(from, testCaseId, scriptletPath);
    }

    public static Scriptlet lookupScriptlet(String from, String path, com.gitb.tdl.TestCase testCase, boolean required) {
        Scriptlet foundScriptlet = null;
        if (StringUtils.isBlank(path)) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "No scriptlet path was provided."));
        }
        String scriptletPath = path.trim();
        if (StringUtils.isNotBlank(from)) {
            // Lookup from a specific test suite.
            foundScriptlet = lookupExternalScriptlet(from.trim(), testCase.getId(), path);
        } else {
            // Find scriptlet in the test case (if it is inline).
            if (testCase.getScriptlets() != null) {
                for (Scriptlet scriptlet: testCase.getScriptlets().getScriptlet()) {
                    if (scriptlet.getId().equals(scriptletPath)) {
                        foundScriptlet = scriptlet;
                        break;
                    }
                }
            }
            if (foundScriptlet == null) {
                // Look also in the current test suite.
                foundScriptlet = lookupExternalScriptlet(null, testCase.getId(), path);
            }
        }
        if (foundScriptlet == null) {
            if (required) {
                if (from == null) {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Scriptlet definition ["+ scriptletPath+"] not found."));
                } else {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Scriptlet definition from ["+StringUtils.defaultString(from, "")+"] path ["+ scriptletPath+"] not found."));
                }
            }
        }
        return foundScriptlet;
    }

    public static void applyStopOnErrorSemantics(TestConstruct step, Boolean parentStopOnErrorSetting) {
        if (step != null) {
            boolean parentStopOnErrorSettingToUse = parentStopOnErrorSetting != null && parentStopOnErrorSetting;
            if (step.isStopOnError() == null) {
                // Inherit parent setting.
                step.setStopOnError(parentStopOnErrorSettingToUse);
            }
            if (step instanceof Sequence) {
                for (Object childStep: ((Sequence)step).getSteps()) {
                    if (childStep instanceof TestConstruct) {
                        applyStopOnErrorSemantics((TestConstruct)childStep, step.isStopOnError());
                    }
                }
            } else {
                // Cover also the steps that have internal sequences.
                if (step instanceof IfStep) {
                    applyStopOnErrorSemantics(((IfStep) step).getThen(), step.isStopOnError());
                    applyStopOnErrorSemantics(((IfStep) step).getElse(), step.isStopOnError());
                } else if (step instanceof WhileStep) {
                    applyStopOnErrorSemantics(((WhileStep) step).getDo(), step.isStopOnError());
                } else if (step instanceof ForEachStep) {
                    applyStopOnErrorSemantics(((ForEachStep) step).getDo(), step.isStopOnError());
                } else if (step instanceof RepeatUntilStep) {
                    applyStopOnErrorSemantics(((RepeatUntilStep) step).getDo(), step.isStopOnError());
                } else if (step instanceof FlowStep) {
                    if (((FlowStep) step).getThread() != null) {
                        for (Sequence thread: ((FlowStep) step).getThread()) {
                            applyStopOnErrorSemantics(thread, step.isStopOnError());
                        }
                    }
                }
            }
        }
    }

    public static void initialiseStepStatusMaps(MapType stepSuccessMap, MapType stepStatusMap, TestConstruct step) {
        if (step != null) {
            // Initialise for the step itself.
            if (step.getId() != null) {
                stepSuccessMap.addItem(step.getId(), new BooleanType(false));
                stepStatusMap.addItem(step.getId(), new StringType(""));
            }
            if (step instanceof Sequence) {
                // Initialise for children.
                for (Object childStep: ((Sequence)step).getSteps()) {
                    if (childStep instanceof TestConstruct) {
                        initialiseStepStatusMaps(stepSuccessMap, stepStatusMap, (TestConstruct)childStep);
                    }
                }
            } else {
                // Initialise for other steps with internal sequences.
                if (step instanceof IfStep) {
                    initialiseStepStatusMaps(stepSuccessMap, stepStatusMap, ((IfStep) step).getThen());
                    initialiseStepStatusMaps(stepSuccessMap, stepStatusMap, ((IfStep) step).getElse());
                } else if (step instanceof WhileStep) {
                    initialiseStepStatusMaps(stepSuccessMap, stepStatusMap, ((WhileStep) step).getDo());
                } else if (step instanceof ForEachStep) {
                    initialiseStepStatusMaps(stepSuccessMap, stepStatusMap, ((ForEachStep) step).getDo());
                } else if (step instanceof RepeatUntilStep) {
                    initialiseStepStatusMaps(stepSuccessMap, stepStatusMap, ((RepeatUntilStep) step).getDo());
                } else if (step instanceof FlowStep) {
                    if (((FlowStep) step).getThread() != null) {
                        for (Sequence thread: ((FlowStep) step).getThread()) {
                            initialiseStepStatusMaps(stepSuccessMap, stepStatusMap, thread);
                        }
                    }
                }
            }
        }
    }

    private static String extractStepName(Object step) {
        String name = step.getClass().getSimpleName();
        if (name.endsWith("Step")) {
            return name.substring(0, name.indexOf("Step"));
        } else {
            return name;
        }
    }

    public static String extractStepDescription(Object step, TestCaseScope scope) {
        String description = null;
        if (step instanceof TestStep) {
            description = ((TestStep) step).getDesc();
            if (description != null && description.isBlank()) {
                description = null;
            }
        }
        if (description == null) {
            description = extractStepName(step);
        } else {
            if (VariableResolver.isVariableReference(description)) {
                description = (String) new VariableResolver(scope).resolveVariable(description).convertTo(DataType.STRING_DATA_TYPE).getValue();
            }
        }
        return description;
    }

}

