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

import com.gitb.common.AliasManager;
import com.gitb.core.ErrorCode;
import com.gitb.core.StepStatus;
import com.gitb.core.TestRole;
import com.gitb.core.TestRoleEnumeration;
import com.gitb.engine.PropertyConstants;
import com.gitb.engine.actors.Actor;
import com.gitb.engine.commands.interaction.PrepareForStopCommand;
import com.gitb.engine.commands.interaction.RestartCommand;
import com.gitb.engine.commands.interaction.StartCommand;
import com.gitb.engine.commands.interaction.StopCommand;
import com.gitb.engine.events.TestStepStatusEventBus;
import com.gitb.engine.events.model.ErrorStatusEvent;
import com.gitb.engine.events.model.InputEvent;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.events.model.TestStepStatusEvent;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.Assign;
import com.gitb.tdl.IfStep;
import com.gitb.tdl.Log;
import com.gitb.tdl.TestConstruct;
import com.gitb.tr.*;
import com.gitb.types.MapType;
import com.gitb.utils.XMLDateTimeUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Props;
import org.apache.pekko.japi.Creator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created by serbay on 9/11/14.
 */
public abstract class AbstractTestStepActor<T> extends Actor {
	private static final Logger logger = LoggerFactory.getLogger(AbstractTestStepActor.class);

	public static final String STEP_SEPARATOR = ".";
	public static final String EXCEPTION_SANITIZATION_EXPRESSION =  "(?:[a-z]+[a-z\\d_]*\\.)*(?:([A-Z]+\\S*)(?:Exception|Error|Fault))";

	protected final T step;
	protected final TestCaseScope scope;
	protected final String stepId;
	protected final StepContext stepContext;

	public AbstractTestStepActor(T step, TestCaseScope scope, String stepId, StepContext stepContext) {
		this.scope = scope;
		this.stepId = stepId;
		this.step = step;
		this.stepContext = stepContext;
	}

	protected Marker addMarker() {
		return MarkerFactory.getDetachedMarker(scope.getContext().getSessionId());
	}

	/**
	 * Initialize the Processor in this function
	 *
	 * @throws Exception
	 */
	protected abstract void init() throws Exception;

	/**
	 * Put your processing logic in this function
	 *
	 * @throws Exception
	 */
	protected abstract void start() throws Exception;

	/**
	 * Stop your execution
	 */
	protected void stop() {
		// Do nothing by default
	}

	/**
	 * Prepare to stop the execution
	 */
	protected void prepareForStop(PrepareForStopCommand command) {
		for (ActorRef child : getContext().getChildren()) {
			try {
				if (!child.equals(command.getOriginalSource())) {
					child.tell(command, self());
				}
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
		reactToPrepareForStop();
	}

	protected void reactToPrepareForStop() {
		stop();
	}

	/**
	 * If step is waiting input in some phase, implement this to handle the input event
	 *
	 * @param event
	 * @throws Exception
	 */
	protected void handleInputEvent(InputEvent event) throws Exception {
	}

	/**
	 * If step is a container step waiting status events from its children, implement this
	 *
	 * @param event
	 * @throws Exception
	 */
	protected void handleStatusEvent(StatusEvent event) throws Exception {
        inform(event);
	}

	/**
	 * Initialization of processor before any processing occurs
	 * (Subscribe to events)
	 */
	@Override
	public void preStart() {
		try {
			super.preStart();
			init();
		} catch (Exception e) {
			error(e);
		}
	}

	/**
	 * Commands and Events for TestStep Processors
	 *
	 * @param message
	 */
	@Override
	public void onReceive(Object message) {
		try {
			super.onReceive(message);
			if (message instanceof ErrorStatusEvent) {
				handleStatusEvent((StatusEvent) message);
			} else if (message instanceof StatusEvent) {
				handleStatusEvent((StatusEvent) message);
			} else if (message instanceof InputEvent) {
				handleInputEvent((InputEvent) message);
			} else if (message instanceof StartCommand) {
				if (skipStep()) {
					skipped();
				} else {
					start();
				}
			} else if (message instanceof StopCommand) {
				stop();
			} else if (message instanceof PrepareForStopCommand) {
				prepareForStop((PrepareForStopCommand)message);
			} else if (message instanceof RestartCommand) {
				stop();
				init();
				start();
			} else {
				throw new GITBEngineInternalError("Invalid command [" + message.getClass().getName() + "]");
			}
		} catch (Exception e) {
			error(e);
		}
	}

	protected boolean skipStep() {
		String flagToCheck = null;
		if (step instanceof TestConstruct testConstruct) {
			flagToCheck = testConstruct.getSkipped();
		} else if (step instanceof Assign assignStep) {
			flagToCheck = assignStep.getSkipped();
		} else if (step instanceof Log logStep) {
			flagToCheck = logStep.getSkipped();
		}
		return TestCaseUtils.resolveBooleanFlag(flagToCheck, false, () -> new VariableResolver(scope));
	}

	protected void waiting() {
		waiting(null);
	}

	protected void waiting(TestStepReportType testStepReport) {
		updateTestStepStatus(StepStatus.WAITING, testStepReport);
	}

	protected void processing() {
		processing(null);
	}

	protected void processing(TestStepReportType testStepReport) {
		updateTestStepStatus(StepStatus.PROCESSING, testStepReport);
	}

	protected void skipped() {
		updateTestStepStatus(StepStatus.SKIPPED, null);
	}

	protected void completed() {
		completed(null);
	}

	protected void completed(TestStepReportType testStepReport) {
		updateTestStepStatus(StepStatus.COMPLETED, testStepReport);
	}

	protected void childrenHasWarning() {
		updateTestStepStatus(new StatusEvent(StepStatus.WARNING, scope, self()), null);
	}

	protected void childrenHasError() {
		updateTestStepStatus(new StatusEvent(StepStatus.ERROR, scope, self()), null);
	}

	protected void error(Throwable throwable) {
		error(throwable, null);
	}

	protected void error(Throwable throwable, TestStepReportType testStepReport) {
		updateTestStepStatus(throwable, testStepReport);
	}

	protected void inform(StepStatus status) {
		inform(status, null);
	}

	protected void inform(StepStatus status, TestStepReportType testStepReport) {
		updateTestStepStatus(status, testStepReport);
	}

	protected void inform(StatusEvent event) {
		inform(event, null);
	}

	protected void inform(StatusEvent event, TestStepReportType testStepReport) {
		updateTestStepStatus(event, testStepReport);
	}

	protected void updateTestStepStatus(Throwable throwable, TestStepReportType report) {
		ErrorStatusEvent statusEvent = new ErrorStatusEvent(throwable, scope, self());

		updateTestStepStatus(statusEvent, report);
	}

	protected void updateTestStepStatus(StepStatus status, TestStepReportType report) {
		StatusEvent statusEvent = new StatusEvent(status, scope, self());
		updateTestStepStatus(statusEvent, report);
	}

	protected void updateTestStepStatus(StatusEvent statusEvent, TestStepReportType report) {
		updateTestStepStatus(getContext(), statusEvent, report);
	}

	protected void updateTestStepStatus(ActorContext context, StepStatus status, TestStepReportType report) {
		StatusEvent statusEvent = new StatusEvent(status, scope, self());
		updateTestStepStatus(context, statusEvent, report);
	}

	protected void updateTestStepStatus(ActorContext context, StatusEvent statusEvent, TestStepReportType report) {
		updateTestStepStatus(context, statusEvent, report, true);
	}

	protected void updateTestStepStatus(ActorContext context, StatusEvent statusEvent, TestStepReportType report, boolean reportTestStepStatus) {
		updateTestStepStatus(context, statusEvent, report, reportTestStepStatus, true);
	}

	protected MapType getStepSuccessMap() {
		return ((MapType)(scope.getVariable(PropertyConstants.STEP_SUCCESS_MAP, true).getValue()));
	}

	protected MapType getStepStatusMap() {
		return ((MapType)(scope.getVariable(PropertyConstants.STEP_STATUS_MAP, true).getValue()));
	}

	protected void updateStepStatusMaps(StepStatus status) {
		if (((TestConstruct)step).getId() != null) {
			TestCaseUtils.updateStepStatusMaps(getStepSuccessMap(), getStepStatusMap(), (TestConstruct) step, scope, status);
		}
	}

	protected void updateTestStepStatus(ActorContext context, StatusEvent statusEvent, TestStepReportType report, boolean reportTestStepStatus, boolean logError) {

		if (logError && statusEvent instanceof ErrorStatusEvent) {
			logger.error(addMarker(), String.format("Unexpected error - step [%s] - ID [%s]", TestCaseUtils.extractStepDescription(step, statusEvent.getScope()), stepId), ((ErrorStatusEvent) statusEvent).getException());
		}

		StepStatus status = statusEvent.getStatus();
		boolean isEndStatus = status == StepStatus.COMPLETED || status == StepStatus.ERROR || status == StepStatus.SKIPPED || status == StepStatus.WARNING;

		if (step instanceof TestConstruct) {
			updateStepStatusMaps(status);
		}

		// Notify the parent step.
		context.parent().tell(statusEvent, self());
		// Complete the current step's report for feedback (if needed).
		if (reportTestStepStatus && stepId != null && !stepId.isEmpty()) {
			if (isEndStatus && report == null) {
				switch (status) {
					case COMPLETED:
						report = constructDefaultReport(TestResultType.SUCCESS);
						break;
					case SKIPPED:
						report = constructDefaultReport(TestResultType.UNDEFINED);
						break;
					case WARNING:
						report = constructDefaultReport(TestResultType.WARNING);
						break;
					case ERROR:
						if (statusEvent instanceof ErrorStatusEvent
								&& ((ErrorStatusEvent) statusEvent).getException() != null) {
							report = constructDefaultErrorReport((ErrorStatusEvent) statusEvent);
						} else {
							report = constructDefaultReport(TestResultType.FAILURE);
						}
						break;
				}
			}
			final TestStepStatusEvent event = new TestStepStatusEvent(scope.getContext().getSessionId(), stepId, status, report, self(), step, scope);
			TestStepStatusEventBus.getInstance().publish(event);
		}
		// If this is the final status for the step stop the actor.
		if (isEndStatus) {
			self().tell(PoisonPill.getInstance(), self());
		}
	}

	protected TestStepReportType constructDefaultReport(TestResultType resultType) {
		TestStepReportType report = null;

		try {
			if (step instanceof IfStep) {
				report = new DR();
			} else {
				report = new SR();
			}
			report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
			report.setResult(resultType);

		} catch (DatatypeConfigurationException e) {
			logger.error(addMarker(), "An error occurred while trying to construct a completed report for [" + step + "] with id [" + stepId + "]");
		}

		return report;
	}

	protected TestStepReportType constructDefaultErrorReport(ErrorStatusEvent statusEvent) {
		TAR report = new TAR();
		try {
			ObjectFactory trObjectFactory = new ObjectFactory();
			report.setResult(TestResultType.FAILURE);
            report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
			report.setReports(new TestAssertionGroupReportsType());

			BAR error = new BAR();
			Exception exception = (Exception) statusEvent.getException();
			String message;
			if (exception instanceof GITBEngineInternalError && exception.getCause() != null) {
				message = exception.getCause().getMessage();
			} else {
				message = exception.getMessage();
			}
			if (message != null) {
				error.setDescription(message.replaceAll(EXCEPTION_SANITIZATION_EXPRESSION, "$1"));
			}

			report.getReports().getInfoOrWarningOrError().add(trObjectFactory.createTestAssertionGroupReportsTypeError(error));

		} catch (DatatypeConfigurationException e) {
			logger.error(addMarker(), "An error occurred while trying to construct a report for exception [" + statusEvent.getException() + "]", e);
		}
		return report;
	}

	public static <T, S extends AbstractTestStepActor<T>> ActorRef create(final Class<S> clazz, ActorContext context, T step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
		String name = (String) FieldUtils.readStaticField(clazz, "NAME");
		if (name == null) {
			return context.actorOf(props(clazz, step, scope, stepId, stepContext));
		} else {
			return context.actorOf(props(clazz, step, scope, stepId, stepContext), getName(name));
		}
	}

	public static <T, S extends AbstractTestStepActor<T>> Props props(final Class<S> clazz, final T step, final TestCaseScope scope, final String stepId) throws Exception {
		return Props.create(clazz, (Creator<S>) () -> ConstructorUtils.invokeConstructor(clazz, step, scope, stepId));
	}

	public static <T, S extends AbstractTestStepActor<T>> Props props(final Class<S> clazz, final T step, final TestCaseScope scope, final String stepId, final StepContext stepContext) throws Exception {
		return Props.create(clazz, (Creator<S>) () -> ConstructorUtils.invokeConstructor(clazz, step, scope, stepId, stepContext));
	}

	public static String getName(String actorName) {
		return actorName + "-" + RandomStringUtils.secure().next(5, true, true);
	}

	TestRole getSUTActor() {
		return getSUTActors().getFirst();
	}

	List<TestRole> getSUTActors() {
		List<TestRole> sutActors = new ArrayList<>();
		if (scope.getContext().getTestCase() != null && scope.getContext().getTestCase().getActors() != null && scope.getContext().getTestCase().getActors().getActor() != null) {
			for (TestRole role: scope.getContext().getTestCase().getActors().getActor()) {
				if (role.getRole() == TestRoleEnumeration.SUT) {
					sutActors.add(role);
				}
			}
		}
		return Collections.unmodifiableList(sutActors);
	}

	protected void handleFutureFailure(Throwable failure) {
		if (failure instanceof GITBEngineInternalError && ((GITBEngineInternalError)failure).getErrorInfo() != null && ((GITBEngineInternalError)failure).getErrorInfo().getErrorCode() == ErrorCode.CANCELLATION) {
			// This is a cancelled step due to a session being stopped.
			updateTestStepStatus(getContext(), new StatusEvent(StepStatus.SKIPPED, scope, self()), null, true, false);
		} else {
			// Unexpected error.
			updateTestStepStatus(getContext(), new ErrorStatusEvent(failure, scope, self()), null, true, true);
		}
	}

	protected String resolveProcessingHandler(String handler, Supplier<VariableResolver> variableResolverSupplier) {
		String handlerIdentifier = handler;
		if (VariableResolver.isVariableReference(handlerIdentifier)) {
			handlerIdentifier = variableResolverSupplier.get().resolveVariableAsString(handlerIdentifier).toString();
		}
		return AliasManager.getInstance().resolveProcessingHandler(handlerIdentifier);
	}

}
