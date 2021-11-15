package jaxws;

import actors.TestSessionUpdateActor$;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.gitb.tbs.InteractWithUsersRequest;
import com.gitb.tbs.TestStepStatus;
import com.gitb.tbs.TestbedClient;
import com.gitb.tbs.Void;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.Endpoint;
import javax.xml.ws.soap.Addressing;

@Addressing(enabled = true, required = true)
@SOAPBinding(parameterStyle= SOAPBinding.ParameterStyle.BARE)
@WebService(name = "TestbedClient", serviceName = "TestbedClient", targetNamespace = "http://www.gitb.com/tbs/v1/")
public class TestbedService implements TestbedClient {

    private final ActorSystem actorSystem;

    public TestbedService(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

    /**
     * Endpoint of the TestbedClient
     */
    public static Endpoint endpoint;

    private ActorSelection getActor() {
        return actorSystem.actorSelection("/user/" + TestSessionUpdateActor$.MODULE$.actorName());
    }

    @Override
    public com.gitb.tbs.Void updateStatus(@WebParam(name = "UpdateStatusRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") TestStepStatus testStepStatus) {
        getActor().tell(testStepStatus, ActorRef.noSender());
        return new Void();
    }

    @Override
    public Void interactWithUsers(@WebParam(name = "InteractWithUsersRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") InteractWithUsersRequest interactWithUsersRequest) {
        getActor().tell(interactWithUsersRequest, ActorRef.noSender());
        return new Void();
    }
}
