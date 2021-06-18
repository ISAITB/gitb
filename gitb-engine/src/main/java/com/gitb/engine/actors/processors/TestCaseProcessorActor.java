package com.gitb.engine.actors.processors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Creator;
import com.gitb.core.AnyContent;
import com.gitb.core.StepStatus;
import com.gitb.engine.SessionManager;
import com.gitb.engine.commands.interaction.*;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.*;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;
import com.gitb.utils.XMLDateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.concurrent.TimeUnit;

/**
 * Created by serbay on 9/5/14.
 * Actor processing the TestCase definition
 */
public class TestCaseProcessorActor extends com.gitb.engine.actors.Actor {
    public static final String PRELIMINARY_STEP_ID = "0";
	public static final String TEST_CASE_STEP_ID = "-1";
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
     *
     * @throws IllegalAccessException
     */
    private void init() throws Exception {
        TestCaseContext context = SessionManager
                .getInstance()
                .getContext(sessionId);
        if (context != null) {
            testCase = context.getTestCase();
            applyStopOnErrorSemantics(testCase.getSteps(), testCase.getSteps().isStopOnError());
            if (testCase.getPreliminary() != null) {
                preliminaryProcessorActor = InteractionStepProcessorActor.create(InteractionStepProcessorActor.class, getContext(), testCase.getPreliminary(), context.getScope(), PRELIMINARY_STEP_ID);
            }
        }
    }

    private void applyStopOnErrorSemantics(Sequence step, boolean stopOnError) {
        if (step != null) {
            if (stopOnError) {
                step.setStopOnError(true);
            }
            for (Object childStep: step.getSteps()) {
                if (childStep instanceof Sequence) {
                    applyStopOnErrorSemantics((Sequence)childStep, step.isStopOnError());
                }
                if (childStep instanceof TestConstruct) {
                    applyStopOnErrorSemantics((TestConstruct)childStep, step.isStopOnError());
                }
            }
        }
    }

    private void applyStopOnErrorSemantics(TestConstruct step, boolean stopOnError) {
        if (stopOnError) {
            step.setStopOnError(true);
        }
        // Cover also the steps that have internal sequences.
        if (step instanceof IfStep) {
            applyStopOnErrorSemantics(((IfStep) step).getThen(), step.isStopOnError());
            applyStopOnErrorSemantics(((IfStep) step).getElse(), step.isStopOnError());
        } else if (step instanceof WhileStep) {
            applyStopOnErrorSemantics(((WhileStep) step).getDo(), step.isStopOnError());
        } else if (step instanceof ForEachStep) {
            applyStopOnErrorSemantics(((ForEachStep) step).getDo(), step.isStopOnError());
        } else if (step instanceof RepeatUntilStep) {
            applyStopOnErrorSemantics(((RepeatUntilStep) step).getDo(), step.isStopOnError());
        } else if (step instanceof FlowStep) {
            if (((FlowStep) step).getThread() != null) {
                for (Sequence thread: ((FlowStep) step).getThread()) {
                    applyStopOnErrorSemantics(thread, step.isStopOnError());
                }
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
	            logger.debug(MarkerFactory.getDetachedMarker(((StartCommand) message).getSessionId()), "Received start command, starting test case sequence.");
                sequenceProcessorActor = RootSequenceProcessorActor.create(getContext(), testCase.getSteps(), context.getScope(), "");
	            sequenceProcessorActor.tell(message, self());
            }
            // Prepare for stop command
            else if (message instanceof PrepareForStopCommand) {
                logger.debug(MarkerFactory.getDetachedMarker(((PrepareForStopCommand) message).getSessionId()), "Received prepare for stop command.");
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
	            logger.debug("Restarting session ["+sessionId+"].");
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
		            logger.debug(MarkerFactory.getDetachedMarker(sessionId), "Initiating preliminary phase.");
		            //Start the processing of preliminary phase
		            preliminaryProcessorActor.tell(new StartCommand(sessionId), self());
	            }
            }
            //Handle the Status events
            else if (message instanceof StatusEvent) {
	            if(getSender().equals(sequenceProcessorActor)) {
		            StepStatus status = ((StatusEvent) message).getStatus();
		            if (status == StepStatus.COMPLETED || status == StepStatus.ERROR || status == StepStatus.WARNING) {
		                // Construct the final report.
                        TestStepReportType resultReport = constructResultReport(status, context);
                        // Notify for the completion of the test session after a grace period.
                        getContext().system().scheduler().scheduleOnce(
                                scala.concurrent.duration.Duration.apply(500L, TimeUnit.MILLISECONDS), () -> {
                                    getContext().getParent().tell(new TestSessionFinishedCommand(sessionId, status, resultReport), self());
                                },
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
        } else {
            report.setResult(TestResultType.FAILURE);
        }
        // Set output message (if defined).
        if (testCase.getOutput() != null) {
            try {
                String outputMessage = null;
                if (report.getResult() == TestResultType.SUCCESS) {
                    outputMessage = calculateOutputMessage(testCase.getOutput().getSuccess(), context);
                } else if (report.getResult() == TestResultType.FAILURE) {
                    outputMessage = calculateOutputMessage(testCase.getOutput().getFailure(), context);
                }
                if (outputMessage != null) {
                    report.setContext(new AnyContent());
                    report.getContext().setValue(outputMessage);
                }
            } catch (Exception e) {
                // Error while calculating output message - report log and fail the test session.
                logger.error(MarkerFactory.getDetachedMarker(sessionId), "Error calculating output message", e);
                report.setResult(TestResultType.FAILURE);
            }
        }
		try {
			report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
		} catch (DatatypeConfigurationException e) {
			logger.error(MarkerFactory.getDetachedMarker(sessionId), "An error occurred.", e);
		}

		return report;
	}

    private String calculateOutputMessage(OutputCaseSet outputSet, TestCaseContext context) {
        String message = null;
        if (outputSet != null) {
            ExpressionHandler expressionHandler = new ExpressionHandler(context.getScope());
            if (outputSet.getCase() != null) {
                 for (OutputCase outputCase: outputSet.getCase()) {
                     boolean condition = false;
                     try {
                         condition = (boolean) expressionHandler.processExpression(outputCase.getCond(), DataType.BOOLEAN_DATA_TYPE).getValue();
                     } catch (Exception e) {
                         logger.warn(MarkerFactory.getDetachedMarker(sessionId), "Skipping output message calculation due to expression error.", e);
                     }
                     if (condition) {
                         message = applyCaseExpression(outputCase.getMessage(), expressionHandler);
                         if (message != null) {
                             break;
                         }
                     }
                 }
                 if (message == null && outputSet.getDefault() != null) {
                     message = applyCaseExpression(outputSet.getDefault(), expressionHandler);
                 }
            }
        }
        return message;
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
