package com.gitb.engine.actors.processors;

import com.gitb.core.Configuration;
import com.gitb.core.ErrorCode;
import com.gitb.engine.actors.ActorSystem;
import com.gitb.engine.commands.messaging.NotificationReceived;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.messaging.TransactionContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.DeferredMessagingReport;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.tdl.ErrorLevel;
import com.gitb.tdl.Send;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.MapType;
import com.gitb.utils.ErrorUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.dispatch.OnFailure;
import org.apache.pekko.dispatch.OnSuccess;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.util.Objects;

/**
 * Created by serbay on 9/30/14.
 *
 * Send step processor actor
 */
public class SendStepProcessorActor extends AbstractMessagingStepProcessorActor<Send> {

	public static final String NAME = "send-p";

	private MessagingContext messagingContext;
	private TransactionContext transactionContext;

	private Promise<TestStepReportType> promise;

	public SendStepProcessorActor(Send step, TestCaseScope scope, String stepId, StepContext stepContext) {
		super(step, scope, stepId, stepContext);
	}

    @Override
	protected void init() {

		final ActorContext context = getContext();

		promise = Futures.promise();

		promise.future().foreach(new OnSuccess<>() {
			@Override
			public void onSuccess(TestStepReportType result) {
				signalStepStatus(result);
			}
		}, context.dispatcher());

		promise.future().failed().foreach(new OnFailure() {
			@Override
			public void onFailure(Throwable failure) {
				handleFutureFailure(failure);
			}
		}, context.dispatcher());
	}

	@Override
	protected void start() {
		processing();
		VariableResolver resolver = new VariableResolver(scope);

		var contexts = determineMessagingContexts(resolver);
		messagingContext = contexts.getLeft();
		transactionContext = contexts.getRight();

		final IMessagingHandler messagingHandler = messagingContext.getHandler();

		if (messagingHandler != null) {
			//id parameter must be set for Send steps, so that sent message
			//is saved to scope
			Future<TestStepReportType> future = Futures.future(() -> {
				if (step.getConfig() != null) {
					for (Configuration config : step.getConfig()) {
						if (VariableResolver.isVariableReference(config.getValue())) {
							config.setValue(resolver.resolveVariableAsString(config.getValue()).toString());
						}
					}
				}

				Message message = getMessageFromBindings(step.getInput());

				MessagingReport report =
						messagingHandler
								.sendMessage(
										messagingContext.getSessionId(),
										transactionContext.getTransactionId(),
										step.getId(),
										step.getConfig(),
										message
								);
				if (report instanceof DeferredMessagingReport deferredReport) {
					deferredReport.getDeferredReport().thenAccept((completedReport) -> {
						self().tell(new NotificationReceived(completedReport), self());
					}).exceptionally(error -> {
						error(error);
						return null;
					});
					return null;
				} else if (report != null) {
					return buildReport(report, resolver);
				} else {
					TAR tar = new TAR();
					tar.setResult(TestResultType.SUCCESS);
					return completeReport(tar, resolver);
				}
			}, getContext().getSystem().dispatchers().lookup(ActorSystem.BLOCKING_IO_DISPATCHER));

			future.foreach(handleSuccess(promise), getContext().dispatcher());
			future.failed().foreach(handleFailure(promise), getContext().dispatcher());
		} else {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Messaging handler is not available"));
		}
	}

	private TAR completeReport(TAR report, VariableResolver resolver) {
		if (resolver == null) {
			resolver = new VariableResolver(scope);
		}
		ErrorLevel errorLevel = TestCaseUtils.resolveReportErrorLevel(step.getLevel(), scope.getContext().getSessionId(), resolver);
		TestCaseUtils.postProcessReport(step.isInvert(), errorLevel, report);
		return report;
	}

	private TAR buildReport(MessagingReport report, VariableResolver resolver) {
		//id parameter must be set for Send steps, so that sent message
		//is saved to scope
		if (step.getId() != null && report.getMessage() != null) {
			Message sentMessage = report.getMessage();
			MapType map = generateOutputWithMessageFields(sentMessage);
			scope
					.createVariable(step.getId())
					.setValue(map);
		}
		return completeReport(report.getReport(), resolver);
	}

	@Override
	public void onReceive(Object message) {
		try {
			if (message instanceof NotificationReceived) {
				signalStepStatus(buildReport(((NotificationReceived) message).getReport(), null));
			} else {
				super.onReceive(message);
			}
		} catch (Exception e) {
			error(e);
		}
	}

	@Override
	protected void stop() {
		if (promise != null && !promise.isCompleted()) {
			promise.tryFailure(new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.CANCELLATION, "Test step ["+stepId+"] is cancelled.")));
		}
	}

    @Override
    protected MessagingContext getMessagingContext() {
        return messagingContext;
    }

	public static ActorRef create(ActorContext context, Send step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
		return context.actorOf(props(SendStepProcessorActor.class, step, scope, stepId, stepContext).withDispatcher(ActorSystem.BLOCKING_DISPATCHER), getName(NAME));
	}

	@Override
	protected String getFrom() {
		return Objects.requireNonNullElseGet(super.getFrom(), () -> scope.getContext().getDefaultNonSutActor());
	}

	@Override
	protected String getTo() {
		return Objects.requireNonNullElseGet(super.getTo(), () -> scope.getContext().getDefaultSutActor());
	}

}
