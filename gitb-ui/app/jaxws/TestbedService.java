/*
 * Copyright (C) 2025 European Union
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

package jaxws;

import actors.SessionManagerActor$;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import com.gitb.tbs.*;
import com.gitb.tbs.Void;

import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.soap.Addressing;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;

@Addressing(required = true)
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
                            .actorSelection("/user/" + SessionManagerActor$.MODULE$.actorName())
                            .resolveOne(Duration.of(5, ChronoUnit.SECONDS))
                            .toCompletableFuture()
                            .get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Thread interrupted", e);
                } catch (ExecutionException e) {
                    throw new IllegalStateException("Unable to lookup session manager actor", e);
                }
            }
        }
        sessionManagerRef.tell(message, ActorRef.noSender());
    }

    @Override
    public Void configurationComplete(ConfigurationCompleteRequest configurationComplete) {
        sendMessage(configurationComplete);
        return new Void();    }

    @Override
    public Void updateStatus(@WebParam(name = "UpdateStatusRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") TestStepStatus testStepStatus) {
        sendMessage(testStepStatus);
        return new Void();
    }

    @Override
    public Void interactWithUsers(@WebParam(name = "InteractWithUsersRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") InteractWithUsersRequest interactWithUsersRequest) {
        sendMessage(interactWithUsersRequest);
        return new Void();
    }
}
