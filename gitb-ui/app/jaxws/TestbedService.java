package jaxws;

import actors.WebSocketActor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tbs.Void;
import com.gitb.tbs.*;
import com.gitb.tr.TAR;
import managers.ReportManager;
import managers.TestResultManager;
import managers.TestbedBackendClient;
import models.TestStepResultInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import utils.JacksonUtil;
import utils.JsonUtil;
import utils.MimeUtil;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.Endpoint;
import javax.xml.ws.soap.Addressing;

@Addressing(enabled = true, required = true)
@SOAPBinding(parameterStyle= SOAPBinding.ParameterStyle.BARE)
@WebService(name = "TestbedClient", serviceName = "TestbedClient", targetNamespace = "http://www.gitb.com/tbs/v1/")
public class TestbedService implements TestbedClient {

    private final static String END_STEP_ID = "-1";
    private final static String LOG_EVENT_STEP_ID = "-999";

    private final Logger logger = LoggerFactory.getLogger(TestbedService.class);

    private final TestResultManager testResultManager;
    private final ReportManager reportManager;
    private final WebSocketActor webSocketActor;
    private final TestbedBackendClient testbedBackendClient;

    public TestbedService(TestResultManager testResultManager, ReportManager reportManager, WebSocketActor webSocketActor, TestbedBackendClient testbedBackendClient) {
        this.testResultManager = testResultManager;
        this.reportManager = reportManager;
        this.webSocketActor = webSocketActor;
        this.testbedBackendClient = testbedBackendClient;
    }

    /**
     * Endpoint of the TestbedClient
     */
    public static Endpoint endpoint;

    @Override
    public com.gitb.tbs.Void updateStatus(@WebParam(name = "UpdateStatusRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") TestStepStatus testStepStatus) {
        try {
            String status  = JacksonUtil.serializeTestStepStatus(testStepStatus);
            String session = testStepStatus.getTcInstanceId();
            String step    = testStepStatus.getStepId();

            //save report
            if(step.equals(END_STEP_ID)){
                String outputMessage = null;
                if (testStepStatus.getReport() instanceof TAR
                        && ((TAR)testStepStatus.getReport()).getContext() != null
                        && ((TAR)testStepStatus.getReport()).getContext().getValue() != null
                        && !((TAR)testStepStatus.getReport()).getContext().getValue().isBlank()
                ) {
                    outputMessage = ((TAR)testStepStatus.getReport()).getContext().getValue().trim();
                }
                var statusUpdates = testResultManager.sessionRemove(session);
                reportManager.finishTestReport(session, testStepStatus.getReport().getResult(), Option.apply(outputMessage));
                var resultInfo = new TestStepResultInfo((short)testStepStatus.getStatus().ordinal(), Option.empty());
                String message = JsonUtil.jsTestStepResultInfo(session, step, resultInfo, Option.apply(outputMessage), statusUpdates).toString();
                webSocketActor.testSessionEnded(session, message);
            } else if (step.equals(LOG_EVENT_STEP_ID)) {
                //send log event
                webSocketActor.broadcast(session, status, false);
            } else {
                Option<String> reportPath = reportManager.createTestStepReport(session, testStepStatus);
                var resultInfo = new TestStepResultInfo((short)testStepStatus.getStatus().ordinal(), reportPath);
                var statusUpdates = testResultManager.sessionUpdate(session, step, resultInfo);
                String message = JsonUtil.jsTestStepResultInfo(session, step, resultInfo, Option.empty(), statusUpdates).toString();
                webSocketActor.broadcast(session, message);
            }
        } catch (JsonProcessingException e) {
            logger.error("Error during test session update for session ["+testStepStatus.getTcInstanceId()+"]", e);
        }
        return new Void();
    }

    @Override
    public Void interactWithUsers(@WebParam(name = "InteractWithUsersRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") InteractWithUsersRequest interactWithUsersRequest) {
        String session = null;
        try {
            if (interactWithUsersRequest.getInteraction() != null) {
                for (Object obj: interactWithUsersRequest.getInteraction().getInstructionOrRequest()) {
                    if (obj instanceof Instruction
                            && StringUtils.isBlank(((Instruction)obj).getName())
                            && ((Instruction)obj).getEmbeddingMethod() == ValueEmbeddingEnumeration.BASE_64
                            && !StringUtils.isBlank(((Instruction)obj).getValue())) {
                        // Determine the file name from the BASE64 content.
                        String mimeType = MimeUtil.getMimeType(((Instruction)obj).getValue(), false);
                        String extension = MimeUtil.getExtensionFromMimeType(mimeType);
                        if (extension != null) {
                            ((Instruction)obj).setName("file"+extension);
                        }
                    }
                }
            }
            session = interactWithUsersRequest.getTcInstanceid();
            if (WebSocketActor.webSockets().contains(session)) {
                String actor   = interactWithUsersRequest.getInteraction().getWith();
                String request = JacksonUtil.serializeInteractionRequest(interactWithUsersRequest);
                if (actor == null) {
                    // if actor not specified, send the request to all actors. Let client side handle this.
                    webSocketActor.broadcast(session, request);
                } else {
                    //send the request only to the given actor
                    webSocketActor.push(session, actor, request);
                }
            } else {
                // This is a headless session - automatically dismiss or complete with an empty request.
                logger.warn("Headless session ["+session+"] expected interaction for step ["+interactWithUsersRequest.getStepId()+"]. Completed automatically with empty result.");
                ProvideInputRequest interactionResult = new ProvideInputRequest();
                interactionResult.setTcInstanceId(session);
                interactionResult.setStepId(interactWithUsersRequest.getStepId());
                testbedBackendClient.service().provideInput(interactionResult);
            }
        } catch (Exception e) {
            logger.error("Error during user interaction for session ["+session+"]", e);
        }
        return new Void();
    }
}
