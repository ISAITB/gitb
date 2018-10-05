package com.gitb.engine.actors.processors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.japi.Creator;
import com.gitb.core.StepStatus;
import com.gitb.core.TestRole;
import com.gitb.core.TestRoleEnumeration;
import com.gitb.engine.actors.Actor;
import com.gitb.engine.commands.interaction.RestartCommand;
import com.gitb.engine.commands.interaction.StartCommand;
import com.gitb.engine.commands.interaction.StopCommand;
import com.gitb.engine.events.TestStepStatusEventBus;
import com.gitb.engine.events.model.ErrorStatusEvent;
import com.gitb.engine.events.model.InputEvent;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.events.model.TestStepStatusEvent;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.*;
import com.gitb.tr.*;
import com.gitb.tr.ObjectFactory;
import com.gitb.utils.XMLDateTimeUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	protected abstract void stop();

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
			logger.error("" + this + " caught an exception", e);
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
		super.onReceive(message);
		try {
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
			} else if (message instanceof RestartCommand) {
				stop();
				init();
				start();
			} else {
				throw new GITBEngineInternalError("Invalid command [" + message.getClass().getName() + "]");
			}
		} catch (Exception e) {
			logger.error("" + this + " caught an exception", e);
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
			logger.error("An error status received in [" + stepId + "]", ((ErrorStatusEvent) statusEvent).getException());
		}

		StepStatus status = statusEvent.getStatus();
		context.parent().tell(statusEvent, self());

		if (status == StepStatus.COMPLETED || status == StepStatus.ERROR) {
			if (statusEvent instanceof ErrorStatusEvent) {
				logger.debug("An error status event is received. Stopping execution of the test step actor [" + stepId + "].");

				self().tell(new StopCommand(scope.getContext().getSessionId()), self());
				self().tell(PoisonPill.getInstance(), self());
			}

			if (report == null) {
				switch (status) {
					case COMPLETED:
						report = constructDefaultCompletedReport();
						break;
					case ERROR:
						if(statusEvent instanceof ErrorStatusEvent
							&& ((ErrorStatusEvent) statusEvent).getException() != null) {
							report = constructDefaultErrorReport((ErrorStatusEvent) statusEvent);
						}
						break;
				}
			}
		}

		if (stepId == null || stepId.isEmpty()) {
			return;
		}

		if (reportTestStepStatus) {
			final TestStepStatusEvent event = new TestStepStatusEvent(scope.getContext().getSessionId(), stepId, status, report, self());

			TestStepStatusEventBus
				.getInstance()
				.publish(event);
		}
	}

	protected TestStepReportType constructDefaultCompletedReport() {
		TestStepReportType report = null;

		try {
			if (step instanceof IfStep) {
				report = new DR();
			} else {
				report = new SR();
			}
			report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
			report.setResult(TestResultType.SUCCESS);

		} catch (DatatypeConfigurationException e) {
			logger.error("An error occurred while trying to construct a completed report for [" + step + "] with id [" + stepId + "]");
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
			if (exception instanceof GITBEngineInternalError && exception.getCause() != null) {
				error.setDescription(exception.getCause().getMessage());
			} else {
				error.setDescription(exception.getMessage());
			}

			report.getReports().getInfoOrWarningOrError().add(trObjectFactory.createTestAssertionGroupReportsTypeError(error));

		} catch (DatatypeConfigurationException e) {
			logger.error("An error occurred while trying to construct a report for exception [" + ((ErrorStatusEvent) statusEvent).getException() + "]", e);
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
		return Props.create(new Creator<S>() {
			@Override
			public S create() throws Exception {
				@SuppressWarnings("unchecked")
				S s = (S) ConstructorUtils.invokeConstructor(clazz, new Object[]{step, scope, stepId});
				return s;
			}
		});
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

}
