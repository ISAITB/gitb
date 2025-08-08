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

import com.gitb.engine.commands.interaction.StartCommand;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.tdl.ForEachStep;
import com.gitb.types.*;
import org.apache.pekko.actor.ActorRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * For each step executor actor.
 */
public class ForEachStepProcessorActor extends AbstractIterationStepActor<ForEachStep> {

	public static final String NAME = "foreach-s-p";

	private TestCaseScope childScope;
	private int iteration;
	private List<DataType> collectionToIterate;
	private boolean recordCounter = true;

	public ForEachStepProcessorActor(ForEachStep step, TestCaseScope scope, String stepId, StepContext stepContext) {
		super(step, scope, stepId, stepContext);
	}

	@Override
	protected void init() throws Exception {
		iteration = 0;
		if (step.getOf() != null) {
			VariableResolver resolver = new VariableResolver(scope);
			DataType referencedValue = resolver.resolveVariable(step.getOf());
			if (referencedValue instanceof ListType listType) {
				collectionToIterate = Collections.unmodifiableList(listType.getElements());
			} else if (referencedValue instanceof MapType mapType) {
				List<MapType> entries = new ArrayList<>();
				mapType.getItems().forEach((key, value) -> {
					var entry = new MapType();
					entry.addItem("key", new StringType(key));
					entry.addItem("value", value);
					entries.add(entry);
				});
				collectionToIterate = Collections.unmodifiableList(entries);
			} else if (referencedValue != null) {
				collectionToIterate = Collections.unmodifiableList(((ListType) referencedValue.convertTo(DataType.LIST_DATA_TYPE).getValue()).getElements());
			} else {
				throw new IllegalStateException("No variable could be found for expression ["+step.getOf()+"]");
			}
			recordCounter = !Objects.equals(step.getItem(), step.getCounter());
		} else {
			recordCounter = true;
		}
		childScope = createChildScope();
	}

	@Override
	protected void start() throws Exception {
		processing();
		if (!loop()) {
			completed();
		}
	}

	@Override
	protected boolean handleStatusEventInternal(StatusEvent event) throws Exception {
		return loop();
	}

	private boolean loop() throws Exception {
		checkIteration(iteration);
		int startIndex = getStartIndex();
		int endIndex = getEndIndex();
		if (iteration < endIndex - startIndex) {
			if (collectionToIterate != null && step.getItem() != null) {
				TestCaseScope.ScopedVariable item = childScope.createVariable(step.getItem());
				item.setValue(collectionToIterate.get(iteration));
			}
			if (recordCounter) {
				TestCaseScope.ScopedVariable counter = childScope.getVariable(step.getCounter());
				NumberType val = (NumberType) counter.getValue();
				val.setValue(startIndex + iteration);
			}
			iteration += 1;
			ActorRef child = SequenceProcessorActor.create(getContext(), step.getDo(), childScope, stepId + ITERATION_OPENING_TAG + (iteration + 1) + ITERATION_CLOSING_TAG, stepContext);
			child.tell(new StartCommand(scope.getContext().getSessionId()), self());
			return true;
		} else {
			return false;
		}
	}

	private int getStartIndex() {
		return getIndexNumber(step.getStart());
	}

	private int getEndIndex() {
		int endIndex;
		if (step.getEnd() != null) {
			endIndex = getIndexNumber(step.getEnd());
			if (collectionToIterate != null && endIndex > collectionToIterate.size()) {
				endIndex = collectionToIterate.size();
			}
		} else if (collectionToIterate != null) {
			endIndex = collectionToIterate.size();
		} else {
			throw new IllegalStateException("A foreach step must either have an 'end' attribute or reference a collection through the 'of' attribute");
		}
		return endIndex;
	}

	private int getIndexNumber(String expression) {
		int numberToReturn;
		if (expression != null) {
			VariableResolver resolver = new VariableResolver(scope);
			if (VariableResolver.isVariableReference(expression)) {
				numberToReturn = resolver.resolveVariableAsNumber(expression).intValue();
			} else {
				numberToReturn = Double.valueOf(expression).intValue();
			}
		} else {
			throw new IllegalStateException("Missing index expression");
		}
		return numberToReturn;
	}

	private TestCaseScope createChildScope() {
		TestCaseScope childScope = scope.createChildScope();
		if (recordCounter) {
			childScope.createVariable(step.getCounter()).setValue(new NumberType(getStartIndex()));
		}
		return childScope;
	}

	public static ActorRef create(ActorContext context, ForEachStep step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
		return create(ForEachStepProcessorActor.class, context, step, scope, stepId, stepContext);
	}
}
