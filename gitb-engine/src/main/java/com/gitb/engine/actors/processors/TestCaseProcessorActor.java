package com.gitb.engine.actors.processors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.japi.Creator;
import com.gitb.core.StepStatus;
import com.gitb.engine.SessionManager;
import com.gitb.engine.TestbedService;
import com.gitb.engine.commands.interaction.*;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.tdl.TestCase;
import com.gitb.tr.SR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.utils.XMLDateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;

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
            if (testCase.getPreliminary() != null) {
                preliminaryProcessorActor = InteractionStepProcessorActor.create(InteractionStepProcessorActor.class, getContext(), testCase.getPreliminary(), context.getScope(), PRELIMINARY_STEP_ID);
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
	            logger.debug("Received start command, starting test case sequence.");
                sequenceProcessorActor = SequenceProcessorActor.create(getContext(), testCase.getSteps(), context.getScope(), "");
	            sequenceProcessorActor.tell(message, self());
            }
            //Stop command for test case processing
            else if (message instanceof StopCommand || message instanceof RestartCommand) {
	            logger.debug("Received stop command, stopping test case sequence.");
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

	            //Stop all the execution tree
	            self().tell(PoisonPill.getInstance(), self());
            }
            //Initiate the preliminary phase execution
            else if (message instanceof InitiatePreliminaryCommand) {
	            if (preliminaryProcessorActor != null) {
		            logger.debug("Initiating preliminary phase.");
		            //Start the processing of preliminary phase
		            preliminaryProcessorActor.tell(new StartCommand(sessionId), self());
	            }
            }
            //Handle the Status events
            else if (message instanceof StatusEvent) {
                // TODO stop the sequenceProcessorActor sequence if it is completed or gets error
	            if(getSender().equals(sequenceProcessorActor)) {
		            StepStatus status = ((StatusEvent) message).getStatus();

		            if(status == StepStatus.COMPLETED || status == StepStatus.ERROR) {

			            TestbedService.updateStatus(sessionId, TEST_CASE_STEP_ID, status, constructResultReport(status));
		            }
	            }
            } else {
                throw new GITBEngineInternalError("Invalid command [" + message.getClass().getName() + "]");
            }
        }catch(Exception e){
            logger.error("InternalServerError",e);
            TestbedService.updateStatus(sessionId, null, StepStatus.ERROR, null);
        }
    }

	private TestStepReportType constructResultReport(StepStatus status) {
		SR report = new SR();
		if(status == StepStatus.COMPLETED) {
			report.setResult(TestResultType.SUCCESS);
		} else {
			report.setResult(TestResultType.FAILURE);
		}
		try {
			report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
		} catch (DatatypeConfigurationException e) {
			logger.error("An error occurred.", e);
		}

		return report;
	}

    public static Props props(final String sessionId) {
        return Props.create(new Creator<TestCaseProcessorActor>() {
            @Override
            public TestCaseProcessorActor create() throws Exception {
                return new TestCaseProcessorActor(sessionId);
            }
        });
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
