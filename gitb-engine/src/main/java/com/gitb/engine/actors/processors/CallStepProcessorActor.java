package com.gitb.engine.actors.processors;

import akka.actor.ActorRef;
import com.gitb.core.ErrorCode;
import com.gitb.core.StepStatus;
import com.gitb.engine.commands.interaction.StartCommand;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.Binding;
import com.gitb.tdl.CallStep;
import com.gitb.tdl.Scriptlet;
import com.gitb.tdl.Variable;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import com.gitb.utils.BindingUtils;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.util.*;

/**
 * Created by serbay on 9/15/14.
 *
 * Call step executor actor
 */
public class CallStepProcessorActor extends AbstractTestStepActor<CallStep> {
	public static final String NAME = "call-s-p";
	private static final Logger LOG = LoggerFactory.getLogger(CallStepProcessorActor.class);

	private Scriptlet scriptlet;
	private TestCaseScope childScope;

	public CallStepProcessorActor(CallStep step, TestCaseScope scope, String stepId) {
		super(step, scope, stepId);
	}

	@Override
	protected void init() throws Exception {
		scriptlet = findScriptlet();
	}

	@Override
	protected void start() throws Exception {
		childScope = createChildScope();
		ActorRef child = SequenceProcessorActor.create(getContext(), scriptlet.getSteps(), childScope, stepId);

		StartCommand command = new StartCommand(scope.getContext().getSessionId());
		child.tell(command, self());

        report(StepStatus.PROCESSING);
    }

    private void report(StepStatus status) {
        updateTestStepStatus(getContext(), new StatusEvent(status), null, false);
    }

	@Override
	protected void handleStatusEvent(StatusEvent event) throws Exception {
		StepStatus status = event.getStatus();
		if (status == StepStatus.COMPLETED || status == StepStatus.ERROR || status == StepStatus.WARNING) {
			generateOutput();
			if (status == StepStatus.COMPLETED) {
				report(StepStatus.COMPLETED);
			} else if(status == StepStatus.ERROR) {
				childrenHasError();
			} else { // WARNING
				childrenHasWarning();
			}
		}
	}

	private void generateOutput() {
		Set<String> specificOutputsToReturn = new HashSet<>();
		for (var output: step.getOutput()) {
			if (StringUtils.isNotBlank(output.getName())) {
				if (specificOutputsToReturn.contains(output.getName())) {
					LOG.warn(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), String.format("Ignoring duplicate output [%s] - step [%s] - ID [%s]", output.getName(), ErrorUtils.extractStepDescription(step), stepId));
				} else {
					specificOutputsToReturn.add(output.getName());
				}
			}
		}
		ExpressionHandler expressionHandler = new ExpressionHandler(childScope);
		Map<String, DataType> elements = new HashMap<>();
		for (var output: scriptlet.getOutput()) {
			if (specificOutputsToReturn.isEmpty() || (output.getName() != null && specificOutputsToReturn.contains(output.getName()))) {
				// Add the scriptlet output to the call outputs.
				DataType result;
				if (StringUtils.isBlank(output.getValue())) {
					if (output.getName() == null) {
						throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, String.format("Scriptlet outputs must either define an expression or be provided with a name to match a variable in the scriptlet's scope - step [%s] - ID [%s]", ErrorUtils.extractStepDescription(step), stepId)));
					} else {
						TestCaseScope.ScopedVariable variable = childScope.getVariable(output.getName());
						if (variable == null) {
							throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, String.format("Scriptlet output [%s] must either define an expression or match a variable in the scriptlet's scope - step [%s] - ID [%s]", output.getName(), ErrorUtils.extractStepDescription(step), stepId)));
						}
						result = variable.getValue();
					}
				} else {
					// Output defines an expression. Use it to define value.
					result = expressionHandler.processExpression(output);
				}
				elements.put(output.getName() == null?"":output.getName(), result);
			}
		}
		// Log a warning for any expected outputs that were not returned.
		specificOutputsToReturn.removeAll(elements.keySet());
		for (var unhandledOutput: specificOutputsToReturn) {
			LOG.warn(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), String.format("Requested output [%s] not found in the scriptlet's outputs - step [%s] - ID [%s]", unhandledOutput, ErrorUtils.extractStepDescription(step), stepId));
		}
		if (step.getId() != null) {
			setOutputMap(elements, step.getId());
		}
		if (step.getOutputAttribute() != null) {
			if (elements.size() > 1) {
				// Set as map.
				setOutputMap(elements, step.getOutputAttribute());
			} else if (elements.size() == 1) {
				// Set as direct result.
				scope.createVariable(step.getOutputAttribute()).setValue(elements.values().iterator().next());
			}
		}
	}

	private void setOutputMap(Map<String, DataType> elements, String variableName) {
		MapType outputs = createOutputMap(elements);
		scope.createVariable(variableName).setValue(outputs);
	}

	private MapType createOutputMap(Map<String, DataType> elements) {
		MapType outputs = new MapType();
		for (Map.Entry<String, DataType> entry : elements.entrySet()) {
			if (!entry.getKey().equals("")) {
				outputs.addItem(entry.getKey(), entry.getValue());
			}
		}
		return outputs;
	}

	private Scriptlet findScriptlet() {
		String testSuiteContext = step.getFrom();
		if (testSuiteContext == null && scope.getTestSuiteContext() != null) {
			testSuiteContext = scope.getTestSuiteContext();
		}
		return scope.getContext().getScriptlet(testSuiteContext, step.getPath(), true);
	}

	private TestCaseScope createChildScope() {
		int parameterCount = 0;
		if (scriptlet.getParams() != null) {
			parameterCount = scriptlet.getParams().getVar().size();
		}
		var inputCount = step.getInput().size();
		if (inputCount == 0 && step.getInputAttribute() != null) {
			inputCount = 1;
		}
		if (parameterCount != inputCount) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Wrong number of parameters for scriptlet ["+scriptlet.getId()+"]. Expected ["+parameterCount+"] but encountered ["+step.getInput().size()+"]."));
		}
		TestCaseScope childScope = scope.createChildScope(scriptlet.getImports(), step.getFrom());
		createScriptletVariables(childScope);
		return childScope;
	}

	private void createScriptletVariables(TestCaseScope childScope) {
		// Parameters.
		if (step.getInput().isEmpty()) {
			if (step.getInputAttribute() != null && scriptlet.getParams() != null && scriptlet.getParams().getVar().size() == 1) {
				var variable = scriptlet.getParams().getVar().iterator().next();
				var resolver = new VariableResolver(scope);
				DataType value;
				if (resolver.isVariableReference(step.getInputAttribute())) {
					var resolvedVariable = resolver.resolveVariable(step.getInputAttribute());
					value = DataTypeFactory.getInstance().create(resolvedVariable.getType());
					value.copyFrom(resolvedVariable);
				} else {
					value = new StringType(step.getInputAttribute());
				}
				value = value.convertTo(variable.getType());
				setInputVariable(childScope, value, null, variable);
			}
		} else {
			boolean isNameBinding = BindingUtils.isNameBinding(step.getInput());
			if (isNameBinding) {
				setInputWithNameBinding(childScope);
			} else {
				setInputWithIndexBinding(childScope);
			}
		}
		// Variables.
		if (scriptlet.getVariables() != null) {
			for (Variable variable : scriptlet.getVariables().getVar()) {
				childScope
						.createVariable(variable.getName())
						.setValue(DataTypeFactory.getInstance().create(variable));
			}
		}
		// Outputs.
		for (Binding output : scriptlet.getOutput()) {
			childScope
				.createVariable(output.getName());
		}
	}

	private void setInputWithIndexBinding(TestCaseScope childScope) {
		ExpressionHandler expressionHandler = new ExpressionHandler(scope);

		for (int i = 0; i < step.getInput().size(); i++) {
			Binding input = step.getInput().get(i);
			Variable variable = scriptlet.getParams().getVar().get(i);

			setInputVariable(childScope, expressionHandler, input, variable);
		}
	}

	private void setInputWithNameBinding(TestCaseScope childScope) {
		ExpressionHandler expressionHandler = new ExpressionHandler(scope);
		for (var input: step.getInput()) {
			Variable variable = getScriptletInputVariable(input.getName());
			setInputVariable(childScope, expressionHandler, input, variable);
		}
	}

	private Variable getScriptletInputVariable(String name) {
		Variable variable = null;

		for (Variable v : scriptlet.getParams().getVar()) {
			if(v.getName().equals(name)) {
				variable = v;
				break;
			}
		}

		if (variable == null) {
			Set<String> expectedInputs = new TreeSet<>();
			scriptlet.getParams().getVar().forEach((v) -> expectedInputs.add(v.getName()));
			if (expectedInputs.isEmpty()) {
				throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Scriptlet was called with an unexpected input ["+name+"]. No inputs were expected to be provided."));
			} else {
				throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Scriptlet was called with an unexpected input ["+name+"]. The scriptlet expected the following inputs ["+ String.join("|", expectedInputs) +"]."));
			}
		}
		return variable;
	}

	private void setInputVariable(TestCaseScope childScope, ExpressionHandler expressionHandler, Binding input, Variable variable) {
		DataType resolvedValue = expressionHandler.processExpression(input, variable.getType());
		DataType valueToSet = DataTypeFactory.getInstance().create(variable.getType());
		valueToSet.copyFrom(resolvedValue, variable.getType());
		setInputVariable(childScope, valueToSet, input.getName(), variable);
	}

	private void setInputVariable(TestCaseScope childScope, DataType value, String inputName, Variable variable) {
		if (inputName == null) {
			childScope
					.createVariable(variable.getName())
					.setValue(value);
		} else {
			childScope
					.createVariable(inputName)
					.setValue(value);
		}
	}

	public static ActorRef create(ActorContext context, CallStep step, TestCaseScope scope, String stepId) throws Exception {
		return create(CallStepProcessorActor.class, context, step, scope, stepId);
	}
}
