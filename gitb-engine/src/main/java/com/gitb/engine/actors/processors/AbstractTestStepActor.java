package com.gitb.engine.actors.processors;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.japi.Creator;
import com.gitb.core.ErrorCode;
import com.gitb.core.StepStatus;
import com.gitb.core.TestRole;
import com.gitb.core.TestRoleEnumeration;
import com.gitb.engine.actors.Actor;
import com.gitb.engine.actors.SessionActor;
import com.gitb.engine.actors.util.ActorUtils;
import com.gitb.engine.commands.interaction.PrepareForStopCommand;
import com.gitb.engine.commands.interaction.RestartCommand;
import com.gitb.engine.commands.interaction.StartCommand;
import com.gitb.engine.commands.interaction.StopCommand;
import com.gitb.engine.events.TestStepStatusEventBus;
import com.gitb.engine.events.model.ErrorStatusEvent;
import com.gitb.engine.events.model.InputEvent;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.events.model.TestStepStatusEvent;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.IfStep;
import com.gitb.tdl.Sequence;
import com.gitb.tdl.TestConstruct;
import com.gitb.tr.*;
import com.gitb.types.BooleanType;
import com.gitb.types.MapType;
import com.gitb.utils.XMLDateTimeUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by serbay on 9/11/14.
 */
public abstract class AbstractTestStepActor<T> extends Actor {
	private static Logger logger = LoggerFactory.getLogger(AbstractTestStepActor.class);

	public static final String STEP_SEPARATOR = ".";
	public static final String EXCEPTION_SANITIZATION_EXPRESSION =  "(?:[a-z]+[a-z\\d_]*\\.)*(?:([A-Z]+\\S*)(?:Exception|Error|Fault))";

	protected final T step;
	protected final TestCaseScope scope;
	protected final String stepId;

	public AbstractTestStepActor(T step, TestCaseScope scope) {
		this.step = step;
		this.scope = scope;
		this.stepId = null;
	}

	public AbstractTestStepActor(T step, TestCaseScope scope, String stepId) {
		this.scope = scope;
		this.stepId = stepId;
		this.step = step;
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
			logger.error(addMarker(), "Processing caught an exception", e);
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
//				inform((StatusEvent) message);
				handleStatusEvent((StatusEvent) message);
			} else if (message instanceof StatusEvent) {
				handleStatusEvent((StatusEvent) message);
			} else if (message instanceof InputEvent) {
				handleInputEvent((InputEvent) message);
			} else if (message instanceof StartCommand) {
				start();
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
			logger.error(addMarker(), "Processing caught an exception", e);
			error(e);
		}
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

	protected void completed() {
		completed(null);
	}

	protected void completed(TestStepReportType testStepReport) {
		updateTestStepStatus(StepStatus.COMPLETED, testStepReport);
	}

	protected void childrenHasWarning() {
		updateTestStepStatus(new StatusEvent(StepStatus.WARNING), null);
	}

	protected void childrenHasError() {
		updateTestStepStatus(new StatusEvent(StepStatus.ERROR), null);
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
		ErrorStatusEvent statusEvent = new ErrorStatusEvent(throwable);

		updateTestStepStatus(statusEvent, report);
	}

	protected void updateTestStepStatus(StepStatus status, TestStepReportType report) {
		StatusEvent statusEvent = new StatusEvent(status);
		updateTestStepStatus(statusEvent, report);
	}

	protected void updateTestStepStatus(StatusEvent statusEvent, TestStepReportType report) {
		updateTestStepStatus(getContext(), statusEvent, report);
	}

	protected void updateTestStepStatus(ActorContext context, StepStatus status, TestStepReportType report) {
		StatusEvent statusEvent = new StatusEvent(status);
		updateTestStepStatus(context, statusEvent, report);
	}

	protected void updateTestStepStatus(ActorContext context, StatusEvent statusEvent, TestStepReportType report) {
		updateTestStepStatus(context, statusEvent, report, true);
	}

	protected void updateTestStepStatus(ActorContext context, StatusEvent statusEvent, TestStepReportType report, boolean reportTestStepStatus) {
		updateTestStepStatus(context, statusEvent, report, reportTestStepStatus, true);
	}

	protected void updateTestStepStatus(ActorContext context, StatusEvent statusEvent, TestStepReportType report, boolean reportTestStepStatus, boolean logError) {

		if (logError && statusEvent instanceof ErrorStatusEvent) {
			logger.error(addMarker(), "An error status received in [" + stepId + "]", ((ErrorStatusEvent) statusEvent).getException());
		}

		StepStatus status = statusEvent.getStatus();
		context.parent().tell(statusEvent, self());

		if (status == StepStatus.COMPLETED || status == StepStatus.ERROR || status == StepStatus.SKIPPED || status == StepStatus.WARNING) {
			self().tell(PoisonPill.getInstance(), self());
//			if (statusEvent instanceof ErrorStatusEvent) {
//				logger.debug(String.format("Stopping execution of the test step actor [%s] of session [%s].", stepId, scope.getContext().getSessionId()));
//				self().tell(PoisonPill.getInstance(), self());
//			}

			if (report == null) {
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
		}

		if (stepId == null || stepId.isEmpty()) {
			return;
		}

		boolean stopTestSession = false;
		if (step instanceof TestConstruct || step instanceof Sequence) {
			boolean stopOnError = false;
			if (step instanceof TestConstruct) {
				stopOnError = ((TestConstruct)step).isStopOnError();
				if (((TestConstruct)step).getId() != null) {
					((MapType)(scope.getVariable(TestCaseContext.STEP_SUCCESS_MAP, true).getValue())).addItem(((TestConstruct)step).getId(), new BooleanType(status == StepStatus.COMPLETED || status == StepStatus.WARNING));
				}
			}
			if (step instanceof Sequence) {
				stopOnError = ((Sequence)step).isStopOnError();
			}
			if (status == StepStatus.ERROR && stopOnError) {
				// We need to stop the complete test session. We only do this if not already signalled.
				if (scope.getContext().getCurrentState() != TestCaseContext.TestCaseStateEnum.STOPPING && scope.getContext().getCurrentState() != TestCaseContext.TestCaseStateEnum.STOPPED) {
					scope.getContext().setCurrentState(TestCaseContext.TestCaseStateEnum.STOPPING);
					stopTestSession = true;
				}
			}
		}
		if (reportTestStepStatus) {
			final TestStepStatusEvent event = new TestStepStatusEvent(scope.getContext().getSessionId(), stepId, status, report, self());
			TestStepStatusEventBus
					.getInstance()
					.publish(event);
		}
		if (stopTestSession) {
			try {
				getContext().system().actorSelection(SessionActor.getPath(scope.getContext().getSessionId())).tell(new PrepareForStopCommand(scope.getContext().getSessionId(), self()), self());
			} catch (Exception e) {
				logger.error(addMarker(), "Error sending the signal to stop the test session from test step actor [" + stepId + "].");
			}
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
			logger.error(addMarker(), "An error occurred while trying to construct a report for exception [" + ((ErrorStatusEvent) statusEvent).getException() + "]", e);
		}
		return report;
	}

	public static <T, S extends AbstractTestStepActor<T>> ActorRef create(final Class<S> clazz, ActorContext context, T step, TestCaseScope scope, String stepId) throws Exception {
		String name = (String) FieldUtils.readStaticField(clazz, "NAME");
		if (name == null) {
			return context.actorOf(props(clazz, step, scope, stepId));
		} else {
			return context.actorOf(props(clazz, step, scope, stepId), getName(name));
		}
	}

	public static <T, S extends AbstractTestStepActor<T>> Props props(final Class<S> clazz, final T step, final TestCaseScope scope, final String stepId) throws Exception {
		return Props.create(clazz, (Creator<S>) () -> ConstructorUtils.invokeConstructor(clazz, step, scope, stepId));
	}

	public static String getName(String actorName) {
		return actorName + "-" + RandomStringUtils.random(5, true, true);
	}

	TestRole getSUTActor() {
		return getSUTActors().get(0);
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
			updateTestStepStatus(getContext(), new StatusEvent(StepStatus.SKIPPED), null, true, false);
		} else {
			// Unexpected error.
			updateTestStepStatus(getContext(), new ErrorStatusEvent(failure), null, true, true);
		}
	}

}
