package com.gitb.engine.actors.processors;

import com.gitb.core.StepStatus;
import com.gitb.engine.commands.interaction.StartCommand;
import com.gitb.engine.events.TestStepStatusEventBus;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.events.model.TestStepStatusEvent;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.tdl.IfStep;
import com.gitb.tr.SR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;
import com.gitb.utils.XMLDateTimeUtils;
import org.apache.pekko.actor.ActorRef;

import javax.xml.datatype.DatatypeConfigurationException;

/**
 * Created by serbay on 9/11/14.
 *
 * If step executor actor
 */
public class IfStepProcessorActor extends AbstractTestStepActor<IfStep> {
	public static final String NAME = "if-s-p";

	public static final String THEN_BRANCH_ID = "[T]";
	public static final String ELSE_BRANCH_ID = "[F]";

	private boolean condition;

	public IfStepProcessorActor(IfStep step, TestCaseScope scope, String stepId, StepContext stepContext) {
		super(step, scope, stepId, stepContext);
	}

	@Override
	protected void init() {
	}

	@Override
	protected void start() throws Exception {
		processing();

		ExpressionHandler expressionHandler = new ExpressionHandler(scope);

		condition = (boolean) expressionHandler
			.processExpression(step.getCond(), DataType.BOOLEAN_DATA_TYPE)
			.getValue();

		ActorRef branch = null;
		if(condition) {
			branch = SequenceProcessorActor.create(getContext(), step.getThen(), scope, stepId + THEN_BRANCH_ID, this.stepContext);
			if (step.getElse() != null) {
				sendSkippedStatusEvent(stepId + ELSE_BRANCH_ID);
			}
		} else {
			if (step.getElse() != null) {
				branch = SequenceProcessorActor.create(getContext(), step.getElse(), scope, stepId + ELSE_BRANCH_ID, this.stepContext);
			}
            sendSkippedStatusEvent(stepId + THEN_BRANCH_ID);
		}

		if (branch != null) {
			StartCommand command = new StartCommand(scope.getContext().getSessionId());
			branch.tell(command, self());
		} else {
			completed();
		}
	}

    private void sendSkippedStatusEvent(String stepId) throws DatatypeConfigurationException {
        TestStepReportType report = new SR();
        report.setResult(TestResultType.UNDEFINED);
        report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());

        TestStepStatusEvent event = new TestStepStatusEvent(scope.getContext().getSessionId(), stepId, StepStatus.SKIPPED, report, step, scope);

        TestStepStatusEventBus.getInstance().publish(event);
    }

	@Override
	protected void handleStatusEvent(StatusEvent event) throws Exception {
        StepStatus status = event.getStatus();
		if(status == StepStatus.COMPLETED) {
			completed();
		} else if (status == StepStatus.WARNING) {
			childrenHasWarning();
		} else if (status == StepStatus.ERROR) {
			childrenHasError();
		}
    }

	public static ActorRef create(ActorContext context, IfStep step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
		return create(IfStepProcessorActor.class, context, step, scope, stepId, stepContext);
	}
}
