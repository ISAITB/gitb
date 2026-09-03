/*
 * Copyright (C) 2026 European Union
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

import com.gitb.core.AnyContent;
import com.gitb.core.Configuration;
import com.gitb.core.ErrorCode;
import com.gitb.core.MessagingModule;
import com.gitb.core.StepStatus;
import com.gitb.engine.CallbackManager;
import com.gitb.engine.actors.ActorSystem;
import com.gitb.engine.commands.interaction.StartCommand;
import com.gitb.engine.commands.messaging.NotificationReceived;
import com.gitb.engine.commands.messaging.ResultRequested;
import com.gitb.engine.commands.messaging.ResultTimeoutExpired;
import com.gitb.engine.commands.messaging.TimeoutExpired;
import com.gitb.engine.events.model.ExitEvent;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.messaging.TransactionContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.*;
import com.gitb.messaging.callback.SessionCallbackData;
import com.gitb.tdl.Binding;
import com.gitb.tdl.ErrorLevel;
import com.gitb.tdl.ExitScopeType;
import com.gitb.tdl.Result;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.MapType;
import com.gitb.utils.BindingUtils;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.runtime.BoxedUnit;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils.getMessageFromBindings;
import static com.gitb.utils.MessagingReportUtils.generateSuccessReport;
import static com.gitb.utils.MessagingReportUtils.getMessagingReportForTimeout;

/**
 * Receive step executor actor
 */
public class ReceiveStepProcessorActor extends AbstractMessagingStepProcessorActor<com.gitb.tdl.Receive> {

	private static final Logger logger = LoggerFactory.getLogger(ReceiveStepProcessorActor.class);
	public static final String NAME = "receive-p";

	/**
	 * Distinguishes how 'result' processing was triggered and therefore how it must be finalised once
	 * result/output has been resolved.
	 */
	private enum ResultMode {
		/** A messaging server (HttpMessagingV2/SoapMessagingV2) is awaiting the resolved response. */
		RESPONSE_HANDLE,
		/** The handler's report is already complete - 'result' only patches it before the step resolves. */
		REPORT_ONLY
	}

	private MessagingContext messagingContext;
	private TransactionContext transactionContext;

	private Promise<TestStepReportType> promise;

	/*
	 * The following fields are only ever populated when the step defines a 'result' element, i.e. once the actor
	 * starts processing it for a genuinely received/matched message.
	 */
	private Message inputMessage;
	private ResultMode resultMode;
	private TestCaseScope resultChildScope;
	private CompletableFuture<Message> pendingResponseHandle;
	private MessagingReport pendingReport;
	private Map<String, DataType> resolvedResultOutputs;
	private boolean resultHadWarning;

	public ReceiveStepProcessorActor(com.gitb.tdl.Receive step, TestCaseScope scope, String stepId, StepContext stepContext) {
		super(step, scope, stepId, stepContext);
	}

	@Override
	protected void init() {
		final ActorContext context = getContext();

		promise = Futures.promise();

		promise.future().onComplete(result -> {
			if (result.isSuccess()) {
				signalStepStatus(result.get());
			} else {
				handleFutureFailure(result.failed().get());
			}
			return BoxedUnit.UNIT;
		}, context.dispatcher());
	}

	@Override
	@SuppressWarnings("resource")
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
				inputMessage = getMessageFromBindings(messagingHandler, step.getInput(), expressionHandler);
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
								scope.getContext().getSystemApiKey(),
								deferredReport.getCallbackData(),
								step.getResult() != null)
						);
						// Handle the report's deferred task (if any).
						scheduleDeferredTask(deferredReport.getDeferredTask());
					}
					return null;
				} else if (step.getResult() != null && report != null && report.getMessage() != null) {
					/*
					 * A message was received immediately (no deferral) and 'result' processing is needed. That
					 * processing spawns a child actor for result/steps and must therefore run on this actor's own
					 * thread rather than here, on the blocking IO dispatcher - route the report back to ourselves.
					 * Returning null keeps the step's promise open (see attachFutureCallbacks).
					 */
					self().tell(new NotificationReceived(report), self());
					return null;
				} else {
					return handleMessagingResult(report);
				}
			}, getContext().getSystem().dispatchers().lookup(ActorSystem.BLOCKING_IO_DISPATCHER));

			attachFutureCallbacks(future, promise, getContext().dispatcher());
		} else {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Messaging handler is not available"));
		}
	}

	@Override
	public void onReceive(Object message) {
		try {
            switch (message) {
                case NotificationReceived notificationMessage -> {
                    if (promise != null && !promise.isCompleted()) {
                        if (notificationMessage.getError() != null) {
                            promise.tryFailure(notificationMessage.getError());
                        } else {
                            logger.debug(addMarker(), "Received notification");
                            resolveReceivedMessage(notificationMessage.getReport());
                        }
                    }
                }
                case ResultRequested resultRequested -> handleResultRequested(resultRequested);
                case ResultTimeoutExpired ignored -> {
                    if (isResultRunning()) {
                        failResult(new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "A timeout occurred while calculating the step's result.")));
                    }
                }
                case TimeoutExpired timeoutExpired -> {
                    // Once a matching message has arrived (and is being resolved, possibly via 'result'), the
                    // original "waiting to receive" timeout no longer applies.
                    if (promise != null && !promise.isCompleted() && resultMode == null) {
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
                                errorIfTimeout = resolver.resolveVariableAsBoolean(step.getTimeoutIsError()).getValue();
                            } else {
                                errorIfTimeout = Boolean.parseBoolean(step.getTimeoutIsError());
                            }
                        }
                        if (errorIfTimeout) {
                            logger.error(addMarker(), "Timeout expired while waiting to receive message");
                        } else {
                            logger.debug(addMarker(), "Timeout expired while waiting to receive message");
                        }
                        promise.trySuccess(handleMessagingResult(getMessagingReportForTimeout(flagName, errorIfTimeout)));
                    }
                }
                case DeferredTask<?> deferredTask -> handleDeferredTask(deferredTask);
                case null, default -> super.onReceive(message);
            }
		} catch (Exception e) {
			error(e);
		}
	}

	@SuppressWarnings("resource")
	private void scheduleDeferredTask(DeferredTask<?> deferredTask) {
		if (deferredTask != null) {
			getContext().system().scheduler().scheduleOnce(
					scala.concurrent.duration.Duration.apply(deferredTask.nextExecutionDelay(), TimeUnit.MILLISECONDS), () -> {
						if (!self().isTerminated()) {
							self().tell(deferredTask, self());
						}
					},
					getContext().dispatcher()
			);
		}
	}


	private <T> void handleDeferredTask(DeferredTask<T> deferredTask) {
		if (deferredTask != null && promise != null && !promise.isCompleted()) {
			try {
				var result = deferredTask.executionHandler().apply(deferredTask.state());
				if (result.report() != null) {
					// Task completed - the report will be delivered as a callback via the CallbackManager.
					logger.debug(addMarker(), "Task completed");
				} else if (result.nextExecutionDelay() != null) {
					if (result.nextState() == null) {
						// Schedule new task execution with the same state (e.g. a retry).
						scheduleDeferredTask(deferredTask.withNewDelay(result.nextExecutionDelay()));
					} else {
						// Schedule new task with new state.
						scheduleDeferredTask(deferredTask.withNewState(result.nextState(), result.nextExecutionDelay()));
					}
				} else {
					// Task expired.
					resolveReceivedMessage(deferredTask.expiryHandler().apply(deferredTask.state()));
				}
			} catch (Exception e) {
				// Unexpected error.
				promise.tryFailure(e);
			}
		}
	}

	/**
	 * Common entry point for every genuinely received/matched message (i.e. excluding the "nothing arrived"
	 * {@link TimeoutExpired} case, which must never trigger 'result' processing). Either resolves the step
	 * immediately - no 'result' is defined, 'result' processing has already been started (this is the follow-up
	 * notification for a step whose response was resolved via {@link #handleResultRequested}), or there is no
	 * message to work from - or starts 'result' processing and resolves the step once it completes.
	 * <p>Must only be called from this actor's own thread.
	 */
	private void resolveReceivedMessage(MessagingReport report) {
		if (promise == null || promise.isCompleted()) {
			return;
		}
		if (step.getResult() == null || resultMode != null || report == null || report.getMessage() == null) {
			promise.trySuccess(handleMessagingResult(report));
		} else {
			// No messaging server is awaiting a live response - the handler's own report is both preview and target.
			resultMode = ResultMode.REPORT_ONLY;
			pendingReport = report;
			startResultProcessing(buildStepOutputMap(report.getMessage()));
		}
	}

	/**
	 * Invoked once the incoming call matching this step (which defines a 'result' element) has actually arrived
	 * and a messaging server is awaiting the resolved response to return for it. The preview exposed to
	 * result/steps is built by the handler itself (see {@link IMessagingHandler#buildResultPreview}) - this actor
	 * stays agnostic of what shape/names a given handler chooses to expose (e.g. HttpMessagingV2/SoapMessagingV2
	 * expose request/response sub-maps; a different protocol may reasonably expose something else).
	 */
	private void handleResultRequested(ResultRequested resultRequested) {
		if (promise != null && !promise.isCompleted() && step.getResult() != null && resultMode == null) {
			resultMode = ResultMode.RESPONSE_HANDLE;
			pendingResponseHandle = resultRequested.responseHandle();
			MapType preview = messagingContext.getHandler().buildResultPreview(resultRequested.request(), inputMessage);
			startResultProcessing(preview);
		} else if (!resultRequested.responseHandle().isDone()) {
			// The step can no longer act on this (e.g. it already completed via its own timeout) - fail the handle so the caller doesn't hang.
			resultRequested.responseHandle().completeExceptionally(new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Receive step [" + stepId + "] is no longer able to process a 'result'.")));
		}
	}

	/**
	 * Creates the isolated child scope for 'result' processing, exposing the given preview under the step's own
	 * variable (i.e. its id) - only if the step actually declares an id, since otherwise there is no variable name
	 * to expose it under; a 'result' that only produces values not derived from the received message (e.g. a
	 * computed timestamp) is a legitimate use case needing no id. Runs result/steps if any, or finalises
	 * immediately if not.
	 */
	private void startResultProcessing(MapType preview) {
		try {
			resultChildScope = scope.createChildScope(step.getId(), null, null, null, true);
			if (step.getId() != null) {
				resultChildScope.createVariable(step.getId()).setValue(preview);
			}
			Result result = step.getResult();
			scheduleResultTimeout(result);
			if (result.getSteps() != null && result.getSteps().getSteps() != null && !result.getSteps().getSteps().isEmpty()) {
				TestCaseUtils.applyStopOnErrorSemantics(step, result.getSteps());
				TestCaseUtils.initialiseStepStatusMaps(getStepSuccessMap(), getStepStatusMap(), getStepReportMap(), result.getSteps(), resultChildScope);
				/*
				 * Add a "[R]" to the current step's ID to give a distinct identifier to the result step, allowing it to
				 * record a different status report than the receive step itself. If we don't do this the receive step's
				 * (different) report will not be recorded as a step report with the same ID would be already found.
				 *
				 * Note that this does not apply to call step reports (which similarly trigger a child sequence) because
				 * the call step itself does not produce its own report.
				 */
				ActorRef child = SequenceProcessorActor.create(getContext(), result.getSteps(), resultChildScope, stepId+"[R]", stepContext);
				child.tell(new StartCommand(scope.getContext().getSessionId()), self());
			} else {
				finalizeResult(false);
			}
		} catch (Exception e) {
			failResult(e);
		}
	}

	@SuppressWarnings("resource")
	private void scheduleResultTimeout(Result result) {
		if (!StringUtils.isBlank(result.getTimeout())) {
			VariableResolver resolver = new VariableResolver(scope);
			long timeout;
			if (VariableResolver.isVariableReference(result.getTimeout())) {
				timeout = resolver.resolveVariableAsNumber(result.getTimeout()).longValue();
			} else {
				timeout = Double.valueOf(result.getTimeout()).longValue();
			}
			final ActorContext context = getContext();
			context.system().scheduler().scheduleOnce(
					scala.concurrent.duration.Duration.apply(timeout, TimeUnit.MILLISECONDS), () -> {
						if (!self().isTerminated()) {
							self().tell(new ResultTimeoutExpired(), self());
						}
					},
					context.dispatcher()
			);
		}
	}

	@Override
	protected void handleStatusEvent(StatusEvent event) throws Exception {
		if (isResultRunning()) {
			// This is the terminal status of the child sequence spawned for this step's 'result/steps'. It is
			// never bubbled up any further - equivalent to how 'hidden' steps are treated in the report/sequence
			// diagram - only the resolved 'result/output' (applied below) is observable to the caller and report.
			if (event instanceof ExitEvent exitEvent && exitEvent.getExitScope() != ExitScopeType.SEQUENCE) {
				setExitScopeToReport(exitEvent.getExitScope());
			}
			StepStatus status = event.getStatus();
			if (status == StepStatus.COMPLETED) {
				finalizeResult(false);
			} else if (status == StepStatus.WARNING) {
				finalizeResult(true);
			} else if (status == StepStatus.ERROR) {
				failResult(new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "An error occured while calculating the step's result.")));
			}
		} else {
			super.handleStatusEvent(event);
		}
	}

	private boolean isResultRunning() {
		return resultMode != null && resolvedResultOutputs == null;
	}

	/**
	 * Resolves 'result/output' in the isolated child scope and then finalises according to the mode in effect:
	 * either handing the resolved response to the waiting messaging server (whose own report will arrive
	 * afterwards, as a notification, and be patched then - see {@link #handleMessagingResult}), or - when no
	 * server is waiting - patching the handler's own report directly and resolving the step.
	 */
	private void finalizeResult(boolean warning) {
		if (resolvedResultOutputs != null) {
			return;
		}
		try {
			ExpressionHandler resultExpressionHandler = new ExpressionHandler(resultChildScope);
			Map<String, DataType> resolved = new HashMap<>();
			for (Binding outputBinding : step.getResult().getOutput()) {
				if (outputBinding.getName() != null) {
					resolved.put(outputBinding.getName(), resultExpressionHandler.processExpression(outputBinding));
				}
			}
			resolvedResultOutputs = resolved;
			resultHadWarning = warning;
			if (resultMode == ResultMode.RESPONSE_HANDLE) {
				// The handler builds its response from the step's own input Message, so every resolved output -
				// including those it consumes for the response itself - must be visible there.
				inputMessage.getFragments().putAll(resolved);
				CompletableFuture<Message> handle = pendingResponseHandle;
				pendingResponseHandle = null;
				if (handle != null && !handle.isDone()) {
					handle.complete(inputMessage);
				}
			} else {
				MessagingReport reportToResolve = pendingReport;
				pendingReport = null;
				promise.trySuccess(handleMessagingResult(reportToResolve));
			}
		} catch (Exception e) {
			// Never leave a waiting messaging server (or the step itself) hanging on an expression failure.
			failResult(e);
		}
	}

	private void failResult(Throwable cause) {
		if (resolvedResultOutputs == null) {
			resolvedResultOutputs = Map.of();
		}
		pendingReport = null;
		if (pendingResponseHandle != null && !pendingResponseHandle.isDone()) {
			pendingResponseHandle.completeExceptionally(cause);
		}
		pendingResponseHandle = null;
		if (promise != null && !promise.isCompleted()) {
			promise.tryFailure(cause);
		}
	}

	/**
	 * Builds the map exposed as the step's own variable from a messaging report's message, honouring the step's
	 * own 'output' bindings. Used both for the step's final variable value and, during 'result' processing when
	 * no messaging server is involved, for the preview of it made available to result/steps.
	 */
	private MapType buildStepOutputMap(Message message) {
		if (step.getOutput().isEmpty()) {
			return generateOutputWithMessageFields(message);
		} else if (BindingUtils.isNameBinding(step.getOutput())) {
			return generateOutputWithNameBinding(message, step.getOutput());
		} else {
			return generateOutputWithModuleDefinition(messagingContext, message);
		}
	}

	private TAR handleMessagingResult(MessagingReport report) {
		TAR reportToReturn;
		Optional<VariableResolver> resolver = Optional.empty();
		if (report != null && report.getMessage() != null) {
			Message message = report.getMessage();
			applyResolvedResultOutputs(report);
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
				map = buildStepOutputMap(message);
				addResolvedResultOutputs(map);
				scope
					.createVariable(step.getId())
					.setValue(map);
			}
			reportToReturn = report.getReport();
		} else {
			reportToReturn = Objects.requireNonNullElseGet(report, () -> generateSuccessReport(null)).getReport();
		}
		ErrorLevel errorLevel = TestCaseUtils.resolveReportErrorLevel(step.getLevel(), scope.getContext().getSessionId(), resolver.orElse(new VariableResolver(scope)));
		TestCaseUtils.postProcessReport(step.isInvert(), errorLevel, reportToReturn);
		if (resultHadWarning && reportToReturn != null && reportToReturn.getResult() == TestResultType.SUCCESS) {
			// The step's own report was otherwise successful but its 'result/steps' completed with a warning.
			reportToReturn.setResult(TestResultType.WARNING);
		}
		return reportToReturn;
	}

	/**
	 * Adds every resolved 'result/output' value not already accounted for by the handler itself (see
	 * {@link IMessagingHandler#getResultOutputNamesHandledInternally()}) to the report, as additional top-level
	 * items alongside whatever the handler itself produced - both to the report's message (which feeds the step's
	 * own variable, via {@link #buildStepOutputMap}/{@link #addResolvedResultOutputs}) and directly to the
	 * report's already-built context (since the context is a one-time snapshot of the message taken when the
	 * report was generated - mutating the message alone would not be reflected in the presented report).
	 */
	private void applyResolvedResultOutputs(MessagingReport report) {
		if (resolvedResultOutputs == null || resolvedResultOutputs.isEmpty()) {
			return;
		}
		Set<String> handledInternally = messagingContext.getHandler().getResultOutputNamesHandledInternally();
		resolvedResultOutputs.forEach((name, value) -> {
			if (!handledInternally.contains(name)) {
				report.getMessage().getFragments().put(name, value);
				patchReportContext(report.getReport(), name, value);
			}
		});
	}

	/** Force-adds the resolved 'result/output' values onto the step's own built output map, even when the step
	 * declares its own 'output' bindings (which would otherwise select/rename fragments and silently drop these). */
	private void addResolvedResultOutputs(MapType map) {
		if (resolvedResultOutputs == null || resolvedResultOutputs.isEmpty()) {
			return;
		}
		Set<String> handledInternally = messagingContext.getHandler().getResultOutputNamesHandledInternally();
		resolvedResultOutputs.forEach((name, value) -> {
			if (!handledInternally.contains(name)) {
				map.addItem(name, value);
			}
		});
	}

	private void patchReportContext(TAR tar, String name, DataType value) {
		if (tar == null) {
			return;
		}
		AnyContent context = tar.getContext();
		if (context == null) {
			context = new AnyContent();
			context.setType(DataType.MAP_DATA_TYPE);
			tar.setContext(context);
		}
		context.getItem().removeIf(existing -> name.equals(existing.getName()));
		context.getItem().add(DataTypeUtils.convertDataTypeToAnyContent(name, value));
	}

	@Override
	protected void stop() {
		super.stop();
		if (pendingResponseHandle != null && !pendingResponseHandle.isDone()) {
			pendingResponseHandle.completeExceptionally(new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.CANCELLATION, "Test step ["+stepId+"] is cancelled.")));
		}
		if (promise != null && !promise.isCompleted()) {
			promise.tryFailure(new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.CANCELLATION, "Test step ["+stepId+"] is cancelled.")));
		}
	}

    @Override
    protected MessagingContext getMessagingContext() {
        return messagingContext;
    }

	public static ActorRef create(ActorContext context, com.gitb.tdl.Receive step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
		return create(ReceiveStepProcessorActor.class, context, step, scope, stepId, stepContext);
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
