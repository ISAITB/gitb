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

package com.gitb.engine.actors.processors;

import com.gitb.core.ErrorCode;
import com.gitb.core.StepStatus;
import com.gitb.engine.commands.interaction.StartCommand;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.ScriptletInfo;
import com.gitb.engine.utils.StepContext;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.*;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import com.gitb.utils.BindingUtils;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.ActorRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.util.*;

/**
 * Call step executor actor
 */
public class CallStepProcessorActor extends AbstractTestStepActor<CallStep> {
	public static final String NAME = "call-s-p";
	private static final Logger LOG = LoggerFactory.getLogger(CallStepProcessorActor.class);

	private Scriptlet scriptlet;
	private boolean standaloneScriptlet;
	private TestCaseScope childScope;

	public CallStepProcessorActor(CallStep step, TestCaseScope scope, String stepId, StepContext stepContext) {
		super(step, scope, stepId, stepContext);
	}

	@Override
	protected void init() throws Exception {
		ScriptletInfo scriptletInfo = findScriptlet();
		scriptlet = scriptletInfo.scriptlet();
		standaloneScriptlet = scriptletInfo.isStandalone();
	}

	@Override
	protected void start() throws Exception {
		childScope = createChildScope();
		TestCaseUtils.applyStopOnErrorSemantics(step, scriptlet.getSteps());
		TestCaseUtils.initialiseStepStatusMaps(getStepSuccessMap(), getStepStatusMap(), scriptlet.getSteps(), childScope);
		ActorRef child = SequenceProcessorActor.create(getContext(), scriptlet.getSteps(), childScope, stepId, stepContext);

		StartCommand command = new StartCommand(scope.getContext().getSessionId());
		child.tell(command, self());

        report(StepStatus.PROCESSING);
    }

    private void report(StepStatus status) {
        updateTestStepStatus(getContext(), new StatusEvent(status, childScope, self()), null, false);
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
		if (scope.getContext().getCurrentState() != TestCaseContext.TestCaseStateEnum.STOPPING && scope.getContext().getCurrentState() != TestCaseContext.TestCaseStateEnum.STOPPED) {
			Set<String> specificOutputsToReturn = new HashSet<>();
			for (var output: step.getOutput()) {
				if (StringUtils.isNotBlank(output.getName())) {
					if (specificOutputsToReturn.contains(output.getName())) {
						LOG.warn(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), "%s: Ignoring duplicate output [%s]".formatted(getScriptletErrorDescription(false), output.getName()));
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
							throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "%s: Scriptlet outputs must either define an expression or be provided with a name to match a variable in the scriptlet's scope.".formatted(getScriptletErrorDescription())));
						} else {
							TestCaseScope.ScopedVariable variable = childScope.getVariable(output.getName());
							if (variable == null) {
								throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "%s: Scriptlet output [%s] must either define an expression or match a variable in the scriptlet's scope.".formatted(getScriptletErrorDescription(), output.getName())));
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
				LOG.warn(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), "%s: Requested output [%s] not found in the scriptlet's outputs".formatted(getScriptletErrorDescription(false), unhandledOutput));
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
	}

	private void setOutputMap(Map<String, DataType> elements, String variableName) {
		MapType outputs = createOutputMap(elements);
		scope.createVariable(variableName).setValue(outputs);
	}

	private MapType createOutputMap(Map<String, DataType> elements) {
		MapType outputs = new MapType();
		for (Map.Entry<String, DataType> entry : elements.entrySet()) {
			if (!entry.getKey().isEmpty()) {
				outputs.addItem(entry.getKey(), entry.getValue());
			}
		}
		return outputs;
	}

	private ScriptletInfo findScriptlet() {
		String testSuiteContext = step.getFrom();
		if (testSuiteContext == null && scope.getTestSuiteContext() != null) {
			testSuiteContext = scope.getTestSuiteContext();
		}
		return scope.getContext().getScriptlet(testSuiteContext, step.getPath(), true);
	}

	private TestCaseScope createChildScope() {
		/*
		 * A standalone scriptlet (one that is not internal to a test case) should have scope isolation. Scope isolation
		 * means that:
		 * - Values can still be looked up from parent scopes.
		 * - Automatic variable assignments will never affect the parent scope (referring to the 'to' of assign steps. These
		 *   should be created in the current (child) scope.
		 */
		TestCaseScope childScope = scope.createChildScope(step.getId(), scriptlet.getImports(), scriptlet.getNamespaces(), step.getFrom(), standaloneScriptlet);
		createScriptletVariables(childScope);
		return childScope;
	}

	private void createScriptletVariables(TestCaseScope childScope) {
		// Parameters.
		if (scriptlet.getParams() != null && !scriptlet.getParams().getVar().isEmpty()) {
			var parameters = new HashSet<String>();
			scriptlet.getParams().getVar().forEach(variable -> parameters.add(variable.getName()));
			if (step.getInput().isEmpty()) {
				if (step.getInputAttribute() != null && scriptlet.getParams().getVar().size() == 1) {
					var variable = scriptlet.getParams().getVar().iterator().next();
					var resolver = new VariableResolver(scope);
					DataType value;
					if (VariableResolver.isVariableReference(step.getInputAttribute())) {
						var resolvedVariable = resolver.resolveVariable(step.getInputAttribute());
						value = DataTypeFactory.getInstance().create(resolvedVariable.getType());
						value.copyFrom(resolvedVariable);
					} else {
						value = new StringType(step.getInputAttribute());
					}
					value = value.convertTo(variable.getType());
					setContextVariable(childScope, value, variable.getName());
					parameters.remove(variable.getName());
				}
			} else {
				boolean isNameBinding = BindingUtils.isNameBinding(step.getInput());
				if (isNameBinding) {
					setInputWithNameBinding(childScope);
					step.getInput().forEach(input -> parameters.remove(input.getName()));
				} else {
					if (step.getInput().size() != scriptlet.getParams().getVar().size()) {
						throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "%s: Wrong number of parameters. Expected [%s] but encountered [%s].".formatted(getScriptletErrorDescription(), scriptlet.getParams().getVar().size(), step.getInput().size())));
					}
					setInputWithIndexBinding(childScope);
					parameters.clear();
				}
			}
			if (!parameters.isEmpty()) {
				// Consider also default values for parameters.
				scriptlet.getParams().getVar().forEach(parameter -> {
					if (parameters.contains(parameter.getName())) {
						if (parameter.isDefaultEmpty()) {
							setContextVariable(childScope, DataTypeFactory.getInstance().create(parameter.getType()), parameter.getName());
							parameters.remove(parameter.getName());
						} else if (!parameter.getValue().isEmpty()) {
							setContextVariable(childScope, DataTypeFactory.getInstance().create(parameter), parameter.getName());
							parameters.remove(parameter.getName());
						}
					}
				});
				// Remove optional parameters that have no provided values.
				scriptlet.getParams().getVar().stream().filter(InputParameter::isOptional).map(Variable::getName).toList().forEach(parameters::remove);
				if (!parameters.isEmpty()) {
					throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, String.format("%s: All required scriptlet parameters must either be provided explicitly via call inputs or have default values. No values could be determined for parameters [%s].", getScriptletErrorDescription(), StringUtils.join(parameters, ", "))));
				}
			}
		}
		// Variables.
		if (scriptlet.getVariables() != null) {
			for (Variable variable : scriptlet.getVariables().getVar()) {
				childScope.createVariable(variable.getName()).setValue(DataTypeFactory.getInstance().create(variable));
			}
		}
		// Outputs.
		for (Binding output : scriptlet.getOutput()) {
			childScope
				.createVariable(output.getName());
		}
	}

	private String getScriptletErrorDescription() {
		return getScriptletErrorDescription(true);
	}

	private String getScriptletErrorDescription(boolean isError) {
		var message = new StringBuilder();
		if (isError) {
			message.append("Error");
		} else {
			message.append("Warning");
		}
		message.append(" in call step");
		if (StringUtils.isNotBlank(step.getId())) {
			message.append(" with ID [%s]".formatted(step.getId()));
		}
		message.append(" for scriptlet with ID [%s] at path [%s]".formatted(scriptlet.getId(), step.getPath()));
		if (StringUtils.isNotBlank(step.getFrom())) {
			message.append(" from test suite [%s]".formatted(step.getFrom()));
		}
		return message.toString();
	}

	private void setInputWithIndexBinding(TestCaseScope childScope) {
		ExpressionHandler expressionHandler = new ExpressionHandler(scope);

		for (int i = 0; i < step.getInput().size(); i++) {
			Binding input = step.getInput().get(i);
			Variable variable = scriptlet.getParams().getVar().get(i);

			setContextVariableFromInput(childScope, expressionHandler, input, variable);
		}
	}

	private void setInputWithNameBinding(TestCaseScope childScope) {
		ExpressionHandler expressionHandler = new ExpressionHandler(scope);
		for (var input: step.getInput()) {
			Variable variable = getScriptletInputVariable(input.getName());
			setContextVariableFromInput(childScope, expressionHandler, input, variable);
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
				throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "%s: Scriptlet was called with an unexpected input [%s]. No inputs were expected to be provided.".formatted(getScriptletErrorDescription(), name)));
			} else {
				throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "%s: Scriptlet was called with an unexpected input [%s]. The scriptlet expected the following inputs [%s].".formatted(getScriptletErrorDescription(), name, String.join("|", expectedInputs))));
			}
		}
		return variable;
	}

	private void setContextVariableFromInput(TestCaseScope childScope, ExpressionHandler expressionHandler, Binding input, Variable variable) {
		DataType resolvedValue = expressionHandler.processExpression(input, variable.getType());
		DataType valueToSet = DataTypeFactory.getInstance().create(variable.getType());
		valueToSet.copyFrom(resolvedValue, variable.getType());
		String name = input.getName();
		if (name == null) {
			name = variable.getName();
		}
		setContextVariable(childScope, valueToSet, name);
	}

	private void setContextVariable(TestCaseScope childScope, DataType value, String variableName) {
		childScope.createVariable(variableName).setValue(value);
	}

	public static ActorRef create(ActorContext context, CallStep step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
		return create(CallStepProcessorActor.class, context, step, scope, stepId, stepContext);
	}
}
