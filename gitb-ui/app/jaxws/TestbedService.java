package jaxws;

import actors.SessionManagerActor$;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.gitb.tbs.*;
import com.gitb.tbs.Void;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.Endpoint;
import javax.xml.ws.soap.Addressing;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;

@Addressing(enabled = true, required = true)
@SOAPBinding(parameterStyle= SOAPBinding.ParameterStyle.BARE)
@WebService(name = "TestbedClient", serviceName = "TestbedClient", targetNamespace = "http://www.gitb.com/tbs/v1/")
public class TestbedService implements TestbedClient {

    private final ActorSystem actorSystem;
    private ActorRef sessionManagerRef;
    private final Object lock = new Object();

    public TestbedService(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

    /**
     * Endpoint of the TestbedClient
     */
    public static Endpoint endpoint;

    private void sendMessage(Object message) {
        if (sessionManagerRef == null) {
            synchronized (lock) {
                try {
                    sessionManagerRef = actorSystem
                            .actorSelection("/user/"+SessionManagerActor$.MODULE$.actorName())
                            .resolveOne(Duration.of(5, ChronoUnit.SECONDS))
                            .toCompletableFuture()
                            .get();
                } catch (ExecutionException | InterruptedException e) {
                    throw new IllegalStateException("Unable to lookup session manager actor", e);
                }
            }
        } else {
            sessionManagerRef.tell(message, ActorRef.noSender());
        }
    }

    @Override
    public Void configurationComplete(ConfigurationCompleteRequest configurationComplete) {
        sendMessage(configurationComplete);
        return new Void();    }

    @Override
    public com.gitb.tbs.Void updateStatus(@WebParam(name = "UpdateStatusRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") TestStepStatus testStepStatus) {
        sendMessage(testStepStatus);
        return new Void();
    }

    @Override
    public Void interactWithUsers(@WebParam(name = "InteractWithUsersRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") InteractWithUsersRequest interactWithUsersRequest) {
        sendMessage(interactWithUsersRequest);
        return new Void();
    }
}
