package com.gitb.engine.actors.processors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import com.gitb.core.ErrorCode;
import com.gitb.core.StepStatus;
import com.gitb.ModuleManager;
import com.gitb.engine.commands.interaction.StartCommand;
import com.gitb.engine.commands.interaction.StopCommand;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.repository.ITestCaseRepository;
import com.gitb.tdl.*;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.MapType;
import com.gitb.utils.BindingUtils;
import com.gitb.utils.ErrorUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by serbay on 9/15/14.
 *
 * Call step executor actor
 */
public class CallStepProcessorActor extends AbstractTestStepActor<CallStep> {
	public static final String NAME = "call-s-p";

	private Scriptlet scriptlet;
	private TestCaseScope childScope;

	public CallStepProcessorActor(CallStep step, TestCaseScope scope, String stepId) {
		super(step, scope, stepId);
	}

	@Override
	protected void init() throws Exception {
		scriptlet = findScriptlet();
		childScope = createChildScope();
	}

	@Override
	protected void start() throws Exception {
		ActorRef child = SequenceProcessorActor.create(getContext(), scriptlet.getSteps(), childScope, stepId);

		StartCommand command = new StartCommand(scope.getContext().getSessionId());
		child.tell(command, self());

        report(StepStatus.PROCESSING);
    }

    private void report(StepStatus status) {
        updateTestStepStatus(getContext(), new StatusEvent(status), null, false);
    }

    @Override
	protected void stop() {
		StopCommand command = new StopCommand(scope.getContext().getSessionId());
		for(ActorRef child : getContext().getChildren()) {
			child.tell(command, self());
		}
	}

	@Override
	protected void handleStatusEvent(StatusEvent event) throws Exception {
//		super.handleStatusEvent(event);

		StepStatus status = event.getStatus();
		if(status == StepStatus.COMPLETED) {

			generateOutput();

			report(StepStatus.COMPLETED);
		} else if(status == StepStatus.ERROR) {
			childrenHasError();
		}
	}

	private Map<String, DataType> generateOutput() throws IOException {
		Map<String, DataType> elements;

		if(step.getOutput().size() > 0) {
			// Call step has id and output bindings are listed
			boolean isNameBinding = BindingUtils.isNameBinding(step.getOutput());

			if(isNameBinding) {
				elements = generateOutputWithNameBinding();
			} else {
				elements = generateOutputWithIndexBinding();
			}

			if(step.getId() != null) {
				setOutputWithId(elements);
			} else {
				setOutputWithElements(elements);
			}
		} else {
			 // Call step has id but output bindings are not listed
			elements = generateOutputWithScriptletOutputs();

			if(step.getId() != null) {
				setOutputWithId(elements);
			} else {
				throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Call step without an id and output bindings"));
			}
		}

		return elements;
	}

	private void setOutputWithElements(Map<String, DataType> elements) throws IOException {
		for (Map.Entry<String, DataType> entry : elements.entrySet()) {
			scope
				.getVariable(entry.getKey())
				.setValue(entry.getValue());
		}
	}

	private void setOutputWithId(Map<String, DataType> elements) {
		MapType outputs = createOutputMap(elements);
		scope
			.createVariable(step.getId())
			.setValue(outputs);
	}

	private Map<String, DataType> generateOutputWithScriptletOutputs() throws IOException {
		Map<String, DataType> elements = new HashMap<>();

		for (int i = 0; i < scriptlet.getOutput().size(); i++) {
			Binding scriptletOutput = scriptlet.getOutput().get(i);

			TestCaseScope.ScopedVariable variable = childScope.getVariable(scriptletOutput.getName());
			DataType result = variable.getValue();

			elements.put(scriptletOutput.getName(), result);
		}

		return elements;
	}

	private Map<String, DataType> generateOutputWithNameBinding() throws IOException {
		Map<String, DataType> elements = new HashMap<>();

		for (int i = 0; i < step.getOutput().size(); i++) {
			Binding output = step.getOutput().get(i);
			Binding scriptletOutput = getScriptletOutputBinding(output.getName());

			TestCaseScope.ScopedVariable variable = childScope.getVariable(scriptletOutput.getName());
			DataType result = variable.getValue();

			elements.put(output.getName(), result);
		}

		return elements;
	}

	private Map<String, DataType> generateOutputWithIndexBinding() throws IOException {
		Map<String, DataType> elements = new HashMap<>();

		for (int i = 0; i < step.getOutput().size(); i++) {
			Binding output = step.getOutput().get(i);
			Binding scriptletOutput = null;

			scriptletOutput = scriptlet.getOutput().get(i);

			TestCaseScope.ScopedVariable variable = childScope.getVariable(scriptletOutput.getName());
			DataType result = variable.getValue();

			elements.put(scriptletOutput.getName(), result);
		}

		return elements;
	}

	private MapType createOutputMap(Map<String, DataType> elements) {
		MapType outputs = new MapType();
		for (Map.Entry<String, DataType> entry : elements.entrySet()) {
			outputs.addItem(entry.getKey(), entry.getValue());
		}
		return outputs;
	}

	private Scriptlet findScriptlet() {
		TestCase testCase = scope.getContext().getTestCase();

		// find scriptlet in the test case (if it is inline)
		for(Scriptlet scriptlet : testCase.getScriptlets().getScriptlet()) {
			if(scriptlet.getId().equals(step.getPath())) {
				return scriptlet;
			}
		}

		// find the scriptlet in repositories
		ITestCaseRepository repository = ModuleManager.getInstance().getTestCaseRepository();
		if(repository.isScriptletAvailable(step.getPath())) {
			return repository.getScriptlet(step.getPath());
		}
		throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Scriptlet definition ["+ step.getPath()+"] cannot be found"));
	}

	private TestCaseScope createChildScope() throws Exception {
		if(scriptlet.getParams().getVar().size() != step.getInput().size()) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Wrong number of parameters for scriptlet ["+scriptlet.getId()+"]"));
		}
		if(step.getOutput().size() > 0 && scriptlet.getOutput().size() != step.getOutput().size()) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Wrong number of outputs for scriptlet["+scriptlet.getId()+"]"));
		}

		TestCaseScope childScope = scope.createChildScope();

		createScriptletVariables(childScope);

		return childScope;
	}

	private void createScriptletVariables(TestCaseScope childScope) {
		boolean isNameBinding = BindingUtils.isNameBinding(step.getInput());

		if(isNameBinding) {
			setInputWithNameBinding(childScope);
		} else {
			setInputWithIndexBinding(childScope);
		}

		for (Variable variable : scriptlet.getVariables().getVar()) {
			childScope
				.createVariable(variable.getName())
				.setValue(DataTypeFactory.getInstance().create(variable));
		}

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

		for (int i = 0; i < step.getInput().size(); i++) {
			Binding input = step.getInput().get(i);
			Variable variable = getScriptletInputVariable(input.getName());

			setInputVariable(childScope, expressionHandler, input, variable);
		}
	}

	private Variable getScriptletInputVariable(String name) {
		Variable variable = null;

		for(Variable v : scriptlet.getParams().getVar()) {
			if(v.getName().equals(name)) {
				variable = v;
				break;
			}
		}

		if(variable == null) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "No variables with name ["+name+"] are found"));
		}
		return variable;
	}

	private Binding getScriptletOutputBinding(String name) {
		for(Binding output : scriptlet.getOutput()) {
			if(output.getName().equals(name)) {
				return output;
			}
		}

		return null;
	}

	private void setInputVariable(TestCaseScope childScope, ExpressionHandler expressionHandler, Binding input, Variable variable) {
		DataType value = expressionHandler.processExpression(input, variable.getType());

		if(input.getName() == null) {
			childScope
				.createVariable(variable.getName())
				.setValue(value);
		} else {
			childScope
				.createVariable(input.getName())
				.setValue(value);
		}
	}

	public static ActorRef create(ActorContext context, CallStep step, TestCaseScope scope, String stepId) throws Exception {
		return create(CallStepProcessorActor.class, context, step, scope, stepId);
	}
}
