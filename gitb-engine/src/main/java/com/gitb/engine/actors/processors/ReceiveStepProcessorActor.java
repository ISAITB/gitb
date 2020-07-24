package com.gitb.engine.actors.processors;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
import com.gitb.core.Configuration;
import com.gitb.core.ErrorCode;
import com.gitb.core.MessagingModule;
import com.gitb.core.StepStatus;
import com.gitb.engine.actors.ActorSystem;
import com.gitb.engine.events.model.ErrorStatusEvent;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.messaging.TransactionContext;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.CallbackManager;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.messaging.utils.MessagingHandlerUtils;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.BooleanType;
import com.gitb.types.MapType;
import com.gitb.utils.BindingUtils;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Created by serbay on 9/30/14.
 *
 * Receive step executor actor
 */
public class ReceiveStepProcessorActor extends AbstractMessagingStepProcessorActor<com.gitb.tdl.Receive> {

    private static final Logger logger = LoggerFactory.getLogger(ReceiveStepProcessorActor.class);

	public static final String NAME = "receive-p";

	private MessagingContext messagingContext;
	private TransactionContext transactionContext;

	private Promise<TestStepReportType> promise;
	private Future<TestStepReportType> future;

	public ReceiveStepProcessorActor(com.gitb.tdl.Receive step, TestCaseScope scope, String stepId) {
		super(step, scope, stepId);
	}

	@Override
	protected void init() throws Exception {
		final ActorContext context = getContext();

		promise = Futures.promise();

		promise.future().onSuccess(new OnSuccess<TestStepReportType>() {
			@Override
			public void onSuccess(TestStepReportType result) throws Throwable {
				if(result != null) {
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

		promise.future().onFailure(new OnFailure() {
			@Override
			public void onFailure(Throwable failure) throws Throwable {
                updateTestStepStatus(context, new ErrorStatusEvent(failure), null, true);
			}
		}, context.dispatcher());
	}

	@Override
	protected void start() throws Exception {
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
		final ActorContext context = getContext();

		waiting();

		if(messagingHandler != null) {
			future = Futures.future(new Callable<TestStepReportType>() {
				@Override
				public TestStepReportType call() throws Exception {

					VariableResolver resolver = new VariableResolver(scope);
					if (step.getConfig() != null) {
						for (Configuration config: step.getConfig()) {
							if (resolver.isVariableReference(config.getValue())) {
								config.setValue(resolver.resolveVariableAsString(config.getValue()).toString());
							}
						}
					}

					MessagingModule moduleDefinition = messagingHandler.getModuleDefinition();
					if (moduleDefinition.getReceiveConfigs() != null) {
						checkRequiredConfigsAndSetDefaultValues(moduleDefinition.getReceiveConfigs().getParam(), step.getConfig());
					}

					Message inputMessage = getMessageFromBindings(step.getInput());
					String callId = UUID.randomUUID().toString();

					CallbackManager.getInstance().prepareForCallback(messagingContext.getSessionId(), callId);

					if (!StringUtils.isBlank(step.getTimeout())) {
                        long timeout;
                        if (resolver.isVariableReference(step.getTimeout())) {
                            timeout = resolver.resolveVariableAsNumber(step.getTimeout()).longValue();
                        } else {
                            timeout = Double.valueOf(step.getTimeout()).longValue();
                        }
                        String flagName = null;
                        if (!StringUtils.isBlank(step.getTimeoutFlag())) {
							if (resolver.isVariableReference(step.getTimeoutFlag())) {
								flagName = resolver.resolveVariableAsString(step.getTimeoutFlag()).toString();
							} else {
								flagName = step.getTimeoutFlag();
							}
						}
						boolean errorIfTimeout = false;
						if (!StringUtils.isBlank(step.getTimeoutIsError())) {
							if (resolver.isVariableReference(step.getTimeoutIsError())) {
								errorIfTimeout = (Boolean)resolver.resolveVariableAsBoolean(step.getTimeoutIsError()).getValue();
							} else {
								errorIfTimeout = Boolean.valueOf(step.getTimeoutIsError());
							}
						}
						Thread timeoutThread = new Thread(new TimeoutRunner(messagingContext.getSessionId(), callId, timeout, flagName, errorIfTimeout));
						messagingContext.getMessagingThreads().add(timeoutThread);
						timeoutThread.start();
					}

					// This call will block until there is a callback response.
					MessagingReport report =
							messagingHandler
									.receiveMessage(
											messagingContext.getSessionId(),
											transactionContext.getTransactionId(),
											callId,
											step.getConfig(),
											inputMessage,
											messagingContext.getMessagingThreads()
									);

					if(report != null && report.getMessage() != null) {
						Message message = report.getMessage();

						if(step.getId() != null) {
							MapType map;

							if (step.getTimeout() != null && !StringUtils.isBlank(step.getTimeoutFlag())) {
								String flagName;
								if (resolver.isVariableReference(step.getTimeoutFlag())) {
									flagName = resolver.resolveVariableAsString(step.getTimeoutFlag()).toString();
								} else {
									flagName = step.getTimeoutFlag();
								}
								if (!message.getFragments().containsKey(flagName)) {
									/*
									The response was not triggered by a timeout but we have a timeout flag defined
									Make sure we have the flag set as false in the response.
									 */
									message.getFragments().put(flagName, new BooleanType(false));
								}
							}

							if(step.getOutput().size() == 0) {
								map = generateOutputWithMessageFields(message);
							} else {
								boolean isNameBinding = BindingUtils.isNameBinding(step.getOutput());
								if(isNameBinding) {
									map = generateOutputWithNameBinding(message, step.getOutput());
								} else {
									map = generateOutputWithModuleDefinition(messagingContext, message);
								}
							}

							scope
								.createVariable(step.getId())
								.setValue(map);
						}
						return report.getReport();
					} else if(report != null) {
                        return report.getReport();
					} else {
						return null;
					}

				}
			}, context.dispatcher());

			future.onSuccess(new OnSuccess<TestStepReportType>() {
				@Override
				public void onSuccess(TestStepReportType result) throws Throwable {
					promise.trySuccess(result);
				}
			}, context.dispatcher());

			future.onFailure(new OnFailure() {
				@Override
				public void onFailure(Throwable failure) throws Throwable {
					messagingHandler.endTransaction(messagingContext.getSessionId(), transactionContext.getTransactionId());
					promise.tryFailure(failure);
				}
			}, context.dispatcher());
		} else {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Messaging handler is not available"));
		}
	}

	@Override
	protected void stop() {
		if(promise != null && !promise.isCompleted()) {
			boolean stopped = promise.tryFailure(new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.CANCELLATION, "Test step ["+stepId+"] is cancelled.")));
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

	public static ActorRef create(ActorContext context, com.gitb.tdl.Receive step, TestCaseScope scope, String stepId) throws Exception {
		return context.actorOf(props(ReceiveStepProcessorActor.class, step, scope, stepId).withDispatcher(ActorSystem.BLOCKING_DISPATCHER), getName(NAME));
	}

	static class TimeoutRunner implements Runnable {

		private final String sessionId;
        private final long timeout;
		private final boolean errorIfTimeout;
		private final String flagName;
		private final String callId;

		TimeoutRunner(String sessionId, String callId, long timeout, String flagName, boolean errorIfTimeout) {
            this.timeout = timeout;
			this.sessionId = sessionId;
			this.flagName = flagName;
			this.errorIfTimeout = errorIfTimeout;
			this.callId = callId;
		}
		
		@Override
		public void run() {
			try {
				Thread.sleep(timeout);
			} catch (InterruptedException e) {
				// Nothing to do.
			} finally {
				CallbackManager.getInstance().callbackReceived(sessionId, callId, MessagingHandlerUtils.getMessagingReportForTimeout(flagName, errorIfTimeout));
			}
		}
	}
}
