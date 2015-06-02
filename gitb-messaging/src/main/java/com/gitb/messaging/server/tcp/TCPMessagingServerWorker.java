package com.gitb.messaging.server.tcp;

import com.gitb.messaging.SessionManager;
import com.gitb.messaging.model.tcp.ITransactionReceiver;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.server.AbstractMessagingServerWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by serbay on 9/24/14.
 *
 * TCP messaging server worker used by TCP messaging server.
 *
 */
public class TCPMessagingServerWorker extends AbstractMessagingServerWorker {
	private static Logger logger = LoggerFactory.getLogger(TCPMessagingServerWorker.class);

    private final List<Socket> waitingTransactions;

	private TCPListenerThread listenerThread;
	private AtomicBoolean active;

    public TCPMessagingServerWorker(int port) {
	    super(port);
        waitingTransactions = new CopyOnWriteArrayList<>();
	    active = new AtomicBoolean(false);
    }

    public synchronized void start() throws IOException {
        if(listenerThread == null) {
            listenerThread = new TCPListenerThread(port);
            listenerThread.start();
        } else if(!isActive()) {
	        listenerThread.start();
        } else {
	        logger.debug("Already started to listen");
        }
    }

    public synchronized void stop() {
        if(listenerThread != null) {
            listenerThread.close();
            listenerThread = null;
        } else {
	        logger.debug("Already stopped listening");
        }
    }

	@Override
	public boolean isActive() {
		return active.get();
	}

    private void tryWaitingTransactions() {
        List<Socket> completedTransactions = new ArrayList<>();

        for(Socket socket : waitingTransactions) {
            InetAddress address = socket.getInetAddress();

            String sessionId = networkingSessionManager.getSessionId(address);
            if(sessionId != null) {

                SessionContext sessionContext = SessionManager.getInstance().getSession(sessionId);

                TransactionContext transactionContext = sessionContext.getTransaction(address, socket.getLocalPort());

                if(transactionContext != null) {
                    ITransactionReceiver receiver = transactionContext.getParameter(ITransactionReceiver.class);

                    receiver.onReceive(socket);

                    completedTransactions.add(socket);
                }
            }
        }

        waitingTransactions.removeAll(completedTransactions);
    }

	private class TCPReceiverThread extends Thread {
        private final Socket socket;

        private TCPReceiverThread(Socket socket) {
            super(TCPReceiverThread.class.getSimpleName() + ":" + socket.getInetAddress() + ":" + socket.getPort());
            this.socket = socket;
        }

        @Override
        public void run() {
            waitingTransactions.add(socket);

            tryWaitingTransactions();
        }
    }

    private class TCPListenerThread extends Thread {

        private int port;
        private ServerSocket serverSocket;

        private TCPListenerThread(int port) throws IOException {
            super(TCPListenerThread.class.getSimpleName() + ":" + port);
            this.port = port;
	        serverSocket = new ServerSocket(port);

	        logger.debug("New server created on: " + serverSocket);
        }

	    public void close() {
		    interrupt();
		    try {
			    serverSocket.close();
		    } catch (IOException e) {
			    logger.error("An error occurred while closing the socket", e);
		    }
	    }

        @Override
        public void run() {
            try {
	            active.set(true);

                while (!Thread.interrupted() && !serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();

                    logger.debug("New socket created: " + socket);

                    TCPReceiverThread receiverThread = new TCPReceiverThread(socket);
                    receiverThread.start();

                }
            } catch (Exception e) {
	            if(!serverSocket.isClosed()) {
		            logger.error("An error occurred", e);
	            }
            } finally {
	            logger.debug("Closing server created on: " + serverSocket);
	            if (!serverSocket.isClosed()) {
		            try {
			            serverSocket.close();
		            } catch (IOException e1) {
			            logger.error("An error occurred while closing the socket");
		            }
	            }
	            active.set(false);
            }
        }
    }
}
