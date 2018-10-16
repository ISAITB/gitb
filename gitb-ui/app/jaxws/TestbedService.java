package jaxws;

import actors.WebSocketActor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tbs.*;
import com.gitb.tbs.Void;
import managers.ReportManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JacksonUtil;
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
                // Send the end session message with a slight delay to avoid race conditions with other ending messages.
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                //send status updates
                                logger.info("Notifying client for end of session ["+session+"]");
                                WebSocketActor.broadcast(session, status);
                            }
                        },
                        1000
                );
            } else {
                ReportManager.createTestStepReport(session, testStepStatus);
                //send status updates
                WebSocketActor.broadcast(session, status);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new Void();
    }

    @Override
    public Void interactWithUsers(@WebParam(name = "InteractWithUsersRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") InteractWithUsersRequest interactWithUsersRequest) {
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
