package jaxws;

import actors.WebSocketActor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.gitb.tbs.*;
import com.gitb.tbs.Void;
import javax.xml.ws.Endpoint;

import managers.ReportManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JacksonUtil;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.soap.Addressing;

@Addressing(enabled = true, required = true)
@SOAPBinding(parameterStyle= SOAPBinding.ParameterStyle.BARE)
@WebService(name = "TestbedClient", serviceName = "TestbedClient", targetNamespace = "http://www.gitb.com/tbs/v1/")
public class TestbedService implements TestbedClient {

    private final static String END_STEP_ID = "-1";

    private final Logger logger = LoggerFactory.getLogger(TestbedService.class);

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
                ReportManager.finishTestReport(session, testStepStatus.getReport().getResult());
            } else {
                ReportManager.createTestStepReport(session, testStepStatus);
            }
            //send status updates
            WebSocketActor.broadcast(session, status);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new Void();
    }

    @Override
    public Void interactWithUsers(@WebParam(name = "InteractWithUsersRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") InteractWithUsersRequest interactWithUsersRequest) {
        try {
            String request = JacksonUtil.serializeInteractionRequest(interactWithUsersRequest);
            String session = interactWithUsersRequest.getTcInstanceid();
            String actor   = interactWithUsersRequest.getInteraction().getWith();
            //if actor not specified, send the request to all actors. Let client side handle this.
            if(actor == null) {
                WebSocketActor.broadcast(session, request);
            }
            //send the request only to the given actor
            else {
                WebSocketActor.push(session, actor, request);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new Void();
    }
}
