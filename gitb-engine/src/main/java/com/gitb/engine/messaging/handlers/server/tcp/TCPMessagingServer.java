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

package com.gitb.engine.messaging.handlers.server.tcp;

import com.gitb.core.ErrorCode;
import com.gitb.engine.messaging.handlers.server.Configuration;
import com.gitb.engine.messaging.handlers.server.IMessagingServer;
import com.gitb.engine.messaging.handlers.server.IMessagingServerWorker;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.utils.ErrorUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gitb.engine.TestEngineConfiguration.DEFAULT_MESSAGING_CONFIGURATION;

/**
 * Created by serbay on 9/22/14.
 */
public class TCPMessagingServer implements IMessagingServer {

    private static TCPMessagingServer instance;

    // initial configuration for the messaging server
    private final Configuration configuration;
    // port -> listener thread map
    private Map<Integer, IMessagingServerWorker> workers;

    public TCPMessagingServer(Configuration configuration) throws IOException {
        this.configuration = configuration;
        this.workers = new ConcurrentHashMap<>();
    }

    /**
     * Starts to listen the next available port
     * @return port number
     * @throws IOException
     */
    @Override
    public synchronized IMessagingServerWorker listenNextAvailablePort() {
        for (int port = configuration.getStart(); port <= configuration.getEnd(); port++) {
            if(!workers.containsKey(port)) {
	            TCPMessagingServerWorker worker = null;
	            try {
		            worker = listen(port);
	            } catch (IOException e) {
		            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Could not open server at the port [" + port + "]"), e);
	            }
	            return worker;
            }
        }

        return null;
    }

    public IMessagingServerWorker getWorker(int port) {
        return workers.get(port);
    }

    public Collection<IMessagingServerWorker> getActiveWorkers() {
        return workers.values();
    }

    private TCPMessagingServerWorker listen(int port) throws IOException {
        TCPMessagingServerWorker worker = new TCPMessagingServerWorker(port);

        workers.put(port, worker);

        worker.start();

        return worker;
    }

    public synchronized void close(int port) {
        IMessagingServerWorker worker = workers.remove(port);
        if(worker != null && worker.isActive()) {
            worker.stop();
        }
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    private static TCPMessagingServer getInstance(Configuration configuration) throws IOException {
        if(instance == null) {
            instance = new TCPMessagingServer(configuration);
        }

        return instance;
    }

    public synchronized static TCPMessagingServer getInstance() throws IOException {
        Configuration defaultConfiguration = DEFAULT_MESSAGING_CONFIGURATION;

        return getInstance(defaultConfiguration);
    }
}
