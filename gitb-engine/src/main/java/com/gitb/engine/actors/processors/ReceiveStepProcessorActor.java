package com.gitb.engine.actors.processors;

import com.gitb.core.Configuration;
import com.gitb.core.ErrorCode;
import com.gitb.core.MessagingModule;
import com.gitb.engine.CallbackManager;
import com.gitb.engine.PropertyConstants;
import com.gitb.engine.actors.ActorSystem;
import com.gitb.engine.commands.messaging.NotificationReceived;
import com.gitb.engine.commands.messaging.TimeoutExpired;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.messaging.TransactionContext;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.DeferredMessagingReport;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.messaging.callback.SessionCallbackData;
import com.gitb.tdl.ErrorLevel;
import com.gitb.tr.TAR;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.BooleanType;
import com.gitb.types.MapType;
import com.gitb.utils.BindingUtils;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.dispatch.OnFailure;
import org.apache.pekko.dispatch.OnSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Receive step executor actor
 */
public class ReceiveStepProcessorActor extends AbstractMessagingStepProcessorActor<com.gitb.tdl.Receive> {

	private static final Logger logger = LoggerFactory.getLogger(ReceiveStepProcessorActor.class);
	public static final String NAME = "receive-p";

	private MessagingContext messagingContext;
	private TransactionContext transactionContext;

	private Promise<TestStepReportType> promise;

	public ReceiveStepProcessorActor(com.gitb.tdl.Receive step, TestCaseScope scope, String stepId, StepContext stepContext) {
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
		final ActorContext context = getContext();

		waiting();

		if (messagingHandler != null) {
			// This call will block until there is a callback response.
			/*
			The response was not triggered by a timeout but we have a timeout flag defined
			Make sure we have the flag set as false in the response.
			 */
			Future<TestStepReportType> future = Futures.future(() -> {
				if (step.getConfig() != null) {
					for (Configuration config : step.getConfig()) {
						if (VariableResolver.isVariableReference(config.getValue())) {
							config.setValue(resolver.resolveVariableAsString(config.getValue()).toString());
						}
					}
				}
				MessagingModule moduleDefinition = messagingHandler.getModuleDefinition();
				if (moduleDefinition != null && moduleDefinition.getReceiveConfigs() != null) {
					checkRequiredConfigsAndSetDefaultValues(moduleDefinition.getReceiveConfigs().getParam(), step.getConfig());
				}
				Message inputMessage = getMessageFromBindings(step.getInput());
				String callId = UUID.randomUUID().toString();
				CallbackManager.getInstance().registerForNotification(self(), messagingContext.getSessionId(), callId);
				if (!StringUtils.isBlank(step.getTimeout())) {
					long timeout;
					if (VariableResolver.isVariableReference(step.getTimeout())) {
						timeout = resolver.resolveVariableAsNumber(step.getTimeout()).longValue();
					} else {
						timeout = Double.valueOf(step.getTimeout()).longValue();
					}
					context.system().scheduler().scheduleOnce(
							scala.concurrent.duration.Duration.apply(timeout, TimeUnit.MILLISECONDS), () -> {
								if (!self().isTerminated()) {
									self().tell(new TimeoutExpired(), self());
								}
							},
							context.dispatcher()
					);
				}
				MessagingReport report = messagingHandler
					.receiveMessage(
							messagingContext.getSessionId(),
							transactionContext.getTransactionId(),
							callId,
							step,
							inputMessage,
							messagingContext.getMessagingThreads()
				);
				if (report instanceof DeferredMessagingReport deferredReport) {
					// This means that we should not resolve this step but rather wait for a message to be delivered to the actor.
					if (deferredReport.getCallbackData() != null) {
						// Register the data needed to respond when receiving a call.
						CallbackManager.getInstance().registerCallbackData(new SessionCallbackData(
								messagingContext.getSessionId(),
								callId,
								((MapType) scope.getVariable(PropertyConstants.SYSTEM_MAP).getValue()).getItem(PropertyConstants.SYSTEM_MAP__API_KEY).toString(),
								deferredReport.getCallbackData())
						);
					}
					return null;
				} else {
					return handleMessagingResult(report);
				}
			}, getContext().getSystem().dispatchers().lookup(ActorSystem.BLOCKING_IO_DISPATCHER));

			future.foreach(handleSuccess(promise), getContext().dispatcher());
			future.failed().foreach(handleFailure(promise), getContext().dispatcher());
		} else {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Messaging handler is not available"));
		}
	}

	@Override
	public void onReceive(Object message) {
		try {
			if (message instanceof NotificationReceived notificationMessage) {
				if (promise != null && !promise.isCompleted()) {
					if (notificationMessage.getError() != null) {
						promise.tryFailure(notificationMessage.getError());
					} else {
						logger.debug(addMarker(), "Received notification");
						promise.trySuccess(handleMessagingResult(notificationMessage.getReport()));
					}
				}
			} else if (message instanceof TimeoutExpired) {
				if (promise != null && !promise.isCompleted()) {
					VariableResolver resolver = new VariableResolver(scope);
					String flagName = null;
					if (!StringUtils.isBlank(step.getTimeoutFlag())) {
						if (VariableResolver.isVariableReference(step.getTimeoutFlag())) {
							flagName = resolver.resolveVariableAsString(step.getTimeoutFlag()).toString();
						} else {
							flagName = step.getTimeoutFlag();
						}
					}
					boolean errorIfTimeout = false;
					if (!StringUtils.isBlank(step.getTimeoutIsError())) {
						if (VariableResolver.isVariableReference(step.getTimeoutIsError())) {
							errorIfTimeout = (Boolean) resolver.resolveVariableAsBoolean(step.getTimeoutIsError()).getValue();
						} else {
							errorIfTimeout = Boolean.parseBoolean(step.getTimeoutIsError());
						}
					}
					if (errorIfTimeout) {
						logger.error(addMarker(), "Timeout expired while waiting to receive message");
					} else {
						logger.debug(addMarker(), "Timeout expired while waiting to receive message");
					}
					promise.trySuccess(handleMessagingResult(MessagingHandlerUtils.getMessagingReportForTimeout(flagName, errorIfTimeout)));
				}
			} else {
				super.onReceive(message);
			}
		} catch (Exception e) {
			error(e);
		}
	}

	private TAR handleMessagingResult(MessagingReport report) {
		TAR reportToReturn;
		Optional<VariableResolver> resolver = Optional.empty();
		if (report != null && report.getMessage() != null) {
			Message message = report.getMessage();
			if (step.getId() != null) {
				MapType map;
				if (step.getTimeout() != null && !StringUtils.isBlank(step.getTimeoutFlag())) {
					String flagName;
					if (VariableResolver.isVariableReference(step.getTimeoutFlag())) {
						resolver = Optional.of(new VariableResolver(scope));
						flagName = resolver.get().resolveVariableAsString(step.getTimeoutFlag()).toString();
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
				if (step.getOutput().isEmpty()) {
					map = generateOutputWithMessageFields(message);
				} else {
					boolean isNameBinding = BindingUtils.isNameBinding(step.getOutput());
					if (isNameBinding) {
						map = generateOutputWithNameBinding(message, step.getOutput());
					} else {
						map = generateOutputWithModuleDefinition(messagingContext, message);
					}
				}
				scope
					.createVariable(step.getId())
					.setValue(map);
			}
			reportToReturn = report.getReport();
		} else if (report != null) {
			reportToReturn = report.getReport();
		} else {
			reportToReturn = MessagingHandlerUtils.generateSuccessReport(null).getReport();
		}
		ErrorLevel errorLevel = TestCaseUtils.resolveReportErrorLevel(step.getLevel(), scope.getContext().getSessionId(), resolver.orElse(new VariableResolver(scope)));
		TestCaseUtils.postProcessReport(step.isInvert(), errorLevel, reportToReturn);
		return reportToReturn;
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

	public static ActorRef create(ActorContext context, com.gitb.tdl.Receive step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
		return context.actorOf(props(ReceiveStepProcessorActor.class, step, scope, stepId, stepContext).withDispatcher(ActorSystem.BLOCKING_DISPATCHER), getName(NAME));
	}

	@Override
	protected String getFrom() {
		return Objects.requireNonNullElseGet(super.getFrom(), () -> scope.getContext().getDefaultSutActor());
	}

	@Override
	protected String getTo() {
		return Objects.requireNonNullElseGet(super.getTo(), () -> scope.getContext().getDefaultNonSutActor());
	}

}
