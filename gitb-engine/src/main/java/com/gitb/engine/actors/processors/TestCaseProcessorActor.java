package com.gitb.engine.actors.processors;

import com.gitb.core.AnyContent;
import com.gitb.core.StepStatus;
import com.gitb.engine.SessionManager;
import com.gitb.engine.commands.interaction.*;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.*;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;
import com.gitb.utils.XMLDateTimeUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.apache.pekko.japi.Creator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Actor processing the TestCase definition
 */
public class TestCaseProcessorActor extends com.gitb.engine.actors.Actor {
    public static final String PRELIMINARY_STEP_ID = "0";
	public static final String TEST_SESSION_END_STEP_ID = "-1";
    public static final String TEST_SESSION_END_EXTERNAL_STEP_ID = "-2";
    Logger logger = LoggerFactory.getLogger(TestCaseProcessorActor.class);
    public static final String NAME = "tc-p";
    //Test Case Execution identifier
    private final String sessionId;
    //Test Case Definition
    private TestCase testCase;
    //Interaction Processor Actor to process preliminary phase command
    private ActorRef preliminaryProcessorActor;
    //Sequence Processor Actor to process the test steps
    private ActorRef sequenceProcessorActor;

    public TestCaseProcessorActor(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Initializing the children actors and the TestCaseContext
     */
    private void init() throws Exception {
        TestCaseContext context = SessionManager
                .getInstance()
                .getContext(sessionId);
        if (context != null) {
            testCase = context.getTestCase();
            TestCaseUtils.applyStopOnErrorSemantics(testCase.getSteps());
            if (testCase.getPreliminary() != null) {
                preliminaryProcessorActor = InteractionStepProcessorActor.create(InteractionStepProcessorActor.class, getContext(), testCase.getPreliminary(), context.getScope(), PRELIMINARY_STEP_ID, null);
            }
        }
    }

    @Override
    public void preStart() throws Exception {
        init();
    }

    @Override
    public void onReceive(Object message)  {
        try {
            super.onReceive(message);
            TestCaseContext context = SessionManager
                    .getInstance()
                    .getContext(sessionId);

            //Start command for test case processing
            if (message instanceof StartCommand) {
                sequenceProcessorActor = RootSequenceProcessorActor.create(getContext(), testCase.getSteps(), context.getScope(), "");
	            sequenceProcessorActor.tell(message, self());
            }
            // Prepare for stop command
            else if (message instanceof PrepareForStopCommand) {
                logger.debug(MarkerFactory.getDetachedMarker(((PrepareForStopCommand) message).getSessionId()), "Preparing to stop");
                if (sequenceProcessorActor != null) {
                    sequenceProcessorActor.tell(message, getSelf());
                }
                if (preliminaryProcessorActor != null) {
                    //Stop child preliminary processor
                    preliminaryProcessorActor.tell(message, getSelf());
                }
            }
            //Stop command for test case processing
            else if (message instanceof RestartCommand) {
                logger.debug("Restarting session [{}].", sessionId);
	            //Stop child sequence processor
	            if (sequenceProcessorActor != null) {
		            sequenceProcessorActor.tell(message, self());
		            sequenceProcessorActor = null;
	            }

	            if (preliminaryProcessorActor != null) {
		            //Stop child preliminary processor
		            preliminaryProcessorActor.tell(message, self());
		            preliminaryProcessorActor = null;
	            }
            }
            //Initiate the preliminary phase execution
            else if (message instanceof InitiatePreliminaryCommand) {
	            if (preliminaryProcessorActor != null) {
		            logger.debug(MarkerFactory.getDetachedMarker(sessionId), "Initiating preliminary phase");
		            //Start the processing of preliminary phase
		            preliminaryProcessorActor.tell(new StartCommand(sessionId), self());
	            }
            }
            //Handle the Status events
            else if (message instanceof StatusEvent) {
	            if(getSender().equals(sequenceProcessorActor)) {
		            StepStatus status = ((StatusEvent) message).getStatus();
		            if (status == StepStatus.COMPLETED || status == StepStatus.ERROR || status == StepStatus.WARNING || status == StepStatus.SKIPPED) {
		                // Construct the final report.
                        TestStepReportType resultReport = constructResultReport(status, context);
                        // Notify for the completion of the test session after a grace period.
                        getContext().system().scheduler().scheduleOnce(
                                scala.concurrent.duration.Duration.apply(500L, TimeUnit.MILLISECONDS), () -> getContext().getParent().tell(new TestSessionFinishedCommand(sessionId, status, resultReport), self()),
                                getContext().dispatcher()
                        );
		            }
	            }
            } else {
                throw new GITBEngineInternalError("Invalid command [" + message.getClass().getName() + "]");
            }
        } catch(Exception e) {
            try {
                logger.error(MarkerFactory.getDetachedMarker(sessionId), "InternalServerError",e);
                // Signal error via normal session channel.
                getContext().getParent().tell(new UnexpectedErrorCommand(sessionId), self());
            } catch (Exception e2) {
                // Last attempt to signal error.
                logger.error("Error while signalling unexpected failure", e2);
            }
        }
    }

	private TestStepReportType constructResultReport(StepStatus status, TestCaseContext context) {
		TAR report = new TAR();
        // We treat a final result of "WARNING" as a "SUCCESS". This is the overall test session result.
        if (status == StepStatus.COMPLETED || status == StepStatus.WARNING) {
            report.setResult(TestResultType.SUCCESS);
        } else if (status == StepStatus.SKIPPED) {
            report.setResult(TestResultType.UNDEFINED);
        } else {
            report.setResult(TestResultType.FAILURE);
        }
        // Set output message (if defined).
        if (testCase.getOutput() != null) {
            var previousState = context.getCurrentState();
            context.setCurrentState(TestCaseContext.TestCaseStateEnum.OUTPUT);
            try {
                List<String> outputMessages = null;
                if (report.getResult() == TestResultType.SUCCESS) {
                    outputMessages = calculateOutputMessage(testCase.getOutput().getSuccess(), context);
                } else if (report.getResult() == TestResultType.FAILURE) {
                    outputMessages = calculateOutputMessage(testCase.getOutput().getFailure(), context);
                } else if (report.getResult() == TestResultType.UNDEFINED) {
                    outputMessages = calculateOutputMessage(testCase.getOutput().getUndefined(), context);
                }
                if (outputMessages != null && !outputMessages.isEmpty()) {
                    report.setContext(new AnyContent());
                    report.getContext().setType("list[string]");
                    for (var outputMessage: outputMessages) {
                        var messageItem = new AnyContent();
                        messageItem.setValue(outputMessage);
                        report.getContext().getItem().add(messageItem);
                    }
                }
            } catch (Exception e) {
                // Error while calculating output message - report log and fail the test session.
                logger.error(MarkerFactory.getDetachedMarker(sessionId), "Error calculating output message", e);
                report.setResult(TestResultType.FAILURE);
            } finally {
                context.setCurrentState(previousState);
            }
        }
		try {
			report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
		} catch (DatatypeConfigurationException e) {
			logger.error(MarkerFactory.getDetachedMarker(sessionId), "An error occurred.", e);
		}

		return report;
	}

    private boolean caseMatches(OutputCase outputCase, ExpressionHandler expressionHandler) {
        boolean condition = false;
        try {
            condition = (boolean) expressionHandler.processExpression(outputCase.getCond(), DataType.BOOLEAN_DATA_TYPE).getValue();
        } catch (Exception e) {
            logger.warn(MarkerFactory.getDetachedMarker(sessionId), "Skipping output message calculation due to expression error.", e);
        }
        return condition;
    }

    private List<String> calculateOutputMessage(OutputCaseSet outputSet, TestCaseContext context) {
        List<String> messages = new ArrayList<>();
        if (outputSet != null) {
            ExpressionHandler expressionHandler = new ExpressionHandler(context.getScope());
            if (outputSet.getCase() != null) {
                boolean keepProcessing = true;
                boolean stopOnUnmatchedCondition = false;
                Iterator<OutputCase> caseIterator = outputSet.getCase().iterator();
                while (caseIterator.hasNext() && keepProcessing) {
                    OutputCase outputCase = caseIterator.next();
                    if (caseMatches(outputCase, expressionHandler)) {
                        String message = applyCaseExpression(outputCase.getMessage(), expressionHandler);
                        if (message != null) {
                            messages.add(message);
                            if (outputSet.getMatch() == null || outputSet.getMatch() == OutputMessageMatchApproach.FIRST) {
                                keepProcessing = false;
                            } else if (outputSet.getMatch() == OutputMessageMatchApproach.CASCADE) {
                                stopOnUnmatchedCondition = true;
                            }
                        }
                    } else if (stopOnUnmatchedCondition) {
                        keepProcessing = false;
                    }
                }
                if (messages.isEmpty() && outputSet.getDefault() != null) {
                    String message = applyCaseExpression(outputSet.getDefault(), expressionHandler);
                    if (message != null) {
                        messages.add(message);
                    }
                }
            }
        }
        return messages;
    }

    private String applyCaseExpression(Expression expression, ExpressionHandler expressionHandler) {
        String caseMessage = (String) expressionHandler.processExpression(expression, DataType.STRING_DATA_TYPE).getValue();
        if (caseMessage != null && !caseMessage.isBlank()) {
            return caseMessage.trim();
        }
        return null;
    }

    public static Props props(final String sessionId) {
        return Props.create(TestCaseProcessorActor.class, (Creator<TestCaseProcessorActor>) () -> new TestCaseProcessorActor(sessionId));
    }

    /**
     * Create a TestCaseProcessor actor reference from the given session id
     *
     * @param context context
     * @param sessionId session id
     * @return test case processor actor reference
     */
    public static ActorRef create(ActorContext context, String sessionId) {
        return context.actorOf(TestCaseProcessorActor.props(sessionId), NAME);
    }
}
