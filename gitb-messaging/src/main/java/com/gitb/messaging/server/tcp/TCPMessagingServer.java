package com.gitb.messaging.server.tcp;

import com.gitb.core.ErrorCode;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.server.Configuration;
import com.gitb.messaging.server.IMessagingServer;
import com.gitb.messaging.server.IMessagingServerWorker;
import com.gitb.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by serbay on 9/22/14.
 */
public class TCPMessagingServer implements IMessagingServer {
    private static Logger logger = LoggerFactory.getLogger(TCPMessagingServer.class);

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
        Configuration defaultConfiguration = Configuration.defaultConfiguration();

        return getInstance(defaultConfiguration);
    }
}
