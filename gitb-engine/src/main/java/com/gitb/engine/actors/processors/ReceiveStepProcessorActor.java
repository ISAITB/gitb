package com.gitb.engine.actors.processors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
import com.gitb.core.*;
import com.gitb.engine.actors.ActorSystem;
import com.gitb.engine.events.model.ErrorStatusEvent;
import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.messaging.TransactionContext;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.tdl.Receive;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.MapType;
import com.gitb.utils.BindingUtils;
import com.gitb.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by serbay on 9/30/14.
 *
 * Receive step executor actor
 */
public class ReceiveStepProcessorActor extends AbstractMessagingStepProcessorActor<Receive> {

    private static final Logger logger = LoggerFactory.getLogger(ReceiveStepProcessorActor.class);

	public static final String NAME = "receive-p";

	private MessagingContext messagingContext;
	private TransactionContext transactionContext;

	private Promise<TestStepReportType> promise;
	private Future<TestStepReportType> future;

	public ReceiveStepProcessorActor(Receive step, TestCaseScope scope, String stepId) {
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

					MessagingModule moduleDefinition = messagingHandler.getModuleDefinition();
					if (moduleDefinition.getReceiveConfigs() != null) {
						checkRequiredParametersAndSetDefaultValues(moduleDefinition.getReceiveConfigs().getParam(), step.getConfig());
					}

					MessagingReport report =
						messagingHandler
							.receiveMessage(
								messagingContext.getSessionId(),
								transactionContext.getTransactionId(),
								step.getConfig()
							);

					if(report != null && report.getMessage() != null) {
						Message message = report.getMessage();

						if(step.getId() != null) {
							MapType map;

							if(step.getOutput().size() == 0) {
								map = generateOutputWithMessageFields(message);
							} else {
                                List<TypedParameter> requiredOutputParameters = new ArrayList<TypedParameter>();
                                if(moduleDefinition.getOutputs() != null) {
                                     requiredOutputParameters.addAll(getRequiredOutputParameters(moduleDefinition.getOutputs().getParam()));
                                }

								if(step.getOutput().size() != requiredOutputParameters.size()) {
									throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Wrong number of outputs for ["+ moduleDefinition.getId()+"]"));
								}

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

	public static ActorRef create(ActorContext context, Receive step, TestCaseScope scope, String stepId) throws Exception {
		return context.actorOf(props(ReceiveStepProcessorActor.class, step, scope, stepId).withDispatcher(ActorSystem.BLOCKING_DISPATCHER), getName(NAME));
	}
}
