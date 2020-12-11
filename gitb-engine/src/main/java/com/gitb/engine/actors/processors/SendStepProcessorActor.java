package com.gitb.engine.actors.processors;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
import com.gitb.core.Configuration;
import com.gitb.core.ErrorCode;
import com.gitb.core.StepStatus;
import com.gitb.engine.actors.ActorSystem;
import com.gitb.engine.events.model.ErrorStatusEvent;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.messaging.TransactionContext;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.tdl.Send;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.MapType;
import com.gitb.utils.ErrorUtils;
import scala.concurrent.Future;
import scala.concurrent.Promise;

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
				if (result != null) {
					if (result.getResult() == TestResultType.SUCCESS) {
						updateTestStepStatus(context, StepStatus.COMPLETED, result);
					} else if (result.getResult() == TestResultType.WARNING) {
						updateTestStepStatus(context, StepStatus.WARNING, result);
					} else {
						updateTestStepStatus(context, StepStatus.ERROR, result);
					}
				} else {
					updateTestStepStatus(context, StepStatus.COMPLETED, null);
				}
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
        TestCaseContext testCaseContext = scope.getContext();

        for(MessagingContext mc : testCaseContext.getMessagingContexts()) {
            if(mc.getTransaction(step.getTxnId()) != null) {
                messagingContext = mc;
                break;
            }
        }

        transactionContext = messagingContext.getTransaction(step.getTxnId());

		final IMessagingHandler messagingHandler = messagingContext.getHandler();

		if(messagingHandler != null) {
			//id parameter must be set for Send steps, so that sent message
			//is saved to scope
			Future<TestStepReportType> future = Futures.future(() -> {

				VariableResolver resolver = new VariableResolver(scope);
				if (step.getConfig() != null) {
					for (Configuration config : step.getConfig()) {
						if (resolver.isVariableReference(config.getValue())) {
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
										step.getConfig(),
										message
								);

				if (report != null) {
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
				} else {
					return null;
				}
			}, getContext().dispatcher());

			future.foreach(new OnSuccess<>() {
				@Override
				public void onSuccess(TestStepReportType result) {
					promise.trySuccess(result);
				}
			}, getContext().dispatcher());

			future.failed().foreach(new OnFailure() {
				@Override
				public void onFailure(Throwable failure) {
					promise.tryFailure(failure);
				}
			}, getContext().dispatcher());
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

    @Override
    protected TransactionContext getTransactionContext() {
        return transactionContext;
    }

	public static ActorRef create(ActorContext context, Send step, TestCaseScope scope, String stepId) throws Exception {
		return context.actorOf(props(SendStepProcessorActor.class, step, scope, stepId).withDispatcher(ActorSystem.BLOCKING_DISPATCHER), getName(NAME));
	}
}
