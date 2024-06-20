package com.gitb.engine.actors.processors;

import com.gitb.core.Configuration;
import com.gitb.core.ErrorCode;
import com.gitb.engine.actors.ActorSystem;
import com.gitb.engine.commands.messaging.NotificationReceived;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.messaging.TransactionContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.DeferredMessagingReport;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * Created by serbay on 9/30/14.
 *
 * Send step processor actor
 */
public class SendStepProcessorActor extends AbstractMessagingStepProcessorActor<Send> {

	public static final String NAME = "send-p";
	private static final Logger logger = LoggerFactory.getLogger(SendStepProcessorActor.class);

	private MessagingContext messagingContext;
	private TransactionContext transactionContext;

	private Promise<TestStepReportType> promise;

	public SendStepProcessorActor(Send step, TestCaseScope scope, String stepId) {
		super(step, scope, stepId);
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
					return buildReport(report);
				} else {
					TAR tar = new TAR();
					tar.setResult(TestResultType.SUCCESS);
					return tar;
				}
			}, getContext().dispatcher());

			future.foreach(handleSuccess(promise), getContext().dispatcher());
			future.failed().foreach(handleFailure(promise), getContext().dispatcher());
		} else {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Messaging handler is not available"));
		}
	}

	private TAR buildReport(MessagingReport report) {
		//id parameter must be set for Send steps, so that sent message
		//is saved to scope
		if (step.getId() != null && report.getMessage() != null) {
			Message sentMessage = report.getMessage();
			MapType map = generateOutputWithMessageFields(sentMessage);
			scope
					.createVariable(step.getId())
					.setValue(map);
		}
		return report.getReport();
	}

	@Override
	public void onReceive(Object message) {
		try {
			if (message instanceof NotificationReceived) {
				signalStepStatus(buildReport(((NotificationReceived) message).getReport()));
			} else {
				super.onReceive(message);
			}
		} catch (Exception e) {
			error(e);
		}
	}

	@Override
	protected void stop() {
		if(promise != null) {
			promise.tryFailure(new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.CANCELLATION, "Test step ["+stepId+"] is cancelled.")));
		}
	}

    @Override
    protected MessagingContext getMessagingContext() {
        return messagingContext;
    }

	public static ActorRef create(ActorContext context, Send step, TestCaseScope scope, String stepId) throws Exception {
		return context.actorOf(props(SendStepProcessorActor.class, step, scope, stepId).withDispatcher(ActorSystem.BLOCKING_DISPATCHER), getName(NAME));
	}
}
